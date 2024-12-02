package com.example.colorr;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.colorr.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.rtugeek.android.colorseekbar.ColorSeekBar;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import it.mirko.rangeseekbar.OnRangeSeekBarListener;
import it.mirko.rangeseekbar.RangeSeekBar;

public class MainActivity extends AppCompatActivity {
    //overlay
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1;
    private boolean overlayEnabled = false;
    private boolean overlayRunning = false;
    private boolean isSwitchingActivities = false;

    TextView centerPlus;

    private GLSurfaceView glSurfaceView;
    private CameraFilterRenderer cameraFilterRenderer;
    private ProcessCameraProvider cameraProvider;

    private Button tritanopiaButton;
    private boolean isTritanopiaEnabled = false;

    private Button propanopiaButton;
    private boolean isPropanopiaEnabled = false;

    private Button deuteranopiaButton;
    private boolean isDeuteranopiaEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tritanopiaButton = findViewById(R.id.tritanopiaButton);
        tritanopiaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle Tritanopia mode
                isTritanopiaEnabled = !isTritanopiaEnabled;
                Log.d("TritanopiaButton", "Tritanopia mode toggled: " + isTritanopiaEnabled);

                // Update the renderer with the new mode
                if (cameraFilterRenderer != null) {
                    cameraFilterRenderer.setTritanopiaMode(isTritanopiaEnabled);
                    glSurfaceView.requestRender();  // Request a render to update the display
                }
            }
        });

        Button protanopiaButton = findViewById(R.id.protanopiaButton);
        Button deuteranopiaButton = findViewById(R.id.deuteranopiaButton);

        protanopiaButton.setOnClickListener(v -> {
            cameraFilterRenderer.setProtanopiaMode(!cameraFilterRenderer.isPropanopiaEnabled());
            cameraFilterRenderer.setDeuteranopiaMode(false); // Disable Deuteranopia if enabling Protanopia
            cameraFilterRenderer.setTritanopiaMode(false); // Disable Tritanopia
            glSurfaceView.requestRender();
        });

        deuteranopiaButton.setOnClickListener(v -> {
            cameraFilterRenderer.setDeuteranopiaMode(!cameraFilterRenderer.isDeuteranopiaEnabled());
            cameraFilterRenderer.setProtanopiaMode(false); // Disable Protanopia if enabling Deuteranopia
            cameraFilterRenderer.setTritanopiaMode(false); // Disable Tritanopia
            glSurfaceView.requestRender();
        });


        Button filter = findViewById(R.id.next);
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSwitchingActivities = true;
                startActivity(new Intent(MainActivity.this, MainActivity2.class));
            }
        });

        Button mimic = findViewById(R.id.nextnext);
        mimic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSwitchingActivities = true;
                startActivity(new Intent(MainActivity.this, ColorBlindnessActivity.class));
            }
        });

        Button test = findViewById(R.id.testy);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSwitchingActivities = true;
                startActivity(new Intent(MainActivity.this, TestView.class));
            }
        });

        // Check and request overlay permission
        if (Settings.canDrawOverlays(this)) {
            startOverlayService();
        } else {
            requestOverlayPermission();
        }

        ToggleButton toggleOverlay = findViewById(R.id.toggleOverlay);
        toggleOverlay.setChecked(false);  // Ensure it starts in the off state
        toggleOverlay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                overlayEnabled = isChecked;
                if (overlayEnabled) {
                    if (Settings.canDrawOverlays(MainActivity.this)) {
                        startOverlayService();  // Start overlay if permission is granted
                    } else {
                        requestOverlayPermission();  // Request permission if not granted
                        toggleOverlay.setChecked(false);  // Reset toggle if permission is not granted
                    }
                } else {
                    if (overlayRunning) {
                        stopOverlayService();  // Stop overlay if it was running
                    }
                }
            }
        });


        // Initialize GLSurfaceView and set renderer
        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        // Initialize the renderer
        cameraFilterRenderer = new CameraFilterRenderer();
        glSurfaceView.setRenderer(cameraFilterRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        // Bring glSurfaceView to the front
        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glSurfaceView.bringToFront();

        // Start CameraX after the renderer is initialized
        startCameraX();

        // Center Plus UI element
        centerPlus = findViewById(R.id.centerPlus);
        centerPlus.bringToFront();
        glSurfaceView.setZOrderMediaOverlay(true);
        centerPlus.setX(glSurfaceView.getWidth() / 2f - centerPlus.getWidth() / 2f);
        centerPlus.setY(glSurfaceView.getHeight() / 2f - centerPlus.getHeight() / 2f);
    }


    private void checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            if (overlayEnabled && !overlayRunning) {
                startOverlayService();
            }
        } else {
            requestOverlayPermission();
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this) && overlayEnabled && !overlayRunning) {
                startOverlayService();
            }
        }
    }

    private void startOverlayService() {
        Intent serviceIntent = new Intent(this, OverlayService.class);
        startService(serviceIntent);
        overlayRunning = true;
    }

    private void stopOverlayService() {
        Intent serviceIntent = new Intent(this, OverlayService.class);
        stopService(serviceIntent);
        overlayRunning = false;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!isSwitchingActivities && overlayEnabled && !overlayRunning) {
            checkOverlayPermission();  // Start overlay if leaving the app and overlay is enabled
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopOverlayService();  // Ensure the overlay stops when the app is destroyed
    }

    private Handler resumeHandler = new Handler();
    private Runnable resumeRunnable = this::initializeGLSurfaceView;

    @Override
    protected void onPause() {
        super.onPause();

        // Detach GLSurfaceView and release resources
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
            ((ViewGroup) glSurfaceView.getParent()).removeView(glSurfaceView);
            glSurfaceView = null;
        }

