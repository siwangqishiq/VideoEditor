package panyi.xyz.videoeditor.model;

import android.media.MediaFormat;

import panyi.xyz.videoeditor.util.TimeUtil;

/**
 *  视频信息
 */
public class VideoInfo {
    public static VideoInfo build(final MediaFormat format){
        final VideoInfo result = new VideoInfo();
        result.mime = format.getString(MediaFormat.KEY_MIME);
        result.duration = format.getLong(MediaFormat.KEY_DURATION)/1000;
        result.width = format.getInteger(MediaFormat.KEY_WIDTH);
        result.height = format.getInteger(MediaFormat.KEY_HEIGHT);
        if(format.containsKey(MediaFormat.KEY_FRAME_RATE)){
            result.sampleRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
        }
        return result;
    }

    public String mime;
    public long duration; //毫秒
    public int width;
    public int height;
    public int sampleRate;

    @Override
    public String toString() {
        return "VideoInfo{" +
                "mime='" + mime + '\'' +
                ", duration=" + TimeUtil.mediaTimeDuration(duration) +
                ", width=" + width +
                ", height=" + height +
                ", sampleRate=" + sampleRate +
                '}';
    }
}
