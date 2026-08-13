#include <jni.h>
#include <android/log.h>
#include "core.h"

#define LOGTAG "skin-jni"

extern "C" JNIEXPORT jlong JNICALL
Java_com_re_skin_NativeBridge_attach(JNIEnv* env, jclass, jstring pkg) {
    if (!pkg) return -1;
    const char* p = env->GetStringUTFChars(pkg, nullptr);
    long r = skin::attach(p);
    env->ReleaseStringUTFChars(pkg, p);
    return (jlong)r;
}

extern "C" JNIEXPORT void JNICALL
Java_com_re_skin_NativeBridge_detach(JNIEnv*, jclass) {
    skin::detach();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_re_skin_NativeBridge_scanValue(JNIEnv*, jclass, jint value) {
    return (jint)skin::scanValue((int)value);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_re_skin_NativeBridge_getScanResult(JNIEnv*, jclass, jint index) {
    return (jlong)skin::getScanResult((int)index);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_re_skin_NativeBridge_scanResultCount(JNIEnv*, jclass) {
    return (jint)skin::scanResultCount();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_re_skin_NativeBridge_applySkin(JNIEnv*, jclass, jlong baseA, jint head, jint face, jint body) {
    return skin::applySkin((uintptr_t)baseA, (int)head, (int)face, (int)body) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_re_skin_NativeBridge_readInt(JNIEnv*, jclass, jlong addr) {
    int v = 0;
    if (skin::memRead((uintptr_t)addr, &v, 4)) return (jlong)v;
    return 0;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_re_skin_NativeBridge_findSkinBase(JNIEnv*, jclass, jint head, jint face, jint body) {
    return (jlong)skin::findSkinBase((int)head, (int)face, (int)body);
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) {
    return JNI_VERSION_1_6;
}
