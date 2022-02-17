package panyi.xyz.videoeditor.module;

import android.content.Context;
import android.view.ViewGroup;

import panyi.xyz.videoeditor.view.VideoEditorGLView;

/**
 *  视频编辑核心类
 *   用于与Ui集成
 */
public class VideoEditor {

    private ViewGroup mContainer;

    private VideoEditorGLView mGLView;

    public void prepare(ViewGroup container){
        mContainer = container;
        addGLView();
    }

    public void free(){

    }

    private void addGLView(){
        mContainer.removeAllViews();

        mGLView = new VideoEditorGLView(mContainer.getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT , ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mGLView , params);
    }
}
