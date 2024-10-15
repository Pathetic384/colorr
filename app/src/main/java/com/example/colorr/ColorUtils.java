package com.example.colorr;import java.util.HashMap;
import android.graphics.Color;

public class ColorUtils {

    // Create a map of common colors with more shades of green and brown
    private static final HashMap<String, Integer> colorMap = new HashMap<String, Integer>() {{
        put("Red", Color.RED);
        put("Green", Color.GREEN);
        put("Light Green", Color.rgb(144, 238, 144));  // Light green
        put("Dark Green", Color.rgb(0, 100, 0));  // Dark green
        put("Blue", Color.BLUE);
        put("Yellow", Color.YELLOW);
        put("Cyan", Color.CYAN);
        put("Magenta", Color.MAGENTA);
        put("Black", Color.BLACK);
        put("White", Color.WHITE);
        put("Gray", Color.GRAY);
        put("Orange", Color.rgb(255, 165, 0)); // Orange
        put("Purple", Color.rgb(128, 0, 128)); // Purple
        put("Brown", Color.rgb(165, 42, 42));  // Brown
        put("Light Brown", Color.rgb(181, 101, 29));  // Light brown
    }};

    // Method to find the nearest color name
    public static String getColorName(int color) {
        int minDistance = Integer.MAX_VALUE;
        String closestColorName = null;

        // Extract RGB components from the color
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        // Iterate through the map and find the closest color
        for (String colorName : colorMap.keySet()) {
            int knownColor = colorMap.get(colorName);
            int knownRed = Color.red(knownColor);
            int knownGreen = Color.green(knownColor);
            int knownBlue = Color.blue(knownColor);

            // Calculate the Euclidean distance between the colors
            int distance = (int) Math.sqrt(Math.pow(red - knownRed, 2) + Math.pow(green - knownGreen, 2) + Math.pow(blue - knownBlue, 2));

            // Find the smallest distance and get the corresponding color name
            if (distance < minDistance) {
                minDistance = distance;
                closestColorName = colorName;
            }
        }

        return closestColorName != null ? closestColorName : "Unknown Color";
    }

    public static int getTransparentColor(int color, float transparency) {
        // Ensure the transparency is between 0.0 and 1.0 (0 = fully transparent, 1 = fully opaque)
        if (transparency < 0.0f) transparency = 0.0f;
        if (transparency > 1.0f) transparency = 1.0f;

        // Extract the RGB components from the color
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        // Calculate the alpha value based on the transparency (0-255)
        int alpha = (int) (transparency * 255);

        // Combine the alpha value with the RGB components to create a transparent color
        return Color.argb(alpha, red, green, blue);
    }
}



