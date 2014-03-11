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
     * @param state
     */
    public void setState(StateMachineState state) {
        if(this.bytes != null) {
            this.bytes.writeInt(this.offset,state.value());
        }
    }

    /**
     *
     * @return
     */
    public StateMachineState getState() {
        int value = -1;
        if(this.bytes != null) {
            value = this.bytes.readInt(this.offset);
        }

        return StateMachineState.fromValue(value);
    }

    /**
     *
     * @param from
     * @param to
     */
    public void waitForState(StateMachineState from, StateMachineState to) {
        if(this.bytes != null) {
            while(!this.bytes.compareAndSwapInt(this.offset,from.value(),to.value())) {
                Thread.yield();
            }
        }
    }

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
            return this.bytes.readInt(this.offset + 4);
        }

        return -1;
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
