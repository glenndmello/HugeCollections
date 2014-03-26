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

import net.openhft.lang.io.Bytes;
import net.openhft.lang.model.Byteable;

/**
 *
 */
public class StateMachineData implements Byteable {
    private Bytes bytes;
    private long offset;

    /**
     * c-tor
     */
    public StateMachineData() {
        this.bytes = null;
        this.offset = -1;
    }

    // *************************************************************************
    //
    // *************************************************************************

    /**
     *
     * @param states
     * @return
     */
    public boolean stateIn(StateMachineState... states) {
        StateMachineState currentState = getState();
        for(StateMachineState state : states) {
            if(state == currentState) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param state
     */
    public void setState(StateMachineState state) {
        if(this.bytes != null) {
            this.bytes.writeInt(this.offset,state.value());
        }
    }

    /**
     *
     * @param from
     * @param to
     */
    public boolean setState(StateMachineState from, StateMachineState to) {
        if(this.bytes != null) {
            return this.bytes.compareAndSwapInt(this.offset,from.value(),to.value());
        }

        return false;
    }

    /**
     *
     * @return
     */
    public StateMachineState getState() {
        int value = -1;
        if(this.bytes != null) {
            value = this.bytes.readVolatileInt(this.offset);
        }

        return StateMachineState.fromValue(value);
    }

    /**
     * Waith for a state transition.
     * It initially spins (1000 iterations), then uses a Thread.yield()
     *
     * @param from
     * @param to
     */
    public void waitForState(StateMachineState from, StateMachineState to) {
        if(this.bytes != null) {
            for(int i=0;i<1000;i++) {
                if(setState(from,to)) {
                    return;
                }
            }

            while(!setState(from,to)) {
                Thread.yield();
            }
        }
    }

    // *************************************************************************
    //
    // *************************************************************************

    /**
     *
     * @param data
     */
    public void setStateData(int data) {
        if(this.bytes != null) {
            this.bytes.writeInt(this.offset + 4,data);
        }
    }

    /**
     *
     * @return
     */
    public int getStateData() {
        if(this.bytes != null) {
            return this.bytes.readVolatileInt(this.offset + 4);
        }

        return -1;
    }

    /**
     *
     * @return
     */
    public int incStateData() {
        if(this.bytes != null) {
            return this.bytes.addInt(this.offset + 4,1);
        }

        return -1;
    }

    /**
     *
     * @return
     */
    public boolean done() {
        if(this.bytes != null) {
            return getStateData() > 100;
        }

        return true;
    }

    // *************************************************************************
    //
    // *************************************************************************

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
