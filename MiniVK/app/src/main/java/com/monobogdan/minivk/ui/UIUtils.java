package com.monobogdan.minivk.ui;

import android.app.Activity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import com.monobogdan.minivk.R;

public class UIUtils {

    public static void notifyNetworkError(Activity activity, String reason)
    {
        Toast.makeText(activity, "Ошибка при загрузке данных (" + reason + ")",
                Toast.LENGTH_LONG).show();
    }
}
