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

package net.openhft.collections;

import org.junit.Test;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 28/02/14.
 */
public class KeySizesTest {
    @Test
    public void testDifferentKeySizes() throws IOException {
        File tempFile = File.createTempFile("delete", "me");
        Map<String, String> map = new SharedHashMapBuilder().create(tempFile, String.class, String.class);

        String k = "";
        for (int i = 0; i < 100; i++) {
            map.put(k, k);
            String k2 = map.get(k);
            assertEquals(k, k2);
            k += "a";
        }
        k = "";
        for (int i = 0; i < 100; i++) {
            String k2 = map.get(k);
            assertEquals(k, k2);
            k += "a";
        }

        ((Closeable) map).close();
        tempFile.delete();
    }
}
