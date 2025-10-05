#include <jni.h>
#include <string>
#include <opencv2/core.hpp>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_gbdetects_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_gbdetects_MainActivity_processFrameStub(
        JNIEnv* env,
        jobject /* this */,
        jstring input) {
    // This is a dummy function.
    // It just confirms that JNI is working.
    // We'll process real frames here later.
    std::string hello = "JNI Bridge OK";
    return env->NewStringUTF(hello.c_str());
}
