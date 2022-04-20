package panyi.xyz.videoeditor.util;

import java.util.Random;

public class UuidUtil {
    private static final Random rnd = new Random();

    /**
     *  生成整型 唯一ID
     * @return
     */
    public static final long genUniqueId(){
        long time = System.currentTimeMillis();
        final int limit = 10000;
        int randomInt = rnd.nextInt(limit);
        return time * limit + randomInt;
    }
}
