package panyi.xyz.videoeditor.util;


import android.text.TextUtils;

import java.io.File;

public class FileUtil {

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
}
