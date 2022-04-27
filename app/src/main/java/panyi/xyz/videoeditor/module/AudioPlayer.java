package panyi.xyz.videoeditor.module;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.MediaUtil;


public class AudioPlayer extends Thread implements IAudioPlayer {
    public static final int STATUS_IDLE = 0; //空闲
    public static final int STATUS_PREPARED = 1; //准备完成   可以开始播放

    private AudioTrack mTrack;

    private MediaExtractor mExtractor;
    private MediaFormat mMediaFormat;
    private MediaCodec mCodec;

    private AtomicBoolean isPlaying = new AtomicBoolean(false);

    private int mStatus = STATUS_IDLE;

    private IAudioPlayer.ProgressCallback mProgressCallback;

    private Handler mCallbackHandler;

    private long mDurationTime = -1;

    @Override
    public void prepare(String path) {
        mExtractor = new MediaExtractor();
        try {
            mExtractor = MediaUtil.createMediaExtractorByMimeType(path , MediaUtil.TYPE_AUDIO);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(mExtractor == null){
            return;
        }

        mMediaFormat = mExtractor.getTrackFormat(mExtractor.getSampleTrackIndex());
        LogUtil.L("info" , mMediaFormat.toString());

        mDurationTime = mMediaFormat.getLong(MediaFormat.KEY_DURATION);

        mStatus = STATUS_PREPARED;
    }

    @Override
    public void play() {
        start();
    }

    @Override
    public void run() {

        AudioTrack.Builder builder = new AudioTrack.Builder();
        builder.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());

        builder.setAudioFormat(new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build());

        builder.setBufferSizeInBytes(AudioTrack.getMinBufferSize(mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                AudioFormat.CHANNEL_IN_STEREO , AudioFormat.ENCODING_PCM_16BIT));
        mTrack = builder.build();


        //prepare mediacodec
        try {
            mCodec = MediaCodec.createDecoderByType(mMediaFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCodec.configure(mMediaFormat , null , null , 0);
        mCodec.start();

        isPlaying.set(true);

        mTrack.play();

        while(isPlaying.get()){
            try{
                int index = mCodec.dequeueInputBuffer(10_000);
                if (index >= 0) {
                    ByteBuffer inputBuffer = mCodec.getInputBuffer(index);
                    inputBuffer.clear();
                    mExtractor.readSampleData(inputBuffer , 0);
                    // LogUtil.L("decode audio" , "time: " + mExtractor.getSampleTime() +"  readsize: " + inputBuffer.remaining());
                    mCodec.queueInputBuffer(index,
                            0, inputBuffer.remaining(), mExtractor.getSampleTime(), 0);
                }

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 10_000);
                if (outputBufferIndex >= 0) {
                    // LogUtil.L("decode audio" , "time : " + bufferInfo.presentationTimeUs);
                    ByteBuffer outBuf = mCodec.getOutputBuffer(outputBufferIndex);
                    // LogUtil.L("decode audio" , "outBuf : " + outBuf.remaining());
                    final long currentTimeUs = bufferInfo.presentationTimeUs;
                    mTrack.write(outBuf ,outBuf.remaining(),
                            AudioTrack.WRITE_BLOCKING );
                    mCodec.releaseOutputBuffer(outputBufferIndex, false);

                    if(mProgressCallback != null){
                        if(mCallbackHandler == null){
                            mProgressCallback.onProgressUpdate(currentTimeUs / 1000 , mDurationTime / 1000);
                        }else{
                            mCallbackHandler.post(()->{
                                if(mProgressCallback == null){
                                    return;
                                }
                                mProgressCallback.onProgressUpdate(currentTimeUs / 1000 , mDurationTime / 1000);
                            });
                        }
                    }
                }else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                }

                if(!isPlaying.get()){
                    break;
                }

                boolean hasData = mExtractor.advance();
                if(!hasData){
                    break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }//end while

        LogUtil.log("read file ended!");
    }

    @Override
    public void pause() {

    }

    @Override
    public void free() {
        isPlaying.set(false);

        if(mExtractor != null){
            mExtractor.release();
        }

        if(mTrack != null){
            mTrack.release();
        }

        mCallbackHandler = null;
        mProgressCallback = null;
    }

    @Override
    public void setProgressListener(ProgressCallback callback, Handler handler) {
        mCallbackHandler = handler;
        mProgressCallback = callback;
    }
}
