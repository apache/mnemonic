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

/**
 *
 *
 */

public interface MemoryNonVolatileEntity<ALLOC_PMem3C93D24F59 extends CommonPersistAllocator<ALLOC_PMem3C93D24F59>> {

  void initializeNonVolatileEntity(ALLOC_PMem3C93D24F59 allocator, EntityFactoryProxy[] efproxys,
      GenericField.GType[] gfields, boolean autoreclaim);

  void createNonVolatileEntity(ALLOC_PMem3C93D24F59 allocator, EntityFactoryProxy[] efproxys,
      GenericField.GType[] gfields, boolean autoreclaim) throws OutOfPersistentMemory;

  void restoreNonVolatileEntity(ALLOC_PMem3C93D24F59 allocator, EntityFactoryProxy[] efproxys,
      GenericField.GType[] gfields, long phandler, boolean autoreclaim) throws RetrieveNonVolatileEntityError;

}
