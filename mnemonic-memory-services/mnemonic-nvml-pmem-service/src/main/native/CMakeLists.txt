#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cmake_minimum_required(VERSION 3.12)
project(pmemallocator)

configure_file (
  "${PROJECT_SOURCE_DIR}/config.h.in"
    "${PROJECT_BINARY_DIR}/config.h"
    )
include_directories(${PROJECT_BINARY_DIR})

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/../dist/native")
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/CMakeModules/")
set(pmemallocator_VERSION_MAJOR 0)
set(pmemallocator_VERSION_MINOR 0)
set(CMAKE_BUILD_TYPE "Release")
set(CMAKE_C_FLAGS_DEBUG "$ENV{CFLAGS} -O0 -Wall -rdynamic -g -ggdb")
set(CMAKE_C_FLAGS_RELEASE "$ENV{CFLAGS} -O3 -Wall")

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

find_package(Threads REQUIRED)
include_directories(${CMAKE_THREAD_LIBS_INIT})

find_library(LIBPMEM_LIBRARIES NAMES libpmem.a libpmem.so)
if (NOT LIBPMEM_LIBRARIES)
   message(FATAL_ERROR "not found pmem library")
endif (NOT LIBPMEM_LIBRARIES)

find_library(LIBPMEMOBJ_LIBRARIES NAMES libpmemobj.a libpmemobj.so)
if (NOT LIBPMEMOBJ_LIBRARIES)
   message(FATAL_ERROR "not found pmemobj library")
endif (NOT LIBPMEMOBJ_LIBRARIES)

add_library(pmemallocator SHARED common.c org_apache_mnemonic_service_memory_internal_PMemServiceImpl.c)
target_include_directories(pmemallocator PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(pmemallocator ${LIBPMEMOBJ_LIBRARIES} ${LIBPMEM_LIBRARIES} ${CMAKE_THREAD_LIBS_INIT})

include (InstallRequiredSystemLibraries)
set (CPACK_RESOURCE_FILE_LICENSE
  "${CMAKE_CURRENT_SOURCE_DIR}/LICENSE")
set (CPACK_PACKAGE_VERSION_MAJOR "${pmemallocator_VERSION_MAJOR}")
set (CPACK_PACKAGE_VERSION_MINOR "${pmemallocator_VERSION_MINOR}")
include (CPack)
