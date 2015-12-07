AC_INIT([libbigdatamem], [0.3], [gang1.wang@intel.com])
AM_INIT_AUTOMAKE([-Wall -Werror foreign])
AC_CONFIG_MACRO_DIR([m4])
if test -z $CFLAGS; then
    CFLAGS="-O0 -ggdb3"
fi
AC_GNU_SOURCE
AC_PROG_CC_C99
AC_PROG_LIBTOOL
AC_CONFIG_HEADERS([config.h])
AC_CONFIG_FILES([Makefile])
AC_CHECK_LIB([vmem],[vmem_create],[],[AC_MSG_ERROR(["no libvmem found"])])
AC_CHECK_LIB([pmalloc],[pmopen],[],[AC_MSG_ERROR(["no libpmalloc found"])])
AC_OUTPUT
