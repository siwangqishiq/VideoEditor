package panyi.xyz.videoeditor.view;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import panyi.xyz.videoeditor.util.LogUtil;

public class VideoEditorGLView extends GLSurfaceView  implements GLSurfaceView.Renderer {
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
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES30.glViewport(0 , 0 , width , height);
        LogUtil.log("GLView width = " + width +" height = " + height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES30.glClearColor(0 , 0, 0, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }
}
