package panyi.xyz.videoeditor.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.config.RequestCode;
import panyi.xyz.videoeditor.util.MediaUtil;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();
        findViewById(R.id.click_btn).setOnClickListener((v)->{
            VideoEditorActivity.start(MainActivity.this);
        });


//        ImageView imgView = findViewById(R.id.show_image);
//        imgView.setImageBitmap(TextRenderHelper.buildFontBit(-1));

        findViewById(R.id.camera_btn).setOnClickListener((v)->{
            CameraActionActivity.start(MainActivity.this);
        });

        findViewById(R.id.trans_btn).setOnClickListener((v)->{
            TransActivity.start(MainActivity.this);
        });

        findViewById(R.id.list_audio).setOnClickListener((v)->{
            SelectFileActivity.startAudioSelector(MainActivity.this , RequestCode.ACTIVITY_REQUEST_SELECT_AUDIO_FILE);
        });

        fillMediaSupports(false , findViewById(R.id.support_decode_formats));
        fillMediaSupports(true , findViewById(R.id.support_encode_formats));
    }

    private void fillMediaSupports(boolean encode , ViewGroup viewGroup){
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo infos[] = codecList.getCodecInfos();
        List<String> mediaList = new ArrayList<String>(16);
        for(MediaCodecInfo info :  infos){
            if(encode == info.isEncoder()){ //encode
                mediaList.add(info.getName() + " mime : " + MediaUtil.supportTypesStr(info.getSupportedTypes()));
            }
        }//end for each

        fillMediaList(viewGroup , mediaList);
    }

    private void fillMediaList(ViewGroup viewgroup , List<String> mediaList){
        viewgroup.removeAllViews();

        for(String mediaName : mediaList){
            TextView textView = new TextView(this);
            textView.setText(mediaName);

            viewgroup.addView(textView);
        }
    }

    private void requestPermission(){
        if(ActivityCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE} , RequestCode.PERMISSION_READ_GALLERY);
        }
    }
}