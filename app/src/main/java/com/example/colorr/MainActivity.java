package com.example.colorr;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    ProcessCameraProvider cameraProvider;
    ColorSeekBar colorSeekBar;

    int currentBlindness = 0;
    Button but1, but2, but3, but4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        colorSeekBar = findViewById(R.id.colorSeekBar);
        but1 = findViewById(R.id.Protanopia);
        but2 = findViewById(R.id.Deuteranopia);
        but3 = findViewById(R.id.Tritanopia);
        but4 = findViewById(R.id.Achromatopsia);

        // Set up click listeners for each button
        but1.setOnClickListener(v -> currentBlindness = 1);  // Protanopia
        but2.setOnClickListener(v -> currentBlindness = 2);  // Deuteranopia
        but3.setOnClickListener(v -> currentBlindness = 3);  // Tritanopia
        but4.setOnClickListener(v -> currentBlindness = 4);  // Achromatopsia

        ProcessCameraProvider.getInstance(this);
        ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderListenableFuture.get();

                    startCameraX(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void startCameraX(ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        Preview preview = new Preview.Builder().build();
        PreviewView pv = findViewById(R.id.pvPreview);
        preview.setSurfaceProvider(pv.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // Set the analyzer to detect the color at the center
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                // Log to verify the analyzer is running
                Log.d("CameraX", "Analyzer is running");

                // Call the method to detect color at center
                detectColorAtCenter(imageProxy);

                // Close the image once done
                imageProxy.close();
            }
        });

        try {
            cameraProvider.unbindAll(); // Unbind previous use cases

            // Bind both the Preview and ImageAnalysis together
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        int position = getColorSeekBarPositionFromHSV(color);

        colorSeekBar.setColorBarPosition(100-position);

        // Get the color name using the custom method
        String colorName = ColorUtils.getColorName(color);

        // Display the color name in the TextView
        TextView tv = findViewById(R.id.textView);
        tv.setText(colorName);


        Log.d("ColorDetection", "Detected color: " + colorName);
    }


    // Convert the detected color to HSV
// Convert RGB color to HSV
    private float[] getHSVfromColor(int color) {
        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
        return hsv;
    }

    // Determine if the color is neutral (grayscale-like)
    private boolean isNeutralColor(float[] hsv) {
        return hsv[1] < 0.15;  // Decreased threshold for neutral colors (greys, whites)
    }

    // Map the color to the ColorSeekBar position
    private int getColorSeekBarPositionFromHSV(int color) {
        float[] hsv = getHSVfromColor(color);

        // Handle greys and neutrals based on their value (brightness)
        if (isNeutralColor(hsv)) {
            // Map neutral colors to the end of the bar based on brightness
            if (hsv[2] >= 0.9) {
                return 0;  // White or very light grey -> map to the end
            } else if (hsv[2] < 0.1) {
                return 0;    // Black -> map to the start
            } else {
                // Scale greys between 0 and 100 based on brightness (value)
                return (int) (hsv[2] * 100);
            }
        }

        // For colorful hues, use the hue component to map the color on the seekbar
        return (int) ((hsv[0] / 360f) * 100);
    }

}