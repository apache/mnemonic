/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.lang3.RandomUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@SuppressWarnings("restriction")
public class DurablePersonRefBreakNGTest {
    public static String uri = "./pobj_person_refbreak.dat";
    private long cKEYCAPACITY;
    private long pic_checksum;
    private long fp_checksum;
    private Random rand;
    @SuppressWarnings({"restriction", "UseOfSunClasses"})
    private sun.misc.Unsafe unsafe;

    protected DurableBuffer<NonVolatileMemAllocator>
    genuptBuffer(NonVolatileMemAllocator act, Checksum cs, int size) {
        DurableBuffer<NonVolatileMemAllocator> ret = null;
        ret = act.createBuffer(size, false);
        if (null == ret) {
            throw new OutOfHybridMemory("Create Durable Buffer Failed.");
        }
        ret.get().clear();
        byte[] rdbytes = RandomUtils.nextBytes(size);
        Assert.assertNotNull(rdbytes);
        ret.get().put(rdbytes);
        cs.update(rdbytes, 0, rdbytes.length);
        return ret;
    }

    protected DurableChunk<NonVolatileMemAllocator>
    genuptChunk(NonVolatileMemAllocator act, Checksum cs, long size) {
        DurableChunk<NonVolatileMemAllocator> ret = null;
        ret = act.createChunk(size, false);
        if (null == ret) {
            throw new OutOfHybridMemory("Create Durable Chunk Failed.");
        }
        byte b;
        for (int i = 0; i < ret.getSize(); ++i) {
            b = (byte) rand.nextInt(255);
            unsafe.putByte(ret.get() + i, b);
            cs.update(b);
        }
        return ret;
    }

    protected int genRandSize() {
        return rand.nextInt(1024 * 1024) + 1024 * 1024;
    }

    @BeforeClass
    public void setup() throws Exception {
        rand = Utils.createRandom();
        unsafe = Utils.getUnsafe();
    }

    @Test(expectedExceptions = { OutOfHybridMemory.class })
    public void testGenPeople() throws OutOfHybridMemory, RetrieveDurableEntityError {
        Random rand = Utils.createRandom();
        NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
                1024L * 1024 * 1024, uri, true);
        cKEYCAPACITY = act.handlerCapacity();
        act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
            @Override
            public boolean reclaim(ByteBuffer mres, Long sz) {
                System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
                        null == sz ? "NULL" : sz.toString()));
                return false;
            }
        });
        act.setChunkReclaimer(new Reclaim<Long>() {
            @Override
            public boolean reclaim(Long mres, Long sz) {
                System.out.println(String.format("Reclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
                        null == sz ? "NULL" : sz.toString()));
                return false;
            }
        });

        for (long i = 0; i < cKEYCAPACITY; ++i) {
            act.setHandler(i, 0L);
        }

        Person<Integer> mother;
        Person<Integer> person;

        Checksum pic_cs = new CRC32();
        pic_cs.reset();
        Checksum fp_cs = new CRC32();
        fp_cs.reset();

        long keyidx = 0;
        long val;

        try {
            while (true) {
                // if (keyidx >= KEYCAPACITY) break;

                keyidx %= cKEYCAPACITY;

                System.out.printf("************ Generating People on Key %d ***********\n", keyidx);

                val = act.getHandler(keyidx);
                if (0L != val) {
                    PersonFactory.restore(act, val, true);
                }
                person = PersonFactory.create(act);
                person.setAge((short) rand.nextInt(50));
                person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
                person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
                person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
                person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);

                person.setPicture(genuptBuffer(act, pic_cs, genRandSize()), true);
                person.setPreference(genuptChunk(act, fp_cs, genRandSize()), true);

                act.setHandler(keyidx, person.getHandler());
                pic_checksum = pic_cs.getValue();
                fp_checksum = fp_cs.getValue();

                for (int deep = 0; deep < rand.nextInt(100); ++deep) {

                    mother = PersonFactory.create(act);
                    mother.setAge((short) (50 + rand.nextInt(50)));
                    mother.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
                    mother.setPicture(genuptBuffer(act, pic_cs, genRandSize()), true);
                    mother.setPreference(genuptChunk(act, fp_cs, genRandSize()), true);

                    person.setMother(mother, true);
                    pic_checksum = pic_cs.getValue();
                    fp_checksum = fp_cs.getValue();

                    person = mother;

                }
                ++keyidx;
            }
        } finally {
            act.close();
        }
    }

    @Test(dependsOnMethods = { "testGenPeople" })
    public void testCheckPeople() throws RetrieveDurableEntityError {
        NonVolatileMemAllocator act = new NonVolatileMemAllocator(Utils.getNonVolatileMemoryAllocatorService("pmalloc"),
                1024 * 1024 * 8, uri, false);
        act.setBufferReclaimer(new Reclaim<ByteBuffer>() {
            @Override
            public boolean reclaim(ByteBuffer mres, Long sz) {
                System.out.println(String.format("Reclaim Memory Buffer: %X  Size: %s", System.identityHashCode(mres),
                        null == sz ? "NULL" : sz.toString()));
                return false;
            }
        });
        act.setChunkReclaimer(new Reclaim<Long>() {
            @Override
            public boolean reclaim(Long mres, Long sz) {
                System.out.println(String.format("Reclaim Memory Chunk: %X  Size: %s", System.identityHashCode(mres),
                        null == sz ? "NULL" : sz.toString()));
                return false;
            }
        });

        Checksum pic_cs = new CRC32();
        pic_cs.reset();
        Checksum fp_cs = new CRC32();
        fp_cs.reset();
        long size;
        byte[] buf;

        long val;
        for (long i = 0; i < cKEYCAPACITY; ++i) {
            System.out.printf("----------Key %d--------------\n", i);
            val = act.getHandler(i);
            if (0L == val) {
                break;
            }
            // autoreclaim must set false for reference breaking operation
            Person<Integer> person = PersonFactory.restore(act, val, false);
            Person<Integer> init_person = person;
            while (null != person) {
                person = person.getMother();
            }
            init_person.refbreak(); // intend to release all marked linked objects and depends on autoreclaim flag
            person = init_person;
            while (null != person) {
                person.testOutput();
                person.getPicture().get().clear();
                buf = new byte[person.getPicture().get().capacity()];
                person.getPicture().get().get(buf);
                pic_cs.update(buf, 0, buf.length);
                byte b;
                for (int j = 0; j < person.getPreference().getSize(); ++j) {
                    b = unsafe.getByte(person.getPreference().get() + j);
                    fp_cs.update(b);
                }
                person = person.getMother();
            }
        }

        act.close();
        Assert.assertEquals(pic_cs.getValue(), pic_checksum);
        Assert.assertEquals(fp_cs.getValue(), fp_checksum);
    }
}
