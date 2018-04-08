package com.example.luban_img_demo.ui;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.example.luban_img_demo.R;
import com.example.luban_img_demo.adapter.ImageAdapter;
import com.example.luban_img_demo.bean.ImageBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.iwf.photopicker.PhotoPicker;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class MarkActivity extends AppCompatActivity {
    private static final String TAG = "Luban";

    private List<ImageBean> mImageList = new ArrayList<>();
    private ImageAdapter mAdapter = new ImageAdapter(mImageList);
    private RecyclerView mRecyclerView;
    private Button btn ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark);
        initView();
        setContent();
    }

    private void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        btn = (Button) findViewById(R.id.fab);
    }

    private void setContent() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        //选取图片
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoPicker.builder()
                        .setPhotoCount(9)
                        .setShowCamera(true)
                        .setShowGif(true)
                        .setPreviewEnabled(false)
                        .start(MarkActivity.this,PhotoPicker.REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE){
            if (data != null){
                mImageList.clear();

                ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);

                compressWithRX(photos);
            }
        }
    }

    /**
     * 同步方法请尽量避免在主线程调用以免阻塞主线程
     * */
    private void compressWithRX(final ArrayList<String> photos) {
        Flowable.just(photos)
                .observeOn(Schedulers.io())
                .map(new Function<List<String>, List<File>>() {
                    @Override
                    public List<File> apply(@NonNull List<String> list) throws Exception {
                        // 同步方法直接返回压缩后的文件
                        return Luban.with(MarkActivity.this).load(list).get();
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<File>>() {
                    @Override
                    public void accept(@NonNull List<File> list) throws Exception {
                        for (File file: list){
                            showResult(photos,file);
                        }
                    }
                });
    }

    /**
     * 压缩图片 Listener 方式
     * */
    private void compressWithLs(final List<String> photos){
        Luban.with(this)
                .load(photos)               // 传人要压缩的图片列表
                .ignoreBy(100)              // 忽略不压缩图片的大小
                .setTargetDir(getPath())    // 设置压缩后文件存储位置
                .setCompressListener(new OnCompressListener() {   //设置回调
                    @Override
                    public void onStart() {
                         // TODO 压缩开始前调用，可以在方法内启动 loading UI
                    }

                    @Override
                    public void onSuccess(File file) {
                        // TODO 压缩成功后调用，返回压缩后的图片文件
                        showResult(photos,file);
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO 当压缩过程出现问题时调用
                    }
                }).launch();    //启动压缩
    }

    /**
     * 压缩图片的路径
     * */
    private String getPath(){
        String path = Environment.getExternalStorageDirectory() + "/Luban/image/";
        File file = new File(path);
        if (file.mkdirs()){
            return path;
        }
        return path;
    }

    private void showResult(List<String> photos,File file){
        int[] originSize = computeSize(photos.get(mAdapter.getItemCount()));
        int[] thumbSize = computeSize(file.getAbsolutePath());
        String originArg = String.format(Locale.CHINA,"原图参数：%d*%d，%d",originSize[0],originSize[1],new File(
                photos.get(mAdapter.getItemCount())).length() >> 10);
        String thumbArg = String.format(Locale.CHINA,"压缩后参数：%d*%d，%d",
                thumbSize[0],thumbSize[1],file.length() >> 10);

        ImageBean imagebean = new ImageBean(originArg,thumbArg,file.getAbsolutePath());
        mImageList.add(imagebean);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 得到图片的大小
     * */
    private int[] computeSize(String srcImg){
        int[] size = new int[2];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;

        BitmapFactory.decodeFile(srcImg,options);
        size[0] = options.outWidth;
        size[1] = options.outHeight;
        return  size;
    }
}
