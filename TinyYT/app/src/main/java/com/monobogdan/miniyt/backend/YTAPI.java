package com.monobogdan.miniyt.backend;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class YTAPI {

    private final String instance = "https://inv.tux.pizza/api/v1/";
    private final String relay = "http://minvk.ru/apirelay.php";

    public static final class Video
    {
        public String preview;
        public String name;
        public String id;
        public String url;

        public int views;
        public String length;
        public int likes;
        public String uploadDate;
    }

    public interface Callback
    {
        void success(String obj);
        void error(String reason);
    }

    public interface PreviewCallback
    {
        void loaded(Bitmap scaledBitmap);
        void error(String reason);
    }

    private Activity primary;
    private ExecutorService imageLoaderThreadPool;

    public YTAPI(Activity primary)
    {
        this.primary = primary;

        imageLoaderThreadPool = Executors.newFixedThreadPool(3);
    }

    public void schedulePreviewDownload(String url, String cacheName, PreviewCallback callback)
    {
        imageLoaderThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    File cacheDir = new File(primary.getApplicationContext().getCacheDir().getAbsolutePath() + "/preview/");
                    cacheDir.mkdir();
                    File cachedPreview = new File(cacheDir.getAbsolutePath() + cacheName);

                    Bitmap bmp = null;

                    if(cachedPreview.exists())
                    {
                        bmp = BitmapFactory.decodeFile(cachedPreview.getAbsolutePath());
                    }
                    else {
                        HttpURLConnection conn = (HttpURLConnection) new URL(relay).openConnection();
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 4.01; Windows NT)");
                        conn.connect();

                        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                        writer.write(url);
                        writer.close();

                        InputStream reader = conn.getInputStream();
                        byte[] buf = new byte[conn.getContentLength()];

                        int ptr = 0;
                        while (ptr < buf.length) {
                            ptr += reader.read(buf, ptr, buf.length - ptr);
                        }
                        Log.i("", "run: " + url + " " + buf.length);
                        bmp = BitmapFactory.decodeByteArray(buf, 0, buf.length);

                        FileOutputStream foStream = new FileOutputStream(cachedPreview);
                        foStream.write(buf);
                    }

                    if(bmp != null) {
                        callback.loaded(Bitmap.createScaledBitmap(bmp, 128, 128, true));
                        bmp.recycle();
                    }
                    else
                    {
                        //callback.error("Ошибка при загрузке превьюшек");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();

                    primary.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.error("Generic error: " + e.getLocalizedMessage());
                        }
                    });
                }
            }
        });
    }

    public Video getVideoDescription(JSONObject obj) throws JSONException
    {
        Video ret = new Video();

        ret.name = obj.getString("title");
        ret.id = obj.getString("videoId");
        ret.likes = obj.getInt("likeCount");
        ret.views = obj.getInt("viewCount");

        JSONArray adaptiveFormats = obj.getJSONArray("formatStreams");

        for(int i = 0; i < adaptiveFormats.length(); i++)
        {
            JSONObject fmt = adaptiveFormats.getJSONObject(i);

            Log.i("", "getVideoDescription: " + fmt.getString("type"));

            if(fmt.getString("type").contains("video/mp4"))
            {
                ret.url = fmt.getString("url");

                break;
            }
        }

        return ret;
    }

    public ArrayList<Video> getVideoList(JSONArray obj) throws JSONException
    {
        ArrayList<Video> ret = new ArrayList<>();

        for (int i = 0; i < obj.length(); i++) {
            JSONObject jVideo = obj.getJSONObject(i);
            Video video = new Video();

                video.id = jVideo.getString("videoId");
                video.name = jVideo.getString("title");
                video.uploadDate = jVideo.getString("publishedText");
                video.views = jVideo.getInt("viewCount");
                JSONArray preview = jVideo.getJSONArray("videoThumbnails");
                video.preview = preview.getJSONObject(preview.length() - 1).getString("url");

                int length = jVideo.getInt("lengthSeconds");
                video.length = (length / 60) + ":" + (length - ((length / 60) * 60));

                ret.add(video);
        }

        return ret;
    }

    public void request(String method, Callback runnable)
    {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    HttpURLConnection conn = (HttpURLConnection) new URL(relay).openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 4.01; Windows NT)");
                    conn.connect();

                    OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(instance + method);
                    writer.close();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String response = "";
                    String line = "";
                    while ((line = reader.readLine()) != null)
                        response += line;

                    String finalResponse = response;
                    Log.i("TAG", "run: " + instance + method);
                    primary.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            runnable.success(finalResponse);
                        }
                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();

                    primary.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            runnable.error("Generic error: " + e.getLocalizedMessage());
                        }
                    });
                }
            }
        });

    }
}
