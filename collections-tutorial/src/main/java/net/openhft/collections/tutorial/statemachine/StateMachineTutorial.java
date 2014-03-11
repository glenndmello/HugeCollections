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
     * @throws Exception
     */
    public static void stateMachineDemo() throws Exception {
        final int entries = 10;

        SharedHashMap<Integer, StateMachineData> map =
            new SharedHashMapBuilder()
                .entries(entries)
                .segments(128)
                .entrySize(24)
                .create(
                    getPersistenceFile(),
                    Integer.class,
                    StateMachineData.class);


        final StateMachineData smd  = map.acquireUsing(0,new StateMachineData());
        final ExecutorService  esvc = Executors.newFixedThreadPool(3);

        esvc.execute(new StateMachineProcessor(smd,5,StateMachineState.STATE_1,StateMachineState.STATE_2));
        esvc.execute(new StateMachineProcessor(smd,5,StateMachineState.STATE_2,StateMachineState.STATE_3));
        esvc.execute(new StateMachineProcessor(smd,5,StateMachineState.STATE_3,StateMachineState.STATE_1));

        //fire the first state change
        smd.setState(StateMachineState.STATE_1);

        esvc.shutdown();
        esvc.awaitTermination(10, TimeUnit.SECONDS);
        
        map.close();
    }

    // *************************************************************************
    // Helpers
    // *************************************************************************

    private static File getPersistenceFile() {
        String TMP = System.getProperty("java.io.tmpdir");
        File file = new File(TMP,"hft-collections-shm-tutorial");
        file.delete();
        file.deleteOnExit();

        return file;
    }

    // *************************************************************************
    //
    // *************************************************************************

    public static void main(String[] args) {
        try {
            StateMachineTutorial.stateMachineDemo();
        } catch (Exception e) {
            LOGGER.warn("Exception",e);
        }
    }
}
