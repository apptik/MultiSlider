package io.apptik.widget;

import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Thumb is the main object in MultiSlider.
 * There could be 0, 1 or many thumbs. Each thumb has a mMin and max limit and a value which
 * should always be between the limits. Each thumb defines a 'Range' the range is always
 * between the value of the Thumb back to the previous Thumb's value or to the beginning of
 * the track.
 */
class ThumbImpl implements IThumb {

    // parent MultiSlider instance
    private final MultiSlider mMultiSlider;

    //abs mMin value for this thumb
    private int mMin;
    //abs max value for this thumb
    private int mMax;
    //current value of this thumb
    private int mValue;
    //thumb tag. can be used for identifying the thumb
    private String mTag = "thumb";
    //thumb drawable, can be shared
    private Drawable mThumb;
    //thumb range drawable, can also be shared
    //this is the line from the beginning or the previous thumb if any until the this one.
    private Drawable mRange;

    private int mOffset;

    //thumb's view binder
    private View mViewBinder = null;
    //save old position for before rebind
    private float mViewBinderX = 0;
    private boolean mViewBinderEnable = true;
    private int mViewBinderVisibility = View.VISIBLE;

    //cannot be moved if invisible and it is not displayed
    private boolean mIsInvisible = false;

    //cannot be moved if not enabled
    private boolean mIsEnabled = true;

    ThumbImpl(final MultiSlider multiSlider) {
        mMultiSlider = multiSlider;
        mMin = mMultiSlider.getMin();
        mMax = mMultiSlider.getMax();
        mValue = mMax;
    }

    public final Drawable getRange() {
        return mRange;
    }

    public final IThumb setRange(final Drawable range) {
        mRange = range;
        return this;
    }
    
    public int getPossibleMin() {
        int res = mMin;
        res += mMultiSlider.getThumbIndex(this) * mMultiSlider.getStepsThumbsApart();
        return res;
    }

    public int getPossibleMax() {
        int res = mMax;
        res -= (mMultiSlider.getThumbsCount() - 1 - mMultiSlider.getThumbIndex(this))
                * mMultiSlider.getStepsThumbsApart();
        return res;
    }

    public int getMin() {
        return mMin;
    }

    public final IThumb setMin(int min) {
        min = Math.max(mMultiSlider.getMin(), Math.min(mMax, min));
        if (mMin != min) {
            mMin = min;
            if (mValue < mMin) {
                mValue = mMin;
                mMultiSlider.invalidate();
            }
        }
        return this;
    }

    public int getMax() {
        return mMax;
    }

    public final IThumb setMax(int max) {
        max = Math.min(mMultiSlider.getMax(), Math.max(mMin, max));
        if (mMax != max) {
            mMax = max;
            if (mValue > mMax) {
                mValue = mMax;
                mMultiSlider.invalidate();
            }
        }
        return this;
    }

    public int getValue() {
        return mValue;
    }

    public final IThumb setValue(final int value) {
        mValue = value;
        return this;
    }

    public String getTag() {
        return mTag;
    }

    public final IThumb setTag(final String tag) {
        mTag = tag;
        return this;
    }

    public final Drawable getThumb() {
        return mThumb;
    }

    public final IThumb setThumb(final Drawable thumb) {
        mThumb = thumb;
        return this;
    }

    public int getOffset() {
        return mOffset;
    }

    public final IThumb setOffset(final int offset) {
        mOffset = offset;
        return this;
    }

    public boolean isEnabled() {
        return !isInvisible() && mIsEnabled;
    }

    public final IThumb setEnabled(final boolean enabled) {
        mIsEnabled = enabled;
        final Drawable drawable = getThumb();
        if (drawable != null) {
            if (isEnabled()) {
                drawable.setState(new int[]{android.R.attr.state_enabled});
            } else {
                drawable.setState(new int[]{-android.R.attr.state_enabled});
            }
        }
        final View binder = getViewBinder();
        if (binder != null) {
            binder.setEnabled(enabled);
        }
        return this;
    }

    public boolean isInvisible() {
        return mIsInvisible;
    }

    public final IThumb setInvisible(final boolean invisible) {
        mIsInvisible = invisible;
        final View binder = getViewBinder();
        if (binder != null) {
            binder.setVisibility(invisible ? View.INVISIBLE : View.VISIBLE);
        }
        return this;
    }

    public final View getViewBinder() {
        return mViewBinder;
    }

    public final IThumb setViewBinder(final View view) {
        //restore properties of the previous view
        if (mViewBinder != null) {
            mViewBinder.setTranslationX(mViewBinderX);
            mViewBinder.setEnabled(mViewBinderEnable);
            mViewBinder.setVisibility(mViewBinderVisibility);
        }
        mViewBinder = view;
        mViewBinderX = mViewBinder.getTranslationX();
        mViewBinderEnable = mViewBinder.isEnabled();
        mViewBinderVisibility = mViewBinder.getVisibility();
        return this;
    }

    public final IThumb updateViewBinderPosition() {
        final View binder = getViewBinder();
        final Drawable drawable = getThumb();

        if (binder == null || drawable == null)
            return this;

        final View binder_parent = (View) binder.getParent();

        /*
         * parent view required for the limit of X-axis position
         */
        if (binder_parent == null)
            return this;

        /*
         * Using post method for avoid incorrect measured width of binded view.
         */
        binder.post(new Runnable() {
            @Override
            public void run() {

                final float thumb_centerX = drawable.getBounds().centerX();
                final int thumb_half_width = drawable.getIntrinsicWidth() / 2;

                final int vb_width = binder.getMeasuredWidth();
                final int vb_half_width = vb_width / 2;

                final float vb_parent_minX = binder_parent.getTranslationX();
                final float vb_parent_maxX = vb_parent_minX + binder_parent.getMeasuredWidth();

                // minimum available X-axis position
                final float vb_minX = Math.max(vb_parent_minX,
                        thumb_centerX - thumb_half_width);

                // maximum available X-axis position
                final float vb_maxX = Math.min(vb_parent_maxX - vb_width,
                        thumb_centerX + thumb_half_width);

                final float vb_x = Math.min(vb_maxX, Math.max(vb_minX, thumb_centerX - vb_half_width));

                binder.setTranslationX(vb_x);
            }
        });

        return this;
    }
}
