package com.example.colorr;

import android.content.Context;
import android.opengl.GLES20;
import java.nio.FloatBuffer;

public class CameraFilterShader {
    private final String vertexShaderCode =
            "attribute vec4 a_Position;" +
                    "attribute vec2 a_TexCoord;" +
                    "varying vec2 v_TexCoord;" +
                    "void main() {" +
                    "    gl_Position = a_Position;" +
                    "    v_TexCoord = a_TexCoord;" +
                    "}";

    private final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "uniform samplerExternalOES u_Texture;" +
                    "uniform float u_MinHue;" +
                    "uniform float u_MaxHue;" +
                    "varying vec2 v_TexCoord;" +

                    "void main() {" +
                    "    vec4 color = texture2D(u_Texture, v_TexCoord);" +
                    "    float maxVal = max(color.r, max(color.g, color.b));" +
                    "    float minVal = min(color.r, min(color.g, color.b));" +
                    "    float delta = maxVal - minVal;" +
                    "    float hue = 0.0;" +

                    "    if (delta == 0.0) {" +
                    "        hue = 0.0;" +
                    "    } else if (maxVal == color.r) {" +
                    "        hue = mod((color.g - color.b) / delta, 6.0);" +
                    "    } else if (maxVal == color.g) {" +
                    "        hue = (color.b - color.r) / delta + 2.0;" +
                    "    } else {" +
                    "        hue = (color.r - color.g) / delta + 4.0;" +
                    "    }" +
                    "    hue *= 60.0;" +
                    "    if (hue < 0.0) hue += 360.0;" +

                    "    if (hue >= u_MinHue && hue <= u_MaxHue) {" +
                    "        gl_FragColor = color;" +
                    "    } else {" +
                    "        float gray = 0.3 * color.r + 0.59 * color.g + 0.11 * color.b;" +
                    "        gl_FragColor = vec4(vec3(gray), color.a);" +
                    "    }" +
                    "}";

    private int program;
    private int positionHandle, texCoordHandle, textureHandle, minHueHandle, maxHueHandle;

    public CameraFilterShader(Context context) {
        program = loadProgram(vertexShaderCode, fragmentShaderCode);
    }

    public void init() {
        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoord");
        textureHandle = GLES20.glGetUniformLocation(program, "u_Texture");
        minHueHandle = GLES20.glGetUniformLocation(program, "u_MinHue");
        maxHueHandle = GLES20.glGetUniformLocation(program, "u_MaxHue");
    }

    public void draw(int textureId) {
        GLES20.glUseProgram(program);

        GLES20.glUniform1i(textureHandle, 0);  // Bind the texture
        GLES20.glUniform1f(minHueHandle, 150.0f); // Example hue range values
        GLES20.glUniform1f(maxHueHandle, 160.0f);

        // Set up your vertex and texture buffers here, then draw the elements.
        // This code assumes you've set up a vertex buffer with position and texture coordinates.

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    private int loadProgram(String vertexSource, String fragmentSource) {
        // Compile shaders and link program
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        return program;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
