package panyi.xyz.videoeditor.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.model.SelectFileItem;
import panyi.xyz.videoeditor.module.VideoEditor;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.view.VideoEditorGLView;

/**
 *
 *
 */
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

    private int mEditorStatus = STATUS_UNSELECT_EDIT_FILE;

    private static final int STATUS_UNSELECT_EDIT_FILE = 0;// 状态 未选择素材
    private static final int STATUS_HAS_SELECTED_EDIT_FILE = 1;//已选择素材

    private VideoEditor mVideoEditor;

    private ViewGroup mGallery;

    private List<Bitmap> bitmapList = new ArrayList<Bitmap>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);
        initView();
        if(permissionCheck()){
            startEditAction();
        }
    }

    /**
     * 权限检查
     * @return
     */
    private boolean permissionCheck(){
        if(ActivityCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,} , REQUEST_CODE_READ_PERMISSION);
            return false;
        }

        return true;
    }

    private void initView(){
        mVideoEditor = new VideoEditor();
        mVideoEditor.initView((ViewGroup)findViewById(R.id.preview_panel));

        findViewById(R.id.btn_test).setOnClickListener((v)->{
            //mVideoEditor.decodeNextFrame();
            mVideoEditor.pauseOrResume();
        });

        mGallery = findViewById(R.id.gallery);

        mVideoEditor.setGetPixelCallback(new VideoEditor.GetThumbImageCallback(){
            @Override
            public void onGetThumbImage(String filepath) {

                TextView textView = new TextView(getContext());
                textView.setText(filepath);

//                ImageView img = new ImageView(getContext());
//                Glide.with(getContext()).load(Uri.fromFile(new File(filepath))).into(img);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(250,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                mGallery.addView(textView , params);
            }
        });



//        //                LogUtil.log("获得截图---> " + bitmap);
//        bitmapList.add(bitmap);

    }

    private Context getContext(){
        return this;
    }

    /**
     * 开始编辑操作
     */
    private void startEditAction(){
        if(mEditorStatus == STATUS_UNSELECT_EDIT_FILE){ //未选择编辑素材  打开选择器 进行选择
            openSelectFile();
        }else if(mEditorStatus == STATUS_HAS_SELECTED_EDIT_FILE){
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_READ_PERMISSION){
            if(ActivityCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED){
                startEditAction();
            }else{
                Toast.makeText(this , R.string.no_permission_error_tips , Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * 打开选择文件页
     */
    private void openSelectFile(){
        SelectFileActivity.start(this , REQUEST_CODE_SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SELECT_FILE && resultCode == Activity.RESULT_OK){
            handleSelectFile(data);
        }
    }

    /**
     *  处理选择文件返回
     * @param data
     */
    private void handleSelectFile(final Intent data){
        if(data == null)
            return;

        final SelectFileItem selectFile = (SelectFileItem)data.getSerializableExtra("data");
        LogUtil.log("select file :" + selectFile.path +"    size: " + selectFile.size +" duration:" + selectFile.duration);

        mVideoEditor.prepare(selectFile);
    }

    @Override
    protected void onDestroy() {
        if(mVideoEditor != null){
            mVideoEditor.free();
        }
        super.onDestroy();
    }
}
