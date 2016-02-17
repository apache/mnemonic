/****************************************************************************************
Revise Date: 20 Apr. 2014
*****************************************************************************************/

#include "com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl.h"

#include <pmalloc.h>

typedef struct {
	//size_t size;
	jlong size;
} PMBHeader;

#define PMBHSZ (sizeof(PMBHeader))

static void **g_pmp_ptr = NULL;
static size_t g_pmp_count = 0;

static pthread_mutex_t *g_pmalloc_mutex_ptr = NULL;

static pthread_rwlock_t g_pmp_rwlock = PTHREAD_RWLOCK_INITIALIZER;

/******************************************************************************
 ** JNI implementations
 *****************************************************************************/

JNIEXPORT
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nallocate(
             JNIEnv* env,
             jobject this, jlong id,
             jlong size, jboolean initzero)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	pthread_mutex_lock(g_pmalloc_mutex_ptr + id);
	jlong ret = 0L;
	void *md = *(g_pmp_ptr + id);
    void* nativebuf = initzero ? pmcalloc(md, 1, size + PMBHSZ) : pmalloc(md, size + PMBHSZ);
    if (NULL != nativebuf) {
    	((PMBHeader *)nativebuf)->size = size + PMBHSZ;
    	ret = addr_to_java(nativebuf + PMBHSZ);
//    	fprintf(stderr, "### nallocate size: %lld, %X, header size: %ld ### \n",
//    			((PMBHeader *)nativebuf)->size, nativebuf-b_addr(*(g_pmp_ptr + id)), PMBHSZ);
    }
    pthread_mutex_unlock(g_pmalloc_mutex_ptr + id);
    pthread_rwlock_unlock(&g_pmp_rwlock);
    return ret;
}

JNIEXPORT
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nreallocate(
             JNIEnv* env,
             jobject this, jlong id,
             jlong address,
             jlong size, jboolean initzero)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	pthread_mutex_lock(g_pmalloc_mutex_ptr + id);
	jlong ret = 0L;
	void *md = *(g_pmp_ptr + id);
	void* nativebuf = NULL;
	void* p = addr_from_java(address);
	if (NULL != p) {
	    nativebuf = pmrealloc(md, p - PMBHSZ, size + PMBHSZ);
	} else {
	    nativebuf = initzero ? pmcalloc(md, 1, size + PMBHSZ) : pmalloc(md, size + PMBHSZ);
	}
	if (nativebuf != NULL) {
		((PMBHeader *)nativebuf)->size = size + PMBHSZ;
		ret = addr_to_java(nativebuf + PMBHSZ);
	}
	pthread_mutex_unlock(g_pmalloc_mutex_ptr + id);
	pthread_rwlock_unlock(&g_pmp_rwlock);
	return ret;
}

JNIEXPORT
void JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nfree(
             JNIEnv* env,
             jobject this, jlong id,
             jlong address)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	pthread_mutex_lock(g_pmalloc_mutex_ptr + id);
	//fprintf(stderr, "nfree Get Called %ld, %X\n", id, address);
	void *md = *(g_pmp_ptr + id);
    void* nativebuf = addr_from_java(address);
    if (nativebuf != NULL) {
//        fprintf(stderr, "### nfree size: %lld, %X ###, header size: %ld \n",
//        		((PMBHeader *)(nativebuf - PMBHSZ))->size, nativebuf - PMBHSZ-b_addr(*(g_pmp_ptr + id)), PMBHSZ);
        pmfree(md, nativebuf - PMBHSZ);
    }
    pthread_mutex_unlock(g_pmalloc_mutex_ptr + id);
    pthread_rwlock_unlock(&g_pmp_rwlock);
}

JNIEXPORT
void JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nsync(
             JNIEnv* env,
             jobject this, jlong id)
{
}

JNIEXPORT
jobject JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_ncreateByteBuffer(
            JNIEnv *env, jobject this, jlong id, jlong size)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	pthread_mutex_lock(g_pmalloc_mutex_ptr + id);
	jobject ret = NULL;
	void *md = *(g_pmp_ptr + id);
    void* nativebuf = pmalloc(md, size + PMBHSZ);
    if (NULL != nativebuf) {
    	((PMBHeader *)nativebuf)->size = size + PMBHSZ;
    	ret = (*env)->NewDirectByteBuffer(env, nativebuf + PMBHSZ, size);
//    	fprintf(stderr, "### ncreateByteBuffer size: %lld, %X ###, header size: %ld \n",
//    			((PMBHeader *)nativebuf)->size, nativebuf-b_addr(*(g_pmp_ptr + id)), PMBHSZ);
    }
    pthread_mutex_unlock(g_pmalloc_mutex_ptr + id);
    pthread_rwlock_unlock(&g_pmp_rwlock);
    return ret;
}

