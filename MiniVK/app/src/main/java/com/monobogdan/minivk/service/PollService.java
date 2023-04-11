package com.monobogdan.minivk.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.monobogdan.minivk.R;
import com.monobogdan.minivk.api.VK;
import com.monobogdan.minivk.api.VKDataSet;
import com.monobogdan.minivk.ui.ChatActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class PollService extends Service {
    private final int UPDATE_INTERVAL = 15000;

    public static final String CATEGORY = "MINIVK_BG_SERVICE";
    public static final String COMMAND_ALTER_STATE = "MINIVK_ALTER_STATE";

    private class IntentReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(COMMAND_ALTER_STATE))
            {
                isUpdating = !isUpdating;
            }
        }
    }

    private VK vkAPI;
    private boolean isUpdating;
    private int lastMessageDate;

    private IntentReceiver receiver;
    private HandlerThread hThread;

    private Notification messageNotification;
    private NotificationManager notifications;

    private void startUpdateThread()
    {
        isUpdating = true;

        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(isUpdating)
                {
                    vkAPI.request("messages.getConversations?extended=1&count=5&", new VK.VKResult() {
                        @Override
                        public void success(JSONObject json) {
                            ArrayList<VKDataSet.Dialog> dialogs = VKDataSet.fetchDialogs(json);

                            if(dialogs.size() > 0 && dialogs.get(0).lastMessageDate == lastMessageDate)
                                return;

                            VKDataSet.Dialog dlg = dialogs.get(0);

                            Intent intent = new Intent();
                            intent.setClass(getApplicationContext(), ChatActivity.class);
                            intent.putExtra("uid", dlg.id);
                            intent.putExtra("name", dlg.name);
                            intent.putExtra("avatar", dlg.avatar);
                            PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                            Notification notify = new Notification();
                            notify.setLatestEventInfo(PollService.this, dlg.name, dlg.lastMessage, pIntent);
                            notify.icon = R.drawable.chat;
                            notifications.notify(1, notify);

                            Log.i("", "success: Test");

                            lastMessageDate = dialogs.get(0).lastMessageDate;
                        }

                        @Override
                        public void failed(String reason) {
                            // Just skip this frame
                        }
                    }, null);
                }

                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        });
    }

    @Override
    public void onCreate() {
        notifications = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        vkAPI = new VK(getApplicationContext());

        IntentFilter filter = new IntentFilter();
        filter.addCategory(CATEGORY);
        filter.addAction(COMMAND_ALTER_STATE);
        receiver = new IntentReceiver();
        registerReceiver(receiver, filter);

        startUpdateThread();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
