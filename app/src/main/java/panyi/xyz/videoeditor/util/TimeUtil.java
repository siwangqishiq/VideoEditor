package panyi.xyz.videoeditor.util;

public class TimeUtil {

    /**
     * 视频长度 时间显示
     *
     * @param time
     * @return
     */
    public static String videoTimeDuration(long time){
        long hour = time/(60*60*1000);
        long minute = (time - hour*60*60*1000)/(60*1000);
        long second = (time - hour*60*60*1000 - minute*60*1000)/1000;

        if(hour == 0){
            return String.format("%02d:%02d" , minute , second);
        }else{
            return String.format("%02d:%02d:%02d" ,hour, minute , second);
        }
    }
}
