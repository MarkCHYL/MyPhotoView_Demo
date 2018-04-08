package demo.mark.com.myphotoview_demo.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import demo.mark.com.myphotoview_demo.FolderBean;
import demo.mark.com.myphotoview_demo.R;

/**
 * 仿微信图片选择器
 * Created by mark on 2018/2/2.
 */

public class ImageSelectActivity extends AppCompatActivity{

    private GridView mGridView;
    private List<String> mImgs;
    private RelativeLayout rlBottom;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();
    //显示加载图片的进度条
    private ProgressDialog mProgressDialog;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x110){
                mProgressDialog.dismiss();
                //绑定数据到View中
                data2View();
            }
        }
    };

    private void data2View() {
        if (mCurrentDir == null){
            Toast.makeText(this,"未扫描到任何图片！",Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_weximg);

        initView();
        initDatas();
        initEvent();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mGridView = findViewById(R.id.gv_imgshow);
        rlBottom = findViewById(R.id.rl_bottom);
        mDirName = findViewById(R.id.tv_allkind);
        mDirCount = findViewById(R.id.tv_allnum);
    }

    /**
     * 初始化数据
     * 利用ContentProvider扫描手机中所有图片
     */
    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Toast.makeText(this,"当前的存储卡不可用！",Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this,null,"正在加载...");

        new Thread(){
            @Override
            public void run() {
                Uri mImagUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = ImageSelectActivity.this.getContentResolver();

                Cursor cursor = cr.query(mImagUri,null,
                        MediaStore.Images.Media.MIME_TYPE + " = ?or "
                        + MediaStore.Images.Media.MIME_TYPE + " = ?"
                        ,new String[]{"image/jpeg","image/png"}
                        ,MediaStore.Images.Media.DATE_MODIFIED);

                //防止父目录重复便利
                Set<String> mDirPaths = new HashSet<String>();

                if (cursor != null) {
                    while (cursor.moveToNext()){
                        String path = cursor.getString(cursor.
                                getColumnIndex(MediaStore.Images.Media.DATA));
                        File  parentFile = new File(path).getParentFile();
                        if (parentFile == null)
                            continue;
                        String dirPath = parentFile.getAbsolutePath();
                        FolderBean folderBean = null;
                        if (mDirPaths.contains(dirPath)){
                            continue;
                        }else {
                            mDirPaths.add(dirPath);
                            folderBean = new FolderBean();
                            folderBean.setDir(dirPath);
                            folderBean.setFirstImgPath(path);
                        }

                        if (parentFile.list() == null)
                            continue;
                        int picSize = parentFile.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                if (name.endsWith(".jpg")
                                        ||name.endsWith(".jpeg")
                                        ||name.endsWith(".png")){
                                    return true;
                                }
                                return false;
                            }
                        }).length;

                        folderBean.setCount(picSize);

                        mFolderBeans.add(folderBean);
                        if (picSize > mMaxCount){
                            mMaxCount = picSize;
                            mCurrentDir = parentFile;
                        }
                    }
                    cursor.close();
                    //通知Handler扫描图片完成
                    handler.sendEmptyMessage(0x110);
                }
            }
        }.start();
    }

    /**
     * 初始化事件
     */
    private void initEvent() {
    }
}
