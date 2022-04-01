package panyi.xyz.videoeditor.view.widget;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.ShaderUtil;
import panyi.xyz.videoeditor.view.VideoEditorGLView;

public class RectWidget implements IRender {
    private VideoEditorGLView contextView;

    private float[] position;
    private FloatBuffer positionBuf;

    private int bufId;
    private int programId;

    private int matrixUniformLocation;

    public RectWidget(VideoEditorGLView view){
        contextView = view;
    }

    @Override
    public void init() {
        position = new float[]{
                0.0f , 0.0f ,
                300.0f , 0.0f,
                300.0f , 400.0f,
                0.0f , 300.0f
        };

        positionBuf = ByteBuffer.allocateDirect(position.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(position);
        positionBuf.position(0);

        int[] bufferIds = new int[1];
        GLES30.glGenBuffers(1 , bufferIds , 0);

        bufId = bufferIds[0];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , bufId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , position.length * Float.BYTES , positionBuf , GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , 0);
        programId = ShaderUtil.buildShaderProgramFromAssets(contextView.getContext() ,
                "rect_vert.glsl",
                "rect_frag.glsl");
        LogUtil.formatLog("rect widget shader program Id = %d" , programId);

        matrixUniformLocation = GLES30.glGetUniformLocation(programId , "uMatrix");
        LogUtil.formatLog("program %d uMatrix loc %d" , programId , matrixUniformLocation);
    }

    @Override
    public void render() {
        LogUtil.log("rect widget start render");

        GLES30.glUseProgram(programId);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , bufId);
        GLES30.glVertexAttribPointer(0, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glUniformMatrix3fv(matrixUniformLocation , 1 , false ,
                contextView.camera.getMatrix(), 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN , 0 , 4);

        GLES30.glDisableVertexAttribArray(0);
    }

    @Override
    public void free() {
        LogUtil.log("rect widget free");
        int[] bufferIds = new int[1];
        bufferIds[0] = bufId;
        GLES30.glDeleteBuffers(1 , bufferIds , 0);
    }
}//end class
