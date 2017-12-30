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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.LinkedList;
import java.util.List;

import io.apptik.widget.mslider.R;

import static io.apptik.widget.Util.requireNonNull;

public class MultiSlider extends View {

    /**
     * Override package access class for public access
     */
    public class Thumb extends ThumbImpl{
        public Thumb() {
            super(MultiSlider.this);
        }
    }

    private interface OnThumbValueChangeListener {
        /**
         * called when thumb value has changed
         *
         * @param multiSlider
         * @param thumb       the thumb which values has changes
         * @param thumbIndex  the index of the thumb
         * @param value       the value that has been set
         */
        void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int
                value);
    }

    private interface OnTrackingChangeListener {
        /**
         * This is called when the user has started touching this widget.
         *
         * @param multiSlider
         * @param thumb       the thumb that has been selected
         * @param value       the initial value of the thumb before any movement
         */
        void onStartTrackingTouch(MultiSlider multiSlider, MultiSlider.Thumb thumb, int value);

        /**
         * This is called when the user either releases his touch or the touch is canceled.
         *
         * @param multiSlider
         * @param thumb       the thumb that has been selected
         * @param value       the last and remaining value of the thumb after the move completes
         */
        void onStopTrackingTouch(MultiSlider multiSlider, MultiSlider.Thumb thumb, int value);
    }

    /**
     * Void listener helper
     */
    public static class SimpleChangeListener implements
            OnThumbValueChangeListener,
            OnTrackingChangeListener
    {
        @Override
        public void onValueChanged(MultiSlider multiSlider,
                                   MultiSlider.Thumb thumb,
                                   int thumbIndex,
                                   int value) {
        }

        @Override
        public void onStartTrackingTouch(MultiSlider multiSlider,
                                         MultiSlider.Thumb thumb,
                                         int value) {
        }

        @Override
        public void onStopTrackingTouch(MultiSlider multiSlider,
                                        MultiSlider.Thumb thumb,
                                        int value) {
        }
    }

    private AccessibilityNodeProvider mAccessibilityNodeProvider;
    private OnThumbValueChangeListener mOnThumbValueChangeListener;
    private OnTrackingChangeListener mOnTrackingChangeListener;

    private int mMinWidth;
    private int mMaxWidth;
    private int mMinHeight;
    private int mMaxHeight;

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

    private boolean mMirrorForRtl = true;

    /**
     * Thumb is the main object in MultiSlider.
     * There could be 0, 1 or many thumbs. Each thumb has a min and max limit and a value which
     * should always be between the limits. Each thumb defines a 'Range' the range is always
     * between the value of the Thumb back to the previous Thumb's value or to the beginning of
     * the track.
     */
    //list of all the loaded thumbs
    private final LinkedList<Thumb> mThumbs = new LinkedList<>();


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
    private final List<Thumb> mDraggingThumbs = new LinkedList<>();
    //thumbs that are currently being touched
    LinkedList<Thumb> exactTouched = null;


    private Drawable defThumbDrawable;
    private int defThumbColor = 0;
    private Drawable defRangeDrawable;
    private int defRangeColor = 0;

    private final TypedArray a;

    public MultiSlider(Context context) {
        this(context, null);
    }

    public MultiSlider(Context context, AttributeSet attrs) {
        this(context, attrs, io.apptik.widget.mslider.R.attr.multiSliderStyle);
    }

    public MultiSlider(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public MultiSlider(Context context, AttributeSet attrs, int defStyle, int styleRes) {
        super(context, attrs, defStyle);
        if ((Build.VERSION.SDK_INT >= 21) && getBackground() == null) {
            setBackgroundResource(R.drawable.control_background_multi_material);
        }

        mUiThreadId = Thread.currentThread().getId();

        a = context.obtainStyledAttributes(attrs, io.apptik.widget.mslider.R.styleable.MultiSlider,
                defStyle, styleRes);
        mNoInvalidate = true;
        int numThumbs = a.getInt(io.apptik.widget.mslider.R.styleable.MultiSlider_thumbNumber, 2);
        initMultiSlider(numThumbs);

        Drawable trackDrawable = a.getDrawable(io.apptik.widget.mslider.R.styleable
                .MultiSlider_android_track);
        if (trackDrawable == null) {
            trackDrawable = ContextCompat.getDrawable(getContext(),
                    R.drawable.multislider_track_material
            );
        }

        setTrackDrawable(getTintedDrawable(trackDrawable, a.getColor(io.apptik.widget.mslider.R
                .styleable.MultiSlider_trackColor, 0)));

        //TODO?
//        mMinWidth = a.getDimensionPixelSize(R.styleable.MultiSlider_minWidth, mMinWidth);
//        mMaxWidth = a.getDimensionPixelSize(R.styleable.MultiSlider_maxWidth, mMaxWidth);
//        mMinHeight = a.getDimensionPixelSize(R.styleable.MultiSlider_minHeight, mMinHeight);
//        mMaxHeight = a.getDimensionPixelSize(R.styleable.MultiSlider_maxHeight, mMaxHeight);


        setStep(a.getInt(io.apptik.widget.mslider.R.styleable.MultiSlider_scaleStep, mStep));
        setStepsThumbsApart(a.getInt(io.apptik.widget.mslider.R.styleable
                        .MultiSlider_stepsThumbsApart,
                mStepsThumbsApart));
        setDrawThumbsApart(a.getBoolean(io.apptik.widget.mslider.R.styleable
                        .MultiSlider_drawThumbsApart,
                mDrawThumbsApart));
        setMax(a.getInt(io.apptik.widget.mslider.R.styleable.MultiSlider_scaleMax, mScaleMax),
                true);
        setMin(a.getInt(io.apptik.widget.mslider.R.styleable.MultiSlider_scaleMin, mScaleMin),
                true);


        mMirrorForRtl = a.getBoolean(io.apptik.widget.mslider.R.styleable.MultiSlider_mirrorForRTL,
                mMirrorForRtl);

        // --> now place thumbs

        defThumbDrawable = a.getDrawable(io.apptik.widget.mslider.R.styleable
                .MultiSlider_android_thumb);

        if (defThumbDrawable == null) {
            if (Build.VERSION.SDK_INT >= 21) {
                defThumbDrawable = ContextCompat.getDrawable(getContext(), R.drawable
                        .multislider_thumb_material_anim);
            } else {
                defThumbDrawable = ContextCompat.getDrawable(getContext(), R.drawable
                        .multislider_thumb_material);
            }
        }

        defRangeDrawable = a.getDrawable(io.apptik.widget.mslider.R.styleable
                .MultiSlider_range);
        if (defRangeDrawable == null) {
            defRangeDrawable = ContextCompat.getDrawable(getContext(),
                    R.drawable.multislider_range_material
            );
        }

        Drawable range1Drawable = a.getDrawable(io.apptik.widget.mslider.R.styleable
                .MultiSlider_range1);
        Drawable range2Drawable = a.getDrawable(io.apptik.widget.mslider.R.styleable
                .MultiSlider_range2);


        defRangeColor = a.getColor(io.apptik.widget.mslider.R.styleable.MultiSlider_rangeColor, 0);
        defThumbColor = a.getColor(io.apptik.widget.mslider.R.styleable.MultiSlider_thumbColor, 0);
        setThumbDrawables(defThumbDrawable, defRangeDrawable, range1Drawable, range2Drawable); //
        // will
        // guess thumbOffset if
        // thumb != null...
        // ...but allow layout to override this

        int thumbOffset = a.getDimensionPixelOffset(io.apptik.widget.mslider.R.styleable
                .MultiSlider_android_thumbOffset, defThumbDrawable.getIntrinsicWidth() / 2);
        setThumbOffset(thumbOffset);

        repositionThumbs();

        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mNoInvalidate = false;
        a.recycle();
    }

    /**
     * @return max number of steps thumb vales can differ
     */
    public int getStepsThumbsApart() {
        return mStepsThumbsApart;
    }

    /**
     * @param stepsThumbsApart max number of steps thumb vales can differ
     */
    public void setStepsThumbsApart(int stepsThumbsApart) {
        if (stepsThumbsApart < 0) stepsThumbsApart = 0;
        this.mStepsThumbsApart = stepsThumbsApart;
    }

    /**
     * @return Step value in scale points
     */
    public int getStep() {
        return mStep;
    }

    /**
     * @param mStep Step value in scale points
     */
    public void setStep(int mStep) {
        this.mStep = mStep;
    }

    /**
     * @return number of scale points
     */
    public int getScaleSize() {
        return mScaleMax - mScaleMin;
    }

    /**
     * Re-position thumbs so they are equally distributed according to the scale
     */
    public void repositionThumbs() {
        final int count = getThumbsCount();

        if (count == 0)
            return;

        if (count > 0) {
            mThumbs.getFirst().setValue(mScaleMin);
        }

        if (count > 1) {
            mThumbs.getLast().setValue(mScaleMax);
        }

        if (count > 2) {
            int even = (mScaleMax - mScaleMin) / (count - 1);
            int lastPos = mScaleMax - even;
            for (int i = count - 2; i > 0; i--) {
                mThumbs.get(i).setValue(lastPos);
                lastPos -= even;
            }
        }
    }

    public boolean isRtlEnabled() {
        return isLayoutRtl() && mMirrorForRtl;
    }

    /**
     * Listener for value changes and start/stop of thumb move.
     *
     * @param l
     */
    public void setOnThumbValueChangeListener(OnThumbValueChangeListener l) {
        mOnThumbValueChangeListener = l;
    }

    /**
     * Listener for value changes and start/stop of thumb move.
     *
     * @param l
     */
    public void setOnTrackingChangeListener(OnTrackingChangeListener l) {
        mOnTrackingChangeListener = l;
    }

    /**
     * @return true if thumbs will be not be drawn on top of each other even in have the same
     * values, false otherwise
     */
    public boolean isDrawThumbsApart() {
        return mDrawThumbsApart;
    }

    /**
     * @param drawThumbsApart if set to true thumbs will be not be drawn on top of each other
     *                        even in have the same values.
     */
    public void setDrawThumbsApart(boolean drawThumbsApart) {
        mDrawThumbsApart = drawThumbsApart;
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

        for (int i = 0; i < numThumbs; i++) {
            Thumb thumb = new Thumb();
            thumb.setMax(getMax()).setMin(getMin()).setTag("thumb " + i);
            mThumbs.add(thumb);
        }
    }

    /**
     * Re-sets the number of thumbs and reposition the thumbs
     *
     * @param numThumbs the new number of thumbs
     * @return the MultiSlider
     */
    public MultiSlider setNumberOfThumbs(int numThumbs) {
        return setNumberOfThumbs(numThumbs, true);
    }

    /**
     * Re-sets the number of thumbs
     *
     * @param numThumbs the new number of thumbs
     * @param repositon if true it will reposition the thumbs to be equally distributed across the
     *                  scale, otherwise all thumbs will be positioned at 0
     * @return the MultiSlider
     */
    public MultiSlider setNumberOfThumbs(int numThumbs, boolean repositon) {
        clearThumbs();
        for (int i = 0; i < numThumbs; i++) {
            addThumb(0);
        }

        if (repositon) {
            repositionThumbs();
        }

        return this;
    }

    /**
     * Add a thumb to the Slider after the last thumb
     *
     * @param thumb Thumb instance in the context of the Slider
     * @return true if the thumb was added and Slider modified
     */
    public boolean addThumb(Thumb thumb) {
        return addThumbOnPos(thumb, getThumbsCount());
    }

    /**
     * Add a thumb to the Slider at a custom position
     *
     * @param thumb thumb instance in the context of the Slider
     * @param pos   the position at which the thumb should be added
     * @return true if the thumb was added and Slider modified
     */
    public boolean addThumbOnPos(Thumb thumb, int pos) {
        if (mThumbs == null)
            throw new NullPointerException(String.format("Thumbs list is null"));
        if (mThumbs.contains(thumb)) {
            return false;
        }
        if (thumb.getThumb() == null) {
            setThumbDrawable(thumb, defThumbDrawable, defThumbColor);
        }
        int paddingLeft = Math.max(getPaddingLeft(), thumb.getOffset());
        int paddingRight = Math.max(getPaddingRight(), thumb.getOffset());
        setPadding(paddingLeft, getPaddingTop(), paddingRight, getPaddingBottom());

        if (thumb.getRange() == null) {
            setRangeDrawable(thumb, defRangeDrawable, defRangeColor);
        }
        mThumbs.add(pos, thumb);
        setThumbValue(thumb, thumb.getValue(), false);
        return true;
    }

    /**
     * Add a thumb with predefined value to the slider after the last thumb
     *
     * @param value the initial thumb value
     * @return the Thumb instance that was added
     */
    public Thumb addThumb(int value) {
        Thumb thumb = new Thumb();
        this.addThumb(thumb);
        thumb.setValue(value);
        return thumb;
    }

    /**
     * Add a thumb to the slider after the last thumb with value to the maximum scale value
     *
     * @return the Thumb instance that was added
     */
    public Thumb addThumb() {
        Thumb thumb = new Thumb();
        this.addThumb(thumb);
        return thumb;
    }

    /**
     * Add a thumb to the Slider at a custom position
     *
     * @param pos the position at which the thumb should be added
     * @param value the initial thumb value
     * @return the Thumb instance that was added
     */
    public Thumb addThumbOnPos(int pos, int value) {
        Thumb thumb = new Thumb();
        thumb.setValue(value);
        this.addThumbOnPos(thumb, pos);
        return thumb;
    }

    /**
     * Add a thumb to the Slider at a custom position
     *
     * @param pos the position at which the thumb should be added
     * @return the Thumb instance that was added
     */
    public Thumb addThumbOnPos(int pos) {
        Thumb thumb = new Thumb();
        this.addThumbOnPos(thumb, pos);
        return thumb;
    }


    /**
     * Remove a thumb from the Slider
     *
     * @param thumb humb instance in the context of the Slider
     * @return true if the thumb was found and removed
     */
    public boolean removeThumb(Thumb thumb) {
        mDraggingThumbs.remove(thumb);
        boolean res = mThumbs.remove(thumb);
        invalidate();
        return res;
    }

    /**
     * Remove a thumb from the Slider identified by its position
     *
     * @param thumbIndex the thumb position starting from 0
     * @return true if the thumb was found and removed
     */
    public Thumb removeThumb(int thumbIndex) {
        mDraggingThumbs.remove(mThumbs.get(thumbIndex));
        invalidate();
        Thumb res = mThumbs.remove(thumbIndex);
        invalidate();
        return res;
    }

    /**
     * Removes all the thumbs in the Slider
     */
    public void clearThumbs() {
        mThumbs.clear();
        mDraggingThumbs.clear();
        invalidate();
    }

    /**
     * set default thumb offset, which will be immediately applied to all the thumbs
     *
     * @param offset thumb offset in pixels
     * @param invalidate require invalidation
     */
    public void setThumbOffset(int offset, boolean invalidate) {
        for (Thumb thumb : mThumbs)
            thumb.setOffset(offset);
        if (invalidate)
            invalidate();
    }

    /**
     * set default thumb offset, which will be immediately applied to all the thumbs
     *
     * @param offset thumb offset in pixels
     */
    public void setThumbOffset(int offset) {
        setThumbOffset(offset, true);
    }

    /**
     * Manually set the track drawable
     *
     * @param d
     */
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

    int getThumbIndex(final IThumb thumb) {
        if (mThumbs == null)
            throw new NullPointerException(String.format("Thumbs list is null"));
        return mThumbs.indexOf(thumb);
    }

    int getThumbsCount() {
        if (mThumbs == null)
            throw new NullPointerException(String.format("Thumbs list is null"));
        return mThumbs.size();
    }

    private int optThumbValue(final Thumb thumb, int value) {
        if (thumb == null || thumb.getThumb() == null)
            return value;

        int currIdx = getThumbIndex(thumb);
        int offset = mStepsThumbsApart * mStep;

        if (getThumbsCount() > currIdx + 1) {
            int value_tmp = getThumb(currIdx + 1).getValue() - offset;
            if (value > value_tmp)
                value = value_tmp;
        }

        if (currIdx > 0) {
            int value_tmp = getThumb(currIdx - 1).getValue() + offset;
            if (value < value_tmp)
                value = value_tmp;
        }

        if ((value - mScaleMin) % mStep != 0) {
            value += mStep - ((value - mScaleMin) % mStep);
        }

        return Math.max(thumb.getMin(), Math.min(thumb.getMax(), value));
    }


    /**
     * Refreshes the value for the specific thumb
     *
     * @param thumb    the thumb which value is going to be changed
     * @param value    the new value
     * @param fromUser if the request is coming from the user or the client
     */
    private synchronized void setThumbValue(Thumb thumb, int value, boolean fromUser) {
        if (thumb == null || thumb.getThumb() == null) return;

        value = optThumbValue(thumb, value);

        if (value != thumb.getValue())
            thumb.setValue(value);

        if (hasOnThumbValueChangeListener()) {
            mOnThumbValueChangeListener.onValueChanged(this, thumb, getThumbIndex(thumb), thumb
                    .getValue());
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
     * used as the new thumb offset (@see #setOffset(int)).
     *
     * @param thumb Drawable representing the thumb
     */
    private void setThumbDrawables(Drawable thumb, Drawable range,
                                   Drawable range1, Drawable range2) {
        if (thumb == null) return;
        Drawable rangeDrawable;

        // This way, calling setThumbDrawables again with the same bitmap will result in
        // it recalculating thumbOffset (if for example it the bounds of the
        // drawable changed)
        int curr = 0;
        int padding = 0;
        int rCol;
        for (Thumb mThumb : mThumbs) {
            curr++;
            if (mThumb.getThumb() != null && thumb != mThumb.getThumb()) {
                mThumb.getThumb().setCallback(null);
            }
            if (curr == 1 && range1 != null) {
                rangeDrawable = range1;
                rCol = a.getColor(io.apptik.widget.mslider.R.styleable.MultiSlider_range1Color, 0);
            } else if (curr == 2 && range2 != null) {
                rangeDrawable = range2;
                rCol = a.getColor(io.apptik.widget.mslider.R.styleable.MultiSlider_range2Color, 0);
            } else {
                rangeDrawable = range;
                rCol = defRangeColor;
            }

            setRangeDrawable(mThumb, rangeDrawable, rCol);
            setThumbDrawable(mThumb, thumb, defThumbColor);
            padding = Math.max(padding, mThumb.getOffset());
        }
        setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
    }

    private void setThumbDrawable(Thumb thumb, Drawable thumbDrawable, int thumbColor) {
        requireNonNull(thumbDrawable);
        Drawable nThumbDrawable = getTintedDrawable(thumbDrawable.getConstantState().newDrawable(),
                thumbColor);
        nThumbDrawable.setCallback(this);

        // Assuming the thumb drawable is symmetric, set the thumb offset
        // such that the thumb will hang halfway off either edge of the
        // progress bar.
        thumb.setOffset(thumbDrawable.getIntrinsicWidth() / 2);

        // If we're updating get the new states
        if (thumb.getThumb() != null && (nThumbDrawable.getIntrinsicWidth() != thumb.getThumb()
                .getIntrinsicWidth()
                || nThumbDrawable.getIntrinsicHeight() != thumb.getThumb().getIntrinsicHeight())) {
            requestLayout();
        }
        thumb.setThumb(nThumbDrawable);

        invalidate();
        if (nThumbDrawable.isStateful()) {
            // Note that if the states are different this won't work.
            // For now, let's consider that an app bug.
            int[] state = getDrawableState();
            nThumbDrawable.setState(state);
        }
    }

    private void setRangeDrawable(Thumb thumb, Drawable rangeDrawable, int rangeColor) {
        requireNonNull(rangeDrawable);
        Drawable nRangeDrawable = getTintedDrawable(rangeDrawable, rangeColor);
        thumb.setRange(nRangeDrawable);
    }

    /**
     * Return the Thumb by its positions - the component that
     * the user can drag back and forth.
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

    /**
     * Set global maximum value and apply it to all thumbs
     *
     * @param max maximum value in scale points
     */
    public synchronized void setMax(int max) {
        setMax(max, true, false);
    }

    /**
     * Set global maximum value
     *
     * @param max                maximum value in scale points
     * @param extendMaxForThumbs if set to true the new max will be applied to all the thumbs.
     */
    public synchronized void setMax(int max, boolean extendMaxForThumbs) {
        setMax(max, extendMaxForThumbs, false);
    }

    /**
     * Set global maximum value
     *
     * @param max                maximum value in scale points
     * @param extendMaxForThumbs if set to true the new max will be applied to all the thumbs.
     * @param repositionThumbs   if set to true the thumbs will change their value and be placed on
     *                           equal distances from each other respecting the new scale
     */
    public synchronized void setMax(int max, boolean extendMaxForThumbs, boolean repositionThumbs) {
        if (max < mScaleMin) {
            throw new IllegalArgumentException(String.format("setMax(%d) < Min(%d)", max, mScaleMin));
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
                repositionThumbs();

            postInvalidate();
        }

        if ((mKeyProgressIncrement == 0) || (mScaleMax / mKeyProgressIncrement > 20)) {
            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            setKeyProgressIncrement(Math.max(1, Math.round((float) mScaleMax / 20)));
        }
    }

    public int getMax() {
        return mScaleMax;
    }

    /**
     * Set global minimum value and apply it to all thumbs
     *
     * @param min minimum value in scale points
     */
    public synchronized void setMin(int min) {
        setMin(min, true, false);
    }

    /**
     * Set global minimum value
     *
     * @param min                minimum value in scale points
     * @param extendMinForThumbs if set to true the new min will be applied to all the thumbs.
     */
    public synchronized void setMin(int min, boolean extendMinForThumbs) {
        setMin(min, extendMinForThumbs, false);
    }

    /**
     * Set global minimum value
     *
     * @param min                minimum value in scale points
     * @param extendMinForThumbs if set to true the new min will be applied to all the thumbs.
     * @param repositionThumbs   if set to true the thumbs will change their value and be placed on
     *                           equal distances from each other respecting the new scale
     */
    public synchronized void setMin(int min, boolean extendMinForThumbs, boolean repositionThumbs) {
        if (min > mScaleMax) {
            throw new IllegalArgumentException(String.format("setMin(%d) > Max(%d)", min, mScaleMax));
        }

        if (min != mScaleMin) {
            mScaleMin = min;

            //check for thumbs out of bounds and adjust the max for those exceeding the new one
            for (Thumb thumb : mThumbs) {
                if (extendMinForThumbs) {
                    thumb.setMin(min);
                } else if (thumb.getMin() < min) {
                    thumb.setMin(min);
                }

                if (thumb.getValue() < min) {
                    setThumbValue(thumb, min, false);
                }

            }
            if (repositionThumbs)
                repositionThumbs();

            postInvalidate();
        }

        if ((mKeyProgressIncrement == 0) || (mScaleMax / mKeyProgressIncrement > 20)) {
            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            setKeyProgressIncrement(Math.max(1, Math.round((float) mScaleMax / 20)));
        }
    }

    public int getMin() {
        return mScaleMin;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        for (Thumb thumb : mThumbs)
            if (who == thumb.getThumb())
                return true;
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
        synchronized (mDraggingThumbs) {
            if (!mDraggingThumbs.isEmpty()) {
                int[] state = getDrawableState();
                for (Thumb thumb : mDraggingThumbs) {
                    if (thumb.getThumb() != null) {
                        thumb.getThumb().setState(state);
                    }
                }
                for (Thumb thumb : mThumbs) {
                    if (!mDraggingThumbs.contains(thumb) && thumb.getThumb() != null && thumb
                            .getThumb().isStateful()) {
                        if (thumb.isEnabled()) {
                            thumb.getThumb().setState(new int[]{android.R.attr.state_enabled,
                                    -android.R.attr.state_pressed});
                        } else {
                            thumb.getThumb().setState(new int[]{-android.R.attr.state_enabled});
                        }
                    }
                }
            } else {
                for (Thumb thumb : mThumbs) {
                    if (thumb.getThumb() != null && thumb.getThumb().isStateful()) {
                        if (thumb.isEnabled()) {
                            thumb.getThumb().setState(new int[]{android.R.attr.state_enabled,
                                    -android.R.attr.state_pressed});
                        } else {
                            thumb.getThumb().setState(new int[]{-android.R.attr.state_enabled});
                        }
                    }
                }
            }
        }
        super.drawableStateChanged();
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
        int currIdx = getThumbIndex(thumb);
        if (currIdx > 0) {
            prevThumb = mThumbs.get(currIdx - 1).getThumb();
        }

        if (thumbHeight > trackHeight) {
            if (thumb != null) {
                setThumbPos(w, h, thumb.getThumb(), prevThumb, thumb.getRange(), scale, 0, thumb
                        .getOffset(), getThumbOptOffset(thumb));
            }
            int gapForCenteringTrack = (thumbHeight - trackHeight) / 2;
            if (mTrack != null) {
                // Canvas will be translated by the padding, so 0,0 is where we start drawing
                mTrack.setBounds(0, gapForCenteringTrack,
                        w - getPaddingRight() - getPaddingLeft(), h - getPaddingBottom() -
                                gapForCenteringTrack
                                - getPaddingTop());
            }
        } else {
            if (mTrack != null) {
                // Canvas will be translated by the padding, so 0,0 is where we start drawing
                mTrack.setBounds(0, 0, w - getPaddingRight() - getPaddingLeft(), h -
                        getPaddingBottom()
                        - getPaddingTop());
            }
            int gap = (trackHeight - thumbHeight) / 2;
            if (thumb != null) {
                setThumbPos(w, h, thumb.getThumb(), prevThumb, thumb.getRange(), scale, gap,
                        thumb.getOffset(), getThumbOptOffset(thumb));
            }
        }

        //update thumbs after it
        for (int i = currIdx + 1; i < getThumbsCount(); ++i) {
            int gap = (trackHeight - thumbHeight) / 2;
            scale = getScaleSize() > 0 ? (float) getThumb(i).getValue() / (float) getScaleSize
                    () : 0;
            setThumbPos(w, h, getThumb(i).getThumb(), getThumb(i - 1).getThumb(),
                    getThumb(i).getRange(), scale, gap, getThumb(i).getOffset(),
                    getThumbOptOffset(getThumb(i)));
        }

        if (thumb != null)
            thumb.updateViewBinderPosition();
    }


    /**
     * @param gap If set to {@link Integer#MIN_VALUE}, this will be ignored and
     */
    private void setThumbPos(int w, int h, Drawable thumb, Drawable prevThumb, Drawable range,
                             float scale, int gap, int thumbOffset, int optThumbOffset) {
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

        final int thumbStart = isRtlEnabled() ?
                available - thumbPos + optThumbOffset : thumbPos + optThumbOffset;

        thumb.setBounds(thumbStart, topBound, thumbStart + thumbWidth, bottomBound);

        int bottom = h - getPaddingTop() + getPaddingBottom();

        int rangeStart = 0;
        if (isRtlEnabled()) {
            rangeStart = available;
        }
        if (prevThumb != null) {
            rangeStart = prevThumb.getBounds().left;
        }
        if (range != null) {
            if (isRtlEnabled()) {
                range.setBounds(thumbStart, 0, rangeStart + optThumbOffset, bottom);
            } else {
                range.setBounds(rangeStart, 0, thumbStart, bottom);
            }
        }

        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int paddingStart;
        if (Build.VERSION.SDK_INT >= 17) {
            paddingStart = getPaddingStart();
        } else {
            paddingStart = getPaddingLeft();
        }
        // --> draw track
        if (mTrack != null) {
            canvas.save();
            canvas.translate(paddingStart, getPaddingTop());
            mTrack.draw(canvas);
            canvas.restore();
        }

        // --> draw ranges

        for (Thumb thumb : mThumbs) {
            if (thumb.getRange() != null) {
                canvas.save();
                canvas.translate(paddingStart, getPaddingTop());
                thumb.getRange().draw(canvas);
                canvas.restore();
            }
        }

        // --> then draw thumbs
        for (Thumb thumb : mThumbs) {
            if (thumb.getThumb() != null && !thumb.isInvisible()) {
                canvas.save();
                // Translate the padding. For the x, we need to allow the thumb to
                // draw in its extra space
                canvas.translate(paddingStart - thumb.getOffset(), getPaddingTop());
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
        if (getThumbsCount() > 0) {
            if (isRtlEnabled()) {
                available -= getThumbOptOffset(mThumbs.getFirst());
            } else {
                available -= getThumbOptOffset(mThumbs.getLast());
            }
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
            if (thumb.getThumb() == null || !thumb.isEnabled()
                    || mDraggingThumbs.contains(thumb)) continue;

            int minV = x - thumb.getThumb().getIntrinsicWidth();
            int maxV = x + thumb.getThumb().getIntrinsicWidth();
            if (thumb.getThumb().getBounds().centerX() >= minV && thumb.getThumb().getBounds()
                    .centerX() <= maxV) {
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
                if (thumb.getThumb() == null || !thumb.isEnabled()
                        || mDraggingThumbs.contains(thumb)) continue;
                int optValue = (getValue(event, thumbs.getFirst()) > thumb.getValue()) ?
                        mScaleMax : mScaleMin;
                int currChange = Math.abs(thumb.getValue() - optThumbValue(thumb, optValue));
                if (currChange > maxChange) {
                    maxChange = currChange;
                    res = thumb;
                }
            }
        }
        return res;
    }

    private Thumb getMostMovableThumb(MotionEvent event) {
        if (exactTouched == null || exactTouched.size() < 1)
            return null;
        if (exactTouched.size() == 1) {
            return exactTouched.getFirst();
        } else {
            return getMostMovable(exactTouched, event);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsUserSeekable || !isEnabled()) {
            return false;
        }
        final int xx = Math.round(event.getX());
        final int yy = Math.round(event.getY());

        int pointerIdx = event.getActionIndex();

        Thumb currThumb = null;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            LinkedList<Thumb> closestOnes =
                    getClosestThumb((int) event.getX(pointerIdx));

            if (isInScrollingContainer() && mDraggingThumbs.size() == 0 &&
                    exactTouched != null && pointerIdx > 0) {
                //we have been here before => we want to use the bar
                Thumb prevThumb = exactTouched.getFirst();
                onStartTrackingTouch(prevThumb);
                exactTouched = null;
            }

            if (closestOnes != null && !closestOnes.isEmpty()) {
                if (closestOnes.size() == 1) {
                    currThumb = closestOnes.getFirst();
                    if (isInScrollingContainer() && mDraggingThumbs.size() == 0) {
                        exactTouched = closestOnes;
                    }
                } else {
                    //we have more than one thumb at the same place and we touched there
                    exactTouched = closestOnes;
                }
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (exactTouched != null && !exactTouched.isEmpty()) {
                currThumb = getMostMovableThumb(event);
                //check if move actually changed value
                // if (currThumb == null) return false;
            } else if (mDraggingThumbs.size() > pointerIdx) {
                currThumb = mDraggingThumbs.get(pointerIdx);
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
            if (mDraggingThumbs.size() > pointerIdx) {
                currThumb = mDraggingThumbs.get(pointerIdx);
            } //else we had a candidate but was never tracked
            else if (exactTouched != null && exactTouched.size() > 0) {
                currThumb = getMostMovableThumb(event);
                exactTouched = null;
            }
        }
//        else {
//            LinkedList<Thumb> closestOnes = getClosestThumb((int) event.getX());
//            currThumb = closestOnes.getFirst();
//        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (isInScrollingContainer() && mDraggingThumbs.size() == 0) {
                    mTouchDownX = event.getX(pointerIdx);
                } else {
                    onStartTrackingTouch(currThumb);
                    setThumbValue(currThumb, getValue(event, currThumb), true);
                    setHotspot(xx, yy, currThumb);
                }
                break;
            //with move we dont have pointer action so set them all
            case MotionEvent.ACTION_MOVE:
                if (mDraggingThumbs.contains(currThumb)) {
                    //need the index
                    int idx = 0;
                    for (Thumb thumb: mDraggingThumbs) {
                        if (thumb != null) {
                            if (thumb.getThumb() != null)
                                invalidate(thumb.getThumb().getBounds());

                            setThumbValue(thumb, getValue(event, idx, thumb), true);
                        }
                        ++idx;
                    }
                    setHotspot(xx, yy, currThumb);
                } else {
                    final float x = event.getX(pointerIdx);
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        onStartTrackingTouch(currThumb);
                        exactTouched = null;
                        setThumbValue(currThumb, getValue(event, currThumb), true);
                        setHotspot(xx, yy, currThumb);
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                //there are other pointers left
            case MotionEvent.ACTION_POINTER_UP:
                if (currThumb != null) {
                    setThumbValue(currThumb, getValue(event, currThumb), true);
                    setHotspot(xx, yy, currThumb);
                    if (!isPressed()) {
                        setPressed(true);
                    }

                    onStopTrackingTouch(currThumb);
                }
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                onStopTrackingTouch();
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private int getValue(MotionEvent event, Thumb thumb) {
        return getValue(event, event.getActionIndex(), thumb);
    }

    private void setHotspot(float x, float y, Thumb thumb) {
        if (thumb == null || thumb.getThumb() == null) return;
        final Drawable background = getBackground();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && background != null) {
            background.setHotspot(x, y);
            Rect rect = thumb.getThumb().getBounds();
            final int offsetY = getPaddingTop();
            background.setHotspotBounds(rect.left, rect.top + offsetY,
                    rect.right, rect.bottom + offsetY);
        }
    }

    private int getThumbOptOffset(Thumb thumb) {
        if (!mDrawThumbsApart)
            return 0;

        if (thumb == null || thumb.getThumb() == null)
            return 0;

        int thumbIdx = getThumbIndex(thumb);

        if (isRtlEnabled()) {
            if (++thumbIdx == getThumbsCount())
                return 0;
        } else {
            if (thumbIdx-- == 0)
                return 0;
        }

        return getThumbOptOffset(getThumb(thumbIdx)) + thumb.getThumb().getIntrinsicWidth();
    }

    private int getValue(MotionEvent event, int pointerIndex, Thumb thumb) {
        final int width = getWidth();
        final int available = getAvailable();

        int optThumbOffset = getThumbOptOffset(thumb);

        int x = (int) event.getX(pointerIndex);
        float scale;
        float progress = mScaleMin;
        if (isRtlEnabled()) {
            if (x > width - getPaddingRight()) {
                scale = 0.0f;
            } else if (x < getPaddingLeft()) {
                scale = 1.0f;
            } else {
                scale = (float) (available - x + getPaddingLeft() + optThumbOffset) / (float)
                        available;
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
        if (thumb != null) {
            mDraggingThumbs.add(thumb);
            if (isPressed()) {
                drawableStateChanged();
            } else {
                setPressed(true);
            }

            if (thumb.getThumb() != null) {
                // This may be within the padding region.
                invalidate(thumb.getThumb().getBounds());
            }
            if (hasOnTrackingChangeListener()) {
                mOnTrackingChangeListener.onStartTrackingTouch(this, thumb, thumb.getValue());
            }
            attemptClaimDrag();
        }
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    void onStopTrackingTouch(Thumb thumb) {
        if (thumb != null) {
            mDraggingThumbs.remove(thumb);
            if (hasOnTrackingChangeListener()) {
                mOnTrackingChangeListener.onStopTrackingTouch(this, thumb, thumb.getValue());
            }
            if (mDraggingThumbs.size() == 0) {
                setPressed(false);
            } else {
                drawableStateChanged();
            }
        }
    }

    void onStopTrackingTouch() {
        for (Thumb thumb : mDraggingThumbs) {
            mDraggingThumbs.remove(thumb);
            if (hasOnTrackingChangeListener()) {
                mOnTrackingChangeListener.onStopTrackingTouch(this, thumb, thumb.getValue());
            }
        }
        setPressed(false);
    }

    private boolean hasOnThumbValueChangeListener() {
        return mOnThumbValueChangeListener != null;
    }

    private boolean hasOnTrackingChangeListener() {
        return mOnTrackingChangeListener != null;
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

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (mAccessibilityNodeProvider == null) {
            mAccessibilityNodeProvider = new VirtualTreeProvider(this);
        }
        return mAccessibilityNodeProvider;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(MultiSlider.class.getName());
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (Build.VERSION.SDK_INT >= 17) {
            super.onRtlPropertiesChanged(layoutDirection);
            invalidate();
        }
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
    }

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
}
