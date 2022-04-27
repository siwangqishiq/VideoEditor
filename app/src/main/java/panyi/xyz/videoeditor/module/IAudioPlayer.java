package panyi.xyz.videoeditor.module;

import android.os.Handler;

/**
 *
 */
public interface IAudioPlayer {
    int STATE_IDLE = 0; //空闲
    int STATE_PREPARED = 1; //准备完成   可以开始播放
    int STATE_PLAYING = 2;//播放中
    int STATE_PAUSED = 3;//暂停

    /**
     *
     * @param path
     */
    void prepare(String path);

    /**
     *  开始播放
     */
    void play();

    /**
     * 暂停
     *
     */
    void pause();

    /**
     * 释放资源
     *
     */
    void free();

    /**
     * 添加进度更新回调
     *
     * @param callback
     * @param handler
     */
    void setProgressListener(ProgressCallback callback , Handler handler);

    /**
     *  获得当前状态
     * @return
     */
    int currentState();

    /**
     *
     * @param timeUs
     */
    void seekTo(long timeUs);

    /**
     *
     * @return
     */
    long getDurationTimeUs();

    interface ProgressCallback{
        void onProgressUpdate(long current , long total);
    }
}
