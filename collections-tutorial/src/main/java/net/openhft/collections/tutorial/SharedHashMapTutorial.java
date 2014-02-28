/*
 * Copyright 2013 Peter Lawrey
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
package net.openhft.collections.tutorial;

import net.openhft.collections.SharedHashMap;
import net.openhft.collections.SharedHashMapBuilder;
import net.openhft.collections.tutorial.values.NativeLongValue;
import net.openhft.lang.values.LongValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 *
 */
public class SharedHashMapTutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedHashMapTutorial.class);

    // *************************************************************************
    //
    // *************************************************************************

    /**
     * Shows Byteable usage in SharedHashMap
     * @throws Exception
     */
    public static void byteableDemo() throws Exception {
        final int entries = 10;

        SharedHashMap<Integer, NativeLongValue> map =
            new SharedHashMapBuilder()
                .entries(entries)
                .segments(128)
                .entrySize(24)
                .create(
                    getPersistenceFile(),
                    Integer.class,
                    NativeLongValue.class);

        // Acquire a value for the given key, as the map is empty, the key will
        // be created and space is allocated to hold the data (max 24 bytes).
        //
        // As the value is Byteable, it will carry a reference to the underlying
        // memory so it can be manipulated direcly by the return value.
        //
        // v1 will be a reference to the newly created object (new NativeLongValue)
        LongValue v1 = map.acquireUsing(
            1,                     // the key
            new NativeLongValue()  // the value (Byteable)
        );

        // v2 will be a brand new object as the value parameter is null
        LongValue v2 = map.acquireUsing(
            1,   // the key
            null // the value
        );

        map.close();
    }

    // *************************************************************************
    // Helpers
    // *************************************************************************

    /**
     * @param sb
     * @param i
     * @return
     */
    private static CharSequence getKeyCharSequence(StringBuilder sb, int i) {
        sb.setLength(0);
        sb.append("key:");
        sb.append(i);

        return sb;
    }

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
            SharedHashMapTutorial.byteableDemo();
        } catch (Exception e) {
            LOGGER.warn("Exception",e);
        }
    }
}
