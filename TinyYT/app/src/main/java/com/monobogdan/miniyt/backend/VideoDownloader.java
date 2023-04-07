package com.monobogdan.miniyt.backend;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class VideoDownloader {
    private File cacheDir;

    public interface Callback
    {
        void success(String fileName);
        void reportProgress(int progress, int total);
        void failed(String reason);
    }

    private boolean jobInProgress;

    public VideoDownloader(Context ct)
    {
        cacheDir = new File("/sdcard/miniyt/");
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public boolean createCacheFolder()
    {
        return cacheDir.mkdirs();
    }

    public void beginDownloading(String url, String fileName, Callback cb)
    {
        if(jobInProgress)
        {
            cb.failed("Загрузка уже идёт");

            return;
        }

        File incompleted = new File(cacheDir.getAbsolutePath() + "/" + fileName + ".incompleted");
        File cached = new File(cacheDir.getAbsolutePath() + "/" + fileName + ".mp4");
        if(cached.exists() && !incompleted.exists())
        {
            cb.success(cached.getAbsolutePath());
            return;
        }

        jobInProgress = true;
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
                    conn.setDoInput(true);
                    conn.setRequestMethod("GET");
                    Log.i("", "run: 2");
                    conn.connect();


                    Log.i("", "run: 1");

                    int len = conn.getContentLength();
                    BufferedInputStream reader = new BufferedInputStream(conn.getInputStream(), 4096000);

                    cached.createNewFile();
                    incompleted.createNewFile();
                    FileOutputStream os = new FileOutputStream(cached);
                    byte[] buffer = new byte[4096000];
                    int numRead = 0;

                    while(numRead < len)
                    {
                        int amt = reader.read(buffer);
                        os.write(buffer, 0, amt);

                        numRead += amt;
                        cb.reportProgress(numRead / 1024, len / 1024);
                    }

                    conn.disconnect();
                    os.close();
                    incompleted.delete();
                    cb.success(cached.getAbsolutePath());
                }
                catch (Exception e)
                {
                    e.printStackTrace();

                    cb.failed(e.getLocalizedMessage());
                }

                jobInProgress = false;
            }
        });
    }
}
