#include <direct.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/fcntl.h>

int file_extra_flags = 0;
#if defined(linux) || defined(__AIX__) || defined(IRIX) || defined(IRIX64) || defined(Windows) || defined (__FreeBSD__)
file_extra_flags |=O_DIRECT;
#endif
#if defined(TRU64)
file_extra_flags |=O_DIRECTIO;
#endif

JNIEXPORT void JNICALL Java_jrds_caching_RrdCachedFileBackend_prepare_1fd(JNIEnv *env, jclass _ignore, jstring filename, jobject fdobj, jboolean readOnly) {
    jfieldID field_fd;
    jmethodID const_fdesc;
    jclass class_fdesc, class_ioex;
    int fd;
    char *fname;
    
    //Prepare the introspection
    class_ioex = (*env)->FindClass(env, "java/io/IOException");
    if (class_ioex == NULL) return;
    class_fdesc = (*env)->FindClass(env, "java/io/FileDescriptor");
    if (class_fdesc == NULL) return;
    field_fd = (*env)->GetFieldID(env, class_fdesc, "fd", "I");
    if (field_fd == NULL) return;
    
    fname = (*env)->GetStringUTFChars(env, filename, NULL);
    
    fd = open(fname, O_RDWR | file_extra_flags);
#if defined(solaris)
    int directio(fd, DIRECTIO_ON);
#endif
    
    (*env)->ReleaseStringUTFChars(env, filename, fname);
    
    if (fd < 0) {
        // open returned an error. Throw an IOException with the error string
        char buf[1024];
        sprintf(buf, "open: %s", strerror(errno));
        (*env)->ThrowNew(env, class_ioex, buf);
        return;
    }
    
    // poke the "fd" field with the file descriptor
    (*env)->SetIntField(env, fdobj, field_fd, fd);
}

