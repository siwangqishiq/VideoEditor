package panyi.xyz.videoeditor.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.config.RequestCode;
import panyi.xyz.videoeditor.model.SelectFileItem;
import panyi.xyz.videoeditor.module.AudioPlayer;
import panyi.xyz.videoeditor.module.IAudioPlayer;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.TimeUtil;

public class AudioPlayerActivity extends AppCompatActivity {

    private static final String INTENT_DATA = "_data";

    public static void start(Activity context , SelectFileItem data){
        Intent it = new Intent(context , AudioPlayerActivity.class);
        it.putExtra(INTENT_DATA , data);
        context.startActivity(it);
    }

    private SelectFileItem mFileData;
    private TextView mTotalTimeText;
    private TextView mCurrentTimeText;
    private SeekBar mSeekBar;

    private IAudioPlayer mAudioPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        mFileData = (SelectFileItem) getIntent().getSerializableExtra(INTENT_DATA);
        initView();

        if(checkCameraPermission()){
            initAction();
        }
    }

    private void initView(){
        mCurrentTimeText = findViewById(R.id.current_time);
        mSeekBar = findViewById(R.id.time_line);
        mTotalTimeText = findViewById(R.id.total_time);

        mTotalTimeText.setText(TimeUtil.mediaTimeDuration(mFileData.duration));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(RequestCode.PERMISSION_CAMERA == requestCode && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            initAction();
        }
    }

    /**
     * 请求相机权限
     */
    private boolean checkCameraPermission(){
        if(ActivityCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }

        ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE} , RequestCode.PERMISSION_READ_GALLERY);
        return false;
    }

    private void initAction(){
        mAudioPlayer = new AudioPlayer();
        mAudioPlayer.prepare(mFileData.path);

        //mAudioPlayer.play(AudioManager.M);


        mAudioPlayer.setProgressListener(new IAudioPlayer.ProgressCallback() {
            @Override
            public void onProgressUpdate(long current, long total) {
                mCurrentTimeText.setText(TimeUtil.mediaTimeDuration(current));

                float percent = ((float)current / total) * 100;
                int progress = (int)((current / (float)total) * 100);
                LogUtil.log("progress = " + progress +"  percent = "+ percent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mSeekBar.setProgress(progress , true);
                }else{
                    mSeekBar.setProgress(progress);
                }
            }
        } , new Handler());
        mAudioPlayer.play();
    }

    @Override
    protected void onDestroy() {
        if(mAudioPlayer != null){
            mAudioPlayer.free();
        }
        super.onDestroy();
    }
}//end class
