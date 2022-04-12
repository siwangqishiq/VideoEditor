package panyi.xyz.videoeditor.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.util.LogUtil;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();
        findViewById(R.id.click_btn).setOnClickListener((v)->{
            VideoEditorActivity.start(MainActivity.this);
        });

//        for(int i = 0 ; i < MediaCodecList.getCodecCount() ; i++){
//            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            // LogUtil.log(info.getName() +"  " +info.getSupportedTypes() + " " + info.toString());
//        }
    }

    private void requestPermission(){
        if(ActivityCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE} , 100);
        }
    }
}