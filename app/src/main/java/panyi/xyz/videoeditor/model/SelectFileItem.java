package panyi.xyz.videoeditor.model;

import java.io.Serializable;

public class SelectFileItem implements Serializable {
    public String name;
    public String path;
    public long duration;
    public int width;
    public int height;
    public long size;

    public String bitrate;
    public String album;
}
