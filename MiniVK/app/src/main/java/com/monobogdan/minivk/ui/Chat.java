package com.monobogdan.minivk.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.monobogdan.minivk.BitmapCache;
import com.monobogdan.minivk.PersistStorage;
import com.monobogdan.minivk.R;
import com.monobogdan.minivk.api.VK;
import com.monobogdan.minivk.api.VKDataSet;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Chat extends Activity {

    private int uid;
    private MediaPlayer voiceMessagePlayer;
    private ListView chatContent;

    private ChatAdapter adapter;

    private EditText messageEdit;
    private long lastMessageDate; // Used to indicate whether to update chat, or not.
    private VK vkAPI;

    private class ChatAdapter extends BaseAdapter
    {
        private List<VKDataSet.Message> messages;


        public void setMessages(List<VKDataSet.Message> messages) {
            this.messages = messages;
        }

        @Override
        public int getCount() {
            return messages == null ? 0 : messages.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            if(view == null)
                view = (ViewGroup) getLayoutInflater().inflate(R.layout.frag_message, null, false);

            VKDataSet.Message msg = messages.get(i);
            ((TextView) view.findViewById(R.id.msg_sender)).setText(msg.senderReadable);

            view.setOnClickListener(null);

            ((TextView) view.findViewById(R.id.msg_sender)).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.msg_content)).setGravity(Gravity.LEFT);
            ((TextView) view.findViewById(R.id.msg_content)).setTextColor(Color.WHITE);

            LinearLayout msgLayout = ((LinearLayout) view.findViewById(R.id.msg_regular));
            ((RelativeLayout.LayoutParams)msgLayout.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);

            ((TextView) view.findViewById(R.id.msg_date)).setText(msg.dateFormatted);

            if (String.valueOf(msg.sender).equals(PersistStorage.getAppInstance().uid)) {
                ((TextView) view.findViewById(R.id.msg_sender)).setVisibility(View.GONE);

                ((RelativeLayout.LayoutParams)msgLayout.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
            }

            if (msg.hasVoiceAttachment) {
                ((TextView) view.findViewById(R.id.msg_content)).setText(" < Голосовое сообщение >");

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPlayVoiceMessage(msg.voiceUrl);
                    }
                });
            }
            else {
                String text = msg.text;

                TextView content = ((TextView) view.findViewById(R.id.msg_content));

                if(msg.hasAttachments)
                {
                    String pronounce = msg.attachments.size() == 1 ? "вложение" : "вложений";
                    text += "\n\n < " + msg.attachments.size() + " " + pronounce + " > ";
                }

                content.setText(text.trim());
            }

            return view;
        }
    }

    private void onPlayVoiceMessage(String fileUrl)
    {
        if(voiceMessagePlayer.isPlaying()) {
            voiceMessagePlayer.stop();
        }
        else {
            try {
                voiceMessagePlayer.reset();
                voiceMessagePlayer.setDataSource(fileUrl);
                voiceMessagePlayer.prepareAsync();

                voiceMessagePlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        voiceMessagePlayer.start();
                    }
                });

            }
            catch(Exception e)
            {
                Log.i("TAG", "onClick: Failed to play voice message");

                e.printStackTrace();
            }
        }
    }

    private boolean isChatUpdated(ArrayList<VKDataSet.Message> messages)
    {
        return messages.size() > 0 ? messages.get(0).date > lastMessageDate : false;
    }

    public void onSendMessage(View sender)
    {
        if(messageEdit.getText().length() > 0)
        {
            if(messageEdit.getText().length() >= 4096)
            {
                Toast.makeText(getApplicationContext(), "Длина сообщения не должна превышать 4096 символов", Toast.LENGTH_LONG).show();

                return;
            }

            int rid = new Random().nextInt();
            vkAPI.request("messages.send?peer_id=" + uid + "&random_id=" + rid + "&message=" + URLEncoder.encode(messageEdit.getText().toString()) + "&", new VK.VKResult() {
                @Override
                public void success(JSONObject json) {

                    messageEdit.setText("");
                }

                @Override
                public void failed(String reason) {
                    UIUtils.notifyNetworkError(Chat.this, reason);
                }
            }, this);
        }
    }

    private void updateChat()
    {
        vkAPI.request("messages.getHistory?user_id=" + uid + "&extended=1&count=100&", new VK.VKResult() {
            @Override
            public void success(JSONObject json) {
                ArrayList<VKDataSet.Message> messages = VKDataSet.fetchMessages(json);

                if (isChatUpdated(messages)) {
                    lastMessageDate = messages.get(0).date;
                    adapter.setMessages(messages);
                    adapter.notifyDataSetChanged();
                }

                findViewById(R.id.header_loading).setVisibility(View.GONE);
            }

            @Override
            public void failed(String reason) {
                UIUtils.notifyNetworkError(Chat.this, reason);
            }
        }, this);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if((keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) && messageEdit.hasFocus())
            onSendMessage(null);

        return super.onKeyUp(keyCode, event);
    }

    private void setupChatUI()
    {
        chatContent = (ListView) findViewById(R.id.chat_content);
        adapter = new ChatAdapter();
        chatContent.setAdapter(adapter);
        messageEdit = (EditText) findViewById(R.id.chat_edit);

        ((TextView) findViewById(R.id.chat_who)).setText(getIntent().getStringExtra("name"));

        String avatar = getIntent().getStringExtra("avatar");

        if(avatar != null) {
            BitmapCache.getBitmapFromCache(getApplicationContext(), Uri.parse(avatar), new BitmapCache.CacheCallback() {
                @Override
                public void success(Bitmap bitmap) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ImageView) findViewById(R.id.chat_avatar)).setImageBitmap(bitmap);
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uid = getIntent().getIntExtra("uid", 0);
        vkAPI = new VK(getApplicationContext());

        setContentView(R.layout.activity_chat);
        setupChatUI();

        voiceMessagePlayer = new MediaPlayer();

        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(!vkAPI.isJobInProgress()) {
                    findViewById(R.id.header_loading).setVisibility(View.VISIBLE);
                    updateChat();
                }

                handler.postDelayed(this, 3500);
            }
        });

    }
}
