package panyi.xyz.videoeditor.util;

/**
 *  LogUtil
 */
public final class LogUtil {
    public static void log(final String msg){
        System.out.println(msg);
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
