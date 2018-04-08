package com.example.luban_img_demo.bean;

/**
 * 图片的实体类
 * Created by Mark on 2017/9/13.
 */
public class ImageBean {
    private String originArg;//图片的原大小
    private String thumbArg;//压缩后的大小
    private String image;//压缩图片的地址（uri）

    public ImageBean(String originArg, String thumbArg, String image) {
        this.originArg = originArg;
        this.thumbArg = thumbArg;
        this.image = image;
    }

    public String getOriginArg() {
        return originArg;
    }

    public void setOriginArg(String originArg) {
        this.originArg = originArg;
    }

    public String getThumbArg() {
        return thumbArg;
    }

    public void setThumbArg(String thumbArg) {
        this.thumbArg = thumbArg;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
