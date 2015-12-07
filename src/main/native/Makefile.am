CFLAGS= -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
ACLOCAL_AMFLAGS=-I m4

lib_LTLIBRARIES=libbigdatamem.la libbigdatapmem.la

libbigdatamem_la_SOURCES=common.c com_intel_bigdatamem_BigDataMemAllocator.c
libbigdatamem_la_LDFLAGS= -shared -lvmem -lpthread

libbigdatapmem_la_SOURCES=common.c com_intel_bigdatamem_BigDataPMemAllocator.c
libbigdatapmem_la_LDFLAGS= -shared -lpmalloc -lpthread
