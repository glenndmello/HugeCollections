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

import net.openhft.lang.model.DataValueClasses;
import net.openhft.lang.model.DataValueGenerator;
import net.openhft.lang.values.IntValue;
import net.openhft.lang.values.LongValue;
import net.openhft.lang.values.LongValue£native;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
public class SharedHashMapTest {

    private StringBuilder sb = new StringBuilder();

    static void assertKeySet(Set<Integer> keySet, int[] expectedKeys) {
        Set<Integer> expectedSet = new HashSet<Integer>();
        for (int expectedKey : expectedKeys) {
            expectedSet.add(expectedKey);
        }
        org.junit.Assert.assertEquals(expectedSet, keySet);
    }

    static void assertValues(Collection<CharSequence> values, CharSequence[] expectedValues) {
        List<String> expectedList = new ArrayList<String>();
        for (CharSequence expectedValue : expectedValues) {
            expectedList.add(expectedValue.toString());
        }
        Collections.sort(expectedList);

        List<String> actualList = new ArrayList<String>();
        for (CharSequence actualValue : values) {
            actualList.add(actualValue.toString());
        }
        Collections.sort(actualList);

        org.junit.Assert.assertEquals(expectedList, actualList);
    }

    static void assertEntrySet(Set<Map.Entry<Integer, CharSequence>> entrySet, int[] expectedKeys, CharSequence[] expectedValues) {
        Set<Map.Entry<Integer, CharSequence>> expectedSet = new HashSet<Map.Entry<Integer, CharSequence>>();
        for (int i = 0; i < expectedKeys.length; i++) {
            expectedSet.add(new AbstractMap.SimpleEntry<Integer, CharSequence>(expectedKeys[i], expectedValues[i]));
        }
        org.junit.Assert.assertEquals(expectedSet, entrySet);
    }

    static void assertMap(Map<Integer, CharSequence> map, int[] expectedKeys, CharSequence[] expectedValues) {
        assertEquals(expectedKeys.length, map.size());
        for (int i = 0; i < expectedKeys.length; i++) {
            org.junit.Assert.assertEquals("On position " + i, expectedValues[i], map.get(expectedKeys[i]));
        }
    }

    @Test
    public void testRemoveWithKey() throws Exception {

        final SharedHashMap<CharSequence, CharSequence> map = new SharedHashMapBuilder()
                .minSegments(2)
                .create(getPersistenceFile(), CharSequence.class, CharSequence.class);

        assertFalse(map.containsKey("key3"));
        map.put("key1", "one");
        map.put("key2", "two");
        assertEquals(2, map.size());

        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
        assertFalse(map.containsKey("key3"));

        assertEquals("one", map.get("key1"));
        assertEquals("two", map.get("key2"));

        final CharSequence result = map.remove("key1");

        assertEquals(1, map.size());

        assertEquals("one", result);
        assertFalse(map.containsKey("key1"));

        assertEquals(null, map.get("key1"));
        assertEquals("two", map.get("key2"));
        assertFalse(map.containsKey("key3"));

        // lets add one more item for luck !
        map.put("key3", "three");
        assertEquals("three", map.get("key3"));
        assertTrue(map.containsKey("key3"));
        assertEquals(2, map.size());

        // and just for kicks we'll overwrite what we have
        map.put("key3", "overwritten");
        assertEquals("overwritten", map.get("key3"));
        assertTrue(map.containsKey("key3"));
        assertEquals(2, map.size());

        map.close();
    }


    @Test
    public void testSize() throws Exception {

        final SharedHashMap<CharSequence, CharSequence> map = new SharedHashMapBuilder()
                .minSegments(1024)
                .removeReturnsNull(true)
                .create(getPersistenceFile(), CharSequence.class, CharSequence.class);


        for (int i = 1; i < 1024; i++) {
            map.put("key" + i, "value");
            assertEquals(i, map.size());
        }

        for (int i = 1023; i >= 1; ) {
            map.remove("key" + i);
            i--;
            assertEquals(i, map.size());
        }
        map.close();
    }

    @Test
    @Ignore //todo: fails on my machine
    public void testRemoveInteger() throws IOException {

        int count = 3000;
        final SharedHashMap<Object, Object> map = new SharedHashMapBuilder()
                .entrySize(count)
                .minSegments(2)
                .create(getPersistenceFile(), Object.class, Object.class);


        for (int i = 1; i < count; i++) {
            map.put(i, i);
            assertEquals(i, map.size());
        }

        for (int i = count - 1; i >= 1; ) {
            Integer j = (Integer) map.put(i, i);
            assertEquals(i, j.intValue());
            Integer j2 = (Integer) map.remove(i);
            assertEquals(i, j2.intValue());
            i--;
            assertEquals(i, map.size());
        }
        map.close();
    }