JNIEXPORT
jobject JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nretrieveByteBuffer(
            JNIEnv *env, jobject this, jlong id, jlong e_addr)
{
	jobject ret = NULL;
	void* p = addr_from_java(e_addr);
    if (NULL != p) {
    	void* nativebuf = p - PMBHSZ;
    	ret = (*env)->NewDirectByteBuffer(env, p, ((PMBHeader *)nativebuf)->size - PMBHSZ);
    }
    return ret;
}

JNIEXPORT
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nretrieveSize(
            JNIEnv *env, jobject this, jlong id, jlong e_addr)
{
	jlong ret = 0L;
	void* p = addr_from_java(e_addr);
    if (NULL != p) {
        void* nativebuf = p - PMBHSZ;
        ret = ((PMBHeader *)nativebuf)->size - PMBHSZ;
//        fprintf(stderr, "### nretrieveSize size: %lld, %X ###, header size: %ld \n",
//        		((PMBHeader *)nativebuf)->size, nativebuf-b_addr(*(g_pmp_ptr + id)), PMBHSZ);
    }
    return ret;
}

JNIEXPORT
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_ngetByteBufferHandler(
            JNIEnv *env, jobject this, jlong id, jobject bytebuf)
{
//	fprintf(stderr, "ngetByteBufferAddress Get Called %X, %X\n", env, bytebuf);
	jlong ret = 0L;
    if (NULL != bytebuf) {
        void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
//    	fprintf(stderr, "ngetByteBufferAddress Get Native address %X\n", nativebuf);
        ret = addr_to_java(nativebuf);
    }
//    fprintf(stderr, "ngetByteBufferAddress returned address %016lx\n", ret);
    return ret;
}

JNIEXPORT
jobject JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nresizeByteBuffer(
            JNIEnv *env, jobject this, jlong id, jobject bytebuf, jlong size)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	pthread_mutex_lock(g_pmalloc_mutex_ptr + id);
	jobject ret = NULL;
	void *md = *(g_pmp_ptr + id);
    if (NULL != bytebuf) {
        void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
        if (nativebuf != NULL) {
            nativebuf = pmrealloc(md, nativebuf - PMBHSZ, size + PMBHSZ);
            if (NULL != nativebuf) {
            	((PMBHeader *)nativebuf)->size = size + PMBHSZ;
            	ret = (*env)->NewDirectByteBuffer(env, nativebuf + PMBHSZ, size);
            }
        }
    }
    pthread_mutex_unlock(g_pmalloc_mutex_ptr + id);
    pthread_rwlock_unlock(&g_pmp_rwlock);
    return ret;
}

JNIEXPORT
void JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_ndestroyByteBuffer(
            JNIEnv *env, jobject this, jlong id, jobject bytebuf)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	pthread_mutex_lock(g_pmalloc_mutex_ptr + id);
	void *md = *(g_pmp_ptr + id);
    if (NULL != bytebuf) {
        void* nativebuf = (*env)->GetDirectBufferAddress(env, bytebuf);
        if (nativebuf != NULL) {
//            fprintf(stderr, "### ndestroyByteBuffer size: %lld, %X, header size: %ld ### \n",
//            		((PMBHeader *)(nativebuf - PMBHSZ))->size, nativebuf - PMBHSZ -b_addr(*(g_pmp_ptr + id)), PMBHSZ);
            pmfree(md, nativebuf - PMBHSZ);
        }
    }
    pthread_mutex_unlock(g_pmalloc_mutex_ptr + id);
    pthread_rwlock_unlock(&g_pmp_rwlock);
}

JNIEXPORT
void JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nsetHandler(
            JNIEnv *env, jobject this, jlong id, jlong key, jlong value)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	pthread_mutex_lock(g_pmalloc_mutex_ptr + id);
	void *md = *(g_pmp_ptr + id);
	if (id < PMALLOC_KEYS && id >= 0) {
		pmalloc_setkey(md, key, (void*)value);
	}
	pthread_mutex_unlock(g_pmalloc_mutex_ptr + id);
	pthread_rwlock_unlock(&g_pmp_rwlock);
}

