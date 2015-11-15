/*
 * Copyright (C) 2015 AppTik Project
 * Copyright (C) 2014 Kalin Maldzhanski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apptik.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.*;

import java.util.LinkedList;

public class MultiSlider extends View {

    public interface OnThumbValueChangeListener {
        void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value);

        void onStartTrackingTouch(MultiSlider multiSlider, MultiSlider.Thumb thumb, int value);

        void onStopTrackingTouch(MultiSlider multiSlider, MultiSlider.Thumb thumb, int value);
    }

    private OnThumbValueChangeListener mOnThumbValueChangeListener;

    int mMinWidth;
    int mMaxWidth;
    int mMinHeight;
    int mMaxHeight;

    /**
     * global Min and Max
     */
    private int mScaleMin;
    private int mScaleMax;
    private int mStep;
    private int mStepsThumbsApart;
    private boolean mDrawThumbsApart;

    private Drawable mTrack;

    //used in constructor to prevent invalidating before ready state
    private boolean mNoInvalidate;
    private long mUiThreadId;

    private boolean mInDrawing;
    private boolean mAttached;
    private boolean mRefreshIsPosted;

    boolean mMirrorForRtl = false;

    //list of all the loaded thumbs
    private LinkedList<Thumb> mThumbs;


    /**
     * Whether this is user seekable.
     */
    boolean mIsUserSeekable = true;

    /**
     * On key presses (right or left), the amount to increment/decrement the
     * progress.
     */
    private int mKeyProgressIncrement = 1;

    private static final int NO_ALPHA = 0xFF;
    private float mDisabledAlpha = 0.5f;

    private int mScaledTouchSlop;
    private float mTouchDownX;
    //thumbs that are currently being dragged
    private LinkedList<Thumb> mDraggingThumbs = new LinkedList<Thumb>();
    //thumbs that are currently being touched
    LinkedList<Thumb> exactTouched = null;

    private final TypedArray a;

    public class Thumb {
        //abs min value for this thumb
        private int min;
        //abs max value for this thumb
        private int max;
        //current value of this thumb
        private int value;
        //thumb drawable, can be shared
        private Drawable thumb;
        //thumb range drawable, can also be shared
        //this is the line from the beginning or the previous thumb if any until the this one.
        private Drawable range;
        private int thumbOffset;

        //cannot be moved if invisible
        private boolean invisibleThumb = false;

        public Drawable getRange() {
            return range;
        }

        public Thumb setRange(Drawable range) {
            this.range = range;
            return this;
        }

        public Thumb() {
        }

        public boolean isInvisibleThumb() {
            return invisibleThumb;
        }

        public void setInvisibleThumb(boolean invisibleThumb) {
            this.invisibleThumb = invisibleThumb;
        }


        public int getDrawableValue() {
            if (thumb == null) return 0;
            return Math.round(getScaleSize() * thumb.getBounds().width() / getWidth());
        }


        /**
         * Only useful the keepThumbsApart is set, otherwise return ScaleMin
         *
         * @return the minimum value a thumb can obtain depending on other thumbs before it
         */
        public int getPossibleMin() {
            int res = mScaleMin;
            res += mThumbs.indexOf(this) * mStepsThumbsApart;
            return res;
        }

        /**
         * Only useful the keepThumbsApart is set, otherwise return ScaleMax
         *
         * @return the maximum value a thumb can have depending the thumbs after it
         */
        public int getPossibleMax() {
            int res = mScaleMax;
            res -= (mThumbs.size() - 1 - mThumbs.indexOf(this)) * mStepsThumbsApart;
            return res;
        }

        public int getMin() {
            return min;
        }

        public Thumb setMin(int min) {
            if (min > this.max) {
                min = this.max;
            }
            if (min < mScaleMin) {
                min = mScaleMin;
            }
            if (this.min != min) {
                this.min = min;
                if (value < this.min) {
                    value = this.min;
                    invalidate();
                }
            }
            return this;
        }

        public int getMax() {
            return max;
        }

        public Thumb setMax(int max) {
            if (max < this.min) {
                max = this.min;
            }
            if (max > mScaleMax) {
                max = mScaleMax;
            }
            if (this.max != max) {
                this.max = max;
                if (value > this.max) {
                    value = this.max;
                    invalidate();
                }
            }
            return this;
        }

        public int getValue() {
            return value;
        }

        public Thumb setValue(int value) {
            setThumbValue(this, value, false);
            return this;
        }

        public Drawable getThumb() {
            return thumb;
        }

        public Thumb setThumb(Drawable mThumb) {
            this.thumb = mThumb;
            return this;
        }

        public int getThumbOffset() {
            return thumbOffset;
        }

        public Thumb setThumbOffset(int mThumbOffset) {
            this.thumbOffset = mThumbOffset;
            return this;
        }


    }

    public MultiSlider(Context context) {
        this(context, null);
    }

    public MultiSlider(Context context, AttributeSet attrs) {
        this(context, attrs, io.apptik.widget.R.attr.multiSliderStyle);
    }

    public MultiSlider(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public MultiSlider(Context context, AttributeSet attrs, int defStyle, int styleRes) {
        super(context, attrs, defStyle);

        mUiThreadId = Thread.currentThread().getId();

        a = context.obtainStyledAttributes(attrs, io.apptik.widget.R.styleable.MultiSlider, defStyle, styleRes);
        mNoInvalidate = true;
        int numThumbs = a.getInt(io.apptik.widget.R.styleable.MultiSlider_thumbNumber, 2);
        initMultiSlider(numThumbs);

        Drawable trackDrawable = a.getDrawable(io.apptik.widget.R.styleable.MultiSlider_android_track);
        if (trackDrawable == null) {
            trackDrawable = ContextCompat.getDrawable(getContext(), io.apptik.widget.R.drawable.multislider_scrubber_track_holo_light);
        }

        setTrackDrawable(getTintedDrawable(trackDrawable, a.getColor(io.apptik.widget.R.styleable.MultiSlider_trackColor, 0)));

        //TODO
//        mMinWidth = a.getDimensionPixelSize(R.styleable.MultiSlider_minWidth, mMinWidth);
//        mMaxWidth = a.getDimensionPixelSize(R.styleable.MultiSlider_maxWidth, mMaxWidth);
//        mMinHeight = a.getDimensionPixelSize(R.styleable.MultiSlider_minHeight, mMinHeight);
//        mMaxHeight = a.getDimensionPixelSize(R.styleable.MultiSlider_maxHeight, mMaxHeight);


        setStep(a.getInt(io.apptik.widget.R.styleable.MultiSlider_scaleStep, mStep));
        setStepsThumbsApart(a.getInt(io.apptik.widget.R.styleable.MultiSlider_stepsThumbsApart, mStepsThumbsApart));
        setDrawThumbsApart(a.getBoolean(io.apptik.widget.R.styleable.MultiSlider_drawThumbsApart, mDrawThumbsApart));
        setMax(a.getInt(io.apptik.widget.R.styleable.MultiSlider_scaleMax, mScaleMax), true);
        setMin(a.getInt(io.apptik.widget.R.styleable.MultiSlider_scaleMin, mScaleMin), true);


        mMirrorForRtl = a.getBoolean(io.apptik.widget.R.styleable.MultiSlider_mirrorForRTL, mMirrorForRtl);

        // --> now place thumbs

        Drawable thumbDrawable = a.getDrawable(io.apptik.widget.R.styleable.MultiSlider_android_thumb);

        if (thumbDrawable == null) {
            thumbDrawable = ContextCompat.getDrawable(getContext(), io.apptik.widget.R.drawable.multislider_scrubber_control_selector_holo_light);
        }

        Drawable range = a.getDrawable(io.apptik.widget.R.styleable.MultiSlider_range);
        if (range == null) {
            range = ContextCompat.getDrawable(getContext(), io.apptik.widget.R.drawable.multislider_scrubber_primary_holo);
        }

        Drawable range1 = a.getDrawable(io.apptik.widget.R.styleable.MultiSlider_range1);
        Drawable range2 = a.getDrawable(io.apptik.widget.R.styleable.MultiSlider_range2);

        setThumbDrawables(thumbDrawable, range, range1, range2); // will guess thumbOffset if thumb != null...
        // ...but allow layout to override this

        int thumbOffset = a.getDimensionPixelOffset(io.apptik.widget.R.styleable.MultiSlider_android_thumbOffset, thumbDrawable.getIntrinsicWidth() / 2);
        setThumbOffset(thumbOffset);

        positionThumbs();

        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mNoInvalidate = false;
        a.recycle();
    }

    public int getStepsThumbsApart() {
        return mStepsThumbsApart;
    }

    public void setStepsThumbsApart(int stepsThumbsApart) {
        if (stepsThumbsApart < 0) stepsThumbsApart = 0;
        this.mStepsThumbsApart = stepsThumbsApart;
    }

    public int getStep() {
        return mStep;
    }

    public void setStep(int mStep) {
        this.mStep = mStep;
    }

    private int getScaleSize() {
        return mScaleMax - mScaleMin;
    }

    private void positionThumbs() {
        if (mThumbs == null || mThumbs.isEmpty()) return;

        if (mThumbs.size() > 0) {
            mThumbs.getFirst().setValue(mScaleMin);
        }
        if (mThumbs.size() > 1) {
            mThumbs.getLast().setValue(mScaleMax);
        }
        if (mThumbs.size() > 2) {
            int even = (mScaleMax - mScaleMin) / (mThumbs.size() - 1);
            int lastPos = mScaleMax - even;
            for (int i = mThumbs.size() - 2; i > 0; i--) {
                mThumbs.get(i).setValue(lastPos);
                lastPos -= even;
            }
        }
    }

    public void setOnThumbValueChangeListener(OnThumbValueChangeListener l) {
        mOnThumbValueChangeListener = l;
    }

    public boolean isDrawThumbsApart() {
        return mDrawThumbsApart;
    }

    public void setDrawThumbsApart(boolean drawThumbsApart) {
        this.mDrawThumbsApart = drawThumbsApart;
    }

    private void initMultiSlider(int numThumbs) {
        mStep = 1;
        mStepsThumbsApart = 0;
        mDrawThumbsApart = false;
        mScaleMin = 0;
        mScaleMax = 100;
        mMinWidth = 24;
        mMaxWidth = 48;
        mMinHeight = 24;
        mMaxHeight = 48;
        mThumbs = new LinkedList<Thumb>();
        for (int i = 0; i < numThumbs; i++) {
            mThumbs.add(new Thumb().setMin(mScaleMin).setMax(mScaleMax));
        }
    }

    public void setThumbOffset(int thumbOffset) {
        for (Thumb thumb : mThumbs) {
            thumb.setThumbOffset(thumbOffset);
        }
        invalidate();
    }

    public void setTrackDrawable(Drawable d) {
        boolean needUpdate;
        if (mTrack != null && d != mTrack) {
            mTrack.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }

        if (d != null) {
            d.setCallback(this);
//            if (canResolveLayoutDirection()) {
//                d.setLayoutDirection(getLayoutDirection());
//            }

            // Make sure the ProgressBar is always tall enough
            int drawableHeight = d.getMinimumHeight();
            if (mMaxHeight < drawableHeight) {
                mMaxHeight = drawableHeight;
                requestLayout();
            }
        }
        mTrack = d;

        if (needUpdate) {
            updateTrackBounds(getWidth(), getHeight());
            updateTrackState();
            //TODO update all thumbs with their range tracks also
        }
    }

    private int optThumbValue(Thumb thumb, int value) {
        if (thumb == null || thumb.getThumb() == null) return value;
        int currIdx = mThumbs.indexOf(thumb);


        if (mThumbs.size() > currIdx + 1 && value > mThumbs.get(currIdx + 1).getValue() - mStepsThumbsApart * mStep) {
            value = mThumbs.get(currIdx + 1).getValue() - mStepsThumbsApart * mStep;
        }

        if (currIdx > 0 && value < mThumbs.get(currIdx - 1).getValue() + mStepsThumbsApart * mStep) {
            value = mThumbs.get(currIdx - 1).getValue() + mStepsThumbsApart * mStep;
        }

        if ((value - mScaleMin) % mStep != 0) {
            value += mStep - ((value - mScaleMin) % mStep);
        }

        if (value < thumb.getMin()) {
            value = thumb.getMin();
        }

        if (value > thumb.getMax()) {
            value = thumb.getMax();
        }

        return value;
    }


    /**
     * Refreshes the value for the specific thumb
     *
     * @param thumb    the thumb which value is going to be changed
     * @param value    the new value
     * @param fromUser if the request is coming form the user or the client
     */
    private synchronized void setThumbValue(Thumb thumb, int value, boolean fromUser) {
        if (thumb == null || thumb.getThumb() == null) return;

        value = optThumbValue(thumb, value);

        if (value != thumb.getValue()) {
            thumb.value = value;
        }
        if (hasOnThumbValueChangeListener()) {
            mOnThumbValueChangeListener.onValueChanged(this, thumb, mThumbs.indexOf(thumb), thumb.getValue());
        }
        updateThumb(thumb, getWidth(), getHeight());
    }

    private synchronized void setThumbValue(int thumb, int value, boolean fromUser) {
        setThumbValue(mThumbs.get(thumb), value, fromUser);
    }

    private void updateTrackBounds(int w, int h) {
        // onDraw will translate the canvas so we draw starting at 0,0.
        // Subtract out padding for the purposes of the calculations below.
        w -= getPaddingRight() + getPaddingLeft();
        h -= getPaddingTop() + getPaddingBottom();

        int right = w;
        int bottom = h;
        int top = 0;
        int left = 0;

        if (mTrack != null) {
            mTrack.setBounds(0, 0, right, bottom);
        }
    }

    private void updateTrackState() {
        int[] state = getDrawableState();

        if (mTrack != null && mTrack.isStateful()) {
            mTrack.setState(state);
        }
    }

    /**
     * Sets the thumb drawable for all thumbs
     * <p/>
     * If the thumb is a valid drawable (i.e. not null), half its width will be
     * used as the new thumb offset (@see #setThumbOffset(int)).
     *
     * @param thumb Drawable representing the thumb
     */
    private void setThumbDrawables(Drawable thumb, Drawable range, Drawable range1, Drawable range2) {
        if (thumb == null) return;
        boolean needUpdate;
        Drawable rangeDrawable;

        // This way, calling setThumbDrawables again with the same bitmap will result in
        // it recalculating thumbOffset (if for example it the bounds of the
        // drawable changed)
        int curr = 0;
        int padding = 0;
        for (Thumb mThumb : mThumbs) {
            curr++;
            if (mThumb.getThumb() != null && thumb != mThumb.getThumb()) {
                mThumb.getThumb().setCallback(null);
                needUpdate = true;
            } else {
                needUpdate = false;
            }

            if (curr == 1 && range1 != null) {
                rangeDrawable = getTintedDrawable(range1, a.getColor(io.apptik.widget.R.styleable.MultiSlider_range1Color, 0));
            } else if (curr == 2 && range2 != null) {
                rangeDrawable = getTintedDrawable(range2, a.getColor(io.apptik.widget.R.styleable.MultiSlider_range2Color, 0));
            } else {
                rangeDrawable = getTintedDrawable(range.getConstantState().newDrawable(), a.getColor(io.apptik.widget.R.styleable.MultiSlider_rangeColor, 0));
            }

            mThumb.setRange(rangeDrawable);

            Drawable newDrawable = getTintedDrawable(thumb.getConstantState().newDrawable(), a.getColor(io.apptik.widget.R.styleable.MultiSlider_thumbColor, 0));
            newDrawable.setCallback(this);

            // Assuming the thumb drawable is symmetric, set the thumb offset
            // such that the thumb will hang halfway off either edge of the
            // progress bar.
            mThumb.setThumbOffset(thumb.getIntrinsicWidth() / 2);

            padding = Math.max(padding, mThumb.getThumbOffset());
            // If we're updating get the new states
            if (needUpdate &&
                    (newDrawable.getIntrinsicWidth() != mThumb.getThumb().getIntrinsicWidth()
                            || newDrawable.getIntrinsicHeight() != mThumb.getThumb().getIntrinsicHeight())) {
                requestLayout();
            }
            mThumb.setThumb(newDrawable);

            if (needUpdate) {
                invalidate();
                if (thumb != null && thumb.isStateful()) {
                    // Note that if the states are different this won't work.
                    // For now, let's consider that an app bug.
                    int[] state = getDrawableState();
                    thumb.setState(state);
                }

            }
        }
        setPadding(padding, getPaddingTop(), padding, getPaddingBottom());

    }

    /**
     * Return the drawable used to represent the scroll thumb - the component that
     * the user can drag back and forth indicating the current value by its position.
     *
     * @return The thumb at position pos
     */
    public Thumb getThumb(int pos) {
        return mThumbs.get(pos);
    }

    /**
     * Sets the amount of progress changed via the arrow keys.
     *
     * @param increment The amount to increment or decrement when the user
     *                  presses the arrow keys.
     */
    public void setKeyProgressIncrement(int increment) {
        mKeyProgressIncrement = increment < 0 ? -increment : increment;
    }

    /**
     * Returns the amount of progress changed via the arrow keys.
     * <p/>
     * By default, this will be a value that is derived from the max progress.
     *
     * @return The amount to increment or decrement when the user presses the
     * arrow keys. This will be positive.
     */
    public int getKeyProgressIncrement() {
        return mKeyProgressIncrement;
    }

    public synchronized void setMax(int max) {
        setMax(max, true, false);
    }

    public synchronized void setMax(int max, boolean extendMaxForThumbs) {
        setMax(max, extendMaxForThumbs, false);
    }

    public synchronized void setMax(int max, boolean extendMaxForThumbs, boolean repositionThumbs) {
        if (max < mScaleMin) {
            max = mScaleMin;
        }

        if (max != mScaleMax) {
            mScaleMax = max;

            //check for thumbs out of bounds and adjust the max for those exceeding the new one
            for (Thumb thumb : mThumbs) {
                if (extendMaxForThumbs) {
                    thumb.setMax(max);
                } else if (thumb.getMax() > max) {
                    thumb.setMax(max);
                }

                if (thumb.getValue() > max) {
                    setThumbValue(thumb, max, false);
                }


            }
            if (repositionThumbs)
                positionThumbs();

            postInvalidate();
        }

        if ((mKeyProgressIncrement == 0) || (mScaleMax / mKeyProgressIncrement > 20)) {
            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            setKeyProgressIncrement(Math.max(1, Math.round((float) mScaleMax / 20)));
        }
    }

    public synchronized void setMin(int min) {
        setMin(min, true, false);
    }

    public synchronized void setMin(int min, boolean extendMaxForThumbs) {
        setMin(min, extendMaxForThumbs, false);
    }

    public synchronized void setMin(int min, boolean extendMaxForThumbs, boolean repositionThumbs) {
        if (min > mScaleMax) {
            min = mScaleMax;
        }

        if (min != mScaleMin) {
            mScaleMin = min;

            //check for thumbs out of bounds and adjust the max for those exceeding the new one
            for (Thumb thumb : mThumbs) {
                if (extendMaxForThumbs) {
                    thumb.setMin(min);
                } else if (thumb.getMin() < min) {
                    thumb.setMin(min);
                }

                if (thumb.getValue() < min) {
                    setThumbValue(thumb, min, false);
                }

            }
            if (repositionThumbs)
                positionThumbs();

            postInvalidate();
        }

        if ((mKeyProgressIncrement == 0) || (mScaleMax / mKeyProgressIncrement > 20)) {
            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            setKeyProgressIncrement(Math.max(1, Math.round((float) mScaleMax / 20)));
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        for (Thumb thumb : mThumbs) {
            if (thumb.getThumb() != null && who == thumb.getThumb()) return true;
        }
        return who == mTrack || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        for (Thumb thumb : mThumbs) {
            if (thumb.getThumb() != null) thumb.getThumb().jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mDraggingThumbs != null && !mDraggingThumbs.isEmpty()) {
            int[] state = getDrawableState();
            for (Thumb thumb : mDraggingThumbs) {
                if (thumb.getThumb() != null)
                    thumb.getThumb().setState(state);
            }
            for (Thumb thumb : mThumbs) {
                if (!mDraggingThumbs.contains(thumb) && thumb.getThumb() != null && thumb.getThumb().isStateful()) {
                    thumb.getThumb().setState(new int[]{android.R.attr.state_enabled});
                }
            }
        } else {
            int[] state = getDrawableState();
            for (Thumb thumb : mThumbs) {
                if (thumb.getThumb() != null && thumb.getThumb().isStateful()) {
                    thumb.getThumb().setState(state);
                }
            }
        }
    }


    /**
     * Updates Thumb drawable position according to the new w,h
     *
     * @param thumb the thumb object
     * @param w     width
     * @param h     height
     */
    private void updateThumb(Thumb thumb, int w, int h) {
        int thumbHeight = thumb == null ? 0 : thumb.getThumb().getIntrinsicHeight();
        // The max height does not incorporate padding, whereas the height
        // parameter does
        int trackHeight = h - getPaddingTop() - getPaddingBottom();

        float scale = getScaleSize() > 0 ? (float) thumb.getValue() / (float) getScaleSize() : 0;

        Drawable prevThumb = null;
        int currIdx = mThumbs.indexOf(thumb);
        if (currIdx > 0) {
            prevThumb = mThumbs.get(currIdx - 1).getThumb();
        }

        if (thumbHeight > trackHeight) {
            if (thumb != null) {
                setThumbPos(w, h, thumb.getThumb(), prevThumb, thumb.getRange(), scale, 0, thumb.getThumbOffset(), getThumbOptOffset(thumb));
            }
            int gapForCenteringTrack = (thumbHeight - trackHeight) / 2;
            if (mTrack != null) {
                // Canvas will be translated by the padding, so 0,0 is where we start drawing
                mTrack.setBounds(0, gapForCenteringTrack,
                        w - getPaddingRight() - getPaddingLeft(), h - getPaddingBottom() - gapForCenteringTrack
                                - getPaddingTop());
            }
        } else {
            if (mTrack != null) {
                // Canvas will be translated by the padding, so 0,0 is where we start drawing
                mTrack.setBounds(0, 0, w - getPaddingRight() - getPaddingLeft(), h - getPaddingBottom()
                        - getPaddingTop());
            }
            int gap = (trackHeight - thumbHeight) / 2;
            if (thumb != null) {
                setThumbPos(w, h, thumb.getThumb(), prevThumb, thumb.getRange(), scale, gap, thumb.getThumbOffset(), getThumbOptOffset(thumb));
            }
        }

        //update thumbs after it
        for (int i = currIdx + 1; i < mThumbs.size(); i++) {
            int gap = (trackHeight - thumbHeight) / 2;
            scale = getScaleSize() > 0 ? (float) mThumbs.get(i).getValue() / (float) getScaleSize() : 0;
            setThumbPos(w, h, mThumbs.get(i).getThumb(), mThumbs.get(i - 1).getThumb(), mThumbs.get(i).getRange(), scale, gap, mThumbs.get(i).getThumbOffset(), getThumbOptOffset(mThumbs.get(i)));
        }
    }


    /**
     * @param gap If set to {@link Integer#MIN_VALUE}, this will be ignored and
     */
    private void setThumbPos(int w, int h, Drawable thumb, Drawable prevThumb, Drawable range, float scale, int gap, int thumbOffset, int optThumbOffset) {
        final int available = getAvailable();
        int thumbWidth = thumb.getIntrinsicWidth();
        int thumbHeight = thumb.getIntrinsicHeight();

        //todo change available before also


        float scaleOffset = getScaleSize() > 0 ? (float) mScaleMin / (float) getScaleSize() : 0;

        int thumbPos = (int) (scale * available - scaleOffset * available + 0.5f);

        int topBound, bottomBound;
        if (gap == Integer.MIN_VALUE) {
            Rect oldBounds = thumb.getBounds();
            topBound = oldBounds.top;
            bottomBound = oldBounds.bottom;
        } else {
            topBound = gap;
            bottomBound = gap + thumbHeight;
        }

        // Canvas will be translated, so 0,0 is where we start drawing
        final int left = (isLayoutRtl() && mMirrorForRtl) ? available - thumbPos - optThumbOffset : thumbPos + optThumbOffset;

        thumb.setBounds(left, topBound, left + thumbWidth, bottomBound);

        w -= getPaddingRight() + getPaddingLeft();
        h -= getPaddingTop() + getPaddingBottom();

        int right = w;
        int bottom = h;

        int leftRange = 0;
        if (prevThumb != null) {
            leftRange = prevThumb.getBounds().left;
        }
        if (range != null) {
            range.setBounds(leftRange, 0, left, bottom);
        }

        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // --> draw track
        if (mTrack != null) {
            // Translate canvas so a indeterminate circular progress bar with padding
            // rotates properly in its animation
            canvas.save();
            if (isLayoutRtl() && mMirrorForRtl) {
                canvas.translate(getWidth() - getPaddingRight(), getPaddingTop());
                canvas.scale(-1.0f, 1.0f);
            } else {
                canvas.translate(getPaddingLeft(), getPaddingTop());
            }
            mTrack.draw(canvas);
            canvas.restore();
        }

        // --> draw ranges

        for (Thumb thumb : mThumbs) {
            if (thumb.getRange() != null) {
                canvas.save();
                if (isLayoutRtl() && mMirrorForRtl) {
                    canvas.translate(getWidth() - getPaddingRight(), getPaddingTop());
                    canvas.scale(-1.0f, 1.0f);
                } else {
                    canvas.translate(getPaddingLeft(), getPaddingTop());
                }
                thumb.getRange().draw(canvas);

                canvas.restore();
            }
        }

        // --> then draw thumbs
        for (Thumb thumb : mThumbs) {
            if (thumb.getThumb() != null && !thumb.isInvisibleThumb()) {
                canvas.save();
                // Translate the padding. For the x, we need to allow the thumb to
                // draw in its extra space
                canvas.translate(getPaddingLeft() - thumb.getThumbOffset(), getPaddingTop());
                // float scale = mScaleMax > 0 ? (float) thumb.getValue() / (float) mScaleMax : 0;
                thumb.getThumb().draw(canvas);

                canvas.restore();
            }
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int maxThumbHeight = 0;
        int maxRangeHeight = 0;
        for (Thumb thumb : mThumbs) {
            if (thumb.getThumb() != null) {
                maxThumbHeight = Math.max(thumb.getThumb().getIntrinsicHeight(), maxThumbHeight);
                maxRangeHeight = Math.max(thumb.getThumb().getIntrinsicHeight(), maxRangeHeight);

            }
        }

        int dw = 0;
        int dh = 0;
        if (mTrack != null) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, mTrack.getIntrinsicWidth()));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, mTrack.getIntrinsicHeight()));
            dh = Math.max(maxRangeHeight, dh);
            dh = Math.max(maxThumbHeight, dh);
        }
        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0));
    }


    public boolean isInScrollingContainer() {
        ViewParent p = getParent();
        while (p != null && p instanceof ViewGroup) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    private int getAvailable() {
        int available = getWidth() - getPaddingLeft() - getPaddingRight();
        if (mThumbs != null && mThumbs.size() > 0) {
            available -= getThumbOptOffset(mThumbs.getLast());
        }
        //TODO check for the offset
        return available;
    }

    /**
     * Get closest thumb to play with,
     * incase more than one get the last one
     *
     * @param x X coordinate of the touch
     * @return
     */
    private LinkedList<Thumb> getClosestThumb(int x) {
        LinkedList<Thumb> exact = new LinkedList<Thumb>();
        Thumb closest = null;
        int currDistance = getAvailable() + 1;

        for (Thumb thumb : mThumbs) {
            if (thumb.getThumb() == null || thumb.isInvisibleThumb()) continue;

            int minV = x - thumb.getThumb().getIntrinsicWidth();
            int maxV = x + thumb.getThumb().getIntrinsicWidth();
            if (thumb.getThumb().getBounds().centerX() >= minV && thumb.getThumb().getBounds().centerX() <= maxV) {
                //we have exact match
                // we add them all so we can choose later which one to move
                exact.add(thumb);
            } else if (Math.abs(thumb.getThumb().getBounds().centerX() - x) <= currDistance) {
                if (Math.abs(thumb.getThumb().getBounds().centerX() - x) == currDistance) {
                    if (x > getWidth() / 2) {
                        //left one(s) has more place to move
                        closest = thumb;
                    } else {
                        //right one(s) has more place to move

                    }
                } else {
                    if (thumb.getThumb() != null) {
                        currDistance = Math.abs(thumb.getThumb().getBounds().centerX() - x);
                        closest = thumb;
                    }
                }
            }
        }

        if (exact.isEmpty() && closest != null) {
            exact.add(closest);
        }
        return exact;
    }

    private Thumb getMostMovable(LinkedList<Thumb> thumbs, MotionEvent event) {
        Thumb res = null;
        int maxChange = 0;
        if (thumbs != null && !thumbs.isEmpty()) {

            if (thumbs.getFirst().getValue() == getValue(event, thumbs.getFirst())) return null;

            for (Thumb thumb : thumbs) {
                int optValue = (getValue(event, thumbs.getFirst()) > thumb.getValue()) ? mScaleMax : mScaleMin;
                int currChange = Math.abs(thumb.getValue() - optThumbValue(thumb, optValue));
                if (currChange > maxChange) {
                    maxChange = currChange;
                    res = thumb;
                }
            }
        }
        return res;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsUserSeekable || !isEnabled()) {
            return false;
        }

        int pointerIdx = event.getActionIndex();


        Thumb currThumb = null;
        if (mDraggingThumbs.size() > pointerIdx) {
            currThumb = mDraggingThumbs.get(pointerIdx);
        } else {

            LinkedList<Thumb> closestOnes = getClosestThumb((int) event.getX(event.getActionIndex()));
            if (closestOnes != null && !closestOnes.isEmpty()) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                    if (closestOnes.size() == 1) {
                        currThumb = closestOnes.getFirst();
                        onStartTrackingTouch(currThumb);
                        drawableStateChanged();
                    } else {
                        //we have more than one thumb at the same place and we touched there
                        exactTouched = closestOnes;
                    }
                } else if (exactTouched != null && !exactTouched.isEmpty() && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    //we have thumbs waiting to be selected to move
                    currThumb = getMostMovable(exactTouched, event);
                    //check if move actually changed value
                    if (currThumb == null) return false;
                    exactTouched = null;
                    onStartTrackingTouch(currThumb);
                    drawableStateChanged();
                } else {
                    currThumb = closestOnes.getFirst();
                    onStartTrackingTouch(currThumb);
                    drawableStateChanged();
                }
            }


        }


        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isInScrollingContainer()) {
                    mTouchDownX = event.getX();
                } else {
                    //currThumb = getClosestThumb(newValue);
                    //onStartTrackingTouch(currThumb);
                    setPressed(true);
                    if (currThumb != null && currThumb.getThumb() != null) {
                        invalidate(currThumb.getThumb().getBounds()); // This may be within the padding region
                    }

                    int value = getValue(event, currThumb);
                    setThumbValue(currThumb, value, true);
                    attemptClaimDrag();
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (isInScrollingContainer()) {
                    mTouchDownX = event.getX();
                } else {
                    //currThumb = getClosestThumb(newValue);
                    //onStartTrackingTouch(currThumb);
                    setPressed(true);
                    if (currThumb != null && currThumb.getThumb() != null) {
                        invalidate(currThumb.getThumb().getBounds()); // This may be within the padding region
                    }

                    setThumbValue(currThumb, getValue(event, currThumb), true);
                    attemptClaimDrag();
                }
                invalidate();
                break;

            //with move we dont have pointer action so set them all
            case MotionEvent.ACTION_MOVE:
                if (!mDraggingThumbs.isEmpty()) {

                    //need the index
                    for (int i = 0; i < mDraggingThumbs.size(); i++) {
                        setPressed(true);
                        if (mDraggingThumbs.get(i) != null && mDraggingThumbs.get(i).getThumb() != null) {
                            invalidate(mDraggingThumbs.get(i).getThumb().getBounds()); // This may be within the padding region
                        }
                        setThumbValue(mDraggingThumbs.get(i), getValue(event, i, mDraggingThumbs.get(i)), true);
                        attemptClaimDrag();
                    }

                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        //currThumb = getClosestThumb(newValue);
                        //onStartTrackingTouch(currThumb);
                        setPressed(true);
                        if (currThumb != null && currThumb.getThumb() != null) {
                            invalidate(currThumb.getThumb().getBounds()); // This may be within the padding region
                        }

                        setThumbValue(currThumb, getValue(event, currThumb), true);
                        attemptClaimDrag();
                    }
                }

                break;

            //there are other pointers left
            case MotionEvent.ACTION_POINTER_UP:
                if (currThumb != null) {
                    setThumbValue(currThumb, getValue(event, currThumb), true);
                    onStopTrackingTouch(currThumb);
                } else {
//                    currThumb = getClosestThumb(newValue);
//                    // Touch up when we never crossed the touch slop threshold should
//                    // be interpreted as a tap-seek to that location.
//                    onStartTrackingTouch(currThumb);
//                    setThumbValue(currThumb, newValue, true);
//                    onStopTrackingTouch(currThumb);
                }

                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;

            //we normally have one single pointer here and its gone now
            case MotionEvent.ACTION_UP:
                if (currThumb != null) {
                    int value = getValue(event, currThumb);
                    setThumbValue(currThumb, value, true);
                    onStopTrackingTouch(currThumb);
                } else {
//                    currThumb = getClosestThumb(newValue);
//                    // Touch up when we never crossed the touch slop threshold should
//                    // be interpreted as a tap-seek to that location.
//                    onStartTrackingTouch(currThumb);
//                    setThumbValue(currThumb, newValue, true);
//                    onStopTrackingTouch();
                }
                setPressed(false);
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mDraggingThumbs != null) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private int getValue(MotionEvent event, Thumb thumb) {
        return getValue(event, event.getActionIndex(), thumb);
    }


    int getThumbOptOffset(Thumb thumb) {
        if (!mDrawThumbsApart) return 0;
        if (thumb == null || thumb.getThumb() == null) return 0;
        int thumbIdx = mThumbs.indexOf(thumb);
        if (isLayoutRtl()) {
            return (thumbIdx == mThumbs.size() - 1) ? 0 : (getThumbOptOffset(mThumbs.get(thumbIdx + 1)) + thumb.getThumb().getIntrinsicWidth());
        } else {
            return (thumbIdx == 0) ? 0 : (getThumbOptOffset(mThumbs.get(thumbIdx - 1)) + thumb.getThumb().getIntrinsicWidth());
        }
    }

    private int getValue(MotionEvent event, int pointerIndex, Thumb thumb) {
        final int width = getWidth();
        final int available = getAvailable();

        int optThumbOffset = getThumbOptOffset(thumb);

        int x = (int) event.getX(pointerIndex);
        float scale;
        float progress = mScaleMin;
        if (isLayoutRtl()) {
            if (x > width - getPaddingRight()) {
                scale = 0.0f;
            } else if (x < getPaddingLeft()) {
                scale = 1.0f;
            } else {
                scale = (float) (available - x + getPaddingLeft() + optThumbOffset) / (float) available;
                progress = mScaleMin;
            }
        } else {
            if (x < getPaddingLeft()) {
                scale = 0.0f;
            } else if (x > width - getPaddingRight()) {
                scale = 1.0f;
            } else {
                scale = (float) (x - getPaddingLeft() - optThumbOffset) / (float) available;
                progress = mScaleMin;
            }
        }

        progress += scale * getScaleSize();

        return Math.round(progress);
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch(Thumb thumb) {
        if (thumb!=null) {
            if(hasOnThumbValueChangeListener()) {
                mOnThumbValueChangeListener.onStartTrackingTouch(this, thumb, thumb.getValue());
            }
            mDraggingThumbs.add(thumb);
        }
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    void onStopTrackingTouch(Thumb thumb) {
        if (thumb!=null) {
            mDraggingThumbs.remove(thumb);
            if(hasOnThumbValueChangeListener()) {
                mOnThumbValueChangeListener.onStopTrackingTouch(this, thumb, thumb.getValue());
            }
        }
        drawableStateChanged();
    }

    void onStopTrackingTouch() {
        mDraggingThumbs.clear();
    }

    private boolean hasOnThumbValueChangeListener() {
        return mOnThumbValueChangeListener != null;
    }


//   void onKeyChange() {
//   }
//
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (isEnabled()) {
//            int progress = getProgress();
//            switch (keyCode) {
//                case KeyEvent.KEYCODE_DPAD_LEFT:
//                    if (progress <= 0) break;
//                    //setProgress(progress - mKeyProgressIncrement, true);
//                    onKeyChange();
//                    return true;
//
//                case KeyEvent.KEYCODE_DPAD_RIGHT:
//                    if (progress >= getMax()) break;
//                    //setProgress(progress + mKeyProgressIncrement, true);
//                    onKeyChange();
//                    return true;
//            }
//        }
//
//        return super.onKeyDown(keyCode, event);
//    }

//    @Override
//    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
//        super.onInitializeAccessibilityEvent(event);
//        event.setClassName(MultiSlider.class.getName());
//    }
//
//    @Override
//    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
//        super.onInitializeAccessibilityNodeInfo(info);
//        info.setClassName(MultiSlider.class.getName());
//
//        if (isEnabled()) {
//            final int progress = getProgress();
//            if (progress > 0) {
//                info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
//            }
//            if (progress < getMax()) {
//                info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
//            }
//        }
//    }
////
//    @Override
//    public boolean performAccessibilityAction(int action, Bundle arguments) {
//        if(Build.VERSION.SDK_INT>=16) {
//            if (super.performAccessibilityAction(action, arguments)) {
//                return true;
//            }
//        }
//        if (!isEnabled()) {
//            return false;
//        }
//        final int progress = getProgress();
//        final int increment = Math.max(1, Math.round((float) getMax() / 5));
//        switch (action) {
//            case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
//                if (progress <= 0) {
//                    return false;
//                }
//                //setProgress(progress - increment, true);
//                onKeyChange();
//                return true;
//            }
//            case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD: {
//                if (progress >= getMax()) {
//                    return false;
//                }
//                //setProgress(progress + increment, true);
//                onKeyChange();
//                return true;
//            }
//        }
//        return false;
//    }

//    @Override
//    public void onRtlPropertiesChanged(int layoutDirection) {
//        if(Build.VERSION.SDK_INT>=17){
//            super.onRtlPropertiesChanged(layoutDirection);
//        }
//
//        int max = getMax();
//        float scale = max > 0 ? (float) getProgress() / (float) max : 0;
//
//        Drawable thumb = mThumb;
//        if (thumb != null) {
//            setThumbPos(getWidth(), thumb, scale, Integer.MIN_VALUE);
//            /*
//             * Since we draw translated, the drawable's bounds that it signals
//             * for invalidation won't be the actual bounds we want invalidated,
//             * so just invalidate this whole view.
//             */
//            invalidate();
//        }
//    }

    public boolean isLayoutRtl() {
        if (Build.VERSION.SDK_INT >= 17) {
            return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
        }

        return false;
    }

    @Override
    public void invalidateDrawable(Drawable dr) {
        if (!mInDrawing) {
            if (verifyDrawable(dr)) {
                final Rect dirty = dr.getBounds();
                final int scrollX = getScrollX() + getPaddingLeft();
                final int scrollY = getScrollY() + getPaddingTop();

                invalidate(dirty.left + scrollX, dirty.top + scrollY,
                        dirty.right + scrollX, dirty.bottom + scrollY);
            } else {
                super.invalidateDrawable(dr);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateTrackBounds(w, h);
        for (Thumb thumb : mThumbs) {
            updateThumb(thumb, w, h);
        }
    }

    private Drawable getTintedDrawable(Drawable drawable, int tintColor) {
        if (drawable != null && tintColor != 0) {
            Drawable wrappedDrawable = DrawableCompat.wrap(drawable.mutate());
            DrawableCompat.setTint(wrappedDrawable, tintColor);
            return wrappedDrawable;
        }
        return drawable;
    }

    public static class SimpleOnThumbValueChangeListener implements OnThumbValueChangeListener {

        @Override
        public void onValueChanged(MultiSlider multiSlider, Thumb thumb, int thumbIndex, int value) {}

        @Override
        public void onStartTrackingTouch(MultiSlider multiSlider, Thumb thumb, int value) {}

        @Override
        public void onStopTrackingTouch(MultiSlider multiSlider, Thumb thumb, int value) {}

    }
}
