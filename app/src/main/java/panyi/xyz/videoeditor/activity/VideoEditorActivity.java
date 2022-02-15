package panyi.xyz.videoeditor.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import panyi.xyz.videoeditor.R;

public class VideoEditorActivity extends AppCompatActivity {
    /**
     *
     * @param context
     */
    public static void start(Activity context){
        final Intent it = new Intent(context , VideoEditorActivity.class);
        context.startActivity(it);
    }

    public static final int REQUEST_CODE_READ_PERMISSION = 1000;
    public static final int REQUEST_CODE_SELECT_FILE = 1001;



    private int mEditorStatus;

    private static final int STATUS_IDLE = 0;// 状态 未选择素材
    private static final int STATUS_HAS_SELECTED_FILE = 1;//已选择素材

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);
        initView();
    }

    private void permissionCheck(){
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,} , REQUEST_CODE_READ_PERMISSION);
    }

    private void initView(){
    }

    /**
     * 打开选择文件页
     */
    private void openSelectFile(){
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
