#include <jni.h>

extern jbyte blob[];
extern int blob_size;

JNIEXPORT jbyteArray JNICALL Java_com_xiaomo_termux_BootstrapManager_getZip(JNIEnv *env, __attribute__((__unused__)) jclass clazz)
{
    jbyteArray ret = (*env)->NewByteArray(env, blob_size);
    (*env)->SetByteArrayRegion(env, ret, 0, blob_size, blob);
    return ret;
}
