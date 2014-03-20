/*
 * Copyright 2014 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.collections.tutorial.statemachine;

import net.openhft.collections.SharedHashMap;
import net.openhft.collections.SharedHashMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class StateMachineTutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachineTutorial.class);

    // *************************************************************************
    //
    // *************************************************************************

    /**
     *
     * @param map
     * @throws Exception
     */
    public static void stateMachineDemo(final SharedHashMap<Integer, StateMachineData> map) throws Exception {
        final StateMachineData smd  = map.acquireUsing(0,new StateMachineData());
        final ExecutorService  esvc = Executors.newFixedThreadPool(3);

        esvc.execute(new StateMachineProcessor(smd,5,StateMachineState.STATE_1,StateMachineState.STATE_2));
        esvc.execute(new StateMachineProcessor(smd,5,StateMachineState.STATE_2,StateMachineState.STATE_3));
        esvc.execute(new StateMachineProcessor(smd,5,StateMachineState.STATE_3,StateMachineState.STATE_1));

        //fire the first state change
        smd.setState(StateMachineState.STATE_0,StateMachineState.STATE_1);

        esvc.shutdown();
        esvc.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     *
     * @param map
     * @throws Exception
     */
    public static void trigger(final SharedHashMap<Integer, StateMachineData> map) throws Exception{
        final StateMachineData smd  = map.acquireUsing(0, new StateMachineData());

        StateMachineState st = smd.getState();
        LOGGER.info("Old state is: {}",st);

        if(st == StateMachineState.STATE_0) {
            //fire the first state change
            smd.setState(StateMachineState.STATE_0,StateMachineState.STATE_1);

            LOGGER.info("New state is: {}",smd.getState());
        }
    }

    /**
     *
     * @param map
     * @param from
     * @param to
     * @throws Exception
     */
    public static void runProcessor(final SharedHashMap<Integer, StateMachineData> map,StateMachineState from,StateMachineState to) throws Exception {
        new StateMachineProcessor(
            map.acquireUsing(0,new StateMachineData()),
            5,
            from,
            to).run();
    }

    // *************************************************************************
    // Helpers
    // *************************************************************************

    /**
     *
     * @return
     * @throws Exception
     */
    private static SharedHashMap<Integer, StateMachineData> getSharedHashMap() throws Exception {
        return new SharedHashMapBuilder()
            .entries(10)
            .minSegments(128)
            .entrySize(24)
            .create(
                getPersistenceFile(),
                Integer.class,
                StateMachineData.class);
    }

    /**
     *
     * @return
     */
    private static File getPersistenceFile() {
        String TMP = System.getProperty("java.io.tmpdir");
        File file = new File(TMP,"hft-collections-shm-tutorial-sm");
        //file.delete();
        //file.deleteOnExit();

        return file;
    }

    // *************************************************************************
    //
    // *************************************************************************

    public static void main(String[] args) throws Exception{
        SharedHashMap<Integer, StateMachineData> map = null;

        try {
            map = getSharedHashMap();

            if(args.length == 0) {
                StateMachineTutorial.stateMachineDemo(map);
            } else {
                if("0".equals(args[0])) {
                    StateMachineTutorial.trigger(map);
                } else if("1".equals(args[0])) {
                    StateMachineTutorial.runProcessor(
                        map,
                        StateMachineState.STATE_1,
                        StateMachineState.STATE_2
                    );
                } else if("2".equals(args[0])) {
                    StateMachineTutorial.runProcessor(
                        map,
                        StateMachineState.STATE_2,
                        StateMachineState.STATE_3
                    );
                } else if("3".equals(args[0])) {
                    StateMachineTutorial.runProcessor(
                        map,
                        StateMachineState.STATE_3,
                        StateMachineState.STATE_1
                    );
                }
            }
        } finally {
            map.close();
        }
    }
}
