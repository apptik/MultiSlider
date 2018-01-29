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
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.apptik.widget.mslider.R;

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD;
import static io.apptik.widget.Util.requireNonNull;

public class MultiSlider extends View {

    public interface OnThumbValueChangeListener {
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

    public interface OnTrackingChangeListener {
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

    private AccessibilityNodeProvider mAccessibilityNodeProvider;
    private OnThumbValueChangeListener mOnThumbValueChangeListener;
    private OnTrackingChangeListener mOnTrackingChangeListener;

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

    boolean mMirrorForRtl = true;

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

    /**
     * Thumb is the main object in MultiSlider.
     * There could be 0, 1 or many thumbs. Each thumb has a min and max limit and a value which
     * should always be between the limits. Each thumb defines a 'Range' the range is always
     * between the value of the Thumb back to the previous Thumb's value or to the beginning of
     * the track.
     */
    public class Thumb {
        //abs min value for this thumb
        int min;
        //abs max value for this thumb
        int max;
        //current value of this thumb
        int value;
        //thumb tag. can be used for identifying the thumb
        String tag = "thumb";
        //thumb drawable, can be shared
        Drawable thumb;
        //thumb range drawable, can also be shared
        //this is the line from the beginning or the previous thumb if any until the this one.
        Drawable range;
        int thumbOffset;

        //cannot be moved if invisible and it is not displayed
        private boolean isInvisible = false;

        //cannot be moved if not enabled
        private boolean isEnabled = true;

        public Thumb() {
            min = mScaleMin;
            max = mScaleMax;
            value = max;
        }

        /**
         * @return the range drawable
         */
        public Drawable getRange() {
            return range;
        }

        /**
         * Set the range drawable
         *
         * @param range
         * @return
         */
        public final Thumb setRange(Drawable range) {
            this.range = range;
            return this;
        }

        public boolean isEnabled() {
            return !isInvisibleThumb() && isEnabled;
        }

        public Thumb setEnabled(boolean enabled) {
            isEnabled = enabled;
            if (getThumb() != null) {
                if (isEnabled()) {
                    getThumb().setState(new int[]{android.R.attr.state_enabled});
                } else {
                    getThumb().setState(new int[]{-android.R.attr.state_enabled});
                }
            }
            return this;
        }

        /**
         * @return true is the thumb is invisible, false otherwise
         */
        public boolean isInvisibleThumb() {
            return isInvisible;
        }

        /**
         * Sets thumb's visibility
         *
         * @param invisibleThumb
         */
        public void setInvisibleThumb(boolean invisibleThumb) {
            this.isInvisible = invisibleThumb;
        }

        /**
         * @return the minimum value a thumb can obtain depending on other thumbs before it
         */
        public int getPossibleMin() {
            int res = min;
            res += mThumbs.indexOf(this) * mStepsThumbsApart;
            return res;
        }

        /**
         * @return the maximum value a thumb can have depending the thumbs after it
         */
        public int getPossibleMax() {
            int res = max;
            res -= (mThumbs.size() - 1 - mThumbs.indexOf(this)) * mStepsThumbsApart;
            return res;
        }

        /**
         * @return the minimum value a thumb can have regardless of the thumbs after it
         */
        public int getMin() {
            return min;
        }

        /**
         * @param min the minimum value a thumb can have
         * @return
         */
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

        /**
         * @return the maximum value a thumb can have regardless of the thumbs after it
         */
        public int getMax() {
            return max;
        }

        /**
         * @param max he maximum value a thumb can have
         * @return
         */
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

        /**
         * @return Thumb's current value
         */
        public int getValue() {
            return value;
        }

        /**
         * Manually set a thumb value
         *
         * @param value
         * @return
         */
        public Thumb setValue(int value) {
            if (mThumbs.contains(this)) {
                setThumbValue(this, value, false);
            } else {
                this.value = value;
            }
            return this;
        }

        public String getTag() {
            return tag;
        }

        public Thumb setTag(String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * @return The thumb drawable
         */
        public Drawable getThumb() {
            return thumb;
        }

        /**
         * @param mThumb the thumb drawable
         * @return
         */
        public Thumb setThumb(Drawable mThumb) {
            this.thumb = mThumb;
            return this;
        }

        /**
         * @return thumb offset in pixels
         */
        public int getThumbOffset() {
            return thumbOffset;
        }

        /**
         * @param mThumbOffset thumb offset in pixels
         * @return
         */
        public Thumb setThumbOffset(int mThumbOffset) {
            this.thumbOffset = mThumbOffset;
            return this;
        }


    }

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

