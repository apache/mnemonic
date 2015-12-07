#ifndef _COMMON_H
#define _COMMON_H
#ifdef __cplusplus
extern "C" {
#endif

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <sys/stat.h>
#include <stdint.h>
#include <assert.h>
#include <pthread.h>
#include <jni.h>

void throw(JNIEnv* env, const char* msg);

void* addr_from_java(jlong addr);

jlong addr_to_java(void* p);

#ifdef __cplusplus
}
#endif
#endif
