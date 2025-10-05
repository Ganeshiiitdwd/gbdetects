package com.example.gbdetects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
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

    static {
        System.loadLibrary("gbdetects");
    }

    public native ByteBuffer processFrame(int w, int h, ByteBuffer y_b, ByteBuffer u_b, ByteBuffer v_b, int y_s, int u_s, int v_s);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gl_v = findViewById(R.id.glSurfaceView);
        gl_v.setEGLContextClientVersion(2);
        rend = new MyGLRenderer();
        gl_v.setRenderer(rend);
        gl_v.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private final CameraDevice.StateCallback st_cb = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cam_d = camera;
            createPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cam_d.close();
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cam_d.close();
            cam_d = null;
        }
    };

    private void openCam() {
        CameraManager man = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cam_id = man.getCameraIdList()[0];
            CameraCharacteristics chars = man.getCameraCharacteristics(cam_id);
            StreamConfigurationMap map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            prev_s = map.getOutputSizes(ImageFormat.YUV_420_888)[0];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 200);
                return;
            }
            man.openCamera(cam_id, st_cb, bg_h);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createPreview() {
        try {
            img_r = ImageReader.newInstance(prev_s.getWidth(), prev_s.getHeight(), ImageFormat.YUV_420_888, 2);
            img_r.setOnImageAvailableListener(reader -> {
                Image img = reader.acquireLatestImage();
                if (img == null) return;

                Image.Plane[] planes = img.getPlanes();
                ByteBuffer y_b = planes[0].getBuffer();
                ByteBuffer u_b = planes[1].getBuffer();
                ByteBuffer v_b = planes[2].getBuffer();

                int y_s = planes[0].getRowStride();
                int u_s = planes[1].getRowStride();
                int v_s = planes[2].getRowStride();

                ByteBuffer proc_frame = processFrame(img.getWidth(), img.getHeight(), y_b, u_b, v_b, y_s, u_s, v_s);

                if (proc_frame != null) {
                    rend.setFrame(img.getHeight(), img.getWidth(), proc_frame);
                    gl_v.requestRender();
                }

                img.close();
            }, bg_h);

            Surface surf = img_r.getSurface();
            final CaptureRequest.Builder cap_b = cam_d.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            cap_b.addTarget(surf);

            cam_d.createCaptureSession(Collections.singletonList(surf), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cam_d == null) return;
                    cap_s = session;
                    try {
                        cap_b.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                        cap_s.setRepeatingRequest(cap_b.build(), null, bg_h);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "Config change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBgThread();
        openCam();
        gl_v.onResume();
    }

    @Override
    protected void onPause() {
        stopBgThread();
        super.onPause();
        gl_v.onPause();
    }

    protected void startBgThread() {
        bg_t = new HandlerThread("Camera BG");
        bg_t.start();
        bg_h = new Handler(bg_t.getLooper());
    }

    protected void stopBgThread() {
        if (bg_t == null) return;
        bg_t.quitSafely();
        try {
            bg_t.join();
            bg_t = null;
            bg_h = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
