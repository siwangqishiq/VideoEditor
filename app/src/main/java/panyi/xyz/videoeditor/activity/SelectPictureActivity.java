
package panyi.xyz.videoeditor.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SelectPictureActivity extends AppCompatActivity {

    public static void start(Activity context , int requestCode){
        Intent it = new Intent(context , SelectPictureActivity.class);
        context.startActivityForResult(it , requestCode);
    }

    @Override
    protected void onCreate(final Bundle b) {
        super.onCreate(b);
        setResult(RESULT_CANCELED);

        Fragment newFragment = new BucketsFragment();
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.replace(android.R.id.content, newFragment);
        transaction.commit();
    }

    void fileSelected(final String imgPath, final String imgTaken, final long imageSize) {
        returnResult(imgPath, imgTaken, imageSize);
    }

    private void returnResult(final String path, final String token, final long size) {
        Intent result = new Intent();
        result.putExtra("path", path);
        result.putExtra("token", token);
        result.putExtra("size", size);
        setResult(RESULT_OK, result);
        finish();
    }
}
