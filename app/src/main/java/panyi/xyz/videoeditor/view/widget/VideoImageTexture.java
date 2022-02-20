package panyi.xyz.videoeditor.view.widget;

import android.graphics.SurfaceTexture;
import android.view.Surface;

public class VideoImageTexture {
    public int textureId;
    public Surface surface;
    public SurfaceTexture surfaceTexture;

    public static VideoImageTexture create(int texId){
        final VideoImageTexture instance = new VideoImageTexture();
        instance.textureId = texId;
        instance.surfaceTexture = new SurfaceTexture(instance.textureId);
        instance.surface = new Surface(instance.surfaceTexture);
        return instance;
    }
}
