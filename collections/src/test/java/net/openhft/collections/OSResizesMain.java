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

import net.openhft.lang.Jvm;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * This example shows that the OS resizes the usage of a SHM as needed.  It is not as critical to worry about this.
 * <p>
 * System memory: 7.7 GB, Extents of map: 137.5 GB, disk used: 21MB, addressRange: 7eec7afbd000-7f0c7c000000
 * </p>
 */
public class OSResizesMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        File file = File.createTempFile("over-sized", "deleteme");
        SharedHashMap<String, String> map = new SharedHashMapBuilder()
                .entrySize(64 * 1024)
                .entries(1024 * 1024)
                .create(file, String.class, String.class);
        for (int i = 0; i < 1000; i++) {
            char[] chars = new char[i];
            Arrays.fill(chars, '+');
            map.put("key-" + i, new String(chars));
        }
        System.out.printf("System memory: %.1f GB, Extents of map: %.1f GB, disk used: %sB, addressRange: %s%n",
                Double.parseDouble(run("head", "-1", "/proc/meminfo").split("\\s+")[1]) / 1e6,
                file.length() / 1e9,
                run("du", "-h", file.getAbsolutePath()).split("\\s")[0],
                run("grep", "over-sized", "/proc/" + Jvm.getProcessId() + "/maps").split("\\s")[0]);

        map.close();
        file.delete();
    }

    static String run(String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        InputStreamReader reader = new InputStreamReader(p.getInputStream());
        StringWriter sw = new StringWriter();
        char[] chars = new char[512];
        for (int len; (len = reader.read(chars)) > 0; )
            sw.write(chars, 0, len);
        int exitValue = p.waitFor();
        if (exitValue != 0)
            sw.write("\nexit=" + exitValue);
        p.destroy();
        return sw.toString();
    }
}
