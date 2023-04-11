package com.monobogdan.minivk.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.monobogdan.minivk.PersistStorage;
import com.monobogdan.minivk.R;
import com.monobogdan.minivk.api.VK;
import com.monobogdan.minivk.api.VKDataSet;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MusicActivity extends VKActivity {

    private class AudioAdapter extends BaseAdapter
    {

        @Override
        public int getCount() {
            return audio != null ? audio.size() : 0;
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
                view = getLayoutInflater().inflate(R.layout.frag_audio, null, false);

            VKDataSet.Audio associated = audio.get(i);

            ((TextView) view.findViewById(R.id.audio_artist)).setText(audio.get(i).artist);
            ((TextView) view.findViewById(R.id.audio_name)).setText(audio.get(i).title);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onPlayTrack(audio.get(i));
                }
            });
            view.setLongClickable(true);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    onDisplayTrackOptions(audio.get(i));

                    return true;
                }
            });

            return view;
        }
    }

    private VK vkAPI;
    private boolean isInSearch;
    private ArrayList<VKDataSet.Audio> audio;
    private boolean inSearch;

    private MediaPlayer mediaPlayer;
    private VKDataSet.Audio currentAudio;

    private Notification notification;
    private NotificationManager notifyManager;

    private AudioAdapter adapter;
    private EditText searchView;
    private ListView audioView;

    private void setupMediaPlayer()
    {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                UIUtils.notifyNetworkError(MusicActivity.this, "Ошибка при воспроизведении трека");
                return false;
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                int idx = audio.indexOf(currentAudio);
                if(!isInSearch && idx < audio.size() - 1 && idx != -1)
                    onPlayTrack(audio.get(idx + 1));

                mediaPlayer.reset();
            }
        });
    }

    private void setupNotification()
    {
        notifyManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notification = new Notification();
        notification.icon = R.drawable.music;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mediaPlayer.isPlaying())
                {
                    PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                            new Intent(getApplicationContext(), MusicActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

                    notification.setLatestEventInfo(MusicActivity.this, "Сейчас играет",
                            currentAudio.artist + " - " + currentAudio.title,
                            intent);
                    //notifyManager.notify(0, notification);
                    //playButton.setImageResource(android.R.drawable.ic_media_pause);
                }
                else
                {
                    notifyManager.cancel(0);
                    //playButton.setImageResource(android.R.drawable.ic_media_play);
                }

                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void onDisplayTrackOptions(VKDataSet.Audio track)
    {

    }

    private void onPlayTrack(VKDataSet.Audio track)
    {
        if(currentAudio == track)
        {
            if(mediaPlayer.isPlaying())
                mediaPlayer.pause();
            else
                mediaPlayer.start();

            return;
        }

        if(mediaPlayer.isPlaying())
            mediaPlayer.stop();
        mediaPlayer.reset();

        vkAPI.audioRequest("act=getDetails&id=" + track.contentId, new VK.VKResult() {
            @Override
            public void success(JSONObject json) {
                String url = VKDataSet.fetchAudioURL(json);

                try {
                    Log.i("", "success: " + url);
                    mediaPlayer.setDataSource("http://90.156.209.92/audiorelay.php?act=stream&url=" + URLEncoder.encode(url));
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.start();
                        }
                    });

                    currentAudio = track;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(String reason) {
                UIUtils.notifyNetworkError(MusicActivity.this, "Не удалось начать воспроизведение (" + reason + ")");
            }
        }, this);
    }

    private void performSearch(String query)
    {
        vkAPI.audioRequest("act=search&query=" + URLEncoder.encode(query.replace(' ', '_')), new VK.VKResult() {
            @Override
            public void success(JSONObject json) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        audio = VKDataSet.fetchAudio(json);
                        isInSearch = true;

                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void failed(String reason) {
                UIUtils.notifyNetworkError(MusicActivity.this, "Не удалось загрузить музычку :( (" + reason + ")");
            }
        }, this);
    }

    public void onOpenSearch(View view)
    {
        /*View v = getLayoutInflater().inflate(R.layout.dialog_search, null, false);
        AlertDialog alertDlg = new AlertDialog.Builder(MusicActivity.this).
                setTitle("Что ищем?").
                setMessage("Введите название трека:").
                setView(v).
                setPositiveButton("Ищем", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        performSearch(((EditText)v.findViewById(R.id.search_field)).getText().toString());

                        dialogInterface.cancel();
                    }
                }).
                setNegativeButton("Не ищем", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).
                show();*/
    }

    public void onGoBack(View view)
    {
        if(isInSearch)
            updateUserAudio();
    }

    private void updateUserAudio()
    {
        vkAPI.audioRequest("act=get&uid=" + PersistStorage.getAppInstance().uid, new VK.VKResult() {
            @Override
            public void success(JSONObject json) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        audio = VKDataSet.fetchAudio(json);
                        isInSearch = false;

                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void failed(String reason) {
                UIUtils.notifyNetworkError(MusicActivity.this, "Не удалось загрузить музычку :( (" + reason + ")");
            }
        }, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        notifyManager.cancel(0);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, DialogsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        //super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vkAPI = new VK(this);

        getLayoutInflater().inflate(R.layout.activity_audio, (ViewGroup) findViewById(R.id.content));

        audioView = (ListView) findViewById(R.id.audio_list);
        adapter = new AudioAdapter();
        audioView.setAdapter(adapter);

        searchView = (EditText) findViewById(R.id.audio_search);
        searchView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER || keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
                    performSearch(searchView.getText().toString());

                    return true;
                }

                return false;
            }
        });
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.toString().length() == 0)
                    updateUserAudio();
            }
        });

        setupNotification();
        setupMediaPlayer();
        updateUserAudio();
    }
}
