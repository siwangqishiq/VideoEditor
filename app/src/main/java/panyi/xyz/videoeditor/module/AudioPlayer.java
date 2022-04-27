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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.MediaUtil;


public class AudioPlayer implements IAudioPlayer {
    private AudioTrack mTrack;

    private MediaExtractor mExtractor;
    private MediaFormat mMediaFormat;
    private MediaCodec mCodec;

    private AtomicBoolean isPlaying = new AtomicBoolean(false);

    private int mStatus = STATE_IDLE;

    private IAudioPlayer.ProgressCallback mProgressCallback;

    private Handler mCallbackHandler;

    //总时长 微秒单位
    private long mDurationTimeUs = -1;

    private long mCurrentTimeUs = -1;

    public static class AudioData{
        byte data[];
        long timeStamp;

        public AudioData(byte[] data , long timeStamp) {
            this.data = data;
            this.timeStamp = timeStamp;
        }
    }

    private LinkedBlockingQueue<AudioData> mBlockingQueue = new LinkedBlockingQueue<AudioData>(16);

    private AudioPlayThread mAudioPlayThread;

    private class AudioPlayThread extends Thread{
        @Override
        public void run() {
            while(isPlaying.get()){
                try {
                    final AudioData audioData = mBlockingQueue.take();
                    mCurrentTimeUs = audioData.timeStamp;

                    mTrack.write(audioData.data , 0 , audioData.data.length);

                    if(mProgressCallback != null){
                        if(mCallbackHandler == null){
                            mProgressCallback.onProgressUpdate(audioData.timeStamp / 1000 , mDurationTimeUs / 1000);
                        }else{
                            mCallbackHandler.post(()->{
                                if(mProgressCallback == null){
                                    return;
                                }
                                mProgressCallback.onProgressUpdate(audioData.timeStamp / 1000 , mDurationTimeUs / 1000);
                            });
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }//end while
        }
    }

    private class AudioDecodeThread extends Thread{
        @Override
        public void run() {
            //prepare mediacodec
            if(mCodec == null){
                try {
                    mCodec = MediaCodec.createDecoderByType(mMediaFormat.getString(MediaFormat.KEY_MIME));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCodec.configure(mMediaFormat , null , null , 0);
                mCodec.start();
            }

            isPlaying.set(true);
            mStatus = STATE_PLAYING;
            mTrack.play();

            //start  consumer thread 消费线程
            mAudioPlayThread = new AudioPlayThread();
            mAudioPlayThread.start();

            while(isPlaying.get()){
                try{
                    int index = mCodec.dequeueInputBuffer(10_000);
                    if (index >= 0) {
                        ByteBuffer inputBuffer = mCodec.getInputBuffer(index);
                        inputBuffer.clear();
                        int readSize = mExtractor.readSampleData(inputBuffer , 0);

                        if(readSize < 0){
                            onReadFileEnd();
                            break;
                        }
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

                        byte[] audioData = new byte[outBuf.remaining()];
                        outBuf.get(audioData);

                        mBlockingQueue.put(new AudioData(audioData , currentTimeUs));
//                    LogUtil.L("queue size" , " block queue size : " + mBlockingQueue.size());
//                    mTrack.write(audioData ,0, audioData.length );

                        mCodec.releaseOutputBuffer(outputBufferIndex, false);

                        boolean hasData = mExtractor.advance();
                        if(!hasData){
                            onReadFileEnd();
                            break;
                        }
                    }else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }//end while

            isPlaying.set(false);
            mStatus = STATE_PREPARED;
        }

        void onReadFileEnd(){
            mExtractor.seekTo(0 , MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        }
    }

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

        mDurationTimeUs = mMediaFormat.getLong(MediaFormat.KEY_DURATION);

        mStatus = STATE_PREPARED;

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
    }

    @Override
    public void play() {
        new AudioDecodeThread().start();
    }

    @Override
    public void pause() {
        if(mStatus != STATE_PLAYING){
            return;
        }

        mBlockingQueue.clear();

        isPlaying.set(false);
        mStatus = STATE_PAUSED;
        mTrack.pause();
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

    @Override
    public int currentState() {
        return mStatus;
    }

    @Override
    public void seekTo(long timeUs) {
        LogUtil.L("seek" , "timeUs = " + timeUs +"  " + mDurationTimeUs);

        if(mStatus == STATE_IDLE || mExtractor == null){
            return;
        }

        if(timeUs < 0 || timeUs > mDurationTimeUs){
            return;
        }

        mBlockingQueue.clear();
        mExtractor.seekTo(timeUs , MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }

    @Override
    public long getDurationTimeUs() {
        return mDurationTimeUs;
    }
}
