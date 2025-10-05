#include <jni.h>
#include <string>
#include <opencv2/imgproc.hpp>
#include <GLES2/gl2.h>

cv::Mat processed_mat;

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_gbdetects_MainActivity_processFrame(
        JNIEnv* env, jobject, jint w, jint h,
        jobject y_buf, jobject u_buf, jobject v_buf,
        jint y_s, jint u_s, jint v_s,
        jint mode) { // Added mode parameter

    auto y = (uint8_t*) env->GetDirectBufferAddress(y_buf);
    auto u = (uint8_t*) env->GetDirectBufferAddress(u_buf);
    auto v = (uint8_t*) env->GetDirectBufferAddress(v_buf);

    cv::Mat y_mat(h, w, CV_8UC1, y, y_s);
    cv::Mat u_mat(h / 2, w / 2, CV_8UC1, u, u_s);
    cv::Mat v_mat(h / 2, w / 2, CV_8UC1, v, v_s);

    switch (mode) {
        case 0: // Raw
            // The YUV_420_888 format has semi-planar U and V. For OpenCV conversion
            // we need to merge them. But since we are getting separate buffers for U and V
            // we can construct a full YUV image for conversion. A simpler way for a raw
            // preview is to just show the Y (grayscale) plane. Let's make a BGRA raw.
            // This requires a full YUV matrix.
        {
            cv::Mat yuv_full(h + h / 2, w, CV_8UC1);
            memcpy(yuv_full.data, y, w * h);
            memcpy(yuv_full.data + w * h, u, w * h / 4);
            memcpy(yuv_full.data + w * h + w * h / 4, v, w * h / 4);
            cv::cvtColor(yuv_full, processed_mat, cv::COLOR_YUV2BGRA_I420);
        }
            break;

        case 1: // Edges
        default:
        {
            cv::Mat canny_out;
            cv::Canny(y_mat, canny_out, 50, 150);
            cv::cvtColor(canny_out, processed_mat, cv::COLOR_GRAY2BGRA, 4);
        }
            break;

        case 2: // Grayscale
            cv::cvtColor(y_mat, processed_mat, cv::COLOR_GRAY2BGRA, 4);
            break;
    }

    return env->NewDirectByteBuffer(processed_mat.data, processed_mat.total() * processed_mat.elemSize());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_gbdetects_MyGLRenderer_updateTexture(
        JNIEnv *env, jobject, jint tex_id, jint w, jint h, jobject data) {
    auto pixels = (uint8_t *) env->GetDirectBufferAddress(data);
    glBindTexture(GL_TEXTURE_2D, tex_id);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, h, w, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
}

