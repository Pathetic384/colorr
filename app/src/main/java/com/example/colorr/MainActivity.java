package com.example.colorr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import it.mirko.rangeseekbar.OnRangeSeekBarListener;
import it.mirko.rangeseekbar.RangeSeekBar;

public class MainActivity extends AppCompatActivity {

    ProcessCameraProvider cameraProvider;
    RangeSeekBar rangeSeekBar;
    int str= 0;
    int ed = 360;

    int current_state = 0;


    ImageView overlayImageView;

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

        Button shift = findViewById(R.id.shift);
        shift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear any existing color filter before applying a new one
                overlayImageView.clearColorFilter();

                // Apply the selected colorblind enhancement based on `current_state`
                switch (current_state) {
                    case 1:
                        applyColorBlindFilter(true, COLOR_MATRIX_ENHANCE_TRITANOPIA);
                        break;
                    case 2:
                        applyColorBlindFilter(true, COLOR_MATRIX_ENHANCE_DEUTERANOPIA);
                        break;
                    case 3:
                        applyColorBlindFilter(true, COLOR_MATRIX_ENHANCE_PROTANOPIA);
                        break;
                    default:
                        Log.d("FilterApplication", "No filter applied; current_state is " + current_state);
                        break;
                }
            }
        });

        // Define buttons and click listeners for each colorblind filter
        // Set up button click listeners
        Button tritanopia = findViewById(R.id.tritanopia);
        tritanopia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_state = 1;
                overlayImageView.clearColorFilter();
                applyColorBlindFilter(true, COLOR_MATRIX_FILTER_TRITANOPIA);
            }
        });

        Button deuteranopia = findViewById(R.id.deuteranopia);
        deuteranopia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_state = 2;
                overlayImageView.clearColorFilter();
                applyColorBlindFilter(true, COLOR_MATRIX_FILTER_DEUTERANOPIA);
            }
        });


        Button prota = findViewById(R.id.protanopia);
        prota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_state = 3;
                overlayImageView.clearColorFilter();
                applyColorBlindFilter(true, COLOR_MATRIX_FILTER_PROTANOPIA);
            }
        });

        overlayImageView = findViewById(R.id.imageView);
        rangeSeekBar = findViewById(R.id.rangeSeekBar);
        rangeSeekBar.setStartProgress(0);
        rangeSeekBar.setEndProgress(360);
        rangeSeekBar.setMax(360);
        rangeSeekBar.setMinDifference(40);
        rangeSeekBar.setOnRangeSeekBarListener(new OnRangeSeekBarListener() {
            @Override
            public void onRangeValues(RangeSeekBar rangeSeekBar, int start, int end) {
                str = start;
                ed = end;
            }
        });


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

    private void applyColorBlindFilter(boolean apply, float[] colorMatrix) {
        if (apply) {
            // Clear any existing filter before applying the new one
            overlayImageView.clearColorFilter();

            ColorMatrix filterMatrix = new ColorMatrix(colorMatrix);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(filterMatrix);
            overlayImageView.setColorFilter(filter);
        } else {
            overlayImageView.clearColorFilter();
        }
    }

    private static final float[] COLOR_MATRIX_ENHANCE_PROTANOPIA = {
            1.2f, -0.2f, 0.0f, 0, 0,
            0.0f, 1.0f, 0.0f, 0, 0,
            0.0f, -0.2f, 1.2f, 0, 0,
            0.0f, 0.0f, 0.0f, 1, 0
    };

    private static final float[] COLOR_MATRIX_ENHANCE_DEUTERANOPIA = {
            1.0f, 0.0f, 0.0f, 0, 0,
            -0.2f, 1.2f, 0.0f, 0, 0,
            0.0f, 0.0f, 1.0f, 0, 0,
            0.0f, 0.0f, 0.0f, 1, 0
    };

    private static final float[] COLOR_MATRIX_ENHANCE_TRITANOPIA = {
            1.0f, 0.0f, 0.0f, 0, 0,
            0.0f, 1.0f, 0.0f, 0, 0,
            -0.1f, 0.0f, 1.1f, 0, 0,
            0.0f, 0.0f, 0.0f, 1, 0
    };

    private static final float[] COLOR_MATRIX_FILTER_PROTANOPIA = {
            0.625f, 0.375f, 0.0f, 0, 0,
            0.7f, 0.3f, 0.0f, 0, 0,
            0.0f, 0.3f, 0.7f, 0, 0,
            0.0f, 0.0f, 0.0f, 1, 0
    };

    // Color matrices for different colorblind filters
    private static final float[] COLOR_MATRIX_FILTER_TRITANOPIA = {
            0.95f, 0.05f, 0.0f, 0, 0,
            0.0f, 0.433f, 0.567f, 0, 0,
            0.0f, 0.475f, 0.525f, 0, 0,
            0.0f, 0.0f, 0.0f, 1, 0
    };

    private static final float[] COLOR_MATRIX_FILTER_DEUTERANOPIA = {
            0.567f, 0.433f, 0.0f, 0, 0,
            0.558f, 0.442f, 0.0f, 0, 0,
            0.0f, 0.242f, 0.758f, 0, 0,
            0.0f, 0.0f, 0.0f, 1, 0
    };




    private void startCameraX(ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        Preview preview = new Preview.Builder().build();
        PreviewView pv = findViewById(R.id.pvPreview);
        preview.setSurfaceProvider(pv.getSurfaceProvider());

        // Initialize ImageAnalysis with original resolution to avoid zoom
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

// Process every 4th frame to reduce lag
        AtomicInteger frameCounter = new AtomicInteger();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            if (frameCounter.getAndIncrement() % 3 == 0) {  // Skip frames to improve performance
                Bitmap bitmap = imageProxyToBitmap(imageProxy);

                // Scale down bitmap resolution by a factor of 4
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                        bitmap.getWidth() / 2,
                        bitmap.getHeight() / 2, true);

                // Perform color processing on the scaled down bitmap
                Bitmap filteredBitmap = applyColorFilter(scaledBitmap);

                // Display scaled and filtered bitmap on ImageView
                runOnUiThread(() -> overlayImageView.setImageBitmap(filteredBitmap));
            }
            detectColorAtCenter(imageProxy);
            imageProxy.close();
        });

        try {
            cameraProvider.unbindAll(); // Unbind previous use cases

            // Bind both the Preview and ImageAnalysis together
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int convertToGrayscale(int r, int g, int b) {
        int gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);
        return Color.rgb(gray, gray, gray);
    }

    private boolean isColorInRange(int r, int g, int b) {
        float[] hsv = new float[3];
        Color.RGBToHSV(r, g, b, hsv);

        // Define the green color range in terms of hue (approx. 85-160 degrees)
        return hsv[0] >= str && hsv[0] <= ed;
    }

    private Bitmap applyColorFilter(Bitmap bitmap) {
        Bitmap filteredBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());

        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                if (isColorInRange(r, g, b)) {
                    // If the color is in the green range, keep it unchanged
                    filteredBitmap.setPixel(x, y, Color.rgb(r, g, b));
                } else {
                    // Convert the pixel to grayscale
                    int grayscale = convertToGrayscale(r, g, b);
                    filteredBitmap.setPixel(x, y, grayscale);
                }
            }
        }

        return filteredBitmap;
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

        //int position = getColorSeekBarPositionFromHSV(color);

        //colorSeekBar.setColorBarPosition(100-position);

        // Get the color name using the custom method
        String colorName = ColorUtils.getColorName(color);

        // Display the color name in the TextView
        TextView tv = findViewById(R.id.textView);
        tv.setText(colorName);


        Log.d("ColorDetection", "Detected color: " + colorName);
    }








//    // Convert the detected color to HSV
//// Convert RGB color to HSV
//    private float[] getHSVfromColor(int color) {
//        float[] hsv = new float[3];
//        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
//        return hsv;
//    }
//
//    // Determine if the color is neutral (grayscale-like)
//    private boolean isNeutralColor(float[] hsv) {
//        return hsv[1] < 0.15;  // Decreased threshold for neutral colors (greys, whites)
//    }
//
//    // Map the color to the ColorSeekBar position
//    private int getColorSeekBarPositionFromHSV(int color) {
//        float[] hsv = getHSVfromColor(color);
//
//        // Handle greys and neutrals based on their value (brightness)
//        if (isNeutralColor(hsv)) {
//            // Map neutral colors to the end of the bar based on brightness
//            if (hsv[2] >= 0.9) {
//                return 0;  // White or very light grey -> map to the end
//            } else if (hsv[2] < 0.1) {
//                return 0;    // Black -> map to the start
//            } else {
//                // Scale greys between 0 and 100 based on brightness (value)
//                return (int) (hsv[2] * 100);
//            }
//        }
//
//        // For colorful hues, use the hue component to map the color on the seekbar
//        return (int) ((hsv[0] / 360f) * 100);
//    }

}