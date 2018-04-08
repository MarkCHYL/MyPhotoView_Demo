package view.demo.mark.thirdselectdemo;

import android.app.Application;

/**
 * Created by mark on 2017/9/4.
 */

public class MyApp extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new LocalFileUncaughtExceptionHandler(this,
                Thread.getDefaultUncaughtExceptionHandler()));
    }
}
