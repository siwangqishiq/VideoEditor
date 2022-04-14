package panyi.xyz.videoeditor.view;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import panyi.xyz.videoeditor.model.VideoInfo;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.view.widget.Camera;
import panyi.xyz.videoeditor.view.widget.IRender;
import panyi.xyz.videoeditor.view.widget.RectWidget;
import panyi.xyz.videoeditor.view.widget.TextRenderHelper;
import panyi.xyz.videoeditor.view.widget.TimelineFramesWidget;
import panyi.xyz.videoeditor.view.widget.VideoFrameCopyWidget;
import panyi.xyz.videoeditor.view.widget.VideoFrameWidget;
import panyi.xyz.videoeditor.view.widget.VideoTimeline;
import panyi.xyz.videoeditor.view.widget.VideoWidget;

/**
 *
 *
 */
public class VideoEditorGLView extends GLSurfaceView  implements GLSurfaceView.Renderer {
    private VideoTimeline mVideoTimeline;

    public static final int TEXTURE_SIZE = 16;

    private Callback mCallback;

    private VideoInfo mVideoInfo;

    public VideoInfo getVideoInfo(){
        return mVideoInfo;
    }

    public interface Callback{
        void onVideoWidgetReady(VideoEditorGLView view);
    }

    public void setCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    public VideoEditorGLView(Context context) {
        super(context);
        init();
    }

    public VideoEditorGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setVideoInfo(VideoInfo info){
        mVideoInfo = info;
    }

    private void init(){
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mVideoTimeline = new VideoTimeline();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        screenWidth = width;
        screenHeight = height;

        GLES30.glViewport(0 , 0 , width , height);
        LogUtil.log("GLView width = " + width +" height = " + height);

        initComponent();

        onInit();
    }

    //正交投影矩阵
    public Camera camera;
    private float screenWidth;
    private float screenHeight;

//    public VideoFrameWidget videoFrameWidget;

//    public VideoFrameCopyWidget videoFrameCopyWidget;

    public TimelineFramesWidget timelineFramesWidget;

    private List<IRender> components = new ArrayList<IRender>(8);

    private TextRenderHelper textRenderHelper;

    private void initComponent(){
        camera = new Camera(0 , 0, screenWidth , screenHeight);

        //add components
//        IRender rectWidget = new RectWidget(this);
//        components.add(rectWidget);

//        VideoWidget videoWidget = new VideoWidget(this);
//        components.add(videoWidget);

//        videoFrameWidget = new VideoFrameWidget(this);
//        components.add(videoFrameWidget);

//        videoFrameCopyWidget = new VideoFrameCopyWidget(this);
//        components.add(videoFrameCopyWidget);

        timelineFramesWidget = new TimelineFramesWidget(this);
        components.add(timelineFramesWidget);

        if(mCallback != null){
            mCallback.onVideoWidgetReady(this );
        }
    }

    private void onInit(){
        textRenderHelper = new TextRenderHelper(getContext()  ,camera.viewWidth , camera.viewHeight);

        for(IRender render : components){
            render.init();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES30.glClearColor(0.0f , 0.0f, 0.0f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        onRender();
    }

    private void onRender(){
        for(IRender render : components){
            render.render();
        }
    }

    public void onDestroy(){
        onFree();
    }

    private void onFree(){
        textRenderHelper.free();
        for(IRender render : components){
            render.free();
        }
    }
}
