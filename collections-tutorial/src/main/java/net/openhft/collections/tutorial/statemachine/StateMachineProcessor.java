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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class StateMachineProcessor implements Runnable {
    private final StateMachineData smd;
    private final int iterations;
    private final StateMachineState from;
    private final StateMachineState to;
    private final Logger logger;

    /**
     *
     * @param smd
     * @param iterations
     * @param from
     * @param to
     */
    public StateMachineProcessor(final StateMachineData smd, int iterations, StateMachineState from,StateMachineState to) {
        this.smd = smd;
        this.iterations = iterations;
        this.from = from;
        this.to = to;
        this.logger = LoggerFactory.getLogger(String.format("%s => %s",from.name(),to.name()));

    }

    @Override
    public void run() {
        for(int i=0;i<iterations;i++) {
            this.logger.info("Wait for {}",this.from);
            smd.waitForState(this.from,StateMachineState.WORKING);

            this.logger.info("{}/{} Status {}, Next {}",
                i,
                iterations,
                this.from,
                this.to);

            smd.setStateData(from.value());
            smd.setState(StateMachineState.WORKING,to);
        }
    }
}
