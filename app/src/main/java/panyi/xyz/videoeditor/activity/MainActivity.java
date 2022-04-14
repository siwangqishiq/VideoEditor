package panyi.xyz.videoeditor.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.text.StaticLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.view.widget.TextRenderHelper;

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

//        ImageView imgView = findViewById(R.id.show_image);
//        imgView.setImageBitmap(TextRenderHelper.buildFontBit(-1));
    }

    private void requestPermission(){
        if(ActivityCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE} , 100);
        }
    }



}