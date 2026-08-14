package com.szn.merger;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.core.widget.NestedScrollView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

public class StableScrollView extends NestedScrollView {

    private View thumb;
    private ViewGroup overlayParent;

    private int smallestThumbHeight = Integer.MAX_VALUE;

    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Runnable resizeRunnable;

    public StableScrollView(Context context) {
        super(context);
        init();
    }

    public StableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVerticalScrollBarEnabled(false);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        post(() -> {
            if (!isAttachedToWindow() || thumb != null) {
                return;
            }
            createThumb();
        });
    }

    private void createThumb() {
        if (!(getParent() instanceof ViewGroup)) {
            return;
        }

        overlayParent = (ViewGroup) getParent();

        thumb = new View(getContext());

        Drawable drawable = getResources().getDrawable(
                R.drawable.slider_log_card,
                getContext().getTheme()
        );

        thumb.setBackground(drawable);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(4), 1);
        params.gravity = Gravity.TOP | Gravity.END;

        overlayParent.addView(thumb, params);
        thumb.bringToFront();

        if (getChildCount() > 0) {
            View content = getChildAt(0);
            layoutListener = () -> updateThumb(true);

            ViewTreeObserver vto = content.getViewTreeObserver();
            if (vto.isAlive()) {
                vto.addOnGlobalLayoutListener(layoutListener);
            }
        }

        updateThumb(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (resizeRunnable != null) {
            handler.removeCallbacks(resizeRunnable);
            resizeRunnable = null;
        }

        if (getChildCount() > 0 && layoutListener != null) {
            View content = getChildAt(0);
            if (content != null) {
                ViewTreeObserver vto = content.getViewTreeObserver();
                if (vto.isAlive()) {
                    vto.removeOnGlobalLayoutListener(layoutListener);
                }
            }
            layoutListener = null;
        }

        // AMAN: Jangan removeView() langsung di dalam onDetachedFromWindow()
        // karena mengganggu loop ViewGroup.dispatchDetachedFromWindow() internal Android.
        if (thumb != null) {
            final View thumbToRemove = thumb;
            thumbToRemove.setVisibility(GONE);

            // Lakukan removal aman setelah siklus detach selesai
            post(() -> {
                if (thumbToRemove.getParent() instanceof ViewGroup) {
                    ((ViewGroup) thumbToRemove.getParent()).removeView(thumbToRemove);
                }
            });
        }

        thumb = null;
        overlayParent = null;

        super.onDetachedFromWindow();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        updateThumb(false);
    }

    private void updateThumb(boolean animateSize) {
        if (thumb == null || overlayParent == null || getChildCount() == 0) {
            return;
        }

        int viewportHeight = getHeight();
        int contentHeight = getChildAt(0).getHeight();

        if (contentHeight <= viewportHeight) {
            thumb.setVisibility(INVISIBLE);
            return;
        }

        thumb.setVisibility(VISIBLE);

        int calculatedHeight = Math.max(
                1,
                (int) ((float) viewportHeight * viewportHeight / contentHeight)
        );

        if (smallestThumbHeight == Integer.MAX_VALUE) {
            smallestThumbHeight = calculatedHeight;
        }

        boolean sizeChanged = calculatedHeight < smallestThumbHeight;

        if (!sizeChanged) {
            updateThumbPosition(contentHeight, viewportHeight, smallestThumbHeight, false);
            return;
        }

        final int targetHeight = calculatedHeight;

        if (resizeRunnable != null) {
            handler.removeCallbacks(resizeRunnable);
        }

        resizeRunnable = () -> {
            if (thumb == null || overlayParent == null || getChildCount() == 0) {
                return;
            }

            smallestThumbHeight = targetHeight;
            int currentContentHeight = getChildAt(0).getHeight();

            updateThumbPosition(currentContentHeight, getHeight(), targetHeight, true);
        };

        handler.postDelayed(resizeRunnable, 150);
    }

    private void updateThumbPosition(
            int contentHeight,
            int viewportHeight,
            int thumbHeight,
            boolean animate
    ) {
        if (thumb == null || overlayParent == null) {
            return;
        }

        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        boolean atBottom = getScrollY() >= maxScroll - 2;

        float fraction = maxScroll > 0 ? getScrollY() / (float) maxScroll : 0f;
        fraction = Math.max(0f, Math.min(1f, fraction));

        int maxThumbTop = Math.max(0, viewportHeight - thumbHeight);

        int[] scrollLocation = new int[2];
        int[] parentLocation = new int[2];

        getLocationOnScreen(scrollLocation);
        overlayParent.getLocationOnScreen(parentLocation);

        ViewGroup.LayoutParams genericParams = thumb.getLayoutParams();
        FrameLayout.LayoutParams params;

        if (genericParams instanceof FrameLayout.LayoutParams) {
            params = (FrameLayout.LayoutParams) genericParams;
        } else {
            params = new FrameLayout.LayoutParams(dp(4), thumbHeight);
        }

        params.width = dp(4);
        params.height = thumbHeight;

        params.rightMargin = overlayParent.getWidth()
                - (scrollLocation[0] - parentLocation[0] + getWidth())
                + dp(8);

        if (atBottom) {
            int scrollBottom = scrollLocation[1] - parentLocation[1] + viewportHeight;
            int parentBottom = overlayParent.getHeight();

            params.gravity = Gravity.BOTTOM | Gravity.END;
            params.bottomMargin = parentBottom - scrollBottom;
            params.topMargin = 0;
        } else {
            int thumbTop = (int) (fraction * maxThumbTop);
            int relativeTop = scrollLocation[1] - parentLocation[1] + thumbTop;

            params.gravity = Gravity.TOP | Gravity.END;
            params.topMargin = relativeTop;
            params.bottomMargin = 0;
        }

        if (animate) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(300);
            TransitionManager.beginDelayedTransition(overlayParent, transition);
        }

        thumb.setLayoutParams(params);
        thumb.bringToFront();
    }

    public void resetScrollbarSize() {
        if (resizeRunnable != null) {
            handler.removeCallbacks(resizeRunnable);
            resizeRunnable = null;
        }

        smallestThumbHeight = Integer.MAX_VALUE;
        updateThumb(false);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}