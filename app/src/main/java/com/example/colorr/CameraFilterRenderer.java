package com.example.colorr;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CameraFilterRenderer implements GLSurfaceView.Renderer {


    private Context context;
    private int shaderProgram;
    private SurfaceTexture surfaceTexture;
    private int textureId = 0;
    private Bitmap cameraFrame;


    public CameraFilterRenderer(Context context) {
        this.context = context;
    }


    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }
    private boolean isTritanopiaEnabled = false;


    private boolean isDeuteranopiaEnabled = false;


    private boolean isPropanopiaEnabled = false;


    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;


    private final float[] squareCoords = {
            -1.0f,  1.0f,   // Top left
            -1.0f, -1.0f,   // Bottom left
            1.0f,  1.0f,   // Top right
            1.0f, -1.0f    // Bottom right
    };


    private final float[] texCoords = {
            0.0f, 0.0f,     // Top left
            0.0f, 1.0f,     // Bottom left
            1.0f, 0.0f,     // Top right
            1.0f, 1.0f      // Bottom right
    };


    private final float[] rgbToLmsMatrix = {
            17.8824f, 43.5161f, 4.11935f,
            3.45565f, 27.1554f, 3.86714f,
            0.0299566f, 0.184309f, 1.467f
    };


    private final float[] lmsToRgbMatrix = {
            0.0809444479f, -0.130504409f, 0.116721066f,
            0.113614708f, -0.0102485335f, -0.0540193266f,
            -0.000365296938f, -0.00412161469f, 0.693511405f
    };


    // Protanopia deficiency matrix
    private final float[] protanopiaMatrix = {
            0.0f, 2.02344f, -2.52581f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f
    };


    // Deuteranopia deficiency matrix
    private final float[] deuteranopiaMatrix = {
            1.0f, 0.0f, 0.0f,
            0.494207f, 0.0f, 1.24827f,
            0.0f, 0.0f, 1.0f
    };


    // Tritanopia deficiency matrix
    private final float[] tritanopiaMatrix = {
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            -0.395913f, 0.801109f, 0.0f
    };


    // Define error shift matrices for each deficiency type
    private final float[] protanopiaErrorShiftMatrix = {
            0.0f, 0.0f, 0.0f,
            0.7f, 1.0f, 0.0f,
            0.7f, 0.0f, 1.0f
    };


    private final float[] deuteranopiaErrorShiftMatrix = {
            1.0f, 0.7f, 0.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 0.7f, 1.0f
    };


    private final float[] tritanopiaErrorShiftMatrix = {
            1.0f, 0.0f, 0.7f,
            0.0f, 1.0f, 0.7f,
            0.0f, 0.0f, 0.0f
    };




    public boolean isPropanopiaEnabled() {
        return isPropanopiaEnabled;
    }


    public boolean isDeuteranopiaEnabled() {
        return isDeuteranopiaEnabled;
    }

    public boolean isTritanopiaEnabled() {
        return isTritanopiaEnabled;
    }

    public CameraFilterRenderer() {


        // Initialize buffers
        vertexBuffer = ByteBuffer.allocateDirect(squareCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(squareCoords).position(0);


        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoordBuffer.put(texCoords).position(0);
    }


    public void setTritanopiaMode(boolean enabled) {
        this.isTritanopiaEnabled = enabled;
        Log.d("CameraFilterRenderer", "Tritanopia mode set to: " + enabled);
    }


    public void setDeuteranopiaMode(boolean enabled) {
        this.isDeuteranopiaEnabled = enabled;
        Log.d("CameraFilterRenderer", "Deuteranopia mode set to: " + enabled);
    }


    public void setProtanopiaMode(boolean enabled) {
        this.isPropanopiaEnabled = enabled;
        Log.d("CameraFilterRenderer", "Protanopia mode set to: " + enabled);


    }


    public void setCameraFrame(Bitmap frame, GLSurfaceView glSurfaceView) {
        if (cameraFrame != null) {
            cameraFrame.recycle();
        }
        this.cameraFrame = frame;
        if (glSurfaceView != null) {
            glSurfaceView.requestRender();
        }
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Compile and link shaders
        shaderProgram = createShaderProgram(getVertexShaderCode(), getFragmentShaderCode());


        // Use the shader program before setting uniforms
        GLES20.glUseProgram(shaderProgram);


        // Assign the sampler to texture unit 0
        int textureUniform = GLES20.glGetUniformLocation(shaderProgram, "texture");
        GLES20.glUniform1i(textureUniform, 0); // GL_TEXTURE0


        // Generate and configure texture
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(shaderProgram);


        // Pass vertex data
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "position");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);


        // Pass texture coordinate data
        int texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "texCoord");
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);


        // Activate texture unit 0 and bind the texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        if (cameraFrame != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, cameraFrame, 0);
            cameraFrame.recycle();
            cameraFrame = null;
        }


        // Pass the mode uniforms for Tritanopia, Protanopia, and Deuteranopia
        setUniform(shaderProgram, "isTritanopiaMode", isTritanopiaEnabled ? 1 : 0);
        setUniform(shaderProgram, "isProtanopiaMode", isPropanopiaEnabled ? 1 : 0);
        setUniform(shaderProgram, "isDeuteranopiaMode", isDeuteranopiaEnabled ? 1 : 0);


        // Pass the correct color matrix based on the selected mode
        int colorMatrixLocation = GLES20.glGetUniformLocation(shaderProgram, "colorMatrix");
        if (colorMatrixLocation >= 0) {
            if (isTritanopiaEnabled) {
                GLES20.glUniformMatrix3fv(colorMatrixLocation, 1, false, tritanopiaMatrix, 0);
                Log.d("CameraFilterRenderer", "Applying Tritanopia matrix");
            } else if (isPropanopiaEnabled) {
                GLES20.glUniformMatrix3fv(colorMatrixLocation, 1, false, protanopiaMatrix, 0);
                Log.d("CameraFilterRenderer", "Applying Protanopia matrix");
            } else if (isDeuteranopiaEnabled) {
                GLES20.glUniformMatrix3fv(colorMatrixLocation, 1, false, deuteranopiaMatrix, 0);
                Log.d("CameraFilterRenderer", "Applying Deuteranopia matrix");
            } else {
                GLES20.glUniformMatrix3fv(colorMatrixLocation, 1, false, new float[9], 0); // No change for normal vision
            }
        }


        // Draw the quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


        // Cleanup
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }


    /**
     * Utility function to set integer uniforms
     */
    private void setUniform(int program, String name, int value) {
        int location = GLES20.glGetUniformLocation(program, name);
        if (location >= 0) {
            GLES20.glUniform1i(location, value);
            Log.d("CameraFilterRenderer", "Set uniform " + name + " to " + value);
        } else {
            Log.e("CameraFilterRenderer", "Error: Uniform " + name + " not found!");
        }
    }

    private String getVertexShaderCode() {
        return "attribute vec4 position;" +
                "attribute vec2 texCoord;" +
                "varying vec2 vTexCoord;" +
                "void main() {" +
                "  gl_Position = position;" +
                "  vTexCoord = texCoord;" +
                "}";
    }


    private String getFragmentShaderCode() {
        return "precision mediump float;\n" +
                "uniform sampler2D texture;\n" +
                "uniform bool isProtanopiaMode;\n" +
                "uniform bool isDeuteranopiaMode;\n" +
                "uniform bool isTritanopiaMode;\n" +
                "varying vec2 vTexCoord;\n" +
                "\n" +
                "// Constants for sRGB to linear RGB conversion and vice versa\n" +
                "const float epsilon = 1e-7;\n" +
                "\n" +
                "// Function to convert sRGB to linear RGB\n" +
                "vec3 sRGBToLinear(vec3 srgb) {\n" +
                "    return mix(srgb / 12.92, pow((srgb + 0.055) / 1.055, vec3(2.4)), step(0.04045, srgb));\n" +
                "}\n" +
                "\n" +
                "// Function to convert linear RGB to sRGB\n" +
                "vec3 linearToSRGB(vec3 linear) {\n" +
                "    linear = max(linear, vec3(epsilon));\n" +
                "    return mix(linear * 12.92, 1.055 * pow(linear, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, linear));\n" +
                "}\n" +
                "\n" +
                "// LMS color space matrix\n" +
                "mat3 lmsMatrix = mat3(\n" +
                "    17.8824, 43.5161, 4.11935,\n" +
                "    3.45565, 27.1554, 3.86714,\n" +
                "    0.0299566, 0.184309, 1.46709\n" +
                ");\n" +
                "\n" +
                "// CVD simulation matrices (Vienot et al. 1999)\n" +
                "mat3 protanopiaMatrix = mat3(\n" +
                "    0, 2.02344, -2.52581,\n" +
                "    0, 1, 0,\n" +
                "    0, 0, 1\n" +
                ");\n" +
                "\n" +
                "mat3 deuteranopiaMatrix = mat3(\n" +
                "    1, 0, 0,\n" +
                "    0.494207, 0, 1.24827,\n" +
                "    0, 0, 1\n" +
                ");\n" +
                "\n" +
                "mat3 tritanopiaMatrix = mat3(\n" +
                "    1, 0, 0,\n" +
                "    0, 1, 0,\n" +
                "    -0.395913, 0.801109, 0\n" +
                ");\n" +
                "\n" +
                "// CVD error modification matrices (adjust as needed)\n" +
                "mat3 protanopiaErrorMatrix = mat3(\n" +
                "    0, 0, 0,\n" +
                "    0.7, 1, 0,\n" +
                "    0.7, 0, 1\n" +
                ");\n" +
                "\n" +
                "mat3 deuteranopiaErrorMatrix = mat3(\n" +
                "    1, 0.7, 0,\n" +
                "    0, 0, 0,\n" +
                "    0, 0.7, 1\n" +
                ");\n" +
                "\n" +
                "mat3 tritanopiaErrorMatrix = mat3(\n" +
                "    1, 0, 0.7,\n" +
                "    0, 1, 0.7,\n" +
                "    0, 0, 0\n" +
                ");\n" +
                "\n" +
                "// Function to simulate CVD\n" +
                "vec3 simulateCVD(vec3 linearRGB, mat3 cvdMatrix) {\n" +
                "    vec3 lms = linearRGB * lmsMatrix;\n" +
                "    vec3 lms_cvd = lms * cvdMatrix;\n" +
                "    vec3 linearRGB_cvd = lms_cvd * inverse(lmsMatrix);\n" +
                "    return linearRGB_cvd;\n" +
                "}\n" +
                "\n" +
                "// Function to apply Daltonization\n" +
                "vec3 daltonize(vec3 color, bool isProtanopia, bool isDeuteranopia, bool isTritanopia) {\n" +
                "    vec3 linearRGB = sRGBToLinear(color);\n" +
                "\n" +
                "    mat3 cvdMatrix;\n" +
                "    mat3 errorMatrix;\n" +
                "    if (isProtanopia) {\n" +
                "        cvdMatrix = protanopiaMatrix;\n" +
                "        errorMatrix = protanopiaErrorMatrix;\n" +
                "    } else if (isDeuteranopia) {\n" +
                "        cvdMatrix = deuteranopiaMatrix;\n" +
                "        errorMatrix = deuteranopiaErrorMatrix;\n" +
                "    } else if (isTritanopia) {\n" +
                "        cvdMatrix = tritanopiaMatrix;\n" +
                "        errorMatrix = tritanopiaErrorMatrix;\n" +
                "    } else {\n" +
                "        return color; // No CVD mode selected, return original color\n" +
                "    }\n" +
                "\n" +
                "    vec3 simulatedLinearRGB = simulateCVD(linearRGB, cvdMatrix);\n" +
                "\n" +
                "    // Calculate error in linear RGB space\n" +
                "    vec3 error = linearRGB - simulatedLinearRGB;\n" +
                "\n" +
                "    // Apply error modification\n" +
                "    vec3 modifiedError = error * errorMatrix;\n" +
                "\n" +
                "    // Add modified error back to the original color\n" +
                "    vec3 daltonizedLinearRGB = linearRGB + modifiedError;\n" +
                "\n" +
                "    // Convert back to sRGB\n" +
                "    return linearToSRGB(daltonizedLinearRGB);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 originalColor = texture2D(texture, vTexCoord);\n" +
                "    vec3 daltonizedColor = daltonize(originalColor.rgb, isProtanopiaMode, isDeuteranopiaMode, isTritanopiaMode);\n" +
                "    gl_FragColor = vec4(daltonizedColor, originalColor.a);\n" +
                "}\n";
    }

    private int createShaderProgram(String vertexCode, String fragmentCode) {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexCode);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);


        int program = GLES20.glCreateProgram();
        if (program == 0) {
            Log.e("CameraFilterRenderer", "Error creating shader program");
            return 0;
        }


        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);


        // Check link status
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e("CameraFilterRenderer", "Program link error: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }


        return program;
    }


    private int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            Log.e("CameraFilterRenderer", "Error creating shader");
            return 0;
        }


        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);


        // Check compile status
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e("CameraFilterRenderer", "Shader compile error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }


        return shader;
    }
}

