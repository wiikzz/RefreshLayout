package com.wiikzz.library.refresh;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

/**
 * Created by wiikii on 16/9/13.
 * Copyright (C) 2014 wiikii. All rights reserved.
 */
public class RefreshLayout extends ViewGroup {

    private static final int MAX_DRAG_DEFAULT_DISTANCE = 600;

    private View mContentView; // 包含的唯一子View(Header与Footer除外)
    private RefreshHandler mHeaderHandler;
    private RefreshHandler mFooterHandler;

    // header相关配置
    private int sHeaderMaxDragDistance;
    private int sHeaderMinDragDistance;
    private int sHeaderSpringDistance;
    // footer相关配置
    private int sFooterMaxDragDistance;
    private int sFooterMinDragDistance;
    private int sFooterSpringDistance;

    private int mCallListenerType = 0;// 0:无刷新操作; 1:下拉刷新操作; 2:上拉加载更多操作
    private RefreshListener mRefreshListener;

    private OverScroller mScroller;

    private RefreshDrawType mDrawType = RefreshDrawType.FOLLOW;

    public enum RefreshDrawType {
        OVERLAP, // 重叠
        FOLLOW   // 跟随
    }

    public RefreshLayout(Context context) {
        super(context);
        initRefreshLayout(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initRefreshLayout(context, attrs);
    }

    private void initRefreshLayout(Context context, AttributeSet attrs) {
        mScroller = new OverScroller(context);
    }

    @Override
    protected void onFinishInflate() {
        int childCount = getChildCount();
        if(childCount > 1) {
            throw new RuntimeException("RefreshLayout can host only one child");
        }

        // 重置RefreshLayout上下Padding
        ViewCompat.setPaddingRelative(this,
                getPaddingLeft(), 0, getPaddingBottom(), 0);

        mContentView = getChildAt(0);
        if(mContentView == null) {
            return;
        }

        // 重置ContentView上下Padding
        ViewCompat.setPaddingRelative(mContentView,
                mContentView.getPaddingLeft(), 0, mContentView.getPaddingBottom(), 0);

        super.onFinishInflate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // measure children
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        if (mHeaderHandler != null && mHeaderView != null){
            int maxDistance = mHeaderHandler.getDragMaxDistance();
            sHeaderMaxDragDistance = maxDistance > 0 ? maxDistance : MAX_DRAG_DEFAULT_DISTANCE;
            int minDistance = mHeaderHandler.getDragMinDistance();
            sHeaderMinDragDistance = minDistance > 0 ? minDistance : mHeaderView.getMeasuredHeight();
            int springDistance = mHeaderHandler.getSpringDistance();
            sHeaderSpringDistance = springDistance > 0 ? springDistance : sHeaderMinDragDistance;
        }

        if (mFooterHandler != null && mFooterView != null) {
            int maxDistance = mFooterHandler.getDragMaxDistance();
            sFooterMaxDragDistance = maxDistance > 0 ? maxDistance : MAX_DRAG_DEFAULT_DISTANCE;
            int minDistance = mFooterHandler.getDragMinDistance();
            sFooterMinDragDistance = minDistance > 0 ? minDistance : mFooterView.getMeasuredHeight();
            int springDistance = mFooterHandler.getSpringDistance();
            sFooterSpringDistance = springDistance > 0 ? springDistance : sFooterMinDragDistance;
        }

        // measure self
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(mContentView == null) {
            return;
        }

        // reset some attrs
        int width = r - l;
        int height = b - t;
        l = 0;
        t = 0;
        r = width;
        b = height;

        if(mHeaderHandler != null && mHeaderView != null) {
            switch (mDrawType) {
                case OVERLAP:
                    mHeaderView.layout(l, t, r, t + mHeaderView.getMeasuredHeight());
                    break;
                case FOLLOW:
                default:
                    mHeaderView.layout(l, t - mHeaderView.getMeasuredHeight(), r, t);
                    break;
            }
        }

        if(mFooterHandler != null && mFooterView != null) {
            switch (mDrawType) {
                case OVERLAP:
                    mFooterView.layout(l, b - mFooterView.getMeasuredHeight(), r, b);
                    break;
                case FOLLOW:
                default:
                    mFooterView.layout(l, b, r, b + mFooterView.getMeasuredHeight());
                    break;
            }
        }


        mContentView.layout(l, t, l + mContentView.getMeasuredWidth(), t + mContentView.getMeasuredHeight());
    }


    private int mActivePointerId = MotionEvent.INVALID_POINTER_ID;
    private float mWholeMoveXDistance; // 一次滑动事件移动的距离(X轴)
    private float mWholeMoveYDistance; // 一次滑动事件移动的距离(Y轴)
    private float mEveryMoveXDistance; // 滑动时每次移动的距离(一次事件内,X轴)
    private float mEveryMoveYDistance; // 滑动时每次移动的距离(一次事件内,Y轴)
    private float mLastMoveXPosition;
    private float mLastMoveYPosition;
    private boolean mShouldControlMotionEvent;
    private boolean mHasCalledReadyEvent;
    private boolean mHasReachUpCPEvent; // 上拉达到临界点
    private boolean mHasReachDownCPEvent;// 下拉达到临界点
    private boolean mDealWithMoveAction;

    private int getRestPointerIndex(MotionEvent ev, int exceptPointerIndex) {
        for(int index = 0; index< MotionEventCompat.getPointerCount(ev); index++) {
            if(index != exceptPointerIndex) {
                return index;
            }
        }

        return 0;
    }

    private void dealWithMultiMotionEvent(MotionEvent ev) {
        int eventAction = MotionEventCompat.getActionMasked(ev);
        switch (eventAction) {
            case MotionEvent.ACTION_DOWN: { // 第一个手指按下
                int pointerIndex = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                mWholeMoveXDistance = 0f;
                mWholeMoveYDistance = 0f;
                mEveryMoveXDistance = 0f;
                mEveryMoveYDistance = 0f;
                mLastMoveXPosition = MotionEventCompat.getX(ev, pointerIndex);
                mLastMoveYPosition = MotionEventCompat.getY(ev, pointerIndex);
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: { // 另一个手指按下
                // 切换到新的pointerId
                int pointerIndex = MotionEventCompat.getActionIndex(ev);
                int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                if(pointerId != mActivePointerId) {
                    mLastMoveXPosition = MotionEventCompat.getX(ev, pointerIndex);
                    mLastMoveYPosition = MotionEventCompat.getY(ev, pointerIndex);
                    mActivePointerId = pointerId;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: { // 有手指移动
                // 更新滑动的相关信息
                int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                float currentMoveXPosition = MotionEventCompat.getX(ev, pointerIndex);
                float currentMoveYPosition = MotionEventCompat.getY(ev, pointerIndex);
                mEveryMoveXDistance = currentMoveXPosition - mLastMoveXPosition;
                mEveryMoveYDistance = currentMoveYPosition - mLastMoveYPosition;
                mWholeMoveXDistance += mEveryMoveXDistance;
                mWholeMoveYDistance += mEveryMoveYDistance;
                mLastMoveXPosition = currentMoveXPosition;
                mLastMoveYPosition = currentMoveYPosition;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: { // 抬起一个手指, 还有手指按在屏幕上
                // 切换到第一个手指
                int upPointerIndex = MotionEventCompat.getActionIndex(ev);
                int upPointerId = MotionEventCompat.getPointerId(ev, upPointerIndex);
                if(upPointerId == mActivePointerId) {
                    // 需要更新
                    int newPointerIndex = getRestPointerIndex(ev, upPointerIndex);
                    mLastMoveXPosition = MotionEventCompat.getX(ev, newPointerIndex);
                    mLastMoveYPosition = MotionEventCompat.getY(ev, newPointerIndex);
                    mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                break;
            }
            case MotionEvent.ACTION_UP: // 所有手指抬起
            case MotionEvent.ACTION_CANCEL: { // 本次事件取消
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 处理多点触控及更新相关移动数据信息
        dealWithMultiMotionEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mShouldControlMotionEvent = false;
                mHasCalledReadyEvent = false;
                mHasReachUpCPEvent = false;
                mHasReachDownCPEvent = false;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                mDealWithMoveAction = true;
                mShouldControlMotionEvent = shouldControlMoveEvent();
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                mDealWithMoveAction = false;
                break;
            }
        }

        // 不破坏原有的消息分发机制
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mShouldControlMotionEvent;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                if(mShouldControlMotionEvent) {
                    executeMoveAction();
                    notifyDragEvent();
                    notifyReadyEvent();
                    notifyDragCPEvent();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                executeResetAction();
                break;
        }

        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        }
    }

    // 判断是否需要控制滑动事件
    private boolean shouldControlMoveEvent() {
        boolean moveHorizontally = Math.abs(mEveryMoveXDistance) > Math.abs(mEveryMoveYDistance);
        if(mContentView == null || moveHorizontally) {
            return false;
        }

        boolean canScrollDown = canChildScrollDown();
        boolean canScrollUp = canChildScrollUp();
        switch (mDrawType) {
            case OVERLAP:
                if(mHeaderHandler != null) {
                    if(canScrollDown && (mEveryMoveYDistance > 0 || (mContentView.getTop() > 20))) {
                        return true;
                    }
                }
                if(mFooterHandler != null) {
                    if(canScrollUp && (mEveryMoveYDistance < 0 || (mContentView.getBottom() < -20))) {
                        return true;
                    }
                }
                break;
            case FOLLOW:
                if(mHeaderHandler != null) {
                    if(canScrollDown && (mEveryMoveYDistance > 0 || getScrollY() < -20)) {
                        return true;
                    }
                }
                if(mFooterHandler != null) {
                    if(canScrollUp && (mEveryMoveYDistance < 0 || getScrollY() > 20)) {
                        return true;
                    }
                }
                break;
        }

        return false;
    }

    private void executeMoveAction() {
        switch (mDrawType) {
            case OVERLAP: {
                float mMoveDistance;
                if (mEveryMoveYDistance > 0) {
                    mMoveDistance = ((float) (sHeaderMaxDragDistance - mContentView.getTop()) / sHeaderMaxDragDistance) * mEveryMoveYDistance / 2;
                } else {
                    mMoveDistance = ((float) (sFooterMaxDragDistance - getHeight() + mContentView.getBottom()) / sFooterMaxDragDistance) * mEveryMoveYDistance / 2;
                }
                int top = (int) (mContentView.getTop() + mMoveDistance);
                mContentView.layout(mContentView.getLeft(), top, mContentView.getRight(), top + mContentView.getMeasuredHeight());
                break;
            }
            case FOLLOW: {
                float mMoveDistance;
                if(mEveryMoveYDistance > 0) {
                    mMoveDistance = ((float) (sHeaderMaxDragDistance + getScrollY()) / sHeaderMaxDragDistance) * mEveryMoveYDistance / 2;
                } else {
                    mMoveDistance = ((float) (sFooterMaxDragDistance - getScrollY()) / sFooterMaxDragDistance) * mEveryMoveYDistance / 2;
                }
                scrollBy(0, (int)(-mMoveDistance));
                break;
            }
        }
    }

    private void notifyDragEvent() {
        switch (mDrawType) {
            case OVERLAP: {
                int contentViewTop = mContentView.getTop();
                if (contentViewTop > 0 && mHeaderHandler != null) {
                    mHeaderHandler.onDragEvent(contentViewTop);
                }
                if (contentViewTop < 0 && mFooterHandler != null) {
                    mFooterHandler.onDragEvent(contentViewTop);
                }
                break;
            }
            case FOLLOW: {
                int contentViewDrag = -getScrollY();
                if (contentViewDrag > 0 && mHeaderHandler != null) {
                    mHeaderHandler.onDragEvent(contentViewDrag);
                }
                if (contentViewDrag < 0 && mFooterHandler != null) {
                    mFooterHandler.onDragEvent(contentViewDrag);
                }
                break;
            }
        }
    }

    private void notifyReadyEvent() {
        if(!mHasCalledReadyEvent){
            if (isHeaderDragEvent()) {
                if(mHeaderHandler != null) {
                    mHeaderHandler.onDragReady();
                }
                mHasCalledReadyEvent = true;
            } else if(isFooterDragEvent()) {
                if(mFooterHandler != null) {
                    mFooterHandler.onDragReady();
                }
                mHasCalledReadyEvent = true;
            }
        }
    }

    private void notifyDragCPEvent() {
        if(mEveryMoveYDistance == 0) {
            return;
        }

        boolean moveUp = mEveryMoveYDistance < 0;
        if(isHeaderDragEvent()) {
            if(!moveUp){
                if((isHeaderOverDrag()) && !mHasReachDownCPEvent){
                    mHasReachDownCPEvent = true;
                    mHasReachUpCPEvent = false;
                    if(mHeaderHandler!=null) {
                        mHeaderHandler.onDragCriticalPoint(true);
                    }
                }
            } else {
                if(!isHeaderOverDrag() && !mHasReachUpCPEvent){
                    mHasReachUpCPEvent = true;
                    mHasReachDownCPEvent = false;
                    if(mHeaderHandler != null) {
                        mHeaderHandler.onDragCriticalPoint(false);
                    }
                }
            }
        } else {
            if(moveUp){
                if(isFooterOverDrag() && !mHasReachUpCPEvent){
                    mHasReachUpCPEvent = true;
                    mHasReachDownCPEvent = false;
                    if(mFooterHandler != null) {
                        mFooterHandler.onDragCriticalPoint(false);
                    }
                }
            } else {
                if(!isFooterOverDrag() && !mHasReachDownCPEvent){
                    mHasReachDownCPEvent = true;
                    mHasReachUpCPEvent = false;
                    if(mFooterHandler != null) {
                        mFooterHandler.onDragCriticalPoint(true);
                    }
                }
            }
        }
    }

    private boolean notifyDragStartAnimalEvent() {
        if(isHeaderOverDrag()) {
            mCallListenerType = 1;
            if(mHeaderHandler != null) {
                mHeaderHandler.onDragStartAnim();
            }
            return true;
        } else if(isFooterOverDrag()) {
            mCallListenerType = 2;
            if(mFooterHandler != null) {
                mFooterHandler.onDragStartAnim();
            }
            return true;
        } else {
            return false;
        }
    }

    private void notifyRefreshOrLoadEvent() {
        if(mRefreshListener != null) {
            if (mCallListenerType == 1) {
                mRefreshListener.onRefreshEvent();
            } else if(mCallListenerType == 2) {
                mRefreshListener.onLoadMoreEvent();
            }
        }
    }

    private void notifyFinishEvent() {
        if(mCallListenerType != 0) {
            if(mCallListenerType == 1 && mHeaderHandler != null) {
                mHeaderHandler.onDragFinishAnim();
            }

            if(mCallListenerType == 2 && mFooterHandler != null) {
                mFooterHandler.onDragFinishAnim();
            }

            mCallListenerType = 0;
        }
    }

    // 回到初初位置
    private void resetInitialPosition() {
        int animationDuration = 100;
        if(mDrawType == RefreshDrawType.OVERLAP) {
            if(mContentView.getTop() == 0) {
                return;
            }

            if (mContentView.getMeasuredHeight() > 0) {
                animationDuration = Math.abs(400 * mContentView.getTop() / mContentView.getMeasuredHeight());
            }
            if(animationDuration < 100) {
                animationDuration = 100;
            }

            Animation animation = new TranslateAnimation(0, 0, mContentView.getTop(), 0);
            animation.setDuration(animationDuration);
            animation.setFillAfter(true);
            mContentView.startAnimation(animation);
            mContentView.layout(
                    mContentView.getLeft(),
                    0,
                    mContentView.getLeft() + mContentView.getMeasuredWidth(),
                    mContentView.getMeasuredHeight()
            );
        } else if(mDrawType == RefreshDrawType.FOLLOW) {
            if (mContentView.getMeasuredHeight()>0) {
                animationDuration = Math.abs(400 * getScrollY() / mContentView.getMeasuredHeight());
            }
            if(animationDuration < 100) {
                animationDuration = 100;
            }

            mScroller.startScroll(0, getScrollY(), 0, -getScrollY(), animationDuration);
            invalidate();
        }

        postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyFinishEvent();
            }
        }, animationDuration);
    }

    // 回到下拉刷新位置
    private void resetRefreshPosition() {
        int animationTime = 200;
        if(mDrawType == RefreshDrawType.OVERLAP) {
            int fromYDelta;
            int toYDelta;
            int l, t, r, b;
            if(isHeaderDragEvent()) {
                fromYDelta = mContentView.getTop() - sHeaderSpringDistance;
                toYDelta = 0;
                l = mContentView.getLeft();
                t = sHeaderSpringDistance;
                r = l + mContentView.getMeasuredWidth();
                b = t + mContentView.getMeasuredHeight();
            } else {
                fromYDelta = mContentView.getTop() + sFooterSpringDistance;
                toYDelta = 0;
                l = mContentView.getLeft();
                t = -sFooterSpringDistance;
                r = l + mContentView.getMeasuredWidth();
                b = t + mContentView.getMeasuredHeight();
            }

            Animation animation = new TranslateAnimation(0, 0,  fromYDelta, toYDelta);
            animation.setDuration(animationTime);
            animation.setFillAfter(true);
            mContentView.startAnimation(animation);
            mContentView.layout(l, t, r, b);
        } else if(mDrawType == RefreshDrawType.FOLLOW) {
            if(isHeaderDragEvent()) {
                mScroller.startScroll(0, getScrollY(), 0, -getScrollY() - sHeaderSpringDistance, animationTime);
                invalidate();
            } else {
                mScroller.startScroll(0, getScrollY(), 0, -getScrollY() + sFooterSpringDistance, animationTime);
                invalidate();
            }
        }

        postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyRefreshOrLoadEvent();
            }
        }, animationTime);
    }

    // 判断是否滑到顶部
    private boolean canChildScrollDown() {
        return !ViewCompat.canScrollVertically(mContentView, -1);
    }

    // 判断是否滑到底部
    private boolean canChildScrollUp() {
        return !ViewCompat.canScrollVertically(mContentView, 1);
    }

    private boolean isHeaderDragEvent() {
        switch (mDrawType) {
            case OVERLAP:
                return mContentView.getTop() > 0;
            case FOLLOW:
                return getScrollY() < 0;
            default:
                return false;
        }
    }

    private boolean isFooterDragEvent() {
        switch (mDrawType) {
            case OVERLAP:
                return mContentView.getTop() < 0;
            case FOLLOW:
                return getScrollY() > 0;
            default:
                return false;
        }
    }

    //判断顶部拉动是否超过临界值
    private boolean isHeaderOverDrag(){
        switch (mDrawType) {
            case OVERLAP:
                return mContentView.getTop() > sHeaderMinDragDistance;
            case FOLLOW:
                return (-getScrollY()) > sHeaderMinDragDistance;
            default:
                return false;
        }
    }

    //判断底部拉动是否超过临界值
    private boolean isFooterOverDrag(){
        switch (mDrawType) {
            case OVERLAP:
                return (getHeight() - mContentView.getBottom()) > sFooterMinDragDistance;
            case FOLLOW:
                return getScrollY() > sFooterMinDragDistance;
            default:
                return false;
        }
    }

    // 重置控件到初始位置,或者刷新位置
    private void executeResetAction() {
        if(mRefreshListener != null && notifyDragStartAnimalEvent()) {
            resetRefreshPosition();
        } else {
            resetInitialPosition();
        }
    }


    /** interface **/
    // 主动调用下拉上拉刷新
    public void startRefreshAction(){
        postDelayed(new Runnable() {
            @Override
            public void run() {
                int animationDuration = 500;
                mCallListenerType = 1;
                if(mDrawType == RefreshDrawType.OVERLAP) {
                    Animation animation = new TranslateAnimation(0, 0,  mContentView.getTop() - sHeaderSpringDistance, 0);
                    animation.setDuration(animationDuration);
                    animation.setFillAfter(true);
                    mContentView.startAnimation(animation);
                    int left = mContentView.getLeft();
                    int top = sHeaderSpringDistance;
                    mContentView.layout(left, top, left + mContentView.getMeasuredWidth(), top + mContentView.getMeasuredHeight());
                } else if(mDrawType == RefreshDrawType.FOLLOW) {
                    mScroller.startScroll(0, getScrollY(), 0, -getScrollY() - sHeaderSpringDistance, animationDuration);
                    invalidate();
                }

                if(mHeaderHandler != null) {
                    mHeaderHandler.onDragStartAnim();
                }

                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        notifyRefreshOrLoadEvent();
                    }
                }, animationDuration + 200L);
            }
        }, 200L);
    }

    // 完成下拉刷新或者上拉加载动作
    public void onRefreshComplete() {
        if(!mDealWithMoveAction && (isHeaderDragEvent() || isFooterDragEvent())) {
            post(new Runnable() {
                @Override
                public void run() {
                    resetInitialPosition();
                }
            });
        }
    }

    private View mHeaderView;
    private View mFooterView;
    public void setHeader(@NonNull RefreshHandler headerHandler) {
        if(mHeaderHandler == null || mHeaderView == null) {
            headerHandler.initHandlerView(this);
            mHeaderView = getChildAt(getChildCount() - 1);
            mHeaderView.setVisibility(VISIBLE);
            mHeaderHandler = headerHandler;
            mContentView.bringToFront();
            requestLayout();
        }
    }

    public void setFooter(@NonNull RefreshHandler footerHandler) {
        if(mFooterHandler == null || mFooterView == null) {
            footerHandler.initHandlerView(this);
            mFooterView = getChildAt(getChildCount() - 1);
            mFooterView.setVisibility(VISIBLE);
            mFooterHandler = footerHandler;
            mContentView.bringToFront();
            requestLayout();
        }
    }

    public RefreshHandler getHeader() {
        return mHeaderHandler;
    }

    public RefreshHandler getFooter() {
        return mFooterHandler;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    public View getFooterView() {
        return mFooterView;
    }

    public void setDrawType(RefreshDrawType type) {
        mDrawType = type;
    }

    public void setRefreshListener(RefreshListener listener) {
        this.mRefreshListener = listener;
    }

    public interface RefreshListener {
        void onRefreshEvent();
        void onLoadMoreEvent();
    }

    // 下拉刷新与上接加载的header与footer
    public interface RefreshHandler {

        /**
         * 获取操作的header或者footer视图
         * @return 实际操作的View
         */
        View initHandlerView(ViewGroup parentView);

        /**
         * 获取下拉或上拉的最小距离
         * @return 小于等于0表示使用View的实际高度
         */
        int getDragMinDistance();

        /**
         * 获取下拉或上拉的最大距离
         * @return 小于等于0表示使用默认值(600)
         */
        int getDragMaxDistance();

        /**
         * 松开回弹后,显示的高度
         * @return 一般不能小于等于0 否则显示不了头部
         */
        int getSpringDistance();

        /**
         * 准备开始下拉或者上拉
         */
        void onDragReady();

        /**
         * 正在下拉或者上拉
         * @param distance 大于0表示下拉,小于0表示上拉
         */
        void onDragEvent(int distance);

        /**
         * 下拉或上拉达到临界点
         * @param pullDown true表示下拉,false表示上拉
         */
        void onDragCriticalPoint(boolean pullDown);

        /**
         * 上拉下拉超过临界点后松开,所需要展示的动画开始
         */
        void onDragStartAnim();

        /**
         * header或者footer已经全部弹回时的回调
         */
        void onDragFinishAnim();
    }
}