    @Test
    public void testRemoveWithKeyAndRemoveReturnsNull() throws Exception {

        final SharedHashMap<CharSequence, CharSequence> map = new SharedHashMapBuilder()
                .minSegments(2)
                .removeReturnsNull(true)
                .create(getPersistenceFile(), CharSequence.class, CharSequence.class);

        assertFalse(map.containsKey("key3"));
        map.put("key1", "one");
        map.put("key2", "two");
        assertEquals(2, map.size());

        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
        assertFalse(map.containsKey("key3"));

        assertEquals("one", map.get("key1"));
        assertEquals("two", map.get("key2"));

        final CharSequence result = map.remove("key1");
        assertEquals(null, result);

        assertEquals(1, map.size());

        assertFalse(map.containsKey("key1"));

        assertEquals(null, map.get("key1"));
        assertEquals("two", map.get("key2"));
        assertFalse(map.containsKey("key3"));

        // lets add one more item for luck !
        map.put("key3", "three");
        assertEquals("three", map.get("key3"));
        assertTrue(map.containsKey("key3"));
        assertEquals(2, map.size());

        // and just for kicks we'll overwrite what we have
        map.put("key3", "overwritten");
        assertEquals("overwritten", map.get("key3"));
        assertTrue(map.containsKey("key3"));
        assertEquals(2, map.size());

        map.close();
    }


