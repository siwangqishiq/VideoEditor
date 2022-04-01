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

    public void initView(ViewGroup container){
        mContainer = container;
        mContext = mContainer.getContext();
    }

    public void free(){
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
//        initMediaCodec();

//        new Thread(()->{
//            int frameCount = 0;
//            ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);
//            while(mVideoExtractor.readSampleData(buf , 0) >= 0){
//                frameCount++;
//                LogUtil.log("frameCount = " + frameCount
//                        +" buf_size=" + buf.remaining()
//                        +" sampleFlag = " + mVideoExtractor.getSampleFlags()
//                        +"  时间戳 = " + (mVideoExtractor.getSampleTime() / 1000));
//                mVideoExtractor.advance();
//            }//end while
//        }).start();

//        if(MediaUtils.hasCodecForMime(false , mVideoInfo.mime)){
//            mVideoExtractor.release();
//            mVideoExtractor = null;
//
//            Toast.makeText(mContext , R.string.video_format_no_support , Toast.LENGTH_LONG).show();
//            return Code.ERROR;
//        }

        // MediaUtils.hasCodecForMime(false , mVideoExtractor.getTrackFormat(mVideoExtractor.getSampleTrackIndex()));
        return Code.SUCCESS;
    }


    private void initMediaCodec(){
        try {
            MediaCodec codec = MediaCodec.createDecoderByType(mVideoInfo.mime);
            MediaFormat mediaFormat = mVideoExtractor.getTrackFormat(mVideoExtractor.getSampleTrackIndex());

            SurfaceTexture mainTexture = new SurfaceTexture(100);
            codec.configure(mediaFormat , new Surface(mainTexture), null ,0);

            codec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    if(index < 0){
                        return;
                    }

//                    LogUtil.log("onInputBufferAvailable index = " + index);

                    ByteBuffer buf = codec.getInputBuffer(index);
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
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    // LogUtil.log("presentationTimeUs = " + info.presentationTimeUs);

                    codec.releaseOutputBuffer(index , info.presentationTimeUs * 1000);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                }
            });
            codec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
