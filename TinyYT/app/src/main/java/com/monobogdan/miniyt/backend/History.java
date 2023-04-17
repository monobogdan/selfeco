package com.monobogdan.miniyt.backend;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class History {
    private final String fileName = "/history.json";

    private File hFile;
    private ArrayList<YTAPI.Video> videos;

    public History(Context ctx)
    {
        hFile = new File(ctx.getFilesDir().getAbsolutePath() + fileName);
        videos = new ArrayList<>();

        try {
            if (!hFile.exists())
                hFile.createNewFile();

            read();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void read() throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(hFile)));
        String json = "";

        while(reader.ready())
            json += reader.readLine();

        reader.close();

        try {
            JSONArray arr = new JSONArray(json);
            videos = new ArrayList<>(20);

            for(int i = 0; i < arr.length(); i++)
            {
                YTAPI.Video vid = new YTAPI.Video();
                JSONObject vidObj = arr.getJSONObject(i);

                vid.id = vidObj.getString("id");
                vid.name = vidObj.getString("name");

                if(vidObj.has("preview"))
                    vid.preview = vidObj.getString("preview");

                videos.add(vid);
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();

            videos = new ArrayList<>(); // Assume history are empty
        }
    }

    public ArrayList<YTAPI.Video> getVideos() {
        return videos;
    }

    public void put(YTAPI.Video video)
    {
        videos.add(0, video);

        if(videos.size() > 20)
            videos.remove(videos.size() - 1);

        JSONArray arr = new JSONArray();
        for(YTAPI.Video vid : videos)
        {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", vid.id);
                obj.put("name", vid.name);
                obj.put("preview", vid.preview);

                arr.put(obj);
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(hFile, false)));
            writer.write(arr.toString());
            writer.close();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }
}
