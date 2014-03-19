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

/**
 *
 */
public enum StateMachineState {
    WORKING(-2),
    UNKNOWN(-1),
    STATE_0(0),
    STATE_1(1),
    STATE_2(2),
    STATE_3(3)
    ;

    private int state;

    /**
     * c-tor
     *
     * @param state
     */
    StateMachineState(int state) {
        this.state = state;
    }

    /**
     *
     * @return
     */
    public int value() {
        return this.state;
    }

    public static StateMachineState fromValue(int value) {
        for(StateMachineState sms : StateMachineState.values()) {
            if(sms.value() == value) {
                return sms;
            }
        }

        return StateMachineState.UNKNOWN;
    }
}
