package com.monobogdan.minivk.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.monobogdan.minivk.R;

public class VKActivity extends Activity {
    private MainMenu mainMenu;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(mainMenu.onTouchEvent(ev))
            return true;

        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.overlay_menu);
        mainMenu = new MainMenu(this);

    }
}
