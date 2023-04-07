package com.monobogdan.minivk.api;

import android.text.format.DateUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VKDataSet {

    public static final class Dialog
    {
        public int id;
        public String avatar;
        public String name;
        public String lastMessage;
        public String date;
        public int inRead;

        public int lastMessageDate;
    }

    public static final class Message
    {
        public int sender;
        public String avatarName;
        public String senderReadable;
        public String text;
        public long date;
        public String dateFormatted;
        public boolean isRead;

        public boolean hasVoiceAttachment;
        public String voiceUrl;

        public boolean hasAttachments;
        public ArrayList<String> attachments; // Pictures are meant to be here
    }

    public static final class User
    {
        public String name;
        public int id;
        public String avatarUrl;
        public boolean isOnline;
    }

    public static final class Audio
    {
        public String contentId; // Used also for caching
        public String artist;
        public String title;
        public String duration;
    }

    private static Map<Integer, User> getUsersFromResponse(JSONArray resp) throws JSONException
    {
        Map<Integer, User> ret = new HashMap<>();

        for(int i = 0; i < resp.length(); i++)
        {
            JSONObject profile = resp.getJSONObject(i);
            User user = new User();

            user.id = profile.getInt("id");
            user.avatarUrl = profile.getString("photo_100");
            user.isOnline = profile.getInt("online") == 1;
            user.name = profile.getString("first_name") + " " + profile.get("last_name");

            ret.put(user.id, user);
        }

        return ret;
    }

    private static String formatDate(long date)
    {
        if(DateUtils.isToday(date)) {
            return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(date * 1000));
        }
        else
        {
            return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(date * 1000));
        }
    }

    public static ArrayList<Message> fetchMessages(JSONObject obj)
    {
        try
        {
            obj = obj.getJSONObject("response");
            JSONArray dialogs = obj.getJSONArray("items");

            ArrayList<Message> ret = new ArrayList<>();
            Map<Integer, User> users = getUsersFromResponse(obj.getJSONArray("profiles"));

            for(int i = 0; i < dialogs.length(); i++)
            {
                JSONObject jmsg = dialogs.getJSONObject(i);
                Message message = new Message();

                message.date = jmsg.getLong("date");
                message.dateFormatted = formatDate(message.date);
                message.text = jmsg.getString("text");
                message.sender = jmsg.getInt("from_id");
                message.senderReadable = users.get(message.sender).name;
                message.avatarName = new File(users.get(message.sender).avatarUrl).getName();

                JSONArray attachments = jmsg.getJSONArray("attachments");

                if(attachments.length() == 1 && attachments.getJSONObject(0).getString("type").equals("audio_message"))
                {
                    message.hasVoiceAttachment = true;
                    message.voiceUrl = attachments.getJSONObject(0).getJSONObject("audio_message").getString("link_mp3");
                }
                else
                {
                    message.hasAttachments = attachments.length() > 0;
                    message.attachments = new ArrayList<>();

                    for(int j = 0; j < attachments.length(); j++)
                    {
                        JSONObject at = attachments.getJSONObject(j);

                        if(at.getString("type").equals("photo")) {
                            JSONArray sizes = at.getJSONObject("photo").getJSONArray("sizes");
                            message.attachments.add(sizes.getJSONObject(sizes.length() - 1).getString("url"));
                        }
                    }
                }

                ret.add(message);
            }

            return ret;
        }
        catch (JSONException e)
        {
            e.printStackTrace();

            return new ArrayList<>();
        }
    }

    public static ArrayList<Dialog> fetchDialogs(JSONObject obj)
    {
        try {
            obj = obj.getJSONObject("response");
            JSONArray dialogs = obj.getJSONArray("items");
            Map<Integer, User> users = getUsersFromResponse(obj.getJSONArray("profiles"));

            ArrayList<Dialog> ret = new ArrayList<>();

            for(int i = 0; i < dialogs.length(); i++)
            {
                JSONObject jdlg = dialogs.getJSONObject(i).getJSONObject("conversation");
                Dialog dialog = new Dialog();

                // Get peer info
                JSONObject peer = jdlg.getJSONObject("peer");

                if(peer.getString("type").equals("group"))
                    continue;

                dialog.name = "Unknown";
                dialog.id = peer.getInt("id");

                if(peer.getString("type").equals("chat"))
                {
                    dialog.name = jdlg.getJSONObject("chat_settings").getString("title");
                }
                else
                {
                    dialog.name = users.get(dialog.id).name;
                    dialog.avatar = users.get(dialog.id).avatarUrl;
                }

                dialog.lastMessageDate = dialogs.getJSONObject(i).getJSONObject("last_message").getInt("date");
                dialog.lastMessage = dialogs.getJSONObject(i).getJSONObject("last_message").getString("text");
                dialog.inRead = jdlg.getInt("in_read");

                ret.add(dialog);
            }

            Collections.sort(ret, new Comparator<Dialog>() {

                @Override
                public int compare(Dialog o1, Dialog o2) {
                    return o1.lastMessageDate > o2.lastMessageDate ? -1 : (o1.lastMessageDate < o2.lastMessageDate ? 1 : 0);
                }
            });

            return ret;
        }
        catch (JSONException e)
        {
            e.printStackTrace();

            return new ArrayList<>();
        }
    }

    public static ArrayList<Audio> fetchAudio(JSONObject obj)
    {
        try {
            ArrayList<Audio> ret = new ArrayList<>();
            JSONArray items = obj.getJSONObject("response").getJSONArray("items");

            for (int i = 0; i < items.length(); i++) {
                JSONObject jaudio = items.getJSONObject(i);
                Audio audio = new Audio();

                audio.artist = jaudio.getString("artist");
                audio.title = jaudio.getString("title");
                audio.contentId = jaudio.getInt("owner_id") + "_" + jaudio.getInt("id");

                ret.add(audio);
            }

            return ret;
        }
        catch(JSONException e)
        {
            e.printStackTrace();

            return new ArrayList<>();
        }
    }

    public static String fetchAudioURL(JSONObject obj)
    {
        try
        {
            return obj.getJSONArray("response").getJSONObject(0).getString("url");
        }
        catch(JSONException e)
        {
            e.printStackTrace();

            return null;
        }
    }
}