    @Test
    public void testReplaceWithKey() throws Exception {

        final SharedHashMap<CharSequence, CharSequence> map = new SharedHashMapBuilder()
                .minSegments(2)
                .create(getPersistenceFile(), CharSequence.class, CharSequence.class);


        map.put("key1", "one");
        map.put("key2", "two");
        assertEquals(2, map.size());

        assertEquals("one", map.get("key1"));
        assertEquals("two", map.get("key2"));

        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));

        final CharSequence result = map.replace("key1", "newValue");

        assertEquals("one", result);
        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
        assertEquals(2, map.size());

        assertEquals("newValue", map.get("key1"));
        assertEquals("two", map.get("key2"));

        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
        assertFalse(map.containsKey("key3"));

        assertEquals(2, map.size());

        // let and one more item for luck !
        map.put("key3", "three");
        assertEquals(3, map.size());

        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
        assertTrue(map.containsKey("key3"));
        assertEquals("three", map.get("key3"));

        // and just for kicks we'll overwrite what we have
        map.put("key3", "overwritten");
        assertEquals("overwritten", map.get("key3"));

        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
        assertTrue(map.containsKey("key3"));

        final CharSequence result2 = map.replace("key2", "newValue");

        assertEquals("two", result2);
        assertEquals("newValue", map.get("key2"));

        final CharSequence result3 = map.replace("rubbish", "newValue");
        assertEquals(null, result3);

        assertFalse(map.containsKey("rubbish"));
        assertEquals(3, map.size());

        map.close();
    }


    @Test
    public void testReplaceWithKeyAnd2Params() throws Exception {

        final SharedHashMap<CharSequence, CharSequence> map = new SharedHashMapBuilder()
                .minSegments(2)
                .create(getPersistenceFile(), CharSequence.class, CharSequence.class);

        map.put("key1", "one");
        map.put("key2", "two");

        assertEquals("one", map.get("key1"));
        assertEquals("two", map.get("key2"));

        final boolean result = map.replace("key1", "one", "newValue");

        assertEquals(true, result);

        assertEquals("newValue", map.get("key1"));
        assertEquals("two", map.get("key2"));

        // let and one more item for luck !
        map.put("key3", "three");
        assertEquals("three", map.get("key3"));

        // and just for kicks we'll overwrite what we have
        map.put("key3", "overwritten");
        assertEquals("overwritten", map.get("key3"));

        final boolean result2 = map.replace("key2", "two", "newValue2");

        assertEquals(true, result2);
        assertEquals("newValue2", map.get("key2"));

        final boolean result3 = map.replace("newKey", "", "newValue");
        assertEquals(false, result3);

        final boolean result4 = map.replace("key2", "newValue2", "newValue2");
        assertEquals(true, result4);

        map.close();
    }

    @Test
    public void testRemoveWithKeyAndValue() throws Exception {

        final SharedHashMap<CharSequence, CharSequence> map = new SharedHashMapBuilder()
                .minSegments(2)
                .create(getPersistenceFile(), CharSequence.class, CharSequence.class);


        map.put("key1", "one");
        map.put("key2", "two");

        assertEquals("one", map.get("key1"));
        assertEquals("two", map.get("key2"));


        // a false remove
        final boolean wasRemoved1 = map.remove("key1", "three");

        assertFalse(wasRemoved1);


        assertEquals(null, map.get("key1"), "one");
        assertEquals("two", map.get("key2"), "two");

        map.put("key1", "one");


        final boolean wasRemoved2 = map.remove("key1", "three");
        assertFalse(wasRemoved2);

        // lets add one more item for luck !
        map.put("key3", "three");
        assertEquals("three", map.get("key3"));

        // and just for kicks we'll overwrite what we have
        map.put("key3", "overwritten");
        assertEquals("overwritten", map.get("key3"));

        map.close();
    }

    @Test
    public void testAcquireWithNullContainer() throws Exception {
        SharedHashMap<CharSequence, LongValue> map = getSharedMap(10 * 1000, 128, 24);
        map.acquireUsing("key", new LongValue£native());
        assertEquals(0, map.acquireUsing("key", null).getValue());

        map.close();
    }

    @Test
    public void testGetWithNullContainer() throws Exception {
        SharedHashMap<CharSequence, LongValue> map = getSharedMap(10 * 1000, 128, 24);
        map.acquireUsing("key", new LongValue£native());
        assertEquals(0, map.getUsing("key", null).getValue());

        map.close();
    }

    @Test
    public void testGetWithoutAcquireFirst() throws Exception {
        SharedHashMap<CharSequence, LongValue> map = getSharedMap(10 * 1000, 128, 24);
        assertNull(map.getUsing("key", new LongValue£native()));

        map.close();
    }

    @Test
    public void testAcquireAndGet() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        int entries = 1000 * 1000;
        SharedHashMap<CharSequence, LongValue> map = getSharedMap(entries, 128, 24);

        LongValue value = new LongValue£native();
        LongValue value2 = new LongValue£native();
        LongValue value3 = new LongValue£native();

        for (int j = 1; j <= 3; j++) {
            for (int i = 0; i < entries; i++) {
                CharSequence userCS = getUserCharSequence(i);

                if (j > 1) {
                    assertNotNull(map.getUsing(userCS, value));
                } else {
                    map.acquireUsing(userCS, value);
                }
                assertEquals(j - 1, value.getValue());

                value.addAtomicValue(1);

                assertEquals(value2, map.acquireUsing(userCS, value2));
                assertEquals(j, value2.getValue());

                assertEquals(value3, map.getUsing(userCS, value3));
                assertEquals(j, value3.getValue());
            }
        }

        map.close();
    }

    @Test
    public void testAcquireFromMultipleThreads() throws Exception {
        SharedHashMap<CharSequence, LongValue> map = getSharedMap(1000 * 1000, 128, 24);

        CharSequence key = getUserCharSequence(0);
        map.acquireUsing(key, new LongValue£native());

        int iterations = 1000;
        int noOfThreads = 10;
        CyclicBarrier barrier = new CyclicBarrier(noOfThreads);

        Thread[] threads = new Thread[noOfThreads];
        for (int t = 0; t < noOfThreads; t++) {
            threads[t] = new Thread(new IncrementRunnable(map, key, iterations, barrier));
            threads[t].start();
        }
        for (int t = 0; t < noOfThreads; t++) {
            threads[t].join();
        }

        assertEquals(noOfThreads * iterations, map.acquireUsing(key, new LongValue£native()).getValue());

        map.close();
    }

    private static final class IncrementRunnable implements Runnable {

        private final SharedHashMap<CharSequence, LongValue> map;

        private final CharSequence key;

        private final int iterations;

        private final CyclicBarrier barrier;

        private IncrementRunnable(SharedHashMap<CharSequence, LongValue> map, CharSequence key, int iterations, CyclicBarrier barrier) {
            this.map = map;
            this.key = key;
            this.iterations = iterations;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                LongValue value = new LongValue£native();
                barrier.await();
                for (int i = 0; i < iterations; i++) {
                    map.acquireUsing(key, value);
                    value.addAtomicValue(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // i7-3970X CPU @ 3.50GHz, hex core: -verbose:gc -Xmx64m
    // to tmpfs file system
    // 10M users, updated 12 times. Throughput 19.3 M ops/sec, no GC!
    // 50M users, updated 12 times. Throughput 19.8 M ops/sec, no GC!
    // 100M users, updated 12 times. Throughput 19.0M ops/sec, no GC!
    // 200M users, updated 12 times. Throughput 18.4 M ops/sec, no GC!
    // 400M users, updated 12 times. Throughput 18.4 M ops/sec, no GC!

    // to ext4 file system.
    // 10M users, updated 12 times. Throughput 17.7 M ops/sec, no GC!
    // 50M users, updated 12 times. Throughput 16.5 M ops/sec, no GC!
    // 100M users, updated 12 times. Throughput 15.9 M ops/sec, no GC!
    // 200M users, updated 12 times. Throughput 15.4 M ops/sec, no GC!
    // 400M users, updated 12 times. Throughput 7.8 M ops/sec, no GC!
    // 600M users, updated 12 times. Throughput 5.8 M ops/sec, no GC!

    // dual E5-2650v2 @ 2.6 GHz, 128 GB: -verbose:gc -Xmx32m
    // to tmpfs
    // TODO small GC on startup should be tidied up, [GC 9216K->1886K(31744K), 0.0036750 secs]
    // 10M users, updated 16 times. Throughput 33.0M ops/sec, VmPeak: 5373848 kB, VmRSS: 544252 kB
    // 50M users, updated 16 times. Throughput 31.2 M ops/sec, VmPeak: 9091804 kB, VmRSS: 3324732 kB
    // 250M users, updated 16 times. Throughput 30.0 M ops/sec, VmPeak:	24807836 kB, VmRSS: 14329112 kB
    // 1000M users, updated 16 times, Throughput 24.1 M ops/sec, VmPeak: 85312732 kB, VmRSS: 57165952 kB
    // 2500M users, updated 16 times, Throughput 23.5 M ops/sec, VmPeak: 189545308 kB, VmRSS: 126055868 kB

    // to ext4
    // 10M users, updated 16 times. Throughput 28.4 M ops/sec, VmPeak: 5438652 kB, VmRSS: 544624 kB
    // 50M users, updated 16 times. Throughput 28.2 M ops/sec, VmPeak: 9091804 kB, VmRSS: 9091804 kB
    // 250M users, updated 16 times. Throughput 26.1 M ops/sec, VmPeak:	24807836 kB, VmRSS: 24807836 kB
    // 1000M users, updated 16 times, Throughput 1.3 M ops/sec, TODO FIX this

    @Test
    public void testIntValue() {
        DataValueGenerator dvg = new DataValueGenerator();
        dvg.setDumpCode(true);
        dvg.acquireNativeClass(IntValue.class);
        dvg.acquireNativeClass(LongValue.class);
    }

    @Test
    @Ignore
    public void testAcquirePerf() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, InterruptedException, ExecutionException {
//        int runs = Integer.getInteger("runs", 10);
        for (int runs : new int[]{10, 50, 250, 500, 1000, 2500}) {
            final long entries = runs * 1000 * 1000L;
            final SharedHashMap<CharSequence, IntValue> map = getSharedStringIntMap(entries, 1024, 20);

            int procs = Runtime.getRuntime().availableProcessors();
            int threads = procs * 2; // runs > 100 ? procs / 2 : procs;
            int count = runs > 500 ? runs > 1200 ? 1 : 3 : 5;
            final int independence = 8; // Math.min(procs, runs > 500 ? 8 : 4);
            System.out.println("\nKey size: " + runs + " Million entries. " + map.builder());
            for (int j = 0; j < count; j++) {
                long start = System.currentTimeMillis();
                ExecutorService es = Executors.newFixedThreadPool(procs);
                List<Future> futures = new ArrayList<Future>();
                for (int i = 0; i < threads; i++) {
                    final int t = i;
                    futures.add(es.submit(new Runnable() {
                        @Override
                        public void run() {
                            IntValue value = nativeIntValue();
                            StringBuilder sb = new StringBuilder();
                            long next = 50 * 1000 * 1000;
                            // use a factor to give up to 10 digit numbers.
                            int factor = Math.max(1, (int) ((10 * 1000 * 1000 * 1000L - 1) / entries));
                            for (long j = t % independence; j < entries + independence - 1; j += independence) {
                                sb.setLength(0);
                                sb.append("us:");
                                sb.append(j * factor);
                                map.acquireUsing(sb, value);
                                long n = value.addAtomicValue(1);
                                assert n > 0 && n < 1000 : "Counter corrupted " + n;
                                if (t == 0 && j >= next) {
                                    long size = map.longSize();
                                    if (size < 0) throw new AssertionError("size: " + size);
                                    System.out.println(j + ", size: " + size);
                                    next += 50 * 1000 * 1000;
                                }
                            }
                        }
                    }));
                }
                for (Future future : futures) {
                    future.get();
                }
                es.shutdown();
                es.awaitTermination(runs / 10 + 1, TimeUnit.MINUTES);
                long time = System.currentTimeMillis() - start;
                System.out.printf("Throughput %.1f M ops/sec%n", threads * entries / independence / 1000.0 / time);
            }
            printStatus();
            File file = map.file();
            map.close();
            file.delete();
        }
    }

    //  i7-3970X CPU @ 3.50GHz, hex core: -Xmx30g -verbose:gc
    // 10M users, updated 12 times. Throughput 16.2 M ops/sec, longest [Full GC 853669K->852546K(3239936K), 0.8255960 secs]
    // 50M users, updated 12 times. Throughput 13.3 M ops/sec,  longest [Full GC 5516214K->5511353K(13084544K), 3.5752970 secs]
    // 100M users, updated 12 times. Throughput 11.8 M ops/sec, longest [Full GC 11240703K->11233711K(19170432K), 5.8783010 secs]
    // 200M users, updated 12 times. Throughput 4.2 M ops/sec, longest [Full GC 25974721K->22897189K(27962048K), 21.7962600 secs]

    // dual E5-2650v2 @ 2.6 GHz, 128 GB: -verbose:gc -Xmx100g
    // 10M users, updated 16 times. Throughput 155.3 M ops/sec, VmPeak: 113291428 kB, VmRSS: 9272176 kB, [Full GC 1624336K->1616457K(7299072K), 2.5381610 secs]
    // 50M users, updated 16 times. Throughput 120.4 M ops/sec, VmPeak: 113291428 kB, VmRSS: 28436248 kB [Full GC 6545332K->6529639K(18179584K), 6.9053810 secs]
    // 250M users, updated 16 times. Throughput 114.1 M ops/sec, VmPeak: 113291428 kB, VmRSS: 76441464 kB  [Full GC 41349527K->41304543K(75585024K), 17.3217490 secs]
    // 1000M users, OutOfMemoryError.

    @Test
    @Ignore
    public void testCHMAcquirePerf() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, InterruptedException {
        for (int runs : new int[]{10, 50, 250, 1000, 2500}) {
            System.out.println("Testing " + runs + " million entries");
            final long entries = runs * 1000 * 1000L;
            final ConcurrentMap<String, AtomicInteger> map = new ConcurrentHashMap<String, AtomicInteger>((int) (entries * 5 / 4), 1.0f, 1024);

            int procs = Runtime.getRuntime().availableProcessors();
            int threads = procs * 2;
            int count = runs > 500 ? runs > 1200 ? 1 : 2 : 3;
            final int independence = Math.min(procs, runs > 500 ? 8 : 4);
            for (int j = 0; j < count; j++) {
                long start = System.currentTimeMillis();
                ExecutorService es = Executors.newFixedThreadPool(procs);
                for (int i = 0; i < threads; i++) {
                    final int t = i;
                    es.submit(new Runnable() {
                        @Override
                        public void run() {
                            StringBuilder sb = new StringBuilder();
                            int next = 50 * 1000 * 1000;
                            // use a factor to give up to 10 digit numbers.
                            int factor = Math.max(1, (int) ((10 * 1000 * 1000 * 1000L - 1) / entries));
                            for (long i = t % independence; i < entries; i += independence) {
                                sb.setLength(0);
                                sb.append("u:");
                                sb.append(i * factor);
                                String key = sb.toString();
                                AtomicInteger count = map.get(key);
                                if (count == null) {
                                    map.put(key, new AtomicInteger());
                                    count = map.get(key);
                                }
                                count.getAndIncrement();
                                if (t == 0 && i == next) {
                                    System.out.println(i);
                                    next += 50 * 1000 * 1000;
                                }
                            }
                        }
                    });
                }
                es.shutdown();
                es.awaitTermination(10, TimeUnit.MINUTES);
                printStatus();
                long time = System.currentTimeMillis() - start;
                System.out.printf("Throughput %.1f M ops/sec%n", threads * entries / 1000.0 / time);
            }
        }
    }

    public static LongValue nativeLongValue() {
        return new LongValue£native();
    }

    public static IntValue nativeIntValue() {
        return DataValueClasses.newDirectReference(IntValue.class);
//        return new LongValue£native();
    }

    private CharSequence getUserCharSequence(int i) {
        sb.setLength(0);
        sb.append("u:");
        sb.append(i * 9876); // test 10 digit user numbers.
        return sb;
    }

    static File getPersistenceFile() {
        String TMP = System.getProperty("java.io.tmpdir");
        File file = new File(TMP + "/shm-test" + System.nanoTime());
        file.delete();
        file.deleteOnExit();
        return file;
    }

    private static SharedHashMap<CharSequence, LongValue> getSharedMap(long entries, int segments, int entrySize) throws IOException {
        return new SharedHashMapBuilder()
                .entries(entries)
                .minSegments(segments)
                .entrySize(entrySize)
                .generatedValueType(true)
                .create(getPersistenceFile(), CharSequence.class, LongValue.class);
    }

    private static SharedHashMap<CharSequence, IntValue> getSharedStringIntMap(long entries, int segments, int entrySize) throws IOException {
        return new SharedHashMapBuilder()
                .entries(entries)
                .minSegments(segments)
                .entrySize(entrySize)
                .generatedValueType(true)
                .putReturnsNull(true)
                .create(getPersistenceFile(), CharSequence.class, IntValue.class);
    }

    private static void printStatus() {
        if (!new File("/proc/self/status").exists()) return;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/self/status"));
            for (String line; (line = br.readLine()) != null; )
                if (line.startsWith("Vm"))
                    System.out.print(line.replaceAll("  +", " ") + ", ");
            System.out.println();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPutAndRemove() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String TMP = System.getProperty("java.io.tmpdir");
        File file = new File(TMP + "/shm-remove-test");
        file.delete();
        file.deleteOnExit();
        int entries = 100 * 1000;
        SharedHashMap<CharSequence, CharSequence> map =
                new SharedHashMapBuilder()
                        .entries(entries)
                        .minSegments(16)
                        .entrySize(32)
                        .putReturnsNull(true)
                        .removeReturnsNull(true)
                        .create(file, CharSequence.class, CharSequence.class);
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        StringBuilder value2 = new StringBuilder();
        for (int j = 1; j <= 3; j++) {
            for (int i = 0; i < entries; i++) {
                key.setLength(0);
                key.append("user:").append(i);
                value.setLength(0);
                value.append("value:").append(i);
//                System.out.println(key);
                assertNull(map.getUsing(key, value));
                assertNull(map.put(key, value));
                assertNotNull(map.getUsing(key, value2));
                assertEquals(value.toString(), value2.toString());
                assertNull(map.remove(key));
                assertNull(map.getUsing(key, value));
            }
        }

        map.close();
    }

    @Test
    public void mapRemoveReflectedInViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        map.remove(2);
        assertMap(map, new int[]{1, 3}, new CharSequence[]{"1", "3"});
        assertEntrySet(entrySet, new int[]{1, 3}, new CharSequence[]{"1", "3"});
        assertEntrySet(map.entrySet(), new int[]{1, 3}, new CharSequence[]{"1", "3"});
        assertKeySet(keySet, new int[]{1, 3});
        assertKeySet(map.keySet(), new int[]{1, 3});
        assertValues(values, new CharSequence[]{"1", "3"});
        assertValues(map.values(), new CharSequence[]{"1", "3"});

        map.close();
    }

    @Test
    public void mapPutReflectedInViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        map.put(4, "4");
        assertMap(map, new int[]{4, 2, 3, 1}, new CharSequence[]{"4", "2", "3", "1"});
        assertEntrySet(entrySet, new int[]{4, 2, 3, 1}, new CharSequence[]{"4", "2", "3", "1"});
        assertEntrySet(map.entrySet(), new int[]{4, 2, 3, 1}, new CharSequence[]{"4", "2", "3", "1"});
        assertKeySet(keySet, new int[]{4, 2, 3, 1});
        assertKeySet(map.keySet(), new int[]{4, 2, 3, 1});
        assertValues(values, new CharSequence[]{"2", "1", "4", "3"});
        assertValues(map.values(), new CharSequence[]{"2", "1", "4", "3"});

        map.close();
    }

    @Test
    public void entrySetRemoveReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        entrySet.remove(new AbstractMap.SimpleEntry<Integer, CharSequence>(2, "2"));
        assertMap(map, new int[]{1, 3}, new CharSequence[]{"1", "3"});
        assertEntrySet(entrySet, new int[]{1, 3}, new CharSequence[]{"1", "3"});
        assertKeySet(keySet, new int[]{1, 3});
        assertValues(values, new CharSequence[]{"1", "3"});

        map.close();
    }

    @Test
    public void keySetRemoveReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        keySet.remove(2);
        assertMap(map, new int[]{1, 3}, new CharSequence[]{"1", "3"});
        assertEntrySet(entrySet, new int[]{1, 3}, new CharSequence[]{"1", "3"});
        assertKeySet(keySet, new int[]{1, 3});
        assertValues(values, new CharSequence[]{"1", "3"});

        map.close();
    }

    @Test
    public void valuesRemoveReflectedInMap() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        values.remove("2");
        assertMap(map, new int[]{1, 3}, new CharSequence[]{"1", "3"});
        assertEntrySet(entrySet, new int[]{1, 3}, new CharSequence[]{"1", "3"});
        assertKeySet(keySet, new int[]{1, 3});
        assertValues(values, new CharSequence[]{"1", "3"});

        map.close();
    }

    @Test
    public void entrySetIteratorRemoveReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        Iterator<Map.Entry<Integer, CharSequence>> entryIterator = entrySet.iterator();
        entryIterator.next();
        entryIterator.next();
        entryIterator.remove();
        assertMap(map, new int[]{2, 3}, new CharSequence[]{"2", "3"});
        assertEntrySet(entrySet, new int[]{2, 3}, new CharSequence[]{"2", "3"});
        assertKeySet(keySet, new int[]{2, 3});
        assertValues(values, new CharSequence[]{"2", "3"});

        map.close();
    }

    @Test
    public void keySetIteratorRemoveReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        Iterator<Integer> keyIterator = keySet.iterator();
        keyIterator.next();
        keyIterator.next();
        keyIterator.remove();
        assertMap(map, new int[]{2, 3}, new CharSequence[]{"2", "3"});
        assertEntrySet(entrySet, new int[]{2, 3}, new CharSequence[]{"2", "3"});
        assertKeySet(keySet, new int[]{2, 3});
        assertValues(values, new CharSequence[]{"2", "3"});

        map.close();
    }

    @Test
    public void valuesIteratorRemoveReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        Iterator<CharSequence> valueIterator = values.iterator();
        valueIterator.next();
        valueIterator.next();
        valueIterator.remove();
        assertMap(map, new int[]{2, 3}, new CharSequence[]{"2", "3"});
        assertEntrySet(entrySet, new int[]{2, 3}, new CharSequence[]{"2", "3"});
        assertKeySet(keySet, new int[]{2, 3});
        assertValues(values, new CharSequence[]{"2", "3"});

        map.close();
    }

    @Test
    public void entrySetRemoveAllReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        entrySet.removeAll(
                Arrays.asList(
                        new AbstractMap.SimpleEntry<Integer, CharSequence>(1, "1"),
                        new AbstractMap.SimpleEntry<Integer, CharSequence>(2, "2")
                )
        );
        assertMap(map, new int[]{3}, new CharSequence[]{"3"});
        assertEntrySet(entrySet, new int[]{3}, new CharSequence[]{"3"});
        assertKeySet(keySet, new int[]{3});
        assertValues(values, new CharSequence[]{"3"});

        map.close();
    }

    @Test
    public void keySetRemoveAllReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        keySet.removeAll(Arrays.asList(1, 2));
        assertMap(map, new int[]{3}, new CharSequence[]{"3"});
        assertEntrySet(entrySet, new int[]{3}, new CharSequence[]{"3"});
        assertKeySet(keySet, new int[]{3});
        assertValues(values, new CharSequence[]{"3"});

        map.close();
    }

    @Test
    public void valuesRemoveAllReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        values.removeAll(Arrays.asList("1", "2"));
        assertMap(map, new int[]{3}, new CharSequence[]{"3"});
        assertEntrySet(entrySet, new int[]{3}, new CharSequence[]{"3"});
        assertKeySet(keySet, new int[]{3});
        assertValues(values, new CharSequence[]{"3"});

        map.close();
    }

    @Test
    public void entrySetRetainAllReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        entrySet.retainAll(
                Arrays.asList(
                        new AbstractMap.SimpleEntry<Integer, CharSequence>(1, "1"),
                        new AbstractMap.SimpleEntry<Integer, CharSequence>(2, "2")
                )
        );
        assertMap(map, new int[]{2, 1}, new CharSequence[]{"2", "1"});
        assertEntrySet(entrySet, new int[]{2, 1}, new CharSequence[]{"2", "1"});
        assertKeySet(keySet, new int[]{2, 1});
        assertValues(values, new CharSequence[]{"2", "1"});

        map.close();
    }

    @Test
    public void keySetRetainAllReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        keySet.retainAll(Arrays.asList(1, 2));
        assertMap(map, new int[]{2, 1}, new CharSequence[]{"2", "1"});
        assertEntrySet(entrySet, new int[]{2, 1}, new CharSequence[]{"2", "1"});
        assertKeySet(keySet, new int[]{2, 1});
        assertValues(values, new CharSequence[]{"2", "1"});

        map.close();
    }

    @Test
    public void valuesRetainAllReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        values.retainAll(Arrays.asList("1", "2"));
        assertMap(map, new int[]{2, 1}, new CharSequence[]{"2", "1"});
        assertEntrySet(entrySet, new int[]{2, 1}, new CharSequence[]{"2", "1"});
        assertKeySet(keySet, new int[]{2, 1});
        assertValues(values, new CharSequence[]{"2", "1"});

        map.close();
    }

    @Test
    public void entrySetClearReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        entrySet.clear();
        org.junit.Assert.assertTrue(map.isEmpty());
        org.junit.Assert.assertTrue(entrySet.isEmpty());
        org.junit.Assert.assertTrue(keySet.isEmpty());
        org.junit.Assert.assertTrue(values.isEmpty());

        map.close();
    }

    @Test
    public void keySetClearReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        keySet.clear();
        org.junit.Assert.assertTrue(map.isEmpty());
        org.junit.Assert.assertTrue(entrySet.isEmpty());
        org.junit.Assert.assertTrue(keySet.isEmpty());
        org.junit.Assert.assertTrue(values.isEmpty());

        map.close();
    }

    @Test
    public void valuesClearReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(3);
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        values.clear();
        org.junit.Assert.assertTrue(map.isEmpty());
        org.junit.Assert.assertTrue(entrySet.isEmpty());
        org.junit.Assert.assertTrue(keySet.isEmpty());
        org.junit.Assert.assertTrue(values.isEmpty());

        map.close();
    }

    @Test
    public void clearMapViaEntryIteratorRemoves() throws IOException {
        int noOfElements = 16 * 1024;
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(noOfElements);

        int sum = 0;
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            it.next();
            it.remove();
            ++sum;
        }
        map.close();

        assertEquals(noOfElements, sum);
    }

    @Test
    public void clearMapViaKeyIteratorRemoves() throws IOException {
        int noOfElements = 16 * 1024;
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(noOfElements);

        Set<Integer> keys = new HashSet<Integer>();
        for (int i = 1; i <= noOfElements; i++) {
            keys.add(i);
        }

        int sum = 0;
        for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            keys.remove(key);
            it.remove();
            ++sum;
        }
        map.close();

        assertEquals(noOfElements, sum);
    }

    @Test
    public void clearMapViaValueIteratorRemoves() throws IOException {
        int noOfElements = 16 * 1024;
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(noOfElements);

        int sum = 0;
        for (Iterator it = map.values().iterator(); it.hasNext(); ) {
            it.next();
            it.remove();
            ++sum;
        }
        map.close();

        assertEquals(noOfElements, sum);
    }

    @Test
    public void entrySetValueReflectedInMapAndOtherViews() throws IOException {
        SharedHashMap<Integer, CharSequence> map = getViewTestMap(0);

        map.put(1, "A");
        Set<Map.Entry<Integer, CharSequence>> entrySet = map.entrySet();
        Set<Integer> keySet = map.keySet();
        Collection<CharSequence> values = map.values();

        assertMap(map, new int[] {1}, new CharSequence[] {"A"});
        assertEntrySet(entrySet, new int[]{1}, new CharSequence[]{"A"});
        assertKeySet(keySet, new int[]{1});
        assertValues(values, new String[]{"A"});

        entrySet.iterator().next().setValue("B");
        assertMap(map, new int[]{1}, new CharSequence[]{"B"});
        assertEntrySet(entrySet, new int[]{1}, new CharSequence[]{"B"});
        assertEntrySet(map.entrySet(), new int[]{1}, new CharSequence[]{"B"});
        assertKeySet(keySet, new int[]{1});
        assertKeySet(map.keySet(), new int[]{1});
        assertValues(values, new String[]{"B"});
        assertValues(map.values(), new String[]{"B"});

        map.close();
    }

    private SharedHashMap<Integer, CharSequence> getViewTestMap(int noOfElements) throws IOException {
        String TMP = System.getProperty("java.io.tmpdir");
        File file = new File(TMP + "/shm-remove-test");
        file.delete();
        file.deleteOnExit();
        int entries = 100 * 1000;
        SharedHashMap<Integer, CharSequence> map =
                new SharedHashMapBuilder()
                        .entries(entries)
                        .minSegments(16)
                        .entrySize(32)
                        .putReturnsNull(true)
                        .removeReturnsNull(true)
                        .create(file, Integer.class, CharSequence.class);

        int[] expectedKeys = new int[noOfElements];
        String[] expectedValues = new String[noOfElements];
        for (int i = 1; i <= noOfElements; i++) {
            String value = "" + i;
            map.put(i, value);
            expectedKeys[i - 1] = i;
            expectedValues[i - 1] = value;
        }

        return map;
    }
}
