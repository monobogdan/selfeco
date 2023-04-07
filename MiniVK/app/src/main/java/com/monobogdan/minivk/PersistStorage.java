package com.monobogdan.minivk;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class PersistStorage {
    private static PersistStorage appInstance;

    private final int PERSIST_VERSION = 100;
    private final String TAG = "PersistStorage";
    private final String FILENAME = "/minivk.obj";

    public String apiToken;
    public String secret;
    public String uid;
    public String audioToken;

    public static PersistStorage getAppInstance() {
        if(appInstance == null)
            appInstance = new PersistStorage();

        return appInstance;
    }

    private PersistStorage()
    {

    }

    public boolean isAuthorized()
    {
        return apiToken != null && apiToken.length() > 0;
    }

    public void load(Context context)
    {
        File file = new File(context.getFilesDir().getAbsolutePath() + FILENAME);

        try {
            BufferedReader strm = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String json = "";

            while(strm.ready())
                json += strm.readLine();
            strm.close();

            JSONObject obj = new JSONObject(json);
            apiToken = obj.getString("apiToken");
            uid = obj.getString("uid");
            secret = obj.getString("secret");
            audioToken = obj.getString("audioToken");
        }
        catch(Exception e)
        {
            Log.i(TAG, "save: Failed to load PersistStorage");
        }
    }

    public void save(Context context)
    {
        File file = new File(context.getFilesDir().getAbsolutePath() + FILENAME);

        try {
            OutputStreamWriter strm = new OutputStreamWriter(new FileOutputStream(file));

            JSONObject obj = new JSONObject();
            obj.put("version", PERSIST_VERSION);
            obj.put("apiToken", apiToken);
            obj.put("uid", uid);
            obj.put("secret", secret);
            obj.put("audioToken", audioToken);

            strm.write(obj.toString());
            strm.close();
        }
        catch(Exception e)
        {
            Log.i(TAG, "save: Failed to save PersistStorage");
        }
    }
}
