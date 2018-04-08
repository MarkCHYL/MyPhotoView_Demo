package demo.mark.com.myphotoview_demo.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 图片的加载管理工具类
 * Created by mark on 2018/1/30.
 */

public class ImageLoader {
    private static ImageLoader mInstance;

    /**
     * 图片管理的核心类
     */
    private LruCache<String, Bitmap> mLruCache;
    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    /**
     * 控制线程池中的线程的数量
     */
    private static final int DEADULT_THREAD_COUNT = 1;
    /**
     * 默认的队列的调度方式
     */
    private Type mType = Type.LIFO;
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * 后台的轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    /**
     * UI线程中的handler
     */
    private Handler mUIHandler;

    //控制轮训线程的并发的类，同步线程的顺序
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);

    private Semaphore mSemaphorePool;
    /**
     * 枚举数据队列的调度方式
     */
    public enum Type {
        FIFO, LIFO;
    }


    public ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    /**
     * 初始化
     *
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        //后台的轮询线程初始化
        this.mPoolThread = new Thread() {
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
//                        super.handleMessage(msg);
                        //线程池中取出一个任务进行执行
                        if (getTask() != null){
                            mThreadPool.execute(getTask());
                        }
                        try {
                            mSemaphorePool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();
        //获取应用的最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        //LruCache的初始化
        this.mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        //创建线程池
        this.mThreadPool = Executors.newFixedThreadPool(threadCount);
        this.mTaskQueue = new LinkedList<Runnable>();
        this.mType = type;

        mSemaphorePool = new Semaphore(threadCount);
    }

    /**
     * 从任务队列取出一个方法
     *
     * @return
     */
    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * 单列模式，避免多个线程同时进入，创建多个实例，拉低程序执行效率
     *
     * @return
     */
    public static ImageLoader getInstance() {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(DEADULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据path为imageView设置图片
     *
     * @param path
     * @param imageView
     */
    @SuppressLint("HandlerLeak")
    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
//                    super.handleMessage(msg);
                    //获取得到的图片，为imageView回调设置图片
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap bitmap = holder.bitmap;
                    ImageView imageView1 = holder.imageView;
                    String path = holder.path;
                    //将tag和img的路径进行比较（图片复用的时候防止图片的使用错乱）
                    if (imageView1.getTag().toString().equals(path)) {
                        imageView1.setImageBitmap(bitmap);
                    }

                }
            };
        }
        //根据path在缓存中获取bitmap
        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            refreshBitmap(path, imageView, bm);
        } else {
            addTask(new Runnable() {
                @Override
                public void run() {
                    //加载图片
                    //图片的压缩
                    //1。获取图片的需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    //2。压缩图片
                    Bitmap bm = decodeSampledBitmapFromPath(path, imageSize.width,
                            imageSize.height);
                    //3.把图拍加入缓存
                    addBitmapToLruCache(path, bm);

                    refreshBitmap(path, imageView, bm);

                    mSemaphorePool.release();
                }
            });
        }
    }

    /**
     * 通知handler刷新图片数据
     *
     * @param path
     * @param imageView
     * @param bm
     */
    private void refreshBitmap(String path, ImageView imageView, Bitmap bm) {
        Message message = Message.obtain();
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.bitmap = bm;
        holder.imageView = imageView;
        holder.path = path;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    /**
     * 将图片加入LruCache
     *
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    /**
     * 根据图片的显示的宽和高来压缩图片方法
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        //1.获取图片的宽和高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = caculateInSampleSize(options, width, height);
        //使用获取的InSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    /**
     * 根据需求的宽和高以及图片的实际宽和高计算SampleSize
     *
     * @param options
     * @param reWidth
     * @param reHeight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reWidth, int reHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;
        if (width > reWidth || height > reHeight) {
            int widthRadio = Math.round(width * 1.0f / reWidth);
            int heightRadio = Math.round(height * 1.0f / reHeight);

            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }

    /**
     * 根据ImageView获取适当的压缩的宽和高
     *
     * @param imageView
     * @return
     */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext()
                .getResources().getDisplayMetrics();

        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        //宽度
        int width = imageView.getWidth();//获取imageView 的实际宽度
        if (width <= 0) {
            width = lp.width;//获取imageView在layout中声明的宽度
        }
        if (width <= 0) {
//            width = imageView.getMaxWidth();//检查最大值
            width = getImageFeildValue(imageView,"mMaxWidth");
        }
        if (width <= 0) {
            width = displayMetrics.widthPixels;//屏幕的宽度
        }

        //高度
        int height = imageView.getHeight();//获取imageView 的实际宽度
        if (height <= 0) {
            height = lp.height;//获取imageView在layout中声明的宽度
        }
        if (height <= 0) {
//            height = imageView.getMaxHeight();//检查最大值
            height = getImageFeildValue(imageView,"mMaxHeight");
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;//屏幕的宽度
        }
        imageSize.height = height;
        imageSize.width = width;
        return imageSize;
    }

    /**
     * 通过反射获取 ImageView 的某个属性
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageFeildValue(Object object,String fieldName){
        int value = 0 ;

        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE){
                value = fieldValue;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }


    private void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);

        try {
            if (mPoolThreadHandler == null)
            mSemaphorePoolThreadHandler.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mPoolThreadHandler.sendEmptyMessage(0X110);
    }

    /**
     * 根据path在缓存中获取bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    /**
     * 图片的尺寸（宽、高）
     */
    private class ImageSize {
        int width;
        int height;
    }

    /**
     * 为了避免handler里面设置图片是造成数据混乱
     */
    private class ImgBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