JNIEXPORT
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_ngetHandler(
            JNIEnv *env, jobject this, jlong id, jlong key)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	pthread_mutex_lock(g_pmalloc_mutex_ptr + id);
	void *md = *(g_pmp_ptr + id);
	jlong ret = (id < PMALLOC_KEYS && id >= 0) ? (long)pmalloc_getkey(md, key) : 0;
	pthread_mutex_unlock(g_pmalloc_mutex_ptr + id);
	pthread_rwlock_unlock(&g_pmp_rwlock);
	return ret;
}

JNIEXPORT
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nhandlerCapacity(
            JNIEnv *env, jobject this)
{
	return  PMALLOC_KEYS;
}

JNIEXPORT
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_ngetBaseAddress(
            JNIEnv *env, jobject this, jlong id)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	void *md = *(g_pmp_ptr + id);
	jlong ret = (long)b_addr(md);
	pthread_rwlock_unlock(&g_pmp_rwlock);
	return ret;
}

JNIEXPORT 
jlong JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_ninit
  (JNIEnv *env, jclass this, jlong capacity, jstring pathname, jboolean isnew)
{
   pthread_rwlock_wrlock(&g_pmp_rwlock);
   size_t ret = -1;
   void *md = NULL;
   const char* mpathname = (*env)->GetStringUTFChars(env, pathname, NULL);
   if (NULL == mpathname) {
	   pthread_rwlock_unlock(&g_pmp_rwlock);
      throw(env, "Big memory path not specified!");
   }
   if ((md = pmopen(mpathname, NULL, capacity)) == NULL) {
	   pthread_rwlock_unlock(&g_pmp_rwlock);
      throw(env, "Big memory init failure!");
   }
   (*env)->ReleaseStringUTFChars(env, pathname, mpathname);
   g_pmp_ptr = realloc(g_pmp_ptr, (g_pmp_count + 1) * sizeof(void*));
   g_pmalloc_mutex_ptr =
		   realloc(g_pmalloc_mutex_ptr, (g_pmp_count + 1) * sizeof(pthread_mutex_t));
   if (NULL != g_pmp_ptr && NULL != g_pmalloc_mutex_ptr) {
      *(g_pmp_ptr + g_pmp_count) = md;
      pthread_mutex_init(g_pmalloc_mutex_ptr + g_pmp_count, NULL);
      ret = g_pmp_count;
      ++g_pmp_count;
   } else {
      pthread_rwlock_unlock(&g_pmp_rwlock);
      throw(env, "Big memory init Out of memory!");
   }
   pthread_rwlock_unlock(&g_pmp_rwlock);
   return ret; 
}

JNIEXPORT 
void JNICALL Java_com_intel_mnemonic_service_allocatorservice_internal_PMallocServiceImpl_nclose
  (JNIEnv *env, jobject this, jlong id)
{
	pthread_rwlock_rdlock(&g_pmp_rwlock);
	pthread_mutex_lock(g_pmalloc_mutex_ptr + id);
	void *md = *(g_pmp_ptr + id);
	pmclose(md);
	*(g_pmp_ptr + id) = NULL;
	pthread_mutex_unlock(g_pmalloc_mutex_ptr + id);
	pthread_mutex_destroy(g_pmalloc_mutex_ptr + id);
	pthread_rwlock_unlock(&g_pmp_rwlock);
}


__attribute__((destructor))  void fini(void)
{
   int i;
   if (NULL != g_pmp_ptr) {
	   for (i = 0; i < g_pmp_count; ++i) {
		   if (NULL != *(g_pmp_ptr + i)){
				pmclose(*(g_pmp_ptr + i));
				*(g_pmp_ptr + i) = NULL;
				pthread_mutex_destroy(g_pmalloc_mutex_ptr + i);
		   }
	   }
       free(g_pmp_ptr);
       g_pmp_ptr = NULL;
       free(g_pmalloc_mutex_ptr);
       g_pmalloc_mutex_ptr = NULL;
       g_pmp_count = 0;
   }
   pthread_rwlock_destroy(&g_pmp_rwlock);
}
