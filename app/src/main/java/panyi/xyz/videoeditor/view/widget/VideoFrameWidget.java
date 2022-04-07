package panyi.xyz.videoeditor.view.widget;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.microedition.khronos.opengles.GL10;

import panyi.xyz.videoeditor.model.VideoInfo;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.ShaderUtil;
import panyi.xyz.videoeditor.view.VideoEditorGLView;

public class VideoFrameWidget implements IRender , SurfaceTexture.OnFrameAvailableListener {
    private VideoEditorGLView contextView;

    private float[] position;
    private FloatBuffer positionBuf;

    private float[] texture;
    private FloatBuffer textureBuf;

    private int posBufId;
    private int textureBufId;
    private int programId;

    private int matrixUniformLocation;

    private SparseArray<SurfaceTexture> surfaceTextures;

    private AtomicInteger needUpdateTexture = new AtomicInteger(0);

    public static final int TEXTURE_COUNT = 64;

    private int[] textureIds;

    private int index = 0;

    public VideoFrameWidget(VideoEditorGLView view){
        contextView = view;
        prepareOesTextureSurface();
    }

    public SurfaceTexture fetchNextSurfaceTexture(){
        final SurfaceTexture result = surfaceTextures.get(textureIds[index]);
        index++;
        index = index % textureIds.length;
        return result;
    }


    /**
     * 创建oes纹理
     * @return
     */
    public void createOesTexture(){
        textureIds = new int[TEXTURE_COUNT];

        GLES30.glGenTextures(textureIds.length, textureIds, 0);
        for(int textureId : textureIds){
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        }//end for each
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
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
        createOesTexture();
        surfaceTextures = new SparseArray<SurfaceTexture>(textureIds.length);

        for(int id : textureIds){
            surfaceTextures.put(id , new SurfaceTexture(id));
            surfaceTextures.get(id).setOnFrameAvailableListener(this);
        }//end for each
    }

    @Override
    public void render() {
        float size = 120.0f;
        float left = 0;
        float top = 0;
        float padding = 10.0f;

        for(int i = 0 ; i < textureIds.length;i++){
            if(left + size > contextView.camera.viewWidth){
                left = 0.0f;
                top += (size + padding);
            }

            renderFrame(left , top , size , size , textureIds[i]);
            left += (size + padding);
        }//end for each
    }

    public void renderFrame(float left , float top , float width , float height , int textureId){
        SurfaceTexture surfaceTexture = surfaceTextures.get(textureId);
        if(surfaceTexture == null)
            return;
        surfaceTexture.updateTexImage();

        positionBuf.position(0);
        positionBuf.put(left );
        positionBuf.put(top);
        positionBuf.put(left + width);
        positionBuf.put(top);
        positionBuf.put(left + width);
        positionBuf.put(top + height);
        positionBuf.put(left);
        positionBuf.put(top + height);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        positionBuf.position(0);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , position.length * Float.BYTES , positionBuf , GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , 0);

        GLES30.glUseProgram(programId);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);

//        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        positionBuf.position(0);
        GLES30.glVertexAttribPointer(0, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , positionBuf);

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

        GLES30.glDeleteBuffers(textureIds.length , textureIds , 0);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        contextView.requestRender();
    }
}//end class
