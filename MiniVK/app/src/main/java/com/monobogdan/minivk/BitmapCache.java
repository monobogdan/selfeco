package com.monobogdan.minivk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class BitmapCache {

    class LifetimedBitmap
    {
        Bitmap bitmap;
        int lifeTime;
    }

    private static ExecutorService threadPool;
    private static HashMap<String, Bitmap> cache;

    public static interface CacheCallback
    {
        void success(Bitmap bitmap);
    }

    static
    {
        cache = new HashMap<>();
        threadPool = Executors.newSingleThreadExecutor();
    }

    public static byte[] downloadBinary(Uri uri)
    {
        try
        {
            HttpsURLConnection conn = (HttpsURLConnection) new URL(uri.toString()).openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.connect();

            BufferedInputStream stream = new BufferedInputStream(conn.getInputStream());
            byte[] ret = new byte[conn.getContentLength()];

            int ptr = 0;
            while(ptr < ret.length)
                ptr += stream.read(ret, ptr, ret.length - ptr);

            return ret;
        }
        catch (Exception e)
        {
            e.printStackTrace();

            return null;
        }
    }

    public static void getBitmapFromCache(Context ctx, Uri uri, CacheCallback cb)
    {
        File cacheDir = ctx.getCacheDir();
        File absPath = new File(cacheDir.getAbsolutePath() + "/" + new File(uri.toString()).getName());

        if(absPath.exists()) {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    Bitmap bmp = null;

                    if(cache.containsKey(absPath.getName())) {
                        bmp = cache.get(absPath.getName());
                    }
                    else
                    {
                        bmp = BitmapFactory.decodeFile(absPath.getAbsolutePath());
                        cache.put(absPath.getName(), bmp);
                    }

                    cb.success(bmp);
                }
            });
        }
        else
        {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    byte[] data = downloadBinary(uri);

                    if(data != null)
                    {
                        try {
                            FileOutputStream strm = new FileOutputStream(absPath.getAbsolutePath());
                            strm.write(data);
                            strm.close();

                            cb.success(BitmapFactory.decodeByteArray(data, 0, data.length));
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }
}
