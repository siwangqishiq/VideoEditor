package panyi.xyz.videoeditor.module;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

import panyi.xyz.videoeditor.model.SelectFileItem;
import panyi.xyz.videoeditor.util.LogUtil;

public class MediaQuery {
    public static List<SelectFileItem> queryAudioFile(final Context context) {
        LogUtil.log("query audio file start");
        final List<SelectFileItem> result = new ArrayList<SelectFileItem>(16);

        final String[] projection = new String[] {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_MODIFIED,};

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null, MediaStore.Audio.Media.DATE_MODIFIED + " DESC");

        if (cur != null) {
            if (cur.moveToFirst()) {
                while (!cur.isAfterLast()) {
                    final SelectFileItem item = new SelectFileItem();
                    item.path = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    item.name = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                    item.duration = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    item.size = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                    item.album = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));

                    result.add(item);
                    cur.moveToNext();
                }
            }
            cur.close();
        }
        LogUtil.log("query video file end");

        return result;
    }


    /**
     * 查询视频文件
     * @param context
     * @return
     */
    public static List<SelectFileItem> queryVideoFileData(final Context context){
        LogUtil.log("query video file start");
        final List<SelectFileItem> result = new ArrayList<SelectFileItem>(16);

        final String[] projection = new String[] {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.BUCKET_ID };

        Cursor cur = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_MODIFIED + " DESC");

        if (cur != null) {
            if (cur.moveToFirst()) {
                while (!cur.isAfterLast()) {
                    final SelectFileItem item = new SelectFileItem();

                    item.name = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                    item.path = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                    item.duration = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                    item.width = cur.getInt(cur.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH));
                    item.height = cur.getInt(cur.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT));
                    item.size = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));

                    result.add(item);
                    cur.moveToNext();
                }
            }
            cur.close();
        }
        LogUtil.log("query video file end");

        return result;
    }
}
