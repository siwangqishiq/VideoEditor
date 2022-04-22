package panyi.xyz.videoeditor.util;

import java.nio.ByteBuffer;

/**
 * 常用工具类
 */
public class CommonUtil {
    public static void  byteBufferWriteString(String str , ByteBuffer buf){
        if(str == null || str.length() == 0){
            buf.putInt(0);
            return;
        }

        byte data[] = str.getBytes();
        buf.putInt(data.length);
        buf.put(data);
    }

    public static String byteBufferReadString(ByteBuffer buf){
        if(buf == null) {
            return null;
        }

        final int size = buf.getInt();
        byte[] strBytes = new byte[size];
        buf.get(strBytes);

        return new String(strBytes);
    }
}
