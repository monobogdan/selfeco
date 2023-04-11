package com.monobogdan.minivk.ui;

import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.monobogdan.minivk.BitmapCache;
import com.monobogdan.minivk.PersistStorage;
import com.monobogdan.minivk.R;
import com.monobogdan.minivk.api.VK;
import com.monobogdan.minivk.api.VKDataSet;

import org.json.JSONObject;

import java.util.ArrayList;

public class DialogsActivity extends VKActivity {

    private VK vkAPI;

    private ArrayList<VKDataSet.Dialog> dialogs;
    private DialogAdapter adapter;
    private boolean isUpdatePaused;

    private ServiceConnection pollRPC;
    private ListView contentPrimary;
    private ProgressBar ilView;
    private int lastMessageDate;

    private final int checkInterval = 3500;

    private class DialogAdapter extends BaseAdapter
    {

        @Override
        public int getCount() {
            return dialogs != null ? dialogs.size() : 0;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        private void attachAvatar(View view, VKDataSet.Dialog dlg)
        {
            BitmapCache.getBitmapFromCache(getApplicationContext(), Uri.parse(dlg.avatar), new BitmapCache.CacheCallback() {
                @Override
                public void success(Bitmap bitmap) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ImageView)view.findViewById(R.id.msg_avatar)).setImageBitmap(bitmap);
                        }
                    });
                }
            });
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            VKDataSet.Dialog dialog = dialogs.get(i);

            if(view == null)
                view = getLayoutInflater().inflate(R.layout.frag_dialog, null, false);

            String lastMsg = dialog.lastMessage;

            if(lastMsg.length() > 20)
                lastMsg = lastMsg.substring(0, 20) + "...";

            ((TextView)view.findViewById(R.id.dialog_name)).setText(dialog.name);
            ((TextView)view.findViewById(R.id.dialog_lastmsg)).setText(lastMsg.length() > 0 ? lastMsg : "<Вложение>");

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(), ChatActivity.class);
                    intent.putExtra("uid", dialog.id);
                    intent.putExtra("name", dialog.name);
                    intent.putExtra("avatar", dialog.avatar);

                    startActivity(intent);
                }
            });

            if(dialog.avatar != null) {
                attachAvatar(view, dialog);
            }
            else
            {
                ((ImageView)view.findViewById(R.id.msg_avatar)).setImageBitmap(null);
            }

            return view;
        }
    }

    private void updateDialogList()
    {
        vkAPI.request("messages.getConversations?extended=1&count=200&", new VK.VKResult() {
            @Override
            public void success(JSONObject json) {
                dialogs = VKDataSet.fetchDialogs(json);

                if(dialogs.size() == 0) {
                    Toast.makeText(DialogsActivity.this, "Не удалось получить список диалогов с сервера", Toast.LENGTH_SHORT).show();
                    return; // Probably server error
                }

                if(dialogs.size() > 0 && dialogs.get(0).lastMessageDate == lastMessageDate)
                    return;

                adapter.notifyDataSetChanged();
                lastMessageDate = dialogs.get(0).lastMessageDate;
            }

            @Override
            public void failed(String reason) {
                UIUtils.notifyNetworkError(DialogsActivity.this, reason);
            }
        }, this);
    }

    @Override
    public void onBackPressed() {
        //finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        ViewGroup content = (ViewGroup) findViewById(R.id.content);
        contentPrimary = new ListView(getApplicationContext());
        contentPrimary.setFocusable(true);
        contentPrimary.requestFocus();
        content.addView(contentPrimary);

        adapter = new DialogAdapter();
        contentPrimary.setFocusable(true);
        contentPrimary.requestFocus();

        contentPrimary.setDivider(null);
        contentPrimary.setAdapter(adapter);
        contentPrimary.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int state) {
                isUpdatePaused = state == SCROLL_STATE_IDLE;
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {

            }
        });

        vkAPI = new VK(getApplicationContext());

        Handler intervalChecker = new Handler();
        intervalChecker.post(new Runnable() {
            @Override
            public void run() {
                if(!vkAPI.isJobInProgress() && !isUpdatePaused)
                    updateDialogList();

                intervalChecker.postDelayed(this, checkInterval);
            }
        });
    }
}
