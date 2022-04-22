package panyi.xyz.videoeditor.module;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.model.Code;
import panyi.xyz.videoeditor.model.SelectFileItem;
import panyi.xyz.videoeditor.model.VideoInfo;
import panyi.xyz.videoeditor.util.FileUtil;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.MediaUtil;
import panyi.xyz.videoeditor.view.VideoEditorGLView;
import panyi.xyz.videoeditor.view.widget.VideoFrameWidget;
import panyi.xyz.videoeditor.view.widget.VideoWidget;

import static android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC;

/**
 *  视频编辑核心类
 *   用于与Ui集成
 */
public class VideoEditor {

    private final String WORK_DIR = "videoeditor";

    private ViewGroup mContainer;
    private Context mContext;

    private VideoEditorGLView mGLView;

    public VideoInfo mVideoInfo;//视频meta信息
    public MediaExtractor mVideoExtractor;
    
    public MediaCodec mVideoCodec;

    private int readFrameCount = 0;

    private boolean isRunning = false;

    private boolean isPause = false;

    private long pauseTime = -1;

    private Handler uiHandler = new Handler();

    public interface GetThumbImageCallback{
        void onGetThumbImage(String filepath);
    }

    private GetThumbImageCallback mGetPixelCallback;

    private File workDir;

    public void initView(ViewGroup container){
        mContainer = container;
        mContext = mContainer.getContext();
    }

    public void free(){
        isRunning = false;
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
        createWorkDir(fileItem.name);

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
        LogUtil.L("videoInfo" , "mime : " + mediaFormat.getString(MediaFormat.KEY_MIME));

        addGLView();

        mGLView.setGetPixelCallback(new VideoEditorGLView.GetPixelCallback() {
            @Override
            public void onGetBitmap(Bitmap bitmap, long timeStamp) {
                final String imageFilePath = saveBitmapToFile(bitmap , timeStamp);
                if(TextUtils.isEmpty(imageFilePath)){
                    return;
                }

                uiHandler.post(()->{
                    if(mGetPixelCallback != null){
                        mGetPixelCallback.onGetThumbImage(imageFilePath);
                    }
                });
            }
        });

        mGLView.setCallback(new VideoEditorGLView.Callback() {
            @Override
            public void onVideoWidgetReady(VideoEditorGLView view) {
                initMediaCodec(view);
            }
        });
        return Code.SUCCESS;
    }

    /**
     * 创建工作目录
     * @param filename
     */
    private void createWorkDir(String filename){
        filename = FileUtil.findNameFromPath(filename);
        workDir = new File(mContext.getCacheDir() , WORK_DIR + File.pathSeparator+ filename);
        if(!workDir.exists()){
            workDir.mkdir();
        }
    }


    private String saveBitmapToFile(Bitmap bitmap, long currentTimeStamp){
        File file =  new File(workDir , String.format("%d_thumb.jpeg" , currentTimeStamp));
        file.deleteOnExit();

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            if(bitmap.compress(Bitmap.CompressFormat.JPEG , 100 , outputStream)){
                outputStream.flush();
                outputStream.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file.getAbsolutePath();
    }

    private void initMediaCodec(VideoEditorGLView view){
        try {
            mVideoCodec = MediaCodec.createDecoderByType(mVideoInfo.mime);
            MediaFormat mediaFormat = mVideoExtractor.getTrackFormat(mVideoExtractor.getSampleTrackIndex());
//            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE , 65536);
//            mVideoCodec.configure(mediaFormat , new Surface(videoWidget.surfaceTexture), null ,0);
            mVideoCodec.configure(mediaFormat , view.timelineFramesWidget.getVideoSurface(),
                    null ,0);

            mVideoCodec.setCallback(new MediaCodec.Callback() {
                private long lastFrameTime = -1;

                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    if(!isRunning || isPause){
                        return;
                    }

                    if(index < 0){
                        return;
                    }

                    // LogUtil.log("onInputBufferAvailable index = " + index);
//                    LogUtil.log("read sample time: " + mVideoExtractor.getSampleTime());
                    ByteBuffer buf = codec.getInputBuffer(index);
                    final int readSize = mVideoExtractor.readSampleData(buf , 0);

                    if(readSize > 0){
                        codec.queueInputBuffer(index , 0 , readSize , mVideoExtractor.getSampleTime() , 0);
                        mVideoExtractor.advance();
                    }else{
                        LogUtil.log("video end!");
                        codec.queueInputBuffer(index , 0 , 0 , 0 , MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int outputBufferId, @NonNull MediaCodec.BufferInfo info) {
                    if(!isRunning){
                        return;
                    }

//                    LogUtil.i("presentationTimeUs = " + info.presentationTimeUs +"   bufferId = "
//                            + outputBufferId +" keyframe: "+  (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME));

//                    codec.releaseOutputBuffer(index , info.presentationTimeUs * 1000);
//                    final ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);

//                    codec.releaseOutputBuffer(outputBufferId , true);

                    codec.releaseOutputBuffer(outputBufferId , true);

                    long time = info.presentationTimeUs / 1000;
                    view.currentTimeStamp = time;

//                    if(time - lastFrameTime >= 60 * 1000){
//                        lastFrameTime = time;
//                        codec.releaseOutputBuffer(outputBufferId , true);
//                    }else{
//                        codec.releaseOutputBuffer(outputBufferId , false);
//                    }

//                    codec.releaseOutputBuffer(outputBufferId , true);
//                    codec.releaseOutputBuffer(outputBufferId , info.presentationTimeUs * 1000);

//                    view.videoFrameWidget.fetchNextSurfaceTexture();
//                     codec.setOutputSurface(view.videoFrameWidget.fetchNextSurfaceTexture());
                    // readFrameCount++;
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    LogUtil.i("decode error " + e.toString());
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    LogUtil.i("decode onOutputFormatChanged " + format);
                }
            });
            mVideoCodec.start();
            isRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pauseOrResume(){
        if(mVideoCodec == null){
            return;
        }

        if(isPause){//play
            mVideoExtractor.seekTo(pauseTime , MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            initMediaCodec(mGLView);
        }else{//pause
            mVideoCodec.stop();

            pauseTime = mVideoExtractor.getSampleTime();
        }

        isPause = !isPause;
    }

    public void suspendMediaCodec(boolean pause){
        if(mVideoCodec == null)
            return;

//        final Bundle params = new Bundle();
//        params.putInt(MediaCodec.PARAMETER_KEY_SUSPEND, pause ? 1 : 0);
//        mVideoCodec.setParameters(params);

        isPause = pause;
    }

    public void setGetPixelCallback(GetThumbImageCallback callback) {
        this.mGetPixelCallback = callback;
    }

    public void decodeNextFrame(){
        readFrameCount = 0;
        mVideoCodec.start();
    }
}
