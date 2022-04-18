package panyi.xyz.videoeditor.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.UnknownHostException;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.module.ITrans;
import panyi.xyz.videoeditor.module.UdpTrans;

/**
 *
 *
 *   10.242.65.75   .xiaomi
 *
 *  10.242.149.4    nexus
 */
public class TransActivity extends AppCompatActivity implements ITrans.OnReceiveCallback {

    public static final int PORT = 4444;

    public static void start(Activity context){
        Intent it = new Intent(context , TransActivity.class);
        context.startActivity(it);
    }

    private ITrans mTrans;

    private TextView mContentText;
    private View mSendBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trans);

        mSendBtn = findViewById(R.id.send_btn);
        mSendBtn.setOnClickListener((v)->{
            sendData();
        });
        mContentText = findViewById(R.id.text_received);

        initTrans();
    }

    private void sendData(){
        final String content = "你好世界!" + System.currentTimeMillis();

        try {
            mTrans.sendData("10.242.149.4" , PORT , content.getBytes());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void initTrans(){
        mTrans = new UdpTrans();
        mTrans.startServer(4444 , this);
    }

    @Override
    protected void onDestroy() {
        mTrans.close();
        super.onDestroy();
    }

    @Override
    public void onReceiveData(byte[] data) {
        final String content = new String(data);
        runOnUiThread(()->{
            mContentText.setText(content);
        });
    }
}//end class
