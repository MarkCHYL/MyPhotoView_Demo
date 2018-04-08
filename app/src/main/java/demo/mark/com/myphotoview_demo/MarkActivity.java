package demo.mark.com.myphotoview_demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import demo.mark.com.myphotoview_demo.ui.AlbumActivity;
import demo.mark.com.myphotoview_demo.ui.ImageSelectActivity;

public class MarkActivity extends AppCompatActivity implements View.OnClickListener{

    private Button btn_album;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark);
        initView();
    }

    private void initView() {
        btn_album = (Button) findViewById(R.id.btn_album);

        btn_album.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_album://图片和视频混合选择
                startActivity(new Intent(this, AlbumActivity.class));
                break;
        }
    }

    /**
     * 仿微信图片选择器
     * @param view
     */
    public void doWexPicImg(View view){
        startActivity(new Intent(this, ImageSelectActivity.class));
    }
}
