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

import javax.microedition.khronos.opengles.GL10;

import panyi.xyz.videoeditor.model.VideoInfo;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.ShaderUtil;
import panyi.xyz.videoeditor.view.VideoEditorGLView;

public class TimelineFramesWidget implements IRender , SurfaceTexture.OnFrameAvailableListener {
    private VideoEditorGLView contextView;
    private FloatBuffer positionBuf;
    private FloatBuffer textureBuf;

    private final int FRAME_SIZE = 1;

    private int posBufId;
    private int textureBufId;
    private int programId;

    private int matrixUniformLocation;
    private int videoTextureMatrixLoc;

    private int videoFrameProgramId;
    private int videoFramePositionBufferId;

    private int videoOesTextureId;

    private SurfaceTexture surfaceTexture;

    private float[] originVideoTextureMatrix = new float[4 * 4];

    private int index = 0;

    private Surface videoSurface;

    //视频帧 显示 纹理ID
    private int videoFrameTextureId;

    public static class RenderCube{
        int textureId;
        int frameBufferId;
        float x;
        float y;
        float width;
        float height;
    }

    private SparseArray<RenderCube> renderCubeList = new SparseArray<RenderCube>(FRAME_SIZE);

    public TimelineFramesWidget(VideoEditorGLView view){
        contextView = view;
        createOesTexture();
        initRenderCubes();

        createVideoFrameTexture();
    }

    private void initRenderCubes(){
        renderCubeList.clear();

        float x = 0;
        float y= 0;
        float width = contextView.camera.viewWidth;
        float height = contextView.camera.viewHeight;

        for(int i = 0 ; i < FRAME_SIZE ; i ++){
            RenderCube renderCube = new RenderCube();
            renderCube.x = x;
            renderCube.y = y;
            renderCube.width = width;
            renderCube.height = height;
            renderCubeList.put(i , renderCube);

            x += width;
            if(x >= contextView.camera.viewWidth - width){
                x = 0;
                y += height;
            }
        }
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
        int textureIds[] = new int[renderCubeList.size()];
        GLES30.glGenTextures(textureIds.length, textureIds, 0);

        for(int i = 0 ; i < textureIds.length ; i++){
            int textureId = textureIds[i];

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D , 0 , GLES30.GL_RGBA ,
                    (int)contextView.camera.viewWidth,
                    (int)contextView.camera.viewHeight ,
                    0 , GLES30.GL_RGBA , GLES30.GL_UNSIGNED_BYTE , null);
            renderCubeList.get(i).textureId = textureId;
        }//end for i

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    public Surface getVideoSurface(){
        if(videoSurface != null){
            videoSurface.release();
        }

        videoSurface = new Surface(surfaceTexture);
        return videoSurface;
    }

    @Override
    public void init() {
        float viewWidth = contextView.camera.viewWidth;
        float viewHeight = contextView.camera.viewHeight;

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
        videoTextureMatrixLoc =  GLES30.glGetUniformLocation(videoFrameProgramId , "uTextureMatrix");

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
        int frameBufIds[] = new int[renderCubeList.size()];
        GLES30.glGenFramebuffers(frameBufIds.length , frameBufIds , 0);

        for(int i = 0 ; i < renderCubeList.size() ;i++){
            int frameBufferId = frameBufIds[i];
            RenderCube renderCube =renderCubeList.get(i);
            renderCube.frameBufferId = frameBufferId;

            int renderBufIds[] = new int[1];
            GLES30.glGenRenderbuffers(1 , renderBufIds , 0);
            int renderBufferId = renderBufIds[0];
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER , renderBufferId);
            GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER , GLES30.GL_DEPTH24_STENCIL8 ,
                    (int)contextView.camera.viewWidth , (int)contextView.camera.viewHeight);
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER , 0);

            GLES30.glBindFramebuffer(GLES20.GL_FRAMEBUFFER , renderCube.frameBufferId);
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0 ,
                    GLES30.GL_TEXTURE_2D , renderCube.textureId , 0);
            GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER , GLES30.GL_DEPTH_STENCIL_ATTACHMENT ,
                    GLES30.GL_RENDERBUFFER , renderBufferId);
        }//end for i

        GLES30.glBindFramebuffer(GLES20.GL_FRAMEBUFFER , 0);
    }

    @Override
    public void render() {
        copyOesVideoToTexture(nextFrameBufferId());

        // renderVideoFrameCube();
        for(int i = 0 ; i < renderCubeList.size() ; i ++){
            RenderCube renderCube = renderCubeList.get(i);
            renderVideoFrameCube(
                    renderCube.textureId,
                    renderCube.x,
                    renderCube.y,
                    renderCube.width,
                    renderCube.height
            );
        }//end for each
    }

    public void renderVideoFrameCube(int textureId , float x , float y , float width , float height){
        GLES30.glUseProgram(videoFrameProgramId);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);

        positionBuf.position(0);
        positionBuf.put(x);
        positionBuf.put(y);
        positionBuf.put(x +width);
        positionBuf.put(y);
        positionBuf.put(x +width);
        positionBuf.put(y + height);
        positionBuf.put(x);
        positionBuf.put(y + height);
        positionBuf.position(0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , videoFramePositionBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , 8 * Float.BYTES , positionBuf , GLES30.GL_STATIC_DRAW);
        GLES30.glVertexAttribPointer(0, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , textureBufId);
        GLES30.glVertexAttribPointer(1, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glUniformMatrix3fv(matrixUniformLocation , 1 , false ,
                contextView.camera.getMatrix(), 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D , textureId);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN , 0 , 4);

        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
    }

    /**
     *  framebuffer copy
     *
     */
    private void copyOesVideoToTexture(int frameBufId){
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(originVideoTextureMatrix);
//        LogUtil.logArray(matrix);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER , frameBufId);
        if(GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) != GLES30.GL_FRAMEBUFFER_COMPLETE){
            LogUtil.log(frameBufId + " framebuffer is Error!");
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER , 0);
            return;
        }

        GLES30.glEnable(GLES30.GL_DEPTH);
        GLES30.glClearColor(1.0f , 1.0f ,1.0f , 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        GLES30.glUseProgram(programId);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        GLES30.glVertexAttribPointer(0, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , textureBufId);
        GLES30.glVertexAttribPointer(1, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glUniformMatrix3fv(matrixUniformLocation , 1 , false ,
                contextView.camera.getMatrix(), 0);

        GLES30.glUniformMatrix4fv(videoTextureMatrixLoc , 1 , false ,
                originVideoTextureMatrix, 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES , videoOesTextureId);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN , 0 , 4);

        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER , 0);
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

        int textureIds[] = new int[renderCubeList.size()];
        for(int i = 0 ; i < textureIds.length;i++){
            textureIds[i] = renderCubeList.get(i).textureId;
        }
        GLES30.glDeleteTextures(renderCubeList.size() , textureIds , 0);

        int frameBufferIds[] = new int[renderCubeList.size()];
        for(int i = 0 ; i < frameBufferIds.length;i++){
            frameBufferIds[i] = renderCubeList.get(i).frameBufferId;
        }
        GLES30.glDeleteFramebuffers(frameBufferIds.length , frameBufferIds , 0);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//         LogUtil.log(" onFrameAvailable");
         contextView.requestRender();
    }

    public int nextFrameBufferId(){
        RenderCube renderCube = renderCubeList.get(index);
        index++;
        index = index % renderCubeList.size();
        return renderCube.frameBufferId;
    }
}//end class
