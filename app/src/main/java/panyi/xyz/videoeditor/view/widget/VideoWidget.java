package panyi.xyz.videoeditor.view.widget;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.ShaderUtil;
import panyi.xyz.videoeditor.view.VideoEditorGLView;

public class VideoWidget implements IRender {
    private VideoEditorGLView contextView;

    private float[] position;
    private FloatBuffer positionBuf;

    private float[] texture;
    private FloatBuffer textureBuf;

    private int posBufId;
    private int textureBufId;
    private int programId;

    private int matrixUniformLocation;

    public VideoWidget(VideoEditorGLView view){
        contextView = view;
    }

    @Override
    public void init() {
        position = new float[]{
                0.0f , 0.0f ,
                contextView.camera.viewWidth, 0.0f,
                contextView.camera.viewWidth , contextView.camera.viewHeight,
                0.0f , contextView.camera.viewHeight
        };

        positionBuf = ByteBuffer.allocateDirect(position.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(position);
        positionBuf.position(0);

        texture = new float[]{
            0.0f , 1.0f,
            1.0f , 1.0f,
            1.0f , 0.0f,
            0.0f , 0.0f,
        };

        textureBuf = ByteBuffer.allocateDirect(texture.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(texture);
        textureBuf.position(0);

        int[] bufferIds = new int[2];
        GLES30.glGenBuffers(2 , bufferIds , 0);

        posBufId = bufferIds[0];
        textureBufId = bufferIds[1];

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , position.length * Float.BYTES , positionBuf , GLES30.GL_STATIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , textureBufId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , texture.length * Float.BYTES , textureBuf , GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , 0);

        programId = ShaderUtil.buildShaderProgramFromAssets(contextView.getContext() ,
                "video_frame_vert.glsl",
                "video_frame_frag.glsl");
        LogUtil.formatLog("video_frame shader program Id = %d" , programId);

        matrixUniformLocation = GLES30.glGetUniformLocation(programId , "uMatrix");
        LogUtil.formatLog("program %d uMatrix loc %d" , programId , matrixUniformLocation);
    }

    @Override
    public void render() {
        LogUtil.log("VideoWidget widget start render");

        GLES30.glUseProgram(programId);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        GLES30.glVertexAttribPointer(0, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , textureBufId);
        GLES30.glVertexAttribPointer(1, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glUniformMatrix3fv(matrixUniformLocation , 1 , false ,
                contextView.camera.getMatrix(), 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN , 0 , 4);

        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
    }

    @Override
    public void free() {
        LogUtil.log("VideoWidget widget free");
        int[] bufferIds = new int[2];
        bufferIds[0] = posBufId;
        bufferIds[1] = textureBufId;
        GLES30.glDeleteBuffers(2 , bufferIds , 0);
    }
}//end class
