package panyi.xyz.videoeditor.view;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import panyi.xyz.videoeditor.math.Matrix;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.ShaderUtil;
import panyi.xyz.videoeditor.view.widget.Camera;
import panyi.xyz.videoeditor.view.widget.VideoTimeline;

/**
 *
 *
 */
public class VideoEditorGLView extends GLSurfaceView  implements GLSurfaceView.Renderer {
    private VideoTimeline mVideoTimeline;

    public static final int TEXTURE_SIZE = 16;

    public VideoEditorGLView(Context context) {
        super(context);
        init();
    }

    public VideoEditorGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
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
        initTextures();
    }

    private void initTextures(){
        int[] ids = new int[TEXTURE_SIZE];
        GLES30.glGenTextures(TEXTURE_SIZE , ids , 0);


    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        screenWidth = width;
        screenHeight = height;

        GLES30.glViewport(0 , 0 , width , height);
        LogUtil.log("GLView width = " + width +" height = " + height);

        initComponent();
    }

    //正交投影矩阵
    private Camera mCamera;
    private float screenWidth;
    private float screenHeight;

    private int shaderProgramId;

    private void initComponent(){
        mCamera = new Camera(0 , 0, screenWidth , screenHeight);
        loadShader();
    }

    private void loadShader(){
        shaderProgramId = ShaderUtil.buildShaderProgramFromAssets(getContext() , "simple_vert.glsl","simple_frag.glsl");
        LogUtil.log("create shader progeamId = " + shaderProgramId);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES30.glClearColor(0 , 0, 0, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        mVideoTimeline.render();
    }

    public void onDestory(){
        IntBuffer buf = ByteBuffer.allocate(4).asIntBuffer();
    }
}
