package com.github.lorcan.base.image;

import android.graphics.Bitmap;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * 使用10个图片存bitmap，其它使用softReference存bitmap
 *
 * @author Tsimle
 */
public class ImageSoftCache implements ImageCache {

    private static final int HARD_CACHE_CAPACITY = 10;

    /**
     * Hard cache, with a fixed maximum capacity and a life duration
     */
    private final HashMap<String, Bitmap> sHardBitmapCache;

    /**
     * Soft cache for bitmaps kicked out of hard cache
     */
    private final HashMap<String, SoftReference<Bitmap>> sSoftBitmapCache;

    private int hitCount;
    private int missCount;

    @SuppressWarnings("serial")
    public ImageSoftCache() {
        sHardBitmapCache = new LinkedHashMap<String, Bitmap>(
                HARD_CACHE_CAPACITY / 2, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(
                    Entry<String, Bitmap> eldest) {
                if (size() > HARD_CACHE_CAPACITY) {
                    // Entries push-out of hard reference cache are transferred
                    // to soft reference cache
                    sSoftBitmapCache.put(eldest.getKey(),
                            new SoftReference<Bitmap>(eldest.getValue()));
                    return true;
                } else {
                    return false;
                }
            }
        };

        sSoftBitmapCache = new HashMap<String, SoftReference<Bitmap>>(
                HARD_CACHE_CAPACITY / 2);
    }

    /**
     * Adds this bitmap to the cache.
     *
     * @param bitmap The newly downloaded bitmap.
     */
    public Bitmap put(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) {
            return null;
        }

        synchronized (this) {
            return sHardBitmapCache.put(key, bitmap);
        }
    }

    /**
     * @param key The URL of the image that will be retrieved from the cache.
     * @return The cached bitmap or null if it was not found.
     */
    public Bitmap get(String key) {
        Bitmap bitmap;
        SoftReference<Bitmap> bitmapReference;

        // First try the hard reference cache
        synchronized (this) {
            bitmap = sHardBitmapCache.get(key);
            if (bitmap != null) {
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
                sHardBitmapCache.remove(key);
                sHardBitmapCache.put(key, bitmap);
                hitCount++;
                return bitmap;
            }

            // Then try the soft reference cache
            bitmapReference = sSoftBitmapCache.get(key);
            if (bitmapReference != null) {
                bitmap = bitmapReference.get();
                if (bitmap != null) {
                    // Bitmap found in soft cache
                    hitCount++;
                    return bitmap;
                } else {
                    // Soft reference has been Garbage Collected
                    sSoftBitmapCache.remove(key);
                }
            }
            missCount++;
        }
        return null;
    }

    /**
     * 删除context对应的图片缓存<br>
     * 在android3.0之前对bitmap recycle，能使图片更早被释放<br>
     * 这个方法一定在Activity destroy时调用，防止bitmap被释放后<br>
     * 用到该bitmap的ImageView抛recycle错误
     */
    public void clear(String keyStart) {
        if (keyStart == null) {
            return;
        }

        // 清空hardcache中含该loadContext的缓存
        String[] arrays = null;
        synchronized (this) {
            int length = sHardBitmapCache.keySet().size();
            arrays = new String[length];
            sHardBitmapCache.keySet().toArray(arrays);
            if (arrays != null) {
                for (String hardKey : arrays) {
                    if (hardKey != null && hardKey.startsWith(keyStart)) {
                        Bitmap bitmap = sHardBitmapCache.get(hardKey);
                        if (null != bitmap && !bitmap.isRecycled()) {
                            bitmap.recycle();
                        }
                        sHardBitmapCache.remove(hardKey);
                    }
                }
            }

            Iterator<String> softIterator = sSoftBitmapCache.keySet()
                    .iterator();
            while (softIterator.hasNext()) {
                String softKey = softIterator.next();
                if (softKey != null && softKey.startsWith(keyStart)) {
                    SoftReference<Bitmap> ref = sSoftBitmapCache.get(softKey);
                    if (ref != null) {
                        Bitmap bitmap = ref.get();
                        if (null != bitmap && !bitmap.isRecycled()) {
                            bitmap.recycle();
                        }
                    }
                    softIterator.remove();
                }
            }
        }
    }

    public synchronized final int hitCount() {
        return hitCount;
    }

    public synchronized final int missCount() {
        return missCount;
    }

    @Override
    public synchronized final String toString() {
        int accesses = hitCount + missCount;
        int hitPercent = accesses != 0 ? (100 * hitCount / accesses) : 0;
        return String.format("ImageCache[hits=%d,misses=%d,hitRate=%d%%]",
                hitCount, missCount, hitPercent);
    }

}