        for (int i = 0; i < numThumbs; i++) {
            mThumbs.add(new Thumb().setMin(mScaleMin).setMax(mScaleMax).setTag("thumb " + i));
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
        return addThumbOnPos(thumb, mThumbs.size());
    }

    /**
     * Add a thumb to the Slider at a custom position
     *
     * @param thumb thumb instance in the context of the Slider
     * @param pos   the position at which the thumb should be added
     * @return true if the thumb was added and Slider modified
     */
    public boolean addThumbOnPos(Thumb thumb, int pos) {
        if (mThumbs.contains(thumb)) {
            return false;
        }
        if (thumb.getThumb() == null) {
            setThumbDrawable(thumb, defThumbDrawable, defThumbColor);
        }
        int paddingLeft = Math.max(getPaddingLeft(), thumb.getThumbOffset());
        int paddingRight = Math.max(getPaddingRight(), thumb.getThumbOffset());
        setPadding(paddingLeft, getPaddingTop(), paddingRight, getPaddingBottom());

        if (thumb.getRange() == null) {
            setRangeDrawable(thumb, defRangeDrawable, defRangeColor);
        }
        mThumbs.add(pos, thumb);
        setThumbValue(thumb, thumb.value, false);
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
     * @param pos   the position at which the thumb should be added
     * @param value the initial thumb value
     * @return the Thumb instance that was added
     */
    public Thumb addThumbOnPos(int pos, int value) {
        Thumb thumb = new Thumb();
        this.addThumbOnPos(thumb, pos);
        thumb.setValue(value);

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
     * @param thumbOffset thumb offset in pixels
     */
    public void setThumbOffset(int thumbOffset) {
        for (Thumb thumb : mThumbs) {
            thumb.setThumbOffset(thumbOffset);
        }
        invalidate();
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

    private int optThumbValue(Thumb thumb, int value) {
        if (thumb == null || thumb.getThumb() == null) return value;
        int currIdx = mThumbs.indexOf(thumb);


        if (mThumbs.size() > currIdx + 1 && value > mThumbs.get(currIdx + 1).getValue() -
                mStepsThumbsApart * mStep) {
            value = mThumbs.get(currIdx + 1).getValue() - mStepsThumbsApart * mStep;
        }

        if (currIdx > 0 && value < mThumbs.get(currIdx - 1).getValue() + mStepsThumbsApart *
                mStep) {
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
     * @param fromUser if the request is coming from the user or the client
     */
    private synchronized void setThumbValue(Thumb thumb, int value, boolean fromUser) {
        if (thumb == null || thumb.getThumb() == null) return;

        value = optThumbValue(thumb, value);

        if (value != thumb.getValue()) {
            thumb.value = value;
        }
        if (hasOnThumbValueChangeListener()) {
            mOnThumbValueChangeListener.onValueChanged(this, thumb, mThumbs.indexOf(thumb), thumb
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
     * used as the new thumb offset (@see #setThumbOffset(int)).
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
            padding = Math.max(padding, mThumb.getThumbOffset());
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
        thumb.setThumbOffset(thumbDrawable.getIntrinsicWidth() / 2);

        // If we're updating get the new states
        if (thumb.getThumb() != null && (nThumbDrawable.getIntrinsicWidth() != thumb.getThumb()
                .getIntrinsicWidth()
                || nThumbDrawable.getIntrinsicHeight() != thumb.getThumb().getIntrinsicHeight())) {
            requestLayout();
        }
        thumb.setThumb(nThumbDrawable);

        invalidate();
        if (nThumbDrawable != null && nThumbDrawable.isStateful()) {
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
        int currIdx = mThumbs.indexOf(thumb);
        if (currIdx > 0) {
            prevThumb = mThumbs.get(currIdx - 1).getThumb();
        }

        if (thumbHeight > trackHeight) {
            if (thumb != null) {
                setThumbPos(w, h, thumb.getThumb(), prevThumb, thumb.getRange(), scale, 0, thumb
                        .getThumbOffset(), getThumbOptOffset(thumb));
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
                        thumb.getThumbOffset(), getThumbOptOffset(thumb));
            }
        }

        //update thumbs after it
        for (int i = currIdx + 1; i < mThumbs.size(); i++) {
            int gap = (trackHeight - thumbHeight) / 2;
            scale = getScaleSize() > 0 ? (float) mThumbs.get(i).getValue() / (float) getScaleSize
                    () : 0;
            setThumbPos(w, h, mThumbs.get(i).getThumb(), mThumbs.get(i - 1).getThumb(), mThumbs
                            .get(i).getRange(), scale, gap, mThumbs.get(i).getThumbOffset(),
                    getThumbOptOffset(mThumbs.get(i)));
        }
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

        final int thumbStart = (isLayoutRtl() && mMirrorForRtl) ?
                available - thumbPos + optThumbOffset : thumbPos + optThumbOffset;

        thumb.setBounds(thumbStart, topBound, thumbStart + thumbWidth, bottomBound);

        int bottom = h - getPaddingTop() + getPaddingBottom();

        int rangeStart = 0;
        if (isLayoutRtl() && mMirrorForRtl) {
            rangeStart = available;
        }
        if (prevThumb != null) {
            rangeStart = prevThumb.getBounds().left;
        }
        if (range != null) {
            if (isLayoutRtl() && mMirrorForRtl) {
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
                canvas.translate(paddingStart, getPaddingTop() - getPaddingBottom());
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
                canvas.translate(paddingStart - thumb.getThumbOffset(), getPaddingTop());
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
            if (isLayoutRtl() && mMirrorForRtl) {
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
                    for (int i = 0; i < mDraggingThumbs.size(); i++) {
                        if (mDraggingThumbs.get(i) != null && mDraggingThumbs.get(i).getThumb()
                                != null) {
                            invalidate(mDraggingThumbs.get(i).getThumb().getBounds());
                        }
                        setThumbValue(mDraggingThumbs.get(i), getValue(event, i, mDraggingThumbs
                                .get(i)), true);


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

    int getThumbOptOffset(Thumb thumb) {
        if (!mDrawThumbsApart) return 0;
        if (thumb == null || thumb.getThumb() == null) return 0;
        int thumbIdx = mThumbs.indexOf(thumb);
        if (isLayoutRtl() && mMirrorForRtl) {
            return (thumbIdx == mThumbs.size() - 1) ? 0 : (getThumbOptOffset(mThumbs.get(thumbIdx
                    + 1)) + thumb.getThumb().getIntrinsicWidth());
        } else {
            return (thumbIdx == 0) ? 0 : (getThumbOptOffset(mThumbs.get(thumbIdx - 1)) + thumb
                    .getThumb().getIntrinsicWidth());
        }
    }

    private int getValue(MotionEvent event, int pointerIndex, Thumb thumb) {
        final int width = getWidth();
        final int available = getAvailable();

        int optThumbOffset = getThumbOptOffset(thumb);

        int x = (int) event.getX(pointerIndex);
        float scale;
        float progress = mScaleMin;
        if (isLayoutRtl() && mMirrorForRtl) {
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

    //

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
            mAccessibilityNodeProvider = new VirtualTreeProvider();
        }
        return mAccessibilityNodeProvider;
    }

    //
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

    /**
     * Void listener helper
     */
    public static class SimpleChangeListener implements OnThumbValueChangeListener,
            OnTrackingChangeListener {

        @Override
        public void onValueChanged(MultiSlider multiSlider, Thumb thumb, int thumbIndex, int
                value) {
        }

        @Override
        public void onStartTrackingTouch(MultiSlider multiSlider, Thumb thumb, int value) {
        }

        @Override
        public void onStopTrackingTouch(MultiSlider multiSlider, Thumb thumb, int value) {
        }

    }

    class VirtualTreeProvider extends AccessibilityNodeProvider {
        static final int ACT_SET_PROGRESS = 16908349;
        final AccessibilityNodeInfo.AccessibilityAction ACTION_SET_PROGRESS;

        public VirtualTreeProvider() {
            if (Build.VERSION.SDK_INT >= 21) {
                ACTION_SET_PROGRESS =
                        new AccessibilityNodeInfo.AccessibilityAction(ACT_SET_PROGRESS, null);
            } else {
                ACTION_SET_PROGRESS = null;
            }
        }

        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int thumbId) {
            AccessibilityNodeInfo info = null;
            if (thumbId == View.NO_ID) {
                // We are requested to create an AccessibilityNodeInfo describing
                // this View, i.e. the root of the virtual sub-tree. Note that the
                // host View has an AccessibilityNodeProvider which means that this
                // provider is responsible for creating the node info for that root.
                info = AccessibilityNodeInfo.obtain(MultiSlider.this);
                onInitializeAccessibilityNodeInfo(info);
                // Add the virtual children of the root View.
                final int childCount = mThumbs.size();
                for (int i = 0; i < childCount; i++) {
                    info.addChild(MultiSlider.this, i);
                }
                if (mThumbs.size() == 1) {
                    info.setScrollable(true);
                    if (Build.VERSION.SDK_INT >= 21) {
                        info.addAction(ACTION_SET_PROGRESS);
                        info.addAction(ACTION_SCROLL_BACKWARD);
                        info.addAction(ACTION_SCROLL_FORWARD);
                    } else {
                        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    }

                }

            } else {
                // Find the view that corresponds to the given id.
                Thumb thumb = mThumbs.get(thumbId);
                if (thumb == null) {
                    return null;
                }
                // Obtain and initialize an AccessibilityNodeInfo with
                // information about the virtual view.
                info = AccessibilityNodeInfo.obtain(MultiSlider.this, thumbId);
                info.setClassName(thumb.getClass().getName());
                info.setParent(MultiSlider.this);
                info.setSource(MultiSlider.this, thumbId);
                info.setContentDescription("Multi-Slider thumb no:" + thumbId);

                if (Build.VERSION.SDK_INT >= 21) {
                    info.addAction(ACTION_SET_PROGRESS);
                    if (thumb.getPossibleMax() > thumb.value) {
                        info.addAction(ACTION_SCROLL_BACKWARD);
                    }
                    if (thumb.getPossibleMax() > thumb.value) {
                        info.addAction(ACTION_SCROLL_FORWARD);
                    }

                } else {
                    if (thumb.getPossibleMin() > thumb.value) {
                        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                    }
                    if (thumb.getPossibleMax() > thumb.value) {
                        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    }
                }


                if (thumb.getThumb() != null) {
                    int[] loc = new int[2];
                    getLocationOnScreen(loc);
                    Rect rect = thumb.getThumb().copyBounds();
                    rect.top += loc[1];
                    rect.left += loc[0];
                    rect.right += loc[0];
                    rect.bottom += loc[1];
                    info.setBoundsInScreen(rect);
                    //TODO somehow this resuls in [0,0][0,0]. wonder check why
                    //info.setBoundsInParent(rect);

                }

                info.setText(thumb.tag + ": " + thumb.value);
                info.setEnabled(thumb.isEnabled());
                if (Build.VERSION.SDK_INT >= 24) {
                    info.setImportantForAccessibility(true);
                }
                info.setVisibleToUser(true);
                info.setScrollable(true);
            }
            return info;
        }

        @Override
        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(
                String searched, int virtualViewId) {
            if (TextUtils.isEmpty(searched)) {
                return Collections.emptyList();
            }
            String searchedLowerCase = searched.toLowerCase();
            List<AccessibilityNodeInfo> result = null;
            if (virtualViewId == View.NO_ID) {
                // If the search is from the root, i.e. this View, go over the virtual
                // children and look for ones that contain the searched string since
                // this View does not contain text itself.
                final int childCount = mThumbs.size();
                for (int i = 0; i < childCount; i++) {
                    Thumb child = mThumbs.get(i);
                    String textToLowerCase = child.tag.toLowerCase();
                    if (textToLowerCase.contains(searchedLowerCase)) {
                        if (result == null) {
                            result = new ArrayList<>();
                        }
                        result.add(createAccessibilityNodeInfo(i));
                    }
                }
            } else {
                // If the search is from a virtual view, find the view. Since the tree
                // is one level deep we add a node info for the child to the result if
                // the child contains the searched text.
                Thumb virtualView = mThumbs.get(virtualViewId);
                if (virtualView != null) {
                    String textToLowerCase = virtualView.tag.toLowerCase();
                    if (textToLowerCase.contains(searchedLowerCase)) {
                        result = new ArrayList<>();
                        result.add(createAccessibilityNodeInfo(virtualViewId));
                    }
                }
            }
            if (result == null) {
                return Collections.emptyList();
            }
            return result;
        }

        @Override
        public AccessibilityNodeInfo findFocus(int focus) {
            return super.findFocus(focus);
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            if (virtualViewId == View.NO_ID) {
                //do nothing ..  for now
                return false;
            } else {
                if (virtualViewId >= mThumbs.size()) return false;
                Thumb thumb = mThumbs.get(virtualViewId);
                if (thumb == null) return false;

                switch (action) {
                    case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
                        thumb.setValue(thumb.value + getStep());
                        return true;

                    case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
                        thumb.setValue(thumb.value - getStep());
                        return true;

                    case ACT_SET_PROGRESS:
                        thumb.setValue(arguments.getInt("value"));
                        return true;
                }
            }

            return false;
        }
    }
}
