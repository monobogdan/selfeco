package com.monobogdan.minivk.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.monobogdan.minivk.PersistStorage;
import com.monobogdan.minivk.R;
import com.monobogdan.minivk.api.VK;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;

public class Auth extends Activity {
    private final String TAG = "Auth";

    private VK vk;

    private EditText loginView;
    private EditText passView;

    private void redirectIfNeeded()
    {
        if(PersistStorage.getAppInstance().isAuthorized())
        {
            startActivity(new Intent(getApplicationContext(), DialogsActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vk = new VK(getApplicationContext());

        PersistStorage.getAppInstance().load(getApplicationContext());
        setContentView(R.layout.activity_auth);

        loginView = (EditText) findViewById(R.id.auth_login);
        passView = (EditText) findViewById(R.id.auth_password);

        redirectIfNeeded();
    }

    public void onAuthorize(View view) {
        ProgressDialog dialog = new ProgressDialog(Auth.this);
        dialog.setMessage("Загрузка...");
        dialog.setCancelable(false);
        dialog.show();

        vk.beginDirectAuthFlow(loginView.getText().toString(), passView.getText().toString(), new VK.AuthResult() {
            @Override
            public void success() {
                vk.request("users.get?", new VK.VKResult() {
                    @Override
                    public void success(JSONObject json) {
                        try {
                            PersistStorage.getAppInstance().uid =
                                    json.getJSONArray("response").getJSONObject(0).getString("id");

                            Log.i("TAG", "success: uid is " + PersistStorage.getAppInstance().uid);
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }

                        PersistStorage.getAppInstance().save(getApplicationContext());
                        dialog.cancel();

                        redirectIfNeeded();
                    }

                    @Override
                    public void failed(String reason) {

                    }
                }, Auth.this);
            }

            @Override
            public void error(String desc) {
                Toast.makeText(Auth.this, desc, Toast.LENGTH_LONG).show();
                dialog.cancel();
            }
        }, this);
    }
}