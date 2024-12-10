package com.example.colorr;


public class ShaderSettings {
    private static ShaderSettings instance = null;
    private int shaderMode = 0; // 0: Normal, 1: Protanopia, 2: Deuteranopia, 3: Tritanopia
    private int hueStart = 0;
    private int hueEnd = 360;

    private ShaderSettings() {} // Private constructor to prevent external instantiation


    public static ShaderSettings getInstance() {
        if (instance == null) {
            instance = new ShaderSettings();
        }
        return instance;
    }

    // ... getter and setter methods ...
    public int getShaderMode() {
        return shaderMode;
    }

    public void setShaderMode(int shaderMode) {
        this.shaderMode = shaderMode;
    }

    public int getHueStart() {
        return hueStart;
    }

    public void setHueStart(int hueStart) {
        this.hueStart = hueStart;
    }

    public int getHueEnd() {
        return hueEnd;
    }

    public void setHueEnd(int hueEnd) {
        this.hueEnd = hueEnd;
    }


}