package com.github.lorcan.base.image;

import android.graphics.Bitmap;

/**
 * 内存缓存图片接口
 *
 * @author Tsimle
 */
public interface ImageCache {

    public Bitmap get(String key);

    public Bitmap put(String key, Bitmap value);

    public void clear(String keyStart);
}
