//For O_DIRECT declaration in linux
#if defined(linux)
#define _GNU_SOURCE
#endif

#include <direct.h>

#include <fcntl.h>
#include <stdio.h>
//Don't use the GNU's strerror_r variant
#ifndef _XOPEN_SOURCE
#define _XOPEN_SOURCE 600
#endif
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/fcntl.h>

#if (defined(sun) && (defined(__svr4__) || defined(__SVR4)))
#define solaris (defined(sun) && (defined(__svr4__) || defined(__SVR4)))
#endif

#if defined(linux) || defined(__AIX__) || defined(IRIX) || defined(IRIX64) || defined(Windows) || defined (__FreeBSD__)
int file_extra_flags = O_DIRECT;
#elif defined(TRU64)
int file_extra_flags = O_DIRECTIO;
#else
int file_extra_flags = 0;
#endif

/*
 * Class:     jrds_caching_FilePage
 * Method:    prepare_fd
 * Signature: (Ljava/lang/String;Ljava/io/FileDescriptor;Z)V
 */
JNIEXPORT void JNICALL Java_jrds_caching_FilePage_prepare_1fd(JNIEnv *env, jclass _ignore, jstring filename, jobject fdobj, jboolean readOnly) {
    jfieldID field_fd;
    jmethodID const_fdesc;
    jclass class_fdesc;
    int fd, errcode;
    char *fname;
    int flags = file_extra_flags;
    
    //Prepare the introspection
    class_fdesc = (*env)->FindClass(env, "java/io/FileDescriptor");
    if (class_fdesc == NULL) return;
    field_fd = (*env)->GetFieldID(env, class_fdesc, "fd", "I");
    if (field_fd == NULL) return;
    
    fname = (*env)->GetStringUTFChars(env, filename, NULL);
    
    if(readOnly == JNI_TRUE)
        flags |= O_RDONLY;
    else
        flags |= O_RDWR |O_CREAT;
    
    fd = open(fname, flags, 0666);
    check_error(env, fd, "open failed");
    
#if defined(solaris)
    if(fd >= 0) {
        errcode = directio(fd, DIRECTIO_ON);
        check_error(env, errcode, "directio mode");
    }
#endif
    
    (*env)->ReleaseStringUTFChars(env, filename, fname);

    // poke the "fd" field with the file descriptor
    if(fd >= 0)
        (*env)->SetIntField(env, fdobj, field_fd, fd);
}

int check_error(JNIEnv *env, int val, char* context) {
    const char buf[1024];
    const char error_buf[1024];
    jclass class_fdesc, class_ioex;

    if (val < 0) {
        class_ioex = (*env)->FindClass(env, "java/io/IOException");
        if (class_ioex == NULL) return;

        // context returned an error. Throw an IOException with the error string
        strerror_r(errno, error_buf, 1024);
        snprintf(buf, 1024, "open: %s (%d)", error_buf, errno);
        (*env)->ThrowNew(env, class_ioex, buf);
        return val;
    }
    
}

#define PAGESIZE jrds_caching_PageCache_PAGESIZE

/*
 * Class:     jrds_caching_PageCache
 * Method:    alignOffset
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_jrds_caching_PageCache_getAlignOffset(JNIEnv *env, jclass _ignore, jobject pageCacheBuffer) {
    void * pointer;
    void * pointer_aligned;
    
    pointer = (*env)->GetDirectBufferAddress(env, pageCacheBuffer);
    pointer_aligned = (((unsigned long)pointer + PAGESIZE - 1) & (~(PAGESIZE-1)));
    return pointer_aligned - pointer;
}

