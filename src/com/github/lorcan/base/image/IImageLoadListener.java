/*
 *
 */
package com.github.lorcan.base.image;

import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * 使用ImageLoader加载图片完毕后回调该函数.
 *
 * @author Tsimle
 */
public interface IImageLoadListener {

    /**
     * On image loaded.
     *
     * @param bm          the bm
     * @param imageView   the image view
     * @param loadSuccess the load success
     */
    void onImageLoaded(Bitmap bm, ImageView imageView, boolean loadSuccess);
}
