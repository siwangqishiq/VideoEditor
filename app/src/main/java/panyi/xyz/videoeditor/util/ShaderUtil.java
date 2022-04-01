package panyi.xyz.videoeditor.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLUtils.texImage2D;

public class ShaderUtil {
    public static int loadTexture(Context context, int resourceId) {
        final int[] textureObjectIds = new int[1];
        GLES30.glGenTextures(1, textureObjectIds, 0);
        if (textureObjectIds[0] == 0) {
            return 0;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        final Bitmap bitmap = BitmapFactory.decodeResource(
                context.getResources(), resourceId, options);

        if (bitmap == null) {
            GLES30.glDeleteTextures(1, textureObjectIds, 0);
            return 0;
        }
        GLES30.glBindTexture(GL_TEXTURE_2D, textureObjectIds[0]);

        // black.
        GLES30.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GLES30.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLES30.glTexParameteri(GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        // Load the bitmap into the bound texture.
        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
        glGenerateMipmap(GL_TEXTURE_2D);

        bitmap.recycle();
        // Unbind from the texture.
        GLES30.glBindTexture(GL_TEXTURE_2D, 0);
        return textureObjectIds[0];
    }

    /**
     *  从Assets中读取shader 构建
     *
     * @param context
     * @param vertexFileName
     * @param fragmentFileName
     * @return
     */
    public static int buildShaderProgramFromAssets(Context context , String vertexFileName , String fragmentFileName){
        final String vertexSrc = readAssetFileAsText(context , vertexFileName);
        final String fragmentSrc = readAssetFileAsText(context , fragmentFileName);

        return buildShaderProgram(vertexSrc, fragmentSrc);
    }

    /**
     * 创建shader program
     *
     * @param vertexShaderCode
     * @param fragShaderCode
     * @return
     */
    public static int buildShaderProgram(String vertexShaderCode, String fragShaderCode) {
        int program = GLES30.glCreateProgram();

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragShaderCode);

        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragShader);
        GLES30.glLinkProgram(program);

        final int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            LogUtil.log("Linking of program failed.");
            LogUtil.log("Results of linking program:\n" + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            return -1;
        }

        return program;
    }

    /**
     * 读取Asset目录下的文本文件
     * @param context
     * @param path
     * @return
     */
    public static String readAssetFileAsText(final Context context ,final String path){
        StringBuilder sb = new StringBuilder();
        InputStream is = null;
        try {
            is = context.getAssets().open(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8 ));
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str).append("\n");
            }//end while
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.log(e.getMessage());
            return null;
        }
        return sb.toString();
    }

    /**
     * 导入 顶点着色器 或者 片段着色器源码
     *
     * @param type
     * @param shaderCode
     * @return
     */
    public static int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        if (shader == 0) {
            LogUtil.log("create shader error!");
        }

        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        final int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            LogUtil.log("Results of compiling source:" + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return -1;
        }

        return shader;
    }
}
