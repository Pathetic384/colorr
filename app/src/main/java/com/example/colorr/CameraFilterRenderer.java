package com.example.colorr;

import android.content.Context;
import android.graphics.Bitmap;
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

    private int shaderProgram = 0;
    private int textureId = 0;
    private Bitmap cameraFrame;
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

    private final float[] tritanopiaMatrix = {
            0.95f,  0.05f,  0.0f,
            0.0f,   0.433f, 0.567f,
            0.0f,   0.475f, 0.525f
    };

    private final float[] protanopiaMatrix = {
            0.567f,  0.433f,  0.0f,   // Red
            0.558f,  0.442f,  0.0f,   // Green
            0.0f,    0.242f,  0.758f  // Blue
    };

    private final float[] deuteranopiaMatrix = {
            0.625f,  0.375f,  0.0f,   // Red
            0.7f,    0.3f,    0.0f,   // Green
            0.0f,    0.3f,    0.7f    // Blue
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


    public void setHueRange(float minHue, float maxHue) {
        // Implement if needed
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

        // Pass the mode uniforms (Tritanopia, Protanopia, Deuteranopia)
        int isTritanopiaLocation = GLES20.glGetUniformLocation(shaderProgram, "isTritanopiaMode");
        int isProtanopiaLocation = GLES20.glGetUniformLocation(shaderProgram, "isProtanopiaMode");
        int isDeuteranopiaLocation = GLES20.glGetUniformLocation(shaderProgram, "isDeuteranopiaMode");

        GLES20.glUniform1i(isTritanopiaLocation, isTritanopiaEnabled ? 1 : 0);
        GLES20.glUniform1i(isProtanopiaLocation, isPropanopiaEnabled ? 1 : 0);
        GLES20.glUniform1i(isDeuteranopiaLocation, isDeuteranopiaEnabled ? 1 : 0);

        Log.d("CameraFilterRenderer", "isTritanopiaEnabled: " + isTritanopiaEnabled);
        Log.d("CameraFilterRenderer", "isProtanopiaEnabled: " + isPropanopiaEnabled);
        Log.d("CameraFilterRenderer", "isDeuteranopiaEnabled: " + isDeuteranopiaEnabled);

        // Pass the correct color matrix
        if (isTritanopiaEnabled) {
            int colorMatrixLocation = GLES20.glGetUniformLocation(shaderProgram, "colorMatrix");
            GLES20.glUniformMatrix3fv(colorMatrixLocation, 1, false, tritanopiaMatrix, 0);
            Log.d("CameraFilterRenderer", "Applying Tritanopia matrix");
        } else if (isPropanopiaEnabled) {
            int colorMatrixLocation = GLES20.glGetUniformLocation(shaderProgram, "colorMatrix");
            GLES20.glUniformMatrix3fv(colorMatrixLocation, 1, false, protanopiaMatrix, 0);
            Log.d("CameraFilterRenderer", "Applying Protanopia matrix");
        } else if (isDeuteranopiaEnabled) {
            int colorMatrixLocation = GLES20.glGetUniformLocation(shaderProgram, "colorMatrix");
            GLES20.glUniformMatrix3fv(colorMatrixLocation, 1, false, deuteranopiaMatrix, 0);
            Log.d("CameraFilterRenderer", "Applying Deuteranopia matrix");
        }

        // Draw the quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
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
        return
                "precision mediump float;" +
                        "uniform sampler2D texture;" +
                        "uniform bool isTritanopiaMode;" +
                        "uniform bool isProtanopiaMode;" +
                        "uniform bool isDeuteranopiaMode;" +
                        "uniform mat3 colorMatrix;" +
                        "varying vec2 vTexCoord;" +
                        "void main() {" +
                        "  vec4 color = texture2D(texture, vTexCoord);" +
                        "  if (isTritanopiaMode) {" +
                        "    vec3 transformed = colorMatrix * color.rgb;" +
                        "    gl_FragColor = vec4(transformed, color.a);" +
                        "  } else if (isProtanopiaMode) {" +
                        "    vec3 transformed = colorMatrix * color.rgb;" +
                        "    gl_FragColor = vec4(transformed, color.a);" +
                        "  } else if (isDeuteranopiaMode) {" +
                        "    vec3 transformed = colorMatrix * color.rgb;" +
                        "    gl_FragColor = vec4(transformed, color.a);" +
                        "  } else {" +
                        "    gl_FragColor = color;" +
                        "  }" +
                        "}";
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