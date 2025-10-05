package com.example.gbdetects;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private final String vs =
            "attribute vec4 pos;" +
                    "attribute vec2 tex;" +
                    "varying vec2 tex_c;" +
                    "void main() {" +
                    "  gl_Position = pos;" +
                    "  tex_c = tex;" +
                    "}";

    private final String fs =
            "precision mediump float;" +
                    "varying vec2 tex_c;" +
                    "uniform sampler2D tex_s;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(tex_s, tex_c);" +
                    "}";

    private int prog;
    private int tex_id;
    private FloatBuffer v_buf;
    private FloatBuffer t_buf;
    private int w;
    private int h;
    private ByteBuffer frame;

    static {
        System.loadLibrary("gbdetects");
    }

    public native void updateTexture(int tex, int w, int h, ByteBuffer data);

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        prog = createProg(vs, fs);
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        tex_id = tex[0];

        float[] v = {-1, -1, -1, 1, 1, -1, 1, 1};
        float[] t = {0, 1, 0, 0, 1, 1, 1, 0};
        v_buf = ByteBuffer.allocateDirect(v.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        v_buf.put(v).position(0);
        t_buf = ByteBuffer.allocateDirect(t.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        t_buf.put(t).position(0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (frame != null) {
            synchronized (this) {
                updateTexture(tex_id, w, h, frame);
                frame = null;
            }
        }

        GLES20.glUseProgram(prog);
        int pos_loc = GLES20.glGetAttribLocation(prog, "pos");
        int tex_loc = GLES20.glGetAttribLocation(prog, "tex");
        int tex_s_loc = GLES20.glGetUniformLocation(prog, "tex_s");

        GLES20.glEnableVertexAttribArray(pos_loc);
        GLES20.glVertexAttribPointer(pos_loc, 2, GLES20.GL_FLOAT, false, 0, v_buf);
        GLES20.glEnableVertexAttribArray(tex_loc);
        GLES20.glVertexAttribPointer(tex_loc, 2, GLES20.GL_FLOAT, false, 0, t_buf);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex_id);
        GLES20.glUniform1i(tex_s_loc, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(pos_loc);
        GLES20.glDisableVertexAttribArray(tex_loc);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public void setFrame(int w, int h, ByteBuffer frame) {
        synchronized (this) {
            this.w = w;
            this.h = h;
            this.frame = frame;
        }
    }

    private int loadShader(int type, String code) {
        int sh = GLES20.glCreateShader(type);
        GLES20.glShaderSource(sh, code);
        GLES20.glCompileShader(sh);
        return sh;
    }

    private int createProg(String vs_s, String fs_s) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vs_s);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fs_s);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);
        return prog;
    }
}
