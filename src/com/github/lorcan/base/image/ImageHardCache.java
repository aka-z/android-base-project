/*
 *
 */
package com.github.lorcan.base.image;

import android.graphics.Bitmap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 完全使用hard cache
 *
 * @author Tsimle
 */
public class ImageHardCache implements ImageCache {

    /**
     * Default memory cache size 5MB
     */
    private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 5;

    private final LinkedHashMap<String, Bitmap> map;

    /**
     * Size of this cache in units. Not necessarily the number of elements.
     */
    private int size;
    private int maxSize;

    private int putCount;
    private int createCount;
    private int evictionCount;
    private int hitCount;
    private int missCount;

    public ImageHardCache() {
        this.maxSize = DEFAULT_MEM_CACHE_SIZE;
        this.map = new LinkedHashMap<String, Bitmap>(0, 0.75f, true);
    }

    @Override
    public Bitmap get(String key) {
        if (key == null) {
            return null;
        }

        Bitmap mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }
        return null;
    }

    @Override
    public Bitmap put(String key, Bitmap value) {
        if (key == null || value == null) {
            return null;
        }

        Bitmap previous;
        synchronized (this) {
            putCount++;
            size += safeSizeOf(key, value);
            previous = map.put(key, value);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }

        trimToSize(maxSize);
        return previous;
    }

    /**
     * @param maxSize the maximum size of the cache before returning. May be -1 to
     *                evict even 0-sized elements.
     */
    private void trimToSize(int maxSize) {
        while (true) {
            String key;
            Bitmap value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize) {
                    break;
                }

                Map.Entry<String, Bitmap> toEvict = (Map.Entry<String, Bitmap>) this.map
                        .entrySet().iterator().next();
                if (toEvict == null) {
                    break;
                }

                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= safeSizeOf(key, value);
                evictionCount++;
            }

            entryRemoved(true, key, value, null);
        }
    }

    public void clear() {
        trimToSize(-1); // -1 will evict 0-sized elements
    }

    /**
     * 删除context对应的图片缓存<br>
     * 在android3.0之前对bitmap recycle，能使图片更早被释放<br>
     * 这个方法一定在Activity destroy时调用，防止bitmap被释放后<br>
     * 用到该bitmap的ImageView抛recycle错误
     */
    @Override
    public void clear(String keyStart) {
        if (keyStart == null) {
            return;
        }
        String[] arrays = null;
        synchronized (this) {
            int length = map.keySet().size();
            arrays = new String[length];
            map.keySet().toArray(arrays);
            if (arrays != null) {
                for (String key : arrays) {
                    if (key != null && key.startsWith(keyStart)) {
                        Bitmap value = map.get(key);
                        if (null != value && !value.isRecycled()) {
                            value.recycle();
                        }
                        map.remove(key);
                        size -= safeSizeOf(key, value);
                        evictionCount++;
                    }
                }
            }
        }
    }

    protected void entryRemoved(boolean evicted, String key, Bitmap oldValue,
                                Bitmap newValue) {
    }

    private int safeSizeOf(String key, Bitmap value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "="
                    + value);
        }
        return result;
    }

    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    /**
     * For caches that do not override {@link #sizeOf}, this returns the number
     * of entries in the cache. For all other caches, this returns the sum of
     * the sizes of the entries in this cache.
     */
    public synchronized final int size() {
        return size;
    }

    /**
     * For caches that do not override {@link #sizeOf}, this returns the maximum
     * number of entries in the cache. For all other caches, this returns the
     * maximum sum of the sizes of the entries in this cache.
     */
    public synchronized final int maxSize() {
        return maxSize;
    }

    /**
     * Returns the number of times {@link #get} returned a value that was
     * already present in the cache.
     */
    public synchronized final int hitCount() {
        return hitCount;
    }

    /**
     * Returns the number of times {@link #get} returned null or required a new
     * value to be created.
     */
    public synchronized final int missCount() {
        return missCount;
    }

    /**
     * Returns the number of times {@link #(Object)} returned a value.
     */
    public synchronized final int createCount() {
        return createCount;
    }

    /**
     * Returns the number of times {@link #put} was called.
     */
    public synchronized final int putCount() {
        return putCount;
    }

    /**
     * Returns the number of values that have been evicted.
     */
    public synchronized final int evictionCount() {
        return evictionCount;
    }

    @Override
    public synchronized final String toString() {
        int accesses = hitCount + missCount;
        int hitPercent = accesses != 0 ? (100 * hitCount / accesses) : 0;
        return String
                .format("ImageCacheAft3[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%,curSize=%d,evictionCount=%d]",
                        maxSize, hitCount, missCount, hitPercent, size,
                        evictionCount);
    }
}