//        // Cancel any pending reinitialization tasks
//        resumeHandler.removeCallbacks(resumeRunnable);
//        stopOverlayService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isSwitchingActivities = false;
        if (overlayRunning) {
            stopOverlayService();  // Stop overlay when returning to the app
        }
        // Delay GLSurfaceView reinitialization to allow time for resources to be cleaned up
        resumeHandler.postDelayed(resumeRunnable, 300); // Adjust delay if necessary
        if (glSurfaceView != null) {
            glSurfaceView.requestRender();
        }
    }

    private void initializeGLSurfaceView() {
        if (glSurfaceView == null) {
            // Create a new instance of GLSurfaceView only if it does not already exist
            glSurfaceView = new GLSurfaceView(this);
            glSurfaceView.setZOrderOnTop(true);
            glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            glSurfaceView.setEGLContextClientVersion(2);
            glSurfaceView.setPreserveEGLContextOnPause(true);
            glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

            // Set the renderer only once
            cameraFilterRenderer = new CameraFilterRenderer();
            glSurfaceView.setRenderer(cameraFilterRenderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

            ViewGroup rootView = findViewById(R.id.root);
            rootView.addView(glSurfaceView);
            glSurfaceView.onResume();  // Resume GLSurfaceView
        } else {
            // Only resume and request render if GLSurfaceView already exists
            glSurfaceView.onResume();
            glSurfaceView.requestRender();
        }
        centerPlus.bringToFront();
        glSurfaceView.setZOrderMediaOverlay(true);
        // Restart CameraX if necessary
        startCameraX();
    }



    private void startCameraX() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        Preview preview = new Preview.Builder().build();
        PreviewView pv = findViewById(R.id.pvPreview);
        preview.setSurfaceProvider(pv.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            cameraFilterRenderer.setCameraFrame(bitmap, glSurfaceView);
            detectColorAtCenter(imageProxy);
            imageProxy.close();
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        // Rotate the bitmap to match the display orientation
        return rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (rotationDegrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    private void detectColorAtCenter(ImageProxy imageProxy) {
        // Get image planes for Y, U, V channels
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();

        ByteBuffer yBuffer = planes[0].getBuffer();  // Y channel
        ByteBuffer uBuffer = planes[1].getBuffer();  // U channel
        ByteBuffer vBuffer = planes[2].getBuffer();  // V channel

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();

        // Get the pixel in the center of the image
        int centerX = width / 2;
        int centerY = height / 2;

        // Calculate the pixel offset in the buffer for the center
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();  // U and V planes have the same stride
        int uvPixelStride = planes[1].getPixelStride();

        // Read Y, U, V values for the center pixel
        int yIndex = centerY * yRowStride + centerX;
        int uvIndex = (centerY / 2) * uvRowStride + (centerX / 2) * uvPixelStride;

        // Extract YUV values
        int y = yBuffer.get(yIndex) & 0xFF;  // Y is a byte, convert to unsigned
        int u = uBuffer.get(uvIndex) & 0xFF; // U is a byte, convert to unsigned
        int v = vBuffer.get(uvIndex) & 0xFF; // V is a byte, convert to unsigned

        // Convert YUV to RGB with refined formula (ITU-R BT.601)
        int r = (int) (y + 1.402 * (v - 128));
        int g = (int) (y - 0.344136 * (u - 128) - 0.714136 * (v - 128));
        int b = (int) (y + 1.772 * (u - 128));

        // Clamp RGB values to [0, 255]
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        // Convert to a color integer
        int color = Color.rgb(r, g, b);

        // Get the color name using the custom method
        String colorName = ColorUtils.getColorName(color);

        // Display the color name in the TextView
        TextView tv = findViewById(R.id.textView);
        tv.bringToFront();

        tv.setText(colorName);


        Log.d("ColorDetection", "Detected color: " + colorName);
    }


}