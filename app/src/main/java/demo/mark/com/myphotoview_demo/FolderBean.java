package demo.mark.com.myphotoview_demo;

/**
 *
 * Created by Administrator on 2018/2/6.
 */

public class FolderBean {
    /**
     * 当前文件夹的路径
     */
    private String dir;
    /**
     * 默认的第一张的图片的路径
     */
    private String firstImgPath;
    private String name;
    private int count;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf = this.dir.indexOf("/");
        this.name = this.dir.substring(lastIndexOf);
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
