package com.monobogdan.minivk.api;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import com.monobogdan.minivk.PersistStorage;

import org.apache.http.util.EncodingUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class VK {

    private final String apiUrl = "http://90.156.209.92/apirelay.php";
    private final String audioUrl = "http://90.156.209.92/audiorelay.php";
    private final String version = "5.131";

    private final String APP_ID = "2274003";
    private final String APP_SECRET = "hHbZxrka2uZ6jB1inYsH";

    private Context context;
    private ExecutorService threadPool;
    private AtomicBoolean jobInProgress;

    final int THREAD_COUNT = 3;

    public interface VKResult
    {
        void success(JSONObject json);
        void failed(String reason);
    }

    public interface AuthResult
    {
        void success();
        void error(String desc);
    }

    private ConnectivityManager conMan;

    public VK(Context context)
    {
        this.context = context;
        threadPool = Executors.newSingleThreadExecutor();

        jobInProgress = new AtomicBoolean();

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
    }

    public boolean isJobInProgress() {
        return jobInProgress.get();
    }

    public void beginDirectAuthFlow(String login, String password, AuthResult auth, Activity thiz)
    {
        String fmt = String.format("https://oauth.vk.com/token?grant_type=password&validate_token=true&client_id=%s&client_secret=%s&username=%s&password=%s",
                APP_ID, APP_SECRET, login, password);

        directRequest(fmt, new VKResult() {
            @Override
            public void success(JSONObject json) {
                try {
                    if (json.has("access_token")) {
                        // Grant us access as official app
                        PersistStorage.getAppInstance().apiToken = json.getString("access_token");

                        auth.success();
                    }
                    else
                        auth.error(json.getString("error_description"));
                }
                catch (JSONException e)
                {
                    auth.error(e.getLocalizedMessage());
                }
            }

            @Override
            public void failed(String reason) {
                auth.error(reason);
            }
        }, thiz);
    }

    private String genericRequest(String request)
    {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(request).openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain");
            //conn.setRequestMethod("POST");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = "";
            while (reader.ready())
                response += reader.readLine();

            return response;
        }
        catch (Exception e)
        {
            e.printStackTrace();

            return "";
        }
    }

    private void directRequest(String url, VKResult handler, Activity thiz)
    {
        jobInProgress.set(true);
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 4.01; Windows NT)");
                    conn.connect();

                    OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(url);
                    writer.close();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String response = "";
                    String line = "";
                    while ((line = reader.readLine()) != null)
                        response += line;

                    String finalResponse = response;
                    Log.i("", "run: " + finalResponse);
                    JSONObject obj = new JSONObject(finalResponse);

                    if(thiz != null)
                        thiz.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handler.success(obj);
                            }
                        });
                    else
                        handler.success(obj);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    handler.failed(e.getLocalizedMessage());
                }

                jobInProgress.set(false);
            }
        });
    }

    public void audioRequest(String request, VKResult handler, Activity thiz)
    {
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                String result = genericRequest(audioUrl + "?" + request);

                try
                {
                    handler.success(new JSONObject(result));
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                    handler.failed("Failed to parse JSON");
                }
            }
        });
    }

    public void request(String request, VKResult handler, Activity thiz)
    {
        request = "/method/" + request + "access_token=" + PersistStorage.getAppInstance().apiToken + "&v=" + version;

        try {
            /*MessageDigest md5 = MessageDigest.getInstance("md5");
            byte[] digest = md5.digest(EncodingUtils.getAsciiBytes( request + PersistStorage.getAppInstance().secret));
            String sig = "";

            for(byte b : digest)
            {
                if(b == 0)
                    break;

                sig += String.format("%x", b);
            }*/

            //Log.i("TAG", "md5: " + sig);
            //Log.i("TAG", "request: " + request + PersistStorage.getAppInstance().secret);
            directRequest("https://api.vk.com" + request, handler, thiz);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
