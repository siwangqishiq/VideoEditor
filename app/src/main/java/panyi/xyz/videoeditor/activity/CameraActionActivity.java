package panyi.xyz.videoeditor.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.config.RequestCode;
import panyi.xyz.videoeditor.util.LogUtil;

public class CameraActionActivity extends AppCompatActivity {

    public static void start(Activity context){
        Intent it = new Intent(context , CameraActionActivity.class);
        context.startActivity(it);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_action);

        if(checkCameraPermission()){
            initAction();
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
        if(ActivityCompat.checkSelfPermission(this , Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            return true;
        }

        ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.CAMERA} , RequestCode.PERMISSION_CAMERA);
        return false;
    }

    private void initAction(){
        CameraManager camManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        if(camManager == null){
            return;
        }

        try {
            String cameraIds[] = camManager.getCameraIdList();
            for(String cameraId : cameraIds){
                LogUtil.log("cameraId : " + cameraId);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}//end class
