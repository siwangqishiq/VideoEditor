package panyi.xyz.videoeditor.util;

import android.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *  LogUtil
 */
public final class LogUtil {
    private static Logger logger = Logger.getLogger(LogUtil.class.getName());
    public static final String TAG = LogUtil.class.getSimpleName();

    public static void i(final String msg){
        Log.i(TAG , genLogMessage(genLogMessage(msg)));
    }

    public static void w(final String msg){
        Log.w(TAG , genLogMessage(genLogMessage(msg)));
    }


    private static String genLogMessage(final String msg){
        return Thread.currentThread() + "  " + msg;
    }

    public static void log(final String msg){
        System.out.println(genLogMessage(msg));
    }

    public static void L(String TAG , String msg){
        Log.d(TAG, msg);
    }

    public static void logArray(float[] matrix){
        System.out.print("[ ");
        for(float v : matrix){
            System.out.print( v+" ");
        }
        System.out.println("]");
    }

    public static void formatLog(String template , Object ... params){
        String msg = String.format(template , params);
        log(msg);
    }

    public static void logMatrix(float[] matrix){
        System.out.println("==========");
        for(int i = 0; i < 3;i++){
            for(int j = 0 ; j < 3;j++){
                System.out.print( String.format("%.2f \t" , matrix[3 * i + j]));
            }
            System.out.println();
        }
        System.out.println("==========");
    }
}
