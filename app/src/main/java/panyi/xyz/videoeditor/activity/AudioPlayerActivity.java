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
import android.widget.ImageView;
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

    private ImageView mPlayControlBtn;

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
        mPlayControlBtn = findViewById(R.id.play_control_btn);

        mTotalTimeText.setText(TimeUtil.mediaTimeDuration(mFileData.duration));

        mPlayControlBtn.setOnClickListener((v)->{
            clickControlButton();
        });
    }

    private void clickControlButton(){
        if(mAudioPlayer == null){
            return;
        }

        int state = mAudioPlayer.currentState();
        if(state == IAudioPlayer.STATE_PLAYING){ //暂停
            mAudioPlayer.pause();
            mPlayControlBtn.setImageResource(R.drawable.ic_play);
        }else{
            mAudioPlayer.play();
            mPlayControlBtn.setImageResource(R.drawable.ic_pause);
        }
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

    private boolean mTouchingSeekBar = false;

    private void initAction(){
        mAudioPlayer = new AudioPlayer();
        mAudioPlayer.prepare(mFileData.path);

        //mAudioPlayer.play(AudioManager.M);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!fromUser){
                    return;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mTouchingSeekBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mTouchingSeekBar = false;

                int progress = seekBar.getProgress();
                long timeUs = (long)(((float)progress / 100.0f) * mAudioPlayer.getDurationTimeUs());
                mAudioPlayer.seekTo(timeUs);
            }
        });

        mAudioPlayer.setProgressListener(new IAudioPlayer.ProgressCallback() {
            @Override
            public void onProgressUpdate(long current, long total) {
                mCurrentTimeText.setText(TimeUtil.mediaTimeDuration(current));

                if(mTouchingSeekBar){
                    return;
                }

                int progress = (int)((current / (float)total) * 100);
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
