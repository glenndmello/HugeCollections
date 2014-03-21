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

3. Support IPC 

  As SharedHashMap can share data among processes via memory mapped files, you can share the state of your system and you can wait for a state to change with a [Compare-and-Swap](http://en.wikipedia.org/wiki/Compare-and-swap) operation:
  
  ```java
      /**
       * Make a transition.
       *
       * @param from the expected state
       * @param to the next state 
       */
      public boolean setState(StateMachineState from, StateMachineState to) {
          if(this.bytes != null) {
              return this.bytes.compareAndSwapInt(this.offset,from.value(),to.value()));
          }
          
          return false;
      }
  
      /**
       *
       * @return the state
       */
      public StateMachineState getState() {
          int value = -1;
          if(this.bytes != null) {
              value = this.bytes.readInt(this.offset);
          }
  
          return StateMachineState.fromValue(value);
      }
      
      /**
       * Busy-wait for a state and make a transition.
       *
       * @param from the state to wait for
       * @param to the next state
       */
      public void waitForState(StateMachineState from, StateMachineState to) {
          if(this.bytes != null) {
              while(!setState(from.value(),to.value())) {
                  Thread.yield();
              }
          }
      }
  ```



For more details about SharedHashMap: 
* https://github.com/OpenHFT/HugeCollections
* http://www.infoq.com/articles/Open-JDK-and-HashMap-Off-Heap
* http://robsjava.blogspot.it/2014/03/sharing-hashmap-between-processes.html



