package panyi.xyz.videoeditor.module;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

import panyi.xyz.videoeditor.model.SelectFileItem;
import panyi.xyz.videoeditor.util.LogUtil;

public class MediaQuery {

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
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " ASC, "
                        + MediaStore.Video.Media.DATE_MODIFIED + " DESC");

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
