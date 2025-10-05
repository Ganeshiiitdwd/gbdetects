#include <jni.h>
#include <string>
#include <opencv2/imgproc.hpp>
#include <GLES2/gl2.h>

// Global variable to hold processed frame data
cv::Mat processed_mat;

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_gbdetects_MainActivity_processFrame(
        JNIEnv* env,
        jobject,
        jint w,
        jint h,
        jobject y_buf,
        jobject u_buf,
        jobject v_buf,
        jint y_s,
        jint u_s,
        jint v_s) {

    auto y = (uint8_t*) env->GetDirectBufferAddress(y_buf);
    auto u = (uint8_t*) env->GetDirectBufferAddress(u_buf);
    auto v = (uint8_t*) env->GetDirectBufferAddress(v_buf);

    cv::Mat gray(h, w, CV_8UC1, y, y_s);
    cv::Mat canny_out;

    cv::Canny(gray, canny_out, 50, 150);

    // Convert Canny's single channel output to 4-channel BGRA for OpenGL
    cv::cvtColor(canny_out, processed_mat, cv::COLOR_GRAY2BGRA, 4);

    return env->NewDirectByteBuffer(processed_mat.data, processed_mat.total() * processed_mat.elemSize());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gbdetects_MyGLRenderer_updateTexture(
        JNIEnv *env,
        jobject,
        jint tex_id,
        jint w,
        jint h,
        jobject data) {

    auto pixels = (uint8_t *) env->GetDirectBufferAddress(data);

    glBindTexture(GL_TEXTURE_2D, tex_id);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    // Corrected typo on the line below from GL_TEXTURE_2d to GL_TEXTURE_2D
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    // Note: The matrix from OpenCV is rotated. We pass w and h swapped.
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, h, w, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
}

