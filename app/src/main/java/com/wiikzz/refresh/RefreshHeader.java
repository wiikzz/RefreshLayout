package com.wiikzz.refresh;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wiikzz.library.refresh.RefreshLayout;

/**
 * Created by wiikii on 16/9/12.
 * Copyright (C) 2014 wiikii. All rights reserved.
 */
public class RefreshHeader implements RefreshLayout.RefreshHandler {
    private Context context;
    private View mView;

    private final int ROTATE_ANIM_DURATION = 180;
    private RotateAnimation mRotateUpAnim;
    private RotateAnimation mRotateDownAnim;

    private TextView headerTitle;
    private ImageView headerArrow;
    private ProgressBar headerProgressbar;
    private View frame;

    public RefreshHeader(Context context){
        this.context = context;
        mRotateUpAnim = new RotateAnimation(0.0f, -180.0f,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        mRotateUpAnim.setDuration(ROTATE_ANIM_DURATION);
        mRotateUpAnim.setFillAfter(true);
        mRotateDownAnim = new RotateAnimation(-180.0f, 0.0f,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        mRotateDownAnim.setDuration(ROTATE_ANIM_DURATION);
        mRotateDownAnim.setFillAfter(true);
    }

    @Override
    public View initHandlerView(ViewGroup parentView) {
        if(mView == null) {
            mView = LayoutInflater.from(context).inflate(R.layout.rl_pull_refresh_header, parentView, true);
            headerTitle = (TextView) mView.findViewById(R.id.rl_pr_header_text);
            headerArrow = (ImageView) mView.findViewById(R.id.rl_pr_header_arrow);
            headerProgressbar = (ProgressBar) mView.findViewById(R.id.rl_pr_header_progressbar);
            frame = mView.findViewById(R.id.rl_pr_header_frame);
            headerProgressbar.setIndeterminateDrawable(ContextCompat.getDrawable(context, R.drawable.rl_progress_loading_anim));
            headerArrow.setImageResource(R.drawable.rl_pull_refresh_arrow);
        }
        return mView;
    }

    @Override
    public int getDragMinDistance() {
        return frame.getMeasuredHeight();
    }

    @Override
    public int getDragMaxDistance() {
        return (int) (frame.getMeasuredHeight() * 2.5);
    }

    @Override
    public int getSpringDistance() {
        return frame.getMeasuredHeight();
    }

    @Override
    public void onDragReady() {

    }

    @Override
    public void onDragEvent(int distance) {

    }

    @Override
    public void onDragCriticalPoint(boolean pullDown) {
        if (pullDown){
            headerTitle.setText("松开刷新");
            if (headerArrow.getVisibility()==View.VISIBLE)
                headerArrow.startAnimation(mRotateUpAnim);
        } else {
            headerTitle.setText("下拉刷新");
            if (headerArrow.getVisibility()==View.VISIBLE)
                headerArrow.startAnimation(mRotateDownAnim);
        }
    }

    @Override
    public void onDragStartAnim() {
        headerTitle.setText("正在刷新");
        headerArrow.setVisibility(View.INVISIBLE);
        headerArrow.clearAnimation();
        headerProgressbar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDragFinishAnim() {
        headerTitle.setText("下拉刷新");
        headerArrow.setVisibility(View.VISIBLE);
        headerProgressbar.setVisibility(View.INVISIBLE);
    }
}
