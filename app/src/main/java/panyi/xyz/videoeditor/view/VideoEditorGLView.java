package panyi.xyz.videoeditor.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import panyi.xyz.videoeditor.model.VideoInfo;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.OpenglEsUtils;
import panyi.xyz.videoeditor.util.TimeUtil;
import panyi.xyz.videoeditor.view.widget.Camera;
import panyi.xyz.videoeditor.view.widget.IRender;
import panyi.xyz.videoeditor.view.widget.TextRenderHelper;
import panyi.xyz.videoeditor.view.widget.TimelineFramesWidget;

/**
 *
 *
 */
public class VideoEditorGLView extends GLSurfaceView  implements GLSurfaceView.Renderer {
    public static final int TEXTURE_SIZE = 16;

    private Callback mCallback;

    private GetPixelCallback mGetPixelCallback;

    private VideoInfo mVideoInfo;

    public VideoInfo getVideoInfo(){
        return mVideoInfo;
    }

    public void setCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    public interface Callback{
        void onVideoWidgetReady(VideoEditorGLView view);
    }

    public interface GetPixelCallback{
        void onGetBitmap(Bitmap bitmap , long timeStamp);
    }

    public VideoEditorGLView(Context context) {
        super(context);
        init();
    }

    public VideoEditorGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GetPixelCallback getGetPixelCallback() {
        return mGetPixelCallback;
    }

    public void setGetPixelCallback(GetPixelCallback mGetPixelCallback) {
        this.mGetPixelCallback = mGetPixelCallback;
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

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        screenWidth = width;
        screenHeight = height;

        GLES30.glViewport(0 , 0 , width , height);
        LogUtil.log("GLView width = " + width +" height = " + height);

        GLES30.glEnable(GLES30.GL_DEPTH_TEST );
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

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

    //文字渲染
    public TextRenderHelper textRenderHelper;

    public long currentTimeStamp = 0;

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

    float x =0 ;
    float y = 0;

    int catchCount = 0;
    int frameCount = 0;

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES30.glClearColor(0.0f , 0.0f, 0.0f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        onRender();

        textRenderHelper.renderText(String.valueOf(frameCount) , 20 , screenHeight - 300 , 300);

        String showTime = TimeUtil.mediaTimeDuration(currentTimeStamp);
        int strWidth = textRenderHelper.calculateTextSize(showTime , 300);
        textRenderHelper.renderText(showTime, screenWidth - strWidth , 0 , 300);

        OpenglEsUtils.debugFps();

        frameCount++;

        scehduleCallback();
    }

    private void scehduleCallback(){
//        if(mGetPixelCallback != null){
//            if(catchCount <= Integer.MAX_VALUE){
//                final Bitmap bitmap = getPixelsFromBuffer(0 , 0 ,(int)screenWidth , (int)screenHeight);
//                mGetPixelCallback.onGetBitmap(bitmap , currentTimeStamp);
//            }
//            catchCount++;
//        }
    }

    private void onRender(){
        for(IRender render : components){
            render.render();
        }
    }

    public static Bitmap getPixelsFromBuffer(int x, int y, int width, int height) {
        int b[] = new int[width * (y + height)];
        int bt[] = new int[width * height];

        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);
        GLES20.glReadPixels(x, y, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

        for (int i = 0, k = 0; i < height; i++, k++) {
            for (int j = 0; j < width; j++) {
                int pix = b[i * width + j];
                int pb = (pix >> 16) & 0xff;
                int pr = (pix << 16) & 0x00ff0000;
                int pix1 = (pix & 0xff00ff00) | pr | pb;
                bt[(height - k - 1) * width + j] = pix1;
            }
        }
        return Bitmap.createBitmap(bt, width, height, Bitmap.Config.ARGB_8888);
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
