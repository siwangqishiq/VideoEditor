package panyi.xyz.videoeditor.module;

import android.os.Handler;

/**
 *
 */
public interface IAudioPlayer {

    void prepare(String path);

    void play();

    void pause();

    void free();

    void setProgressListener(ProgressCallback callback , Handler handler);

    interface ProgressCallback{
        void onProgressUpdate(long current , long total);
    }
}
