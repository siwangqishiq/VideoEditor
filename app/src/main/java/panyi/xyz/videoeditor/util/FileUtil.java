package panyi.xyz.videoeditor.util;


import android.text.TextUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class FileUtil {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String findNameFromPath(String filepath){
        if(TextUtils.isEmpty(filepath)){
            return "";
        }

        if(filepath.indexOf(File.pathSeparator) != -1){
             int start = filepath.lastIndexOf(File.pathSeparator);
             return filepath.substring(start , filepath.length());
        }
        return "";
    }

    // 字节数据 16进制
    public static String hex(byte[] data){
        if(data == null || data.length == 0){
            return "";
        }

        char[] hexChars = new char[data.length * 2];
        for (int j = 0; j < data.length; j++) {
            int v = data[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
