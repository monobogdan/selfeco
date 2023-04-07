package com.monobogdan.minivk.ui;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.monobogdan.minivk.R;

public class MainMenu {

    private class GestureHandler implements GestureDetector.OnGestureListener
    {

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float vx, float v1) {
            if(vx > 50 && !isOpen) {
                onMenuTrigger();

                return true;
            }

            if(vx < 50 && isOpen) {
                onMenuTrigger();

                return true;
            }

            return false;
        }
    }

    private Activity activity;
    private View view;
    private boolean isOpen;

    private GestureDetector gDetector;

    public MainMenu(Activity thiz)
    {
        activity = thiz;
        gDetector = new GestureDetector(new GestureHandler());

        view = thiz.findViewById(R.id.menu);
        setupNavigator();

        thiz.findViewById(R.id.header_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onMenuTrigger();
            }
        });
    }

    private void setupNavigator()
    {
        activity.findViewById(R.id.mm_dialogs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(activity.getClass() == DialogsActivity.class)
                    return;

                Intent intent = new Intent(activity, DialogsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
                onMenuTrigger();
            }
        });

        activity.findViewById(R.id.mm_music).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(activity.getClass() == MusicActivity.class)
                    return;

                Intent intent = new Intent(activity, MusicActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
                onMenuTrigger();
            }
        });
    }

    private void onMenuTrigger()
    {
        Animation anim = view.getAnimation();

        if(anim != null && !view.getAnimation().hasEnded())
            return;

        Animation bringAnim = null;

        view.setVisibility(View.VISIBLE);

        if(!isOpen) {
            bringAnim = new TranslateAnimation(-activity.findViewById(R.id.content).getWidth(), 0, 0, 0);
            bringAnim.setDuration(1000);
            view.startAnimation(bringAnim);
        }
        else
        {
            bringAnim = new TranslateAnimation(0, -view.getWidth(), 0, 0);
            bringAnim.setDuration(1000);
            view.startAnimation(bringAnim);

            bringAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }

        isOpen = !isOpen;
    }

    public boolean onTouchEvent(MotionEvent e)
    {
        //return gDetector.onTouchEvent(e);

        return false;
    }

    public boolean isOpen() {
        return isOpen;
    }
}
