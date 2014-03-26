SharedHashMap - Writing a simple IPC system
===========================================

OpenHFT SharedHashMap is a blazing fast, persisted, off-heap Java Map which can be used not only to share data among processes but also as IPC system (on the same host), here we'll show hot to implement a simple state machine.

1. Obtain SharedHashMap via Maven
  
  Maven:
  ```xml
  <dependency>
    <groupId>net.openhft</groupId>
    <artifactId>HugeCollections</artifactId>
    <version>3.0d</version>
  </dependency>
  ```

  Gradle:
  ```groovy
  compile 'net.openhft:HugeCollections:3.0d'
  ```

2. Access to SharedHashMap data    

  SharedHaspMap supports any Serializable object as value but the most efficient way to access the underlying data is to implement [Byteable](http://openhft.github.io/Java-Lang/apidocs/net/openhft/lang/model/Byteable.html) so you can avoid object serialization and data copy, here a very basic implementation:
  
  
  ```java
  public class StateMachineData implements Byteable {
      private Bytes bytes = null;
      private long offset = -1;
  
      @Override
      public void bytes(Bytes bytes, long offset) {
          this.bytes = bytes;
          this.offset = offset;
      }
  
      @Override
      public Bytes bytes() {
          return this.bytes;
      }
  
      @Override
      public long offset() {
          return this.offset;
      }
  
      @Override
      public int maxSize() {
          return 16;
      }
  }
  ```

3. Add support for IPC to SharedHashMap data

  As SharedHashMap can share data among processes via memory mapped files, you can share the state of your system and you can wait for a state with a [Compare-and-Swap](http://en.wikipedia.org/wiki/Compare-and-swap) operation:
  
  ```java
    /**
     * Set the state.
     *
     * @param state 
     */
    public void setState(StateMachineState state) {
        if(this.bytes != null) {
            this.bytes.writeInt(this.offset,state.value());
        }
    }

    /**
     * Set the state.
     *
     * @param from the expected state
     * @param to the next state
     */
    public boolean setState(StateMachineState from, StateMachineState to) {
        if(this.bytes != null) {
            return this.bytes.compareAndSwapInt(this.offset,from.value(),to.value());
        }
        
        return false;
    }

    /**
     * 
     * @return the current state
     */
    public StateMachineState getState() {
        int value = -1;
        if(this.bytes != null) {
            value = this.bytes.readVolatileInt(this.offset);
        }

        return StateMachineState.fromValue(value);
    }

    /**
     * Wait for a state and make a transition.
     * It spins initially (1000 iterations), then uses a Thread.yield() .
     *
     * @param from the state to wait for 
     * @param to the next state
     */
    public void waitForState(StateMachineState from, StateMachineState to) {
        if(this.bytes != null) {
            // spin
            for(int i=0;i<1000;i++) {
                if(setState(from,to)) {
                    return;
                }
            }

            // yeld
            while(!setState(from,to)) {
                Thread.yield();
            }
        }
    }
  ```
  
4. Implements the state processor

  ```java
  public class StateMachineProcessor implements Runnable {
      private final StateMachineData smd;
      private final StateMachineState from;
      private final StateMachineState transition;
      private final StateMachineState to;
      private final Logger logger;

      /**
       *
       * @param smd
       * @param from
       * @param to
       */
      public StateMachineProcessor(final StateMachineData smd, StateMachineState from,StateMachineState transition, StateMachineState to) {
          this.smd = smd;
          this.from = from;
          this.transition = transition;
          this.to = to;
  
          // Set an informative log name:
          //   STATE_2 => STATE_2_WORKING => STATE_3
          this.logger = LoggerFactory.getLogger(
              String.format("%s => %s => %s",from.name(),transition.name(),to.name())
          );
      }

      @Override
      public void run() {
          // Loop untill a shared done event is set
          while(!smd.done()) {
              // If the processor was interrupted while working on this transition
              // resume it
              if (smd.stateIn(transition)) {
                  doProcess();
              }
  
              // Wait for a state transition
              logger.info("Wait for {}", from);
              smd.waitForState(from, transition);
  
              doProcess();
          }
      }
  
      /**
       * Do something.
       *
       * In this case we do some very basic tasks:
       * - increment a shared counter
       * - trigger the next step
       */
      private void doProcess() {
          smd.incStateData();
  
          logger.info("Status {}, Next {}, Data {}",
              from,
              to,
              smd.getStateData()
          );
  
          smd.setState(transition,to);
      }
  }
  ```

5. Implement the state machine node 

  ```java
  SharedHashMap<Integer, StateMachineData> map = null;

  try {
      map = new SharedHashMapBuilder()
          .entries(8)
          .minSegments(128)
          .entrySize(128)
          .create(
              new File(System.getProperty("java.io.tmpdir"),"hft-state-machin"),
              Integer.class,
              StateMachineData.class);

      if(args.length == 0) {
          StateMachineTutorial.stateMachineDemo(map);
      } else {
          if("0".equals(args[0])) {
              StateMachineData smd =
                  map.acquireUsing(0, new StateMachineData());

              StateMachineState st = smd.getState();
              if(st == StateMachineState.STATE_0) {
                  //fire the first state change
                  smd.setStateData(0);
                  smd.setState(StateMachineState.STATE_0,StateMachineState.STATE_1);
              }
          } else if("1".equals(args[0])) {
              StateMachineProcessor.runProcessor(
                  map.acquireUsing(0,new StateMachineData()),
                  StateMachineState.STATE_1,
                  StateMachineState.STATE_1_WORKING,
                  StateMachineState.STATE_2);
          } else if("2".equals(args[0])) {
              StateMachineProcessor.runProcessor(
                  map.acquireUsing(0,new StateMachineData()),
                  StateMachineState.STATE_2,
                  StateMachineState.STATE_2_WORKING,
                  StateMachineState.STATE_3);
          } else if("3".equals(args[0])) {
              StateMachineProcessor.runProcessor(
                  map.acquireUsing(0,new StateMachineData()),
                  StateMachineState.STATE_3,
                  StateMachineState.STATE_3_WORKING,
                  StateMachineState.STATE_1);
          }
      }
  } finally {
      if(map != null) {
          map.close();
      }
  }
  ```

6. Run the state machine 

  Run the processors:
  ```
  mvn exec:java -Dexec.mainClass="net.openhft.collections.tutorial.statemachine.StateMachineTutorial" -Dexec.arguments=3
  [net.openhft.collections.tutorial.statemachine.StateMachineTutorial.main()] INFO STATE_3 => STATE_3_WORKING => STATE_1 - Wait for STATE_3
  
  mvn exec:java -Dexec.mainClass="net.openhft.collections.tutorial.statemachine.StateMachineTutorial" -Dexec.arguments=2
  [net.openhft.collections.tutorial.statemachine.StateMachineTutorial.main()] INFO STATE_2 => STATE_2_WORKING => STATE_3 - Wait for STATE_2
  
  mvn exec:java -Dexec.mainClass="net.openhft.collections.tutorial.statemachine.StateMachineTutorial" -Dexec.arguments=1
  [net.openhft.collections.tutorial.statemachine.StateMachineTutorial.main()] INFO STATE_1 => STATE_1_WORKING => STATE_2 - Wait for STATE_1
  ```
  
  Start the state machine:
  ```
  mvn exec:java -Dexec.mainClass="net.openhft.collections.tutorial.statemachine.StateMachineTutorial" -Dexec.arguments=3
  
  [net.openhft.collections.tutorial.statemachine.StateMachineTutorial.main()] INFO STATE_1 => STATE_1_WORKING => STATE_2 - Status STATE_1, Next STATE_2, Data 1
  [net.openhft.collections.tutorial.statemachine.StateMachineTutorial.main()] INFO STATE_1 => STATE_1_WORKING => STATE_2 - Wait for STATE_1

  [net.openhft.collections.tutorial.statemachine.StateMachineTutorial.main()] INFO STATE_2 => STATE_2_WORKING => STATE_3 - Status STATE_2, Next STATE_3, Data 2
  [net.openhft.collections.tutorial.statemachine.StateMachineTutorial.main()] INFO STATE_2 => STATE_2_WORKING => STATE_3 - Wait for STATE_2

  [net.openhft.collections.tutorial.statemachine.StateMachineTutorial.main()] INFO STATE_3 => STATE_3_WORKING => STATE_1 - Status STATE_3, Next STATE_1, Data 3
  [net.openhft.collections.tutorial.statemachine.StateMachineTutorial.main()] INFO STATE_3 => STATE_3_WORKING => STATE_1 - Wait for STATE_3
  
  ...
  ```


Notes:
* This example is quite aggressive as each processor watches for changes in a busy-loop 
* One beautiful side effect of SharedHashMap used as IPC system is that a process can start and resume very quickly as the data and state information are immediately available 


[Here] (https://github.com/lburgazzoli/HugeCollections/tree/HFT-LEARN/collections-tutorial/src/main/java/net/openhft/collections/tutorial/statemachine) the full example.


For more details about SharedHashMap: 
* https://github.com/OpenHFT/HugeCollections
* http://www.infoq.com/articles/Open-JDK-and-HashMap-Off-Heap
* http://robsjava.blogspot.it/2014/03/sharing-hashmap-between-processes.html



