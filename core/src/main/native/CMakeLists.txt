cmake_minimum_required(VERSION 2.8)
project(bigdatamem)

configure_file (
  "${PROJECT_SOURCE_DIR}/config.h.in"
    "${PROJECT_BINARY_DIR}/config.h"
    )
include_directories(${PROJECT_BINARY_DIR})

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/CMakeModules/")
set(pmalloc_VERSION_MAJOR 0)
set(pmalloc_VERSION_MINOR 0)
set(CMAKE_BUILD_TYPE "Relase")
set(CMAKE_C_FLAGS_DEBUG "$ENV{CFLAGS} -O0 -Wall -g -ggdb")
set(CMAKE_C_FLAGS_RELEASE "$ENV{CFLAGS} -O3 -Wall")

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

find_package(Threads REQUIRED)
include_directories(${CMAKE_THREAD_LIBS_INIT})

find_library(LIBVMEM_LIBRARIES vmem)
if (NOT LIBVMEM_LIBRARIES)
   message(FATAL_ERROR "not found vmem library")
endif (NOT LIBVMEM_LIBRARIES)

find_library(LIBPMALLOC_LIBRARIES pmalloc)
if (NOT LIBPMALLOC_LIBRARIES)
   message(FATAL_ERROR "not found pmalloc library")
endif (NOT LIBPMALLOC_LIBRARIES)

add_library(bigdatamem SHARED common.c com_intel_bigdatamem_BigDataMemAllocator.c)
target_include_directories(bigdatamem PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

add_library(bigdatapmem SHARED common.c com_intel_bigdatamem_BigDataPMemAllocator.c)
target_include_directories(bigdatapmem PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

include (InstallRequiredSystemLibraries)
set (CPACK_RESOURCE_FILE_LICENSE
  "${CMAKE_CURRENT_SOURCE_DIR}/LICENSE")
set (CPACK_PACKAGE_VERSION_MAJOR "${pmalloc_VERSION_MAJOR}")
set (CPACK_PACKAGE_VERSION_MINOR "${pmalloc_VERSION_MINOR}")
include (CPack)
