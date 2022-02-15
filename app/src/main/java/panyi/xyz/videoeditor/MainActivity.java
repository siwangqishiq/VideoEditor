package panyi.xyz.videoeditor;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import panyi.xyz.videoeditor.activity.VideoEditorActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.click_btn).setOnClickListener((v)->{
            VideoEditorActivity.start(MainActivity.this);
        });
    }
}