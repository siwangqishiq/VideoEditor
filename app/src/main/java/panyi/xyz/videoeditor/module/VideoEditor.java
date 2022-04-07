package panyi.xyz.videoeditor.module;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.model.Code;
import panyi.xyz.videoeditor.model.SelectFileItem;
import panyi.xyz.videoeditor.model.VideoInfo;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.MediaUtil;
import panyi.xyz.videoeditor.view.VideoEditorGLView;
import panyi.xyz.videoeditor.view.widget.VideoFrameWidget;
import panyi.xyz.videoeditor.view.widget.VideoWidget;

/**
 *  视频编辑核心类
 *   用于与Ui集成
 */
public class VideoEditor {

    private ViewGroup mContainer;
    private Context mContext;

    private VideoEditorGLView mGLView;

    public VideoInfo mVideoInfo;//视频meta信息
    public MediaExtractor mVideoExtractor;

    public MediaCodec mVideoCodec;

    public void initView(ViewGroup container){
        mContainer = container;
        mContext = mContainer.getContext();
    }

    public void free(){
        if(mVideoCodec != null){
            mVideoCodec.release();
        }

        if(mVideoExtractor != null){
            mVideoExtractor.release();
        }
        if(mGLView != null){
            mGLView.onDestroy();
        }
    }

    private void addGLView(){
        mContainer.removeAllViews();

        mGLView = new VideoEditorGLView(mContainer.getContext());
        mGLView.setVideoInfo(mVideoInfo);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT , ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mGLView , params);
    }

    public int prepare(final SelectFileItem fileItem){
        try {
            mVideoExtractor = MediaUtil.createMediaExtractorByMimeType(fileItem.path , MediaUtil.TYPE_VIDEO);
        } catch (IOException e) {
            e.printStackTrace();
            mVideoExtractor = null;
        }

        if(mVideoExtractor == null){
            Toast.makeText(mContext , R.string.video_format_no_support , Toast.LENGTH_LONG).show();
            return Code.ERROR;
        }
        MediaFormat mediaFormat = mVideoExtractor.getTrackFormat(mVideoExtractor.getSampleTrackIndex());
        LogUtil.log(mediaFormat.toString());
        mVideoInfo = VideoInfo.build(mediaFormat);

        LogUtil.log("video meta: " + mVideoInfo.toString());

        addGLView();

        mGLView.setCallback(new VideoEditorGLView.Callback() {
            @Override
            public void onVideoWidgetReady(VideoEditorGLView view) {
                initMediaCodec(view);
            }
        });

        return Code.SUCCESS;
    }


    private void initMediaCodec(VideoEditorGLView view){
        try {
            mVideoCodec = MediaCodec.createDecoderByType(mVideoInfo.mime);
            MediaFormat mediaFormat = mVideoExtractor.getTrackFormat(mVideoExtractor.getSampleTrackIndex());
//            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE , 65536);
//            mVideoCodec.configure(mediaFormat , new Surface(videoWidget.surfaceTexture), null ,0);
            mVideoCodec.configure(mediaFormat , new Surface(view.videoFrameWidget.fetchNextSurfaceTexture()),
                    null ,0);

            mVideoCodec.setCallback(new MediaCodec.Callback() {
                private int readFrameCount = 0;

                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    if(index < 0){
                        return;
                    }
                    // LogUtil.log("onInputBufferAvailable index = " + index);
                    ByteBuffer buf = codec.getInputBuffer(index);

                    LogUtil.log("read sample time: " + mVideoExtractor.getSampleTime());
                    final int readSize = mVideoExtractor.readSampleData(buf , 0);

                    if(readSize > 0){
                        codec.queueInputBuffer(index , 0 , readSize , mVideoExtractor.getSampleTime() , 0);

                        //to next frame
                        mVideoExtractor.advance();
                    }else{
                        codec.queueInputBuffer(index , 0 , 0 , 0 , MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int outputBufferId, @NonNull MediaCodec.BufferInfo info) {
                    LogUtil.log("presentationTimeUs = " + info.presentationTimeUs +"   bufferId = "
                            + outputBufferId +" keyframe: "+  (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME));

//                    codec.releaseOutputBuffer(index , info.presentationTimeUs * 1000);
//                    final ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);
                    codec.releaseOutputBuffer(outputBufferId , true);

                    readFrameCount++;
                    codec.setOutputSurface(new Surface(view.videoFrameWidget.fetchNextSurfaceTexture()));
                    if(readFrameCount >= VideoFrameWidget.TEXTURE_COUNT){
                        codec.stop();
                        LogUtil.log("decode frame : " + readFrameCount +" media codec stop");
                    }
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                }
            });
            mVideoCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
