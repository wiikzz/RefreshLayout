package com.wiikzz.refresh;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ProgressBar;

import com.wiikzz.library.refresh.RefreshLayout;


/**
 * Created by wiikii on 16/9/12.
 * Copyright (C) 2014 wiikii. All rights reserved.
 */
public class RefreshFooter implements RefreshLayout.RefreshHandler {
    private Context mContext;
    private int mRotationAnimResourceId;
    private View mView;

    private RotateAnimation mRotateAnimation;
    private ProgressBar mProgressBar;

    public RefreshFooter(Context context){
        this(context, R.drawable.rl_progress_gear_anim);
    }

    public RefreshFooter(Context context, int rotationAnimResourceId){
        this.mContext = context;
        this.mRotationAnimResourceId = rotationAnimResourceId;

        mRotateAnimation = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        mRotateAnimation.setInterpolator(new LinearInterpolator());
        mRotateAnimation.setRepeatCount(Integer.MAX_VALUE);
        mRotateAnimation.setDuration(600);
        mRotateAnimation.setFillAfter(true);
    }

    @Override
    public View initHandlerView(ViewGroup parentView) {
        if(mView == null) {
            mView = LayoutInflater.from(mContext).inflate(R.layout.rl_pull_refresh_footer, parentView, true);
            mProgressBar = (ProgressBar) mView.findViewById(R.id.feedback_rotation_footer_progress);
            mProgressBar.setIndeterminateDrawable(ContextCompat.getDrawable(mContext, mRotationAnimResourceId));
        }
        return mView;
    }

    @Override
    public int getDragMinDistance() {
        return 0;
    }

    @Override
    public int getDragMaxDistance() {
        return 0;
    }

    @Override
    public int getSpringDistance() {
        return 0;
    }

    @Override
    public void onDragReady() {

    }

    @Override
    public void onDragEvent(int distance) {
        float rota = Math.abs(distance) / 2f;
        ViewCompat.setRotation(mProgressBar, rota);
    }

    @Override
    public void onDragCriticalPoint(boolean pullDown) {

    }

    @Override
    public void onDragStartAnim() {
        mProgressBar.startAnimation(mRotateAnimation);
    }

    @Override
    public void onDragFinishAnim() {
        mProgressBar.clearAnimation();
    }
}
