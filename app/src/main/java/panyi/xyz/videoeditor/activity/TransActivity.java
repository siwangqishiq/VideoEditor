package panyi.xyz.videoeditor.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.config.RequestCode;
import panyi.xyz.videoeditor.model.SelectFileItem;
import panyi.xyz.videoeditor.module.trans.ITrans;
import panyi.xyz.videoeditor.module.trans.UdpTrans;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.MediaUtil;

import static panyi.xyz.videoeditor.config.RequestCode.ACTIVITY_REQUEST_SELECT_VEDIO_FILE;

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
    private ImageView imageView;

    private View mSelectBtn;

    private MediaExtractor mVideoExtractor;
    private MediaCodec mVideoCodec;

    private boolean isRunning = false;

    private SurfaceView mRenderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trans);

        mSendBtn = findViewById(R.id.send_btn);
        mSendBtn.setOnClickListener((v)->{
            sendData();
        });
        mContentText = findViewById(R.id.text_received);
        imageView = findViewById(R.id.image);

        mSelectBtn = findViewById(R.id.select_video_btn);
        mSelectBtn.setOnClickListener((v)->{
            startSelectFile();
        });

//        mRenderView = findViewById(R.id.render_view);
        initTrans();
    }

    /**
     * 权限检查
     * @return
     */
    private boolean permissionCheck(){
        if(ActivityCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,} , RequestCode.PERMISSION_READ_GALLERY);
            return false;
        }

        return true;
    }

    private void startSelectFile(){
        if(permissionCheck()){
            SelectFileActivity.start(this , ACTIVITY_REQUEST_SELECT_VEDIO_FILE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ACTIVITY_REQUEST_SELECT_VEDIO_FILE && resultCode == Activity.RESULT_OK){
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

        prepare(selectFile);
    }

    private void prepare(final SelectFileItem fileItem){
        new Thread(()->{
            try {
                mVideoExtractor = MediaUtil.createMediaExtractorByMimeType(fileItem.path , MediaUtil.TYPE_VIDEO);
            } catch (IOException e) {
                e.printStackTrace();
                mVideoExtractor = null;
            }

            if(mVideoExtractor == null){
                Toast.makeText(this , R.string.video_format_no_support , Toast.LENGTH_LONG).show();
                return;
            }

            MediaFormat mediaFormat = mVideoExtractor.getTrackFormat(mVideoExtractor.getSampleTrackIndex());
            LogUtil.log(mediaFormat.toString());

            isRunning = true;

            ByteBuffer buf = ByteBuffer.allocate(1024* 1024);

            while(isRunning){
                LogUtil.log("video time: " + mVideoExtractor.getSampleTime());
                buf.position(0);
                int readSize = mVideoExtractor.readSampleData(buf , 0);
                if(readSize > 0){
                    LogUtil.log("readSize : " + readSize +"  buf size: " + buf.remaining());

                    byte[] sendBuf = new byte[buf.remaining()];
                    buf.get(sendBuf);
                    if(mTrans != null){
                        try {
                            mTrans.sendData("127.0.0.1" , PORT , 0 , sendBuf);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }

                    mVideoExtractor.advance();
                }else{
                    break;
                }
            }//end while
            LogUtil.log("video end!s" + mVideoExtractor.getSampleTime());
        }).start();

       // initMediaCodec(mediaFormat);
    }

//    private void initMediaCodec(MediaFormat mediaFormat){
//        try {
//            mVideoCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
////            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE , 65536);
////            mVideoCodec.configure(mediaFormat , new Surface(videoWidget.surfaceTexture), null ,0);
//            mVideoCodec.configure(mediaFormat , null, null ,0);
//
//            mVideoCodec.setCallback(new MediaCodec.Callback() {
//                @Override
//                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
//                    if(!isRunning){
//                        return;
//                    }
//
//                    if(index < 0){
//                        return;
//                    }
//
//                    ByteBuffer buf = codec.getInputBuffer(index);
//                    final int readSize = mVideoExtractor.readSampleData(buf , 0);
//
//                    if(readSize > 0){
//                        codec.queueInputBuffer(index , 0 , readSize , mVideoExtractor.getSampleTime() , 0);
//                        mVideoExtractor.advance();
//                    }else{
//                        LogUtil.log("video end!");
//                        codec.queueInputBuffer(index , 0 , 0 , 0 , MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                    }
//                }
//
//                @Override
//                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int outputBufferId, @NonNull MediaCodec.BufferInfo info) {
//                    if(!isRunning){
//                        return;
//                    }
//
//                    ByteBuffer buf = codec.getOutputBuffer(outputBufferId);
//                    LogUtil.log("send buf size: " + buf.remaining());
//                    byte[] yuvData = new byte[buf.remaining()];
//                    buf.get(yuvData);
//
//                    if(mTrans != null){
//                        try {
//                            mTrans.sendData("127.0.0.1",PORT , yuvData);
//                        } catch (UnknownHostException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    codec.releaseOutputBuffer(outputBufferId , false);
//                }
//
//                @Override
//                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
//                    LogUtil.i("decode error " + e.toString());
//                }
//
//                @Override
//                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
//                    LogUtil.i("decode onOutputFormatChanged " + format);
//                }
//            });
//            mVideoCodec.start();
//            isRunning = true;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void sendData(){
//        final String content = "你好世界!" + System.currentTimeMillis();

        Bitmap bitmap = BitmapFactory.decodeResource(getResources() , R.drawable.t2);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        bitmap.recycle();
        try {
            mTrans.sendData(mContentText.getText().toString().trim() , PORT , 233 , byteArray);
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

        if(mVideoExtractor !=  null){
            mVideoExtractor.release();
        }

        isRunning = false;

        if(mVideoCodec != null){
            mVideoCodec.release();
        }
        super.onDestroy();
    }

//    @Override
//    public void onReceiveData(byte[] data) {
//        //final String content = new String(data);
////        final Bitmap bitmap = BitmapFactory.decodeByteArray(data , 0 , data.length);
////
////        runOnUiThread(()->{
////            // mContentText.setText(content);
////            imageView.setImageBitmap(bitmap);
////        });
////        LogUtil.log("get data size = " + data.length);
//    }

    @Override
    public void onReceiveData(int what, byte[] data) {
        final Bitmap bitmap = BitmapFactory.decodeByteArray(data , 0 , data.length);

        runOnUiThread(()->{
            // mContentText.setText(content);
            imageView.setImageBitmap(bitmap);
        });
        LogUtil.log("get data size = " + data.length);
    }
}//end class
