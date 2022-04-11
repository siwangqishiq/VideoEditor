package panyi.xyz.videoeditor.view.widget;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.SparseArray;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.microedition.khronos.opengles.GL10;

import panyi.xyz.videoeditor.model.VideoInfo;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.ShaderUtil;
import panyi.xyz.videoeditor.view.VideoEditorGLView;

public class VideoFrameCopyWidget implements IRender , SurfaceTexture.OnFrameAvailableListener {
    private VideoEditorGLView contextView;
    private FloatBuffer positionBuf;

    private FloatBuffer textureBuf;

    private int posBufId;
    private int textureBufId;
    private int programId;

    private int matrixUniformLocation;

    private int videoFrameProgramId;
    private int videoFramePositionBufferId;

    private int videoOesTextureId;

    private SurfaceTexture surfaceTexture;

    private int index = 0;

    private int frameBufferId;

    private Surface videoSurface;

    private int renderBufferId;

    //视频帧 显示 纹理ID
    private int videoFrameTextureId;

    public VideoFrameCopyWidget(VideoEditorGLView view){
        contextView = view;
        createOesTexture();
        createVideoFrameTexture();
    }

    /**
     * 创建oes纹理
     * @return
     */
    public void createOesTexture(){
        int textureIds[] = new int[1];

        GLES30.glGenTextures(textureIds.length, textureIds, 0);
        videoOesTextureId = textureIds[0];

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoOesTextureId);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        surfaceTexture = new SurfaceTexture(videoOesTextureId);
        surfaceTexture.setOnFrameAvailableListener(this);
    }

    /**
     * 创建用于显示的普通纹理
     *
     */
    private void createVideoFrameTexture(){
        int textureIds[] = new int[1];
        GLES30.glGenTextures(textureIds.length, textureIds, 0);

        videoFrameTextureId = textureIds[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, videoFrameTextureId);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D , 0 , GLES30.GL_RGBA ,
                (int)contextView.camera.viewWidth,
                (int)contextView.camera.viewHeight ,
                0 , GLES30.GL_RGBA , GLES30.GL_UNSIGNED_BYTE , null);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    public Surface getVideoSurface(){
        if(videoSurface == null){
            videoSurface = new Surface(surfaceTexture);
        }
        return videoSurface;
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
        float position[] = new float[]{
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


        int[] bufferIds = new int[3];
        GLES30.glGenBuffers(bufferIds.length , bufferIds , 0);

        posBufId = bufferIds[0];
        textureBufId = bufferIds[1];
        videoFramePositionBufferId = bufferIds[2];

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , position.length * Float.BYTES , positionBuf , GLES30.GL_STATIC_DRAW);

        float texture[] = new float[]{
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

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , textureBufId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , texture.length * Float.BYTES , textureBuf , GLES30.GL_STATIC_DRAW);
        // GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , 0);

        programId = ShaderUtil.buildShaderProgramFromAssets(contextView.getContext() ,
                "video_frame_vert.glsl",
                "video_frame_frag.glsl");
        LogUtil.formatLog("video_frame shader program Id = %d" , programId);

        matrixUniformLocation = GLES30.glGetUniformLocation(programId , "uMatrix");
        LogUtil.formatLog("program %d uMatrix loc %d" , programId , matrixUniformLocation);


        videoFrameProgramId = ShaderUtil.buildShaderProgramFromAssets(contextView.getContext() ,
                "video_frame_cube_vert.glsl",
                "video_frame_cube_frag.glsl");
        LogUtil.formatLog("videoFrameProgramId shader program Id = %d" , videoFrameProgramId);
        float x = 0;
        float y = 0;
        float width = 200;
        float height = 200;
        float framePosition[] = {
                x , y ,
                x +width, y,
                x +width, y + height,
                x, y + height,
        };

        FloatBuffer framePositionBuf = ByteBuffer.allocateDirect(framePosition.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(framePosition);
        framePositionBuf.position(0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , videoFramePositionBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , framePosition.length * Float.BYTES , framePositionBuf , GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , 0);

        initFrameBuffer();
    }

    private void initFrameBuffer(){
        int frameBufIds[] = new int[1];
        GLES30.glGenFramebuffers(frameBufIds.length , frameBufIds , 0);
        frameBufferId = frameBufIds[0];

        int renderBufIds[] = new int[1];
        GLES30.glGenRenderbuffers(1 , renderBufIds , 0);
        renderBufferId = renderBufIds[0];
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER , renderBufferId);
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER , GLES30.GL_DEPTH24_STENCIL8 ,
                (int)contextView.camera.viewWidth , (int)contextView.camera.viewHeight);
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER , 0);

        GLES30.glBindFramebuffer(GLES20.GL_FRAMEBUFFER , frameBufferId);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0 ,
                GLES30.GL_TEXTURE_2D , videoFrameTextureId , 0);
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER , GLES30.GL_DEPTH_STENCIL_ATTACHMENT ,
                GLES30.GL_RENDERBUFFER , renderBufferId);
        GLES30.glBindFramebuffer(GLES20.GL_FRAMEBUFFER , 0);
    }

    @Override
    public void render() {
        // renderFrame(left , top , size , size);
        copyOesVideoToTexture();
        renderVideoFrameCube();
    }

    public void renderVideoFrameCube(){
        GLES30.glUseProgram(videoFrameProgramId);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , videoFramePositionBufferId);
        GLES30.glVertexAttribPointer(0, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , textureBufId);
        GLES30.glVertexAttribPointer(1, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glUniformMatrix3fv(matrixUniformLocation , 1 , false ,
                contextView.camera.getMatrix(), 0);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN , 0 , 4);

        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
    }

    /**
     *  framebuffer copy
     *
     */
    private void copyOesVideoToTexture(){
        surfaceTexture.updateTexImage();

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER , frameBufferId);
        if(GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) != GLES30.GL_FRAMEBUFFER_COMPLETE){
            LogUtil.w(frameBufferId + " framebuffer is Error!");
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER , 0);
            return;
        }


        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER , 0);
    }

    public void renderFrame(float left , float top , float width , float height){
        // LogUtil.log("render frame");
        surfaceTexture.updateTexImage();

//        positionBuf.position(0);
//        positionBuf.put(left );
//        positionBuf.put(top);
//        positionBuf.put(left + width);
//        positionBuf.put(top);
//        positionBuf.put(left + width);
//        positionBuf.put(top + height);
//        positionBuf.put(left);
//        positionBuf.put(top + height);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        positionBuf.position(0);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , 8 * Float.BYTES , positionBuf , GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , 0);

        GLES30.glUseProgram(programId);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);

        positionBuf.position(0);
        GLES30.glVertexAttribPointer(0, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , positionBuf);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , textureBufId);
        GLES30.glVertexAttribPointer(1, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glUniformMatrix3fv(matrixUniformLocation , 1 , false ,
                contextView.camera.getMatrix(), 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES , videoOesTextureId);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN , 0 , 4);

        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
    }

    @Override
    public void free() {
        LogUtil.log("VideoWidget widget free");
        int[] bufferIds = new int[3];
        bufferIds[0] = posBufId;
        bufferIds[1] = textureBufId;
        bufferIds[2] = videoFramePositionBufferId;
        GLES30.glDeleteBuffers(2 , bufferIds , 0);

        int oesTextureIds[] = new int[1];
        oesTextureIds[0] = videoOesTextureId;
        GLES30.glDeleteBuffers(oesTextureIds.length , oesTextureIds , 0);

        int textureIds[] = new int[1];
        textureIds[0] = videoFrameTextureId;
        GLES30.glDeleteBuffers(textureIds.length , textureIds , 0);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        LogUtil.log(" onFrameAvailable");
         contextView.requestRender();
    }
}//end class
