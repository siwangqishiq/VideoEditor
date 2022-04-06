package panyi.xyz.videoeditor.view.widget;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.microedition.khronos.opengles.GL10;

import panyi.xyz.videoeditor.model.VideoInfo;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.ShaderUtil;
import panyi.xyz.videoeditor.view.VideoEditorGLView;

public class VideoWidget implements IRender , SurfaceTexture.OnFrameAvailableListener {
    private VideoEditorGLView contextView;

    private float[] position;
    private FloatBuffer positionBuf;

    private float[] texture;
    private FloatBuffer textureBuf;

    private int posBufId;
    private int textureBufId;
    private int programId;

    private int matrixUniformLocation;

    private int textureId;

    public SurfaceTexture surfaceTexture;

    private AtomicInteger needUpdateTexture = new AtomicInteger(0);

    public VideoWidget(VideoEditorGLView view){
        contextView = view;
        prepareOesTextureSurface();
    }


    /**
     * 创建oes纹理
     * @return
     */
    public int createOesTexture(){
        int[] textures = new int[1];

        GLES30.glGenTextures(1, textures, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);

        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        int textureId = textures[0];
        return textureId;
    }

    private float[] calFrameSize(){
        VideoInfo info = contextView.getVideoInfo();
        float width = info.width;
        float height = info.height;

        float[] result = new float[2];
        if(width >= height){
            result[0] = contextView.camera.viewWidth;
            result[1] = result[0] / (width / height);
        }else{
            result[1] = contextView.camera.viewHeight;
            result[0] = result[1] * (width / height);
        }
        return result;
    }

    @Override
    public void init() {
        float[] size = calFrameSize();
        float viewWidth = size[0];
        float viewHeight = size[1];
        position = new float[]{
                0.0f , 0.0f ,
                viewWidth, 0.0f,
                viewWidth , viewHeight,
                0.0f , viewHeight
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

    /**
     * 创建Video 纹理
     *
     */
    private void prepareOesTextureSurface(){
        textureId = createOesTexture();
        surfaceTexture = new SurfaceTexture(textureId);

        surfaceTexture.setOnFrameAvailableListener(this);
    }

    @Override
    public void render() {
        // LogUtil.log("VideoWidget widget start render need update Texture " + needUpdateTexture.get());

        if(needUpdateTexture.get() != 0){
            LogUtil.log("update texture");
            surfaceTexture.updateTexImage();
            needUpdateTexture.decrementAndGet();
        }

        GLES30.glUseProgram(programId);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        GLES30.glVertexAttribPointer(0, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , textureBufId);
        GLES30.glVertexAttribPointer(1, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glUniformMatrix3fv(matrixUniformLocation , 1 , false ,
                contextView.camera.getMatrix(), 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES , textureId);

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

        int[] textureIds = new int[1];
        textureIds[0] = textureId;
        GLES30.glDeleteBuffers(1 , textureIds , 0);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        LogUtil.log("onFrameAvailable " + surfaceTexture.getTimestamp());

//        needUpdateTexture = true;
        needUpdateTexture.incrementAndGet();
        contextView.requestRender();
    }
}//end class
