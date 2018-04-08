package demo.mark.com.myimageselecter_demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import demo.mark.com.myimageselecter_demo.bean.ImageBean;
import demo.mark.com.myimageselecter_demo.ui.ImageSelectActivity;
import demo.mark.com.myimageselecter_demo.widget.NineImageView;

public class MarkActivity extends AppCompatActivity {
    private NineImageView mNineImageView;
    private ImageView imageView;
    private Button button,button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark);
        button = (Button) findViewById(R.id.button);
        button1= (Button) findViewById(R.id.button1);
        mNineImageView = (NineImageView) findViewById(R.id.nineImageView);
        imageView= (ImageView) findViewById(R.id.imageView);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MarkActivity.this, ImageSelectActivity.class);
                intent.putExtra("select",9);
                startActivityForResult(intent, 0);
            }
        });
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MarkActivity.this, ImageSelectActivity.class);
                intent.putExtra("select",1);
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==Activity.RESULT_OK){
            switch (requestCode){
                //多图
                case 0:
                    Bundle bundle = data.getExtras();
                    ArrayList<ImageBean> list = bundle.getParcelableArrayList("selectImages");
                    LayoutInflater inflater = LayoutInflater.from(this);
                    //避免重复添加
                    if (mNineImageView.getChildCount() > 0) {
                        mNineImageView.removeAllViews();
                    }
                    for (int i = 0; i < list.size(); i++) {
                        ImageView imageView = (ImageView) inflater.inflate(R.layout.nine_image, mNineImageView, false);
                        Glide.with(this).load(list.get(i).getPath()).into(imageView);
                        mNineImageView.addView(imageView);
                    }
                    break;
                //头像
                case 1:
                    byte [] bitmap=data.getByteArrayExtra("bitmap");
                    Glide.with(this).load(bitmap).into(imageView);
                    break;
            }
        }

    }
}
