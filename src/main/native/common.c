/****************************************************************************************
Revise Date: 20 Apr. 2014
*****************************************************************************************/

#include <common.h>


/******************************************************************************
 ** Generally-useful functions for JNI programming.
 *****************************************************************************/

/**
 *  Throws a RuntimeException, with either an explicit message or the message
 *  corresponding to the current system error value.
 */
void throw(JNIEnv* env, const char* msg)
{
    if (msg == NULL)
        msg = sys_errlist[errno];

    jclass xklass = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, xklass, msg);
}

void* addr_from_java(jlong addr) {
  // This assert fails in a variety of ways on 32-bit systems.
  // It is impossible to predict whether native code that converts
  // pointers to longs will sign-extend or zero-extend the addresses.
  //assert(addr == (uintptr_t)addr, "must not be odd high bits");
  return (void*)(uintptr_t)addr;
}

jlong addr_to_java(void* p) {
  assert(p == (void*)(uintptr_t)p);
  return (long)(uintptr_t)p;
}

