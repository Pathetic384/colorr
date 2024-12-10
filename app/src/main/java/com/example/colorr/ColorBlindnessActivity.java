package com.example.colorr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import android.os.Bundle;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.view.View;
import android.widget.AdapterView;
import androidx.appcompat.app.AppCompatActivity;

public class ColorBlindnessActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private ColorBlindnessRenderer colorBlindnessRenderer;
    private ProcessCameraProvider cameraProvider;
    private PreviewView previewView;
    //private Spinner colorBlindnessSpinner;
    private ConstraintLayout navigation;
    private ConstraintLayout colorBlindnessType;
    ImageButton back;
    private Button red, green, blue, normal;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_blindness);
        back = findViewById(R.id.back2);

        // Initialize UI components
        previewView = findViewById(R.id.pvPreview);
        glSurfaceView = findViewById(R.id.glSurfaceView);
        //colorBlindnessSpinner = findViewById(R.id.spinnerColorBlindness);

        back.setOnClickListener(v -> {
            startActivity(new Intent(ColorBlindnessActivity.this, MainActivity.class));
        });

        // Set up the Spinner for color blindness selection
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.color_blindness_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //colorBlindnessSpinner.setAdapter(adapter);

        // Initialize GLSurfaceView and set renderer
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        // Initialize the color blindness renderer
        colorBlindnessRenderer = new ColorBlindnessRenderer();
        glSurfaceView.setRenderer(colorBlindnessRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        // Bring glSurfaceView to the front
        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glSurfaceView.bringToFront();

        // Start CameraX after the renderer is initialized
        startCameraX();
        red = findViewById(R.id.red);
        green = findViewById(R.id.green);
        blue = findViewById(R.id.blue);
        normal = findViewById(R.id.normal);
        
        normal.setSelected(true);


        normal.setOnClickListener(v -> {
            normal.setSelected(true);
            red.setSelected(false);
            green.setSelected(false);
            blue.setSelected(false);
            colorBlindnessRenderer.setColorBlindnessType(0);
            glSurfaceView.requestRender();
        });

        red.setOnClickListener(v -> {
            normal.setSelected(false);
            red.setSelected(true);
            green.setSelected(false);
            blue.setSelected(false);
            colorBlindnessRenderer.setColorBlindnessType(1); // Protanopia
            glSurfaceView.requestRender();
        });

        green.setOnClickListener(v -> {
            normal.setSelected(false);
            red.setSelected(false);
            green.setSelected(true);
            blue.setSelected(false);
            colorBlindnessRenderer.setColorBlindnessType(2); // Deuteranopia
            glSurfaceView.requestRender();
        });

        blue.setOnClickListener(v -> {
            normal.setSelected(false);
            red.setSelected(false);
            green.setSelected(false);
            blue.setSelected(true);
            colorBlindnessRenderer.setColorBlindnessType(3); // Tritanopia
            glSurfaceView.requestRender();
        });
        navigation = findViewById(R.id.constraintLayout2);
        colorBlindnessType = findViewById(R.id.constraintLayout);
        navigation.bringToFront();
        colorBlindnessType.bringToFront();
        glSurfaceView.setZOrderMediaOverlay(true);

        ImageButton startTutorialButton = findViewById(R.id.tutorial);
        startTutorialButton.setOnClickListener(v -> startTutorial(back, red, green, blue, normal));
    }


    private void startTutorial(View back, View red, View green, View blue, View normal) {
        new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(back, "Back Button", "This button navigates back to the main activity.")
                                .outerCircleColor(R.color.teal_200) // Custom color for this target
                                .outerCircleAlpha(0.96f)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(20)
                                .descriptionTextSize(16)
                                .cancelable(true),
                        TapTarget.forView(red, "Red Filter", "This button applies the red filter for color adjustment.")
                                .outerCircleColor(R.color.red_200) // Custom color for this target
                                .outerCircleAlpha(0.96f)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(20)
                                .descriptionTextSize(16)
                                .cancelable(true),
                        TapTarget.forView(green, "Green Filter", "This button applies the green filter for color adjustment.")
                                .outerCircleColor(R.color.green_200) // Custom color for this target
                                .outerCircleAlpha(0.96f)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(20)
                                .descriptionTextSize(16)
                                .cancelable(true),
                        TapTarget.forView(blue, "Blue Filter", "This button applies the blue filter for color adjustment.")
                                .outerCircleColor(R.color.blue_200) // Custom color for this target
                                .outerCircleAlpha(0.96f)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(20)
                                .descriptionTextSize(16)
                                .cancelable(true),
                        TapTarget.forView(normal, "Normal View", "This button resets the filters to normal view.")
                                .outerCircleColor(R.color.purple_200) // Custom color for this target
                                .outerCircleAlpha(0.96f)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(20)
                                .descriptionTextSize(16)
                                .cancelable(true)
                )
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        // Tutorial finished
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        // Each step of the tutorial
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                        // Tutorial canceled
                    }
                })
                .start();
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
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            colorBlindnessRenderer.setCameraFrame(bitmap, glSurfaceView);
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
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        return rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (rotationDegrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
