
package panyi.xyz.videoeditor.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import panyi.xyz.videoeditor.R;
import panyi.xyz.videoeditor.fragment.FileSelectFragment;
import panyi.xyz.videoeditor.model.SelectFileItem;

/**
 * 选择需要编辑的文件
 *
 */
public class SelectFileActivity extends AppCompatActivity {

    public static void start(Activity context , int requestCode){
        Intent it = new Intent(context , SelectFileActivity.class);
        context.startActivityForResult(it , requestCode);
    }

    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_select_file);
        installToolbar();

        FileSelectFragment newFragment = new FileSelectFragment();
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.replace(R.id.container, newFragment);
        transaction.commit();
    }

    private void installToolbar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(R.string.select_file);
    }
}
