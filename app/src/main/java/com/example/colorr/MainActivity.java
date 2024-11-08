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
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

    private GLSurfaceView glSurfaceView;
    private CameraFilterRenderer cameraFilterRenderer;

    RangeSeekBar rangeSeekBar;
    int str = 0;
    int ed = 360;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button test = findViewById(R.id.testy);
        test.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(MainActivity.this, TestView.class));
                }
        });


        rangeSeekBar = findViewById(R.id.rangeSeekBar);
        rangeSeekBar.setStartProgress(0);
        rangeSeekBar.setEndProgress(360);
        rangeSeekBar.setMax(360);
        rangeSeekBar.setMinDifference(40);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.bringToFront();
        glSurfaceView.setZOrderOnTop(true); // Required to make the background transparent
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glSurfaceView.setEGLContextClientVersion(2); // OpenGL ES 2.0
        cameraFilterRenderer = new CameraFilterRenderer(this);
        glSurfaceView.setRenderer(cameraFilterRenderer);

        rangeSeekBar.setOnRangeSeekBarListener(new OnRangeSeekBarListener() {
            @Override
            public void onRangeValues(RangeSeekBar rangeSeekBar, int start, int end) {
                str = start;
                ed = end;
                cameraFilterRenderer.setHueRange(str, ed); // Pass hue range to renderer
            }
        });

        TextView centerPlus = findViewById(R.id.centerPlus);
        centerPlus.bringToFront();
        glSurfaceView.setZOrderMediaOverlay(true);
        centerPlus.setX(glSurfaceView.getWidth() / 2f - centerPlus.getWidth() / 2f);
        centerPlus.setY(glSurfaceView.getHeight() / 2f - centerPlus.getHeight() / 2f);

        startCameraX();

    }

    private void startCameraX() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
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
            cameraFilterRenderer.setCameraFrame(bitmap);
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
