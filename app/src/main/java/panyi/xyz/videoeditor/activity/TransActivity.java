package panyi.xyz.videoeditor.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import panyi.xyz.videoeditor.util.CommonUtil;
import panyi.xyz.videoeditor.util.FileUtil;
import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.MediaUtil;

import static android.media.MediaExtractor.SAMPLE_FLAG_SYNC;
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

    public static final int TYPE_META = 101;
    public static final int TYPE_VIDEO = 100;

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

    private FrameLayout mContainer;

    private MediaFormat mediaFormat;

    private Surface mSurface;

    //远端视频解码器
    private MediaCodec remoteVideoCodec;

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

        mContainer = findViewById(R.id.container);

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
//         prepare(selectFile);
//        prepareAndStart(selectFile);

        prepareAndSend(selectFile);
    }

    /**
     * 向远端发送编码的视频流  推流
     *
     * @param fileItem
     */
    private void prepareAndSend(final SelectFileItem fileItem){
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

        mediaFormat = mVideoExtractor.getTrackFormat(mVideoExtractor.getSampleTrackIndex());

        int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        String mime = mediaFormat.getString(MediaFormat.KEY_MIME);

        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.putInt(width);
        buf.putInt(height);
        CommonUtil.byteBufferWriteString(mime , buf);

        if(MediaFormat.MIMETYPE_VIDEO_AVC.equals(mime)){//h264编码
            ByteBuffer spsBuf = mediaFormat.getByteBuffer("csd-0");
            byte[] spsData = new byte[spsBuf.remaining()];
            spsBuf.get(spsData);
            spsBuf.position(0);

            buf.putInt(spsData.length);
            buf.put(spsData);

            ByteBuffer ppsBuf = mediaFormat.getByteBuffer("csd-1");
            byte[] ppsData = new byte[ppsBuf.remaining()];
            ppsBuf.get(ppsData);
            ppsBuf.position(0);

            buf.putInt(ppsData.length);
            buf.put(ppsData);

            LogUtil.L("debug" , "right sps:" + FileUtil.hex(spsData) );
            LogUtil.L("debug" , "right pps:" + FileUtil.hex(ppsData) );
        }else if(MediaFormat.MIMETYPE_VIDEO_HEVC.equals(mime)){//h265

        }

        buf.flip();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);

        new Thread(()->{
            try {
                mTrans.sendData(remoteAddress() , PORT , TYPE_META , data);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }).start();

        new Handler().postDelayed(()->{
            startSendVideoDataThread();
        },2000);
    }

    /**
     *
     * @param width
     * @param height
     * @param mime
     */
    private void initRemoteVideoMediaCodec(int width , int height , String mime ,
                                           final ByteBuffer spsBuf , final ByteBuffer ppsBuf){
        final MediaFormat mFormat = MediaFormat.createVideoFormat(mime , width , height);

        SurfaceView renderView = new SurfaceView(this);
        renderView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                mSurface = holder.getSurface();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                mSurface = holder.getSurface();

                try {
                    remoteVideoCodec = MediaCodec.createDecoderByType(mFormat.getString(MediaFormat.KEY_MIME));
                    mFormat.setByteBuffer("csd-0" , spsBuf);
                    mFormat.setByteBuffer("csd-1" , ppsBuf);

//                    LogUtil.L("debug" , "error format: " + mFormat);
//                    LogUtil.L("debug" , "good format: " + mediaFormat);

                    remoteVideoCodec.configure(mFormat , mSurface , null , 0);

                    remoteVideoCodec.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                LogUtil.log("release SurfaceHolder");
                mSurface = null;
            }
        });

        runOnUiThread(()->{
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT ,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            mContainer.addView(renderView, params);
        });
    }

    private void startSendVideoDataThread(){
        LogUtil.log("开启发送视频文件数据线程...");

        isRunning = true;
        new Thread(()->{
            ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);
            int readSize = 0;
            while(readSize >= 0 && isRunning){
                buf.clear();
                readSize = mVideoExtractor.readSampleData(buf, 0);

                byte[] data = new byte[buf.remaining()];
                buf.get(data);
                if(mTrans != null){
                    try {
                        mTrans.sendData(remoteAddress() , PORT , TYPE_VIDEO , data);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }

                if(!isRunning){
                    break;
                }

                mVideoExtractor.advance();
            }//end while
            mVideoExtractor.release();
        }).start();
    }

    private void prepareAndStart(final SelectFileItem fileItem){
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

        try {
            mVideoCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));


            imgWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH );
            imgHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);

            sendVideoMeta(imgWidth , imgHeight);

            mVideoCodec.configure(mediaFormat , null, null ,0);

            HandlerThread videoDecodeThread = new HandlerThread("video decode");
            videoDecodeThread.start();
            mVideoCodec.setCallback(new MediaCodec.Callback() {
                private byte[] yuvData = new byte[10 * 1024 * 1024];

                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    if(!isRunning){
                        return;
                    }

                    if(index < 0){
                        return;
                    }

                    ByteBuffer buf = codec.getInputBuffer(index);
                    final int readSize = mVideoExtractor.readSampleData(buf , 0);

                    if(readSize > 0){
                        if(!isRunning){
                            return;
                        }
                        codec.queueInputBuffer(index , 0 , readSize , mVideoExtractor.getSampleTime() , 0);
                        mVideoExtractor.advance();
                    }else{
                        LogUtil.log("video end!");
                        codec.queueInputBuffer(index , 0 , 0 , 0 , MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int outputBufferId, @NonNull MediaCodec.BufferInfo info) {
                    if(!isRunning){
                        return;
                    }

                    ByteBuffer buf = codec.getOutputBuffer(outputBufferId);
                    // LogUtil.log("send buf size: " + buf.remaining());
//                    byte[] yuvData = new byte[buf.remaining()];

                    buf.get(yuvData , 0 , buf.remaining());

                    if(mTrans != null){
                        try {
                            mTrans.sendData(remoteAddress(),PORT ,TYPE_VIDEO, yuvData);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }

                    codec.releaseOutputBuffer(outputBufferId , false);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    LogUtil.i("decode error " + e.toString());
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    LogUtil.i("decode onOutputFormatChanged " + format);
                }
            } ,new Handler(videoDecodeThread.getLooper()));

            isRunning = true;
            mVideoCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendVideoMeta(int videoWidth , int videoHeight){
        if(mTrans == null){
            return;
        }

        ByteBuffer buf = ByteBuffer.allocate(4 + 4);
        buf.putInt(videoWidth);
        buf.putInt(videoHeight);

        buf.flip();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);

        final String address = remoteAddress();
        new Thread(()->{
            try {
                mTrans.sendData(address , PORT , TYPE_META ,  data);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String remoteAddress(){
        String content = mContentText.getText().toString().trim();
        if(TextUtils.isEmpty(content)){
            return "127.0.0.1";
        }

        return content;
    }

    private void prepare(final SelectFileItem fileItem){
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

        mediaFormat = mVideoExtractor.getTrackFormat(mVideoExtractor.getSampleTrackIndex());

//        new Thread(()->{
//            LogUtil.log(mediaFormat.toString());
//            isRunning = true;
//            ByteBuffer buf = ByteBuffer.allocate(1024* 1024);
//
//            while(isRunning){
//                LogUtil.log("video time: " + mVideoExtractor.getSampleTime());
//                buf.position(0);
//                int readSize = mVideoExtractor.readSampleData(buf , 0);
//                if(readSize > 0){
//                    LogUtil.log("readSize : " + readSize +"  buf size: " + buf.remaining());
//
//                    byte[] sendBuf = new byte[buf.remaining()];
//                    buf.get(sendBuf);
//                    if(mTrans != null){
//                        try {
//                            mTrans.sendData(mContentText.getText().toString() , PORT , 0 , sendBuf);
//                        } catch (UnknownHostException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    mVideoExtractor.advance();
//                }else{
//                    break;
//                }
//            }//end while
//            LogUtil.log("video end!s" + mVideoExtractor.getSampleTime());
//        }).start();

//        mRenderView.getHolder().addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(@NonNull SurfaceHolder holder) {
//                mSurface = holder.getSurface();
//            }
//
//            @Override
//            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
//                mSurface = holder.getSurface();
//
//                initMediaCodec(mediaFormat);
//            }
//
//            @Override
//            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
//                LogUtil.log("release SurfaceHolder");
//                mSurface = null;
//            }
//        });
    }

    @Override
    public void onReceiveData(int what, byte[] data) {
//        LogUtil.L("ReceivedData" ,"get data size = " + data.length);
        if(what == TYPE_META){
            ByteBuffer buf = ByteBuffer.wrap(data);

            int width = buf.getInt();
            int height = buf.getInt();

            String mime = CommonUtil.byteBufferReadString(buf);

            LogUtil.L("remoteVideo" , "width : " + width);
            LogUtil.L("remoteVideo" , "height : " + height);
            LogUtil.L("remoteVideo" , "mime : " + mime);

            int spsDataSize = buf.getInt();
            byte[] spsData = new byte[spsDataSize];
            buf.get(spsData);

            int ppsDataSize = buf.getInt();
            byte[] ppsData = new byte[ppsDataSize];
            buf.get(ppsData);

            LogUtil.L("debug" , "received sps:" + FileUtil.hex(spsData) );
            LogUtil.L("debug" , "received pps:" + FileUtil.hex(ppsData) );

            isRunning = true;
            initRemoteVideoMediaCodec(width , height , mime ,
                    ByteBuffer.wrap(spsData) , ByteBuffer.wrap(ppsData));

//            initRemoteVideoMediaCodec(width , height , mime , null , null);
        }else if(what == TYPE_VIDEO){
            handleRemoteVideoData(data);
        }
    }

    private void handleRemoteVideoData(byte[] data){
        if(!isRunning || remoteVideoCodec == null){
            return;
        }

        try{
            int index = remoteVideoCodec.dequeueInputBuffer(100000);
            if (index >= 0) {
                ByteBuffer inputBuffer = remoteVideoCodec.getInputBuffer(index);
                inputBuffer.clear();
                inputBuffer.put(data, 0, data.length);
                remoteVideoCodec.queueInputBuffer(index,
                        0, data.length, System.currentTimeMillis(), 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = remoteVideoCodec.dequeueOutputBuffer(bufferInfo, 100000);
            while (outputBufferIndex > 0) {
                remoteVideoCodec.releaseOutputBuffer(outputBufferIndex, true);
                outputBufferIndex = remoteVideoCodec.dequeueOutputBuffer(bufferInfo, 0);
            }//end while
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initMediaCodec(MediaFormat mediaFormat){
        try {
            mVideoCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
            mVideoCodec.configure(mediaFormat , mSurface, null ,0);

            mVideoCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    if(!isRunning){
                        return;
                    }

                    if(index < 0){
                        return;
                    }

//                    LogUtil.L("videoInfo" , "" + codec.getInputFormat());
//                    byte[] sps = format.getByteBuffer("csd-0").array();
//                    byte[] pps = format.getByteBuffer("csd-1").array();
//                    LogUtil.L("videoInfo" , "sps: " + FileUtil.hex(sps));
//                    LogUtil.L("videoInfo" , "pps: " + FileUtil.hex(pps));

                    ByteBuffer buf = codec.getInputBuffer(index);
                    buf.clear();
                    final int readSize = mVideoExtractor.readSampleData(buf , 0);

                    if(readSize > 0){
                        LogUtil.L("VideoRead" , "size: " +buf.remaining());
                        byte head[] = new byte[8];
                        for(int i = 0 ; i < head.length;i++){
                            head[i] = buf.get(i);
                        }
                        LogUtil.L("VideoRead" , "head: " + FileUtil.hex(head));

//                        int offset = 4;
//                        if (buf.get(2) == 0x01) {
//                            offset = 3;
//                        }
//                        int type = (buf.get(offset) & 0x7E)>>1;
//                        LogUtil.L("VideoRead" , "type: " + type +" time: "
//                                +mVideoExtractor.getSampleTime() +"  flag: " + mVideoExtractor.getSampleFlags()
//                                +" size = " + readSize +" - " + buf.remaining());

                        codec.queueInputBuffer(index , 0 , readSize , mVideoExtractor.getSampleTime() , 0);
                        mVideoExtractor.advance();
                    }else{
                        LogUtil.log("video end!");
                        codec.queueInputBuffer(index , 0 , 0 , 0 , MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int outputBufferId, @NonNull MediaCodec.BufferInfo info) {
                    if(!isRunning){
                        return;
                    }

                    ByteBuffer buf = codec.getOutputBuffer(outputBufferId);
//                    LogUtil.log("send buf size: " + buf.remaining());
                    byte[] yuvData = new byte[buf.remaining()];
                    buf.get(yuvData);


//                    if(mTrans != null){
//                        try {
//                            mTrans.sendData("127.0.0.1",PORT ,  0 ,yuvData);
//                        } catch (UnknownHostException e) {
//                            e.printStackTrace();
//                        }
//                    }
                    codec.releaseOutputBuffer(outputBufferId , true);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    LogUtil.i("decode error " + e.toString());
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    LogUtil.L( "videoInfo","decode onOutputFormatChanged " + format);

//                    byte[] sps = format.getByteBuffer("csd-0").array();
//                    byte[] pps = format.getByteBuffer("csd-1").array();
//                    LogUtil.L("videoInfo" , "sps: " + FileUtil.hex(sps));
//                    LogUtil.L("videoInfo" , "pps: " + FileUtil.hex(pps));
                }
            });
            mVideoCodec.start();
            isRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendData(){
//        final String content = "你好世界!" + System.currentTimeMillis();

//        Bitmap bitmap = BitmapFactory.decodeResource(getResources() , R.drawable.t2);
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//        byte[] byteArray = stream.toByteArray();
//        bitmap.recycle();
//        try {
//            mTrans.sendData(mContentText.getText().toString().trim() , PORT , 233 , byteArray);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
    }

    private void initTrans(){
        mTrans = new UdpTrans();
        mTrans.startServer(PORT , this);
    }

    @Override
    protected void onDestroy() {
        isRunning = false;
        mTrans.close();

        if(mVideoExtractor !=  null){
            mVideoExtractor.release();
        }

        if(mVideoCodec != null){
            mVideoCodec.release();
        }

        if(remoteVideoCodec != null){
            remoteVideoCodec.release();
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

    int imgWidth = 1920;
    int imgHeight = 1080;

//    @Override
//    public void onReceiveData(int what, byte[] data) {
//        LogUtil.log("get data size = " + data.length);
//        if(what == TYPE_META){
//            ByteBuffer buf = ByteBuffer.wrap(data);
////            buf.flip();
//
//            imgWidth = buf.getInt();
//            imgHeight = buf.getInt();
//
//            LogUtil.L("meta" , "width x height " + imgWidth +" " + imgHeight);
//        }else if(what == TYPE_VIDEO){
//            YuvImage yuvImage = new YuvImage(data , ImageFormat.NV21 , imgWidth,imgHeight,null);
//
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            yuvImage.compressToJpeg(new Rect(0, 0, imgWidth, imgHeight), 100, out);
//
//            byte[] imageBytes = out.toByteArray();
//            final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//
//            // final Bitmap bitmap = BitmapFactory.decodeByteArray(data , 0 , data.length);
//
//            runOnUiThread(()->{
//                // mContentText.setText(content);
//                imageView.setImageBitmap(bitmap);
//            });
//        }
//
//    }
}//end class
