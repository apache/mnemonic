/****************************************************************************************
Revise Date: 20 Apr. 2014
*****************************************************************************************/

#include "com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl.h"

#include <libvmem.h>

static VMEM **g_vmp_ptr = NULL;
static size_t g_vmp_count = 0;

static pthread_mutex_t *g_vmem_mutex_ptr = NULL;

static pthread_rwlock_t g_vmem_rwlock = PTHREAD_RWLOCK_INITIALIZER;

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

JNIEXPORT
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl_nallocate(
             JNIEnv* env,
             jobject this, jlong id,
             jlong size, jboolean initzero)
{
	pthread_rwlock_rdlock(&g_vmem_rwlock);
	pthread_mutex_lock(g_vmem_mutex_ptr + id);
    void* nativebuf = initzero ? vmem_calloc(*(g_vmp_ptr + id), 1, size) : vmem_malloc(*(g_vmp_ptr + id), size);
    pthread_mutex_unlock(g_vmem_mutex_ptr + id);
    pthread_rwlock_unlock(&g_vmem_rwlock);
    return addr_to_java(nativebuf);
}

JNIEXPORT
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl_nreallocate(
             JNIEnv* env,
             jobject this, jlong id,
             jlong address,
             jlong size, jboolean initzero)
{
	pthread_rwlock_rdlock(&g_vmem_rwlock);
	pthread_mutex_lock(g_vmem_mutex_ptr + id);

    void* p = addr_from_java(address);
    
    void* nativebuf = vmem_realloc(*(g_vmp_ptr + id), p, size);
    
    pthread_mutex_unlock(g_vmem_mutex_ptr + id);
    pthread_rwlock_unlock(&g_vmem_rwlock);
    return addr_to_java(nativebuf);
}

JNIEXPORT
void JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl_nfree(
             JNIEnv* env,
             jobject this, jlong id,
             jlong address)
{
	pthread_rwlock_rdlock(&g_vmem_rwlock);
	pthread_mutex_lock(g_vmem_mutex_ptr + id);
    void* nativebuf = addr_from_java(address);
    if (nativebuf != NULL)
        vmem_free(*(g_vmp_ptr + id), nativebuf);
    pthread_mutex_unlock(g_vmem_mutex_ptr + id);
    pthread_rwlock_unlock(&g_vmem_rwlock);
}

JNIEXPORT
void JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl_nsync(
             JNIEnv* env,
             jobject this, jlong id)
{
}

JNIEXPORT
jobject JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl_ncreateByteBuffer(
            JNIEnv *env, jobject this, jlong id, jlong size)
{
	pthread_rwlock_rdlock(&g_vmem_rwlock);
	pthread_mutex_lock(g_vmem_mutex_ptr + id);
	jobject ret = NULL;
    void* nativebuf = vmem_malloc(*(g_vmp_ptr + id), size);
    ret = NULL != nativebuf ? (*env)->NewDirectByteBuffer(env, nativebuf, size) : NULL;
    pthread_mutex_unlock(g_vmem_mutex_ptr + id);
    pthread_rwlock_unlock(&g_vmem_rwlock);
    return ret;
}

JNIEXPORT
jobject JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl_nresizeByteBuffer(
            JNIEnv *env, jobject this, jlong id, jobject bytebuf, jlong size)
{
	pthread_rwlock_rdlock(&g_vmem_rwlock);
	pthread_mutex_lock(g_vmem_mutex_ptr + id);
	jobject ret = NULL;
    if (NULL != bytebuf) {
        void* nativebuf = (void*)(*env)->GetDirectBufferAddress(env, bytebuf);
        if (nativebuf != NULL) {
            nativebuf = vmem_realloc(*(g_vmp_ptr + id), nativebuf, size);
            ret = NULL != nativebuf ? (*env)->NewDirectByteBuffer(env, nativebuf, size) : NULL;
        }
    }
    pthread_mutex_unlock(g_vmem_mutex_ptr + id);
    pthread_rwlock_unlock(&g_vmem_rwlock);
    return ret;
}

JNIEXPORT
void JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl_ndestroyByteBuffer(
            JNIEnv *env, jobject this, jlong id, jobject bytebuf)
{
	pthread_rwlock_rdlock(&g_vmem_rwlock);
	pthread_mutex_lock(g_vmem_mutex_ptr + id);
    if (NULL != bytebuf) {
        void* nativebuf = (void*)(*env)->GetDirectBufferAddress(env, bytebuf);
        if (nativebuf != NULL) {
            vmem_free(*(g_vmp_ptr + id), nativebuf);
        }
    }
    pthread_mutex_unlock(g_vmem_mutex_ptr + id);
    pthread_rwlock_unlock(&g_vmem_rwlock);
}

JNIEXPORT 
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl_ninit
  (JNIEnv *env, jclass this, jlong capacity, jstring pathname, jboolean isnew)
{
   pthread_rwlock_wrlock(&g_vmem_rwlock);
   size_t ret = -1;
   VMEM *vmp = NULL;
   const char* mpathname = (*env)->GetStringUTFChars(env, pathname, NULL);
   if (NULL == mpathname) {
	  pthread_rwlock_unlock(&g_vmem_rwlock);
      throw(env, "Big memory path not specified!");
   }
   if ((vmp = vmem_create(mpathname, capacity)) == NULL) {
	  pthread_rwlock_unlock(&g_vmem_rwlock);
      throw(env, "Big memory init failure!");
   }
   g_vmp_ptr = realloc(g_vmp_ptr, (g_vmp_count + 1) * sizeof(VMEM*));
   g_vmem_mutex_ptr =
		   realloc(g_vmem_mutex_ptr, (g_vmp_count + 1) * sizeof(pthread_mutex_t));
   if (NULL != g_vmp_ptr && NULL != g_vmem_mutex_ptr) {
      g_vmp_ptr[g_vmp_count] = vmp;
      pthread_mutex_init(g_vmem_mutex_ptr + g_vmp_count, NULL);
      ret = g_vmp_count;
      g_vmp_count++;
   } else {
	  pthread_rwlock_unlock(&g_vmem_rwlock);
      throw(env, "Big memory init Out of memory!");
   }
   pthread_rwlock_unlock(&g_vmem_rwlock);
   return ret; 
}

JNIEXPORT 
void JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_VMemServiceImpl_nclose
  (JNIEnv *env, jobject this, jlong id)
{
	pthread_rwlock_rdlock(&g_vmem_rwlock);
	pthread_mutex_lock(g_vmem_mutex_ptr + id);

    pthread_mutex_unlock(g_vmem_mutex_ptr + id);
    pthread_rwlock_unlock(&g_vmem_rwlock);
}


__attribute__((destructor))  void fini(void)
{
   int i;
   if (NULL != g_vmp_ptr) {
	   for (i = 0; i < g_vmp_count; ++i) {
		   if (NULL != *(g_vmp_ptr + i)){
		     /* vmem_close(*(g_vmp_ptr + i)); undefined function */
				*(g_vmp_ptr + i) = NULL;
				pthread_mutex_destroy(g_vmem_mutex_ptr + i);
		   }
	   }
       free(g_vmp_ptr);
       g_vmp_ptr = NULL;
       free(g_vmem_mutex_ptr);
       g_vmem_mutex_ptr = NULL;
       g_vmp_count = 0;
   }
}
