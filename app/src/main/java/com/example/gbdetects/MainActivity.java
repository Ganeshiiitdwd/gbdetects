package com.example.gbdetects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.widget.Button; // Import Button
import android.widget.Toast;
import java.nio.ByteBuffer;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView gl_v;
    private MyGLRenderer rend;
    private CameraDevice cam_d;
    private CameraCaptureSession cap_s;
    private ImageReader img_r;
    private Size prev_s;
    private Handler bg_h;
    private HandlerThread bg_t;
    private Button toggle_b;

    // --- Mode state for toggling effects ---
    // 0 = Raw, 1 = Edges, 2 = Grayscale
    private int proc_mode = 1;
    // ---------------------------------------

    static { System.loadLibrary("gbdetects"); }

    // --- Updated JNI signature to accept a mode ---
    public native ByteBuffer processFrame(int w, int h, ByteBuffer y_b, ByteBuffer u_b, ByteBuffer v_b, int y_s, int u_s, int v_s, int mode);
    // --------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gl_v = findViewById(R.id.glSurfaceView);
        gl_v.setEGLContextClientVersion(2);
        rend = new MyGLRenderer();
        gl_v.setRenderer(rend);
        gl_v.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        toggle_b = findViewById(R.id.toggleButton);
        toggle_b.setOnClickListener(v -> {
            proc_mode = (proc_mode + 1) % 3; // Cycle through 0, 1, 2
            switch (proc_mode) {
                case 0: toggle_b.setText("Raw"); break;
                case 1: toggle_b.setText("Edges"); break;
                case 2: toggle_b.setText("Grayscale"); break;
            }
        });
    }

    private final CameraDevice.StateCallback st_cb = new CameraDevice.StateCallback() {
        @Override public void onOpened(@NonNull CameraDevice c) { cam_d = c; createPreview(); }
        @Override public void onDisconnected(@NonNull CameraDevice c) { c.close(); }
        @Override public void onError(@NonNull CameraDevice c, int e) { c.close(); cam_d = null; }
    };

    private void openCam() {
        CameraManager man = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String id = man.getCameraIdList()[0];
            CameraCharacteristics chars = man.getCameraCharacteristics(id);
            StreamConfigurationMap map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            prev_s = map.getOutputSizes(ImageFormat.YUV_420_888)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 200);
                return;
            }
            man.openCamera(id, st_cb, bg_h);
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private void createPreview() {
        try {
            img_r = ImageReader.newInstance(prev_s.getWidth(), prev_s.getHeight(), ImageFormat.YUV_420_888, 2);
            img_r.setOnImageAvailableListener(reader -> {
                Image img = reader.acquireLatestImage();
                if (img == null) return;
                Image.Plane[] p = img.getPlanes();
                // --- Pass the current mode to the JNI function ---
                ByteBuffer frame = processFrame(img.getWidth(), img.getHeight(), p[0].getBuffer(), p[1].getBuffer(), p[2].getBuffer(), p[0].getRowStride(), p[1].getRowStride(), p[2].getRowStride(), proc_mode);
                // ---------------------------------------------------
                if (frame != null) {
                    rend.setFrame(img.getHeight(), img.getWidth(), frame);
                    gl_v.requestRender();
                }
                img.close();
            }, bg_h);

            Surface surf = img_r.getSurface();
            final CaptureRequest.Builder cap_b = cam_d.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            cap_b.addTarget(surf);
            cam_d.createCaptureSession(Collections.singletonList(surf), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(@NonNull CameraCaptureSession s) {
                    if (cam_d == null) return;
                    cap_s = s;
                    try {
                        cap_b.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                        cap_s.setRepeatingRequest(cap_b.build(), null, bg_h);
                    } catch (CameraAccessException e) { e.printStackTrace(); }
                }
                @Override public void onConfigureFailed(@NonNull CameraCaptureSession s) { Toast.makeText(MainActivity.this, "Config change", Toast.LENGTH_SHORT).show(); }
            }, null);
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    @Override protected void onResume() { super.onResume(); startBgThread(); openCam(); gl_v.onResume(); }
    @Override protected void onPause() { stopBgThread(); super.onPause(); gl_v.onPause(); }
    protected void startBgThread() { bg_t = new HandlerThread("Camera BG"); bg_t.start(); bg_h = new Handler(bg_t.getLooper()); }
    protected void stopBgThread() { if (bg_t == null) return; bg_t.quitSafely(); try { bg_t.join(); bg_t = null; bg_h = null; } catch (InterruptedException e) { e.printStackTrace(); } }
    @Override public void onRequestPermissionsResult(int rc, @NonNull String[] p, @NonNull int[] gr) { super.onRequestPermissionsResult(rc, p, gr); if (rc == 200 && gr[0] != PackageManager.PERMISSION_GRANTED) { Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show(); finish(); } }
}

