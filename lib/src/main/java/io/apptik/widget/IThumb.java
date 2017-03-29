package io.apptik.widget;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface IThumb {

    /**
     * @return Thumb's current value
     */
    int getValue();

    /**
     * Manually set a thumb value
     *
     * @param value
     * @return self
     */
    IThumb setValue(int value);

    /**
     * @return the minimum value a thumb can have regardless of the thumbs after it
     */
    int getMin();

    /**
     * @param min the minimum value a thumb can have
     * @return self
     */
    IThumb setMin(int min);

    /**
     * @return the maximum value a thumb can have regardless of the thumbs after it
     */
    int getMax();

    /**
     * @param max the maximum value a thumb can have
     * @return self
     */
    IThumb setMax(int max);

    /**
     * @return the minimum value a thumb can obtain depending on other thumbs before it
     */
    int getPossibleMin();

    /**
     * @return the maximum value a thumb can have depending the thumbs after it
     */
    int getPossibleMax();

    /**
     * @return Thumb's current tag
     */
    String getTag();

    /**
     * Manually set a thumb tag
     *
     * @param tag
     * @return self
     */
    IThumb setTag(String tag);

    /**
     * @return The thumb drawable
     */
    Drawable getThumb();

    /**
     * @param thumb the thumb drawable
     * @return self
     */
    IThumb setThumb(Drawable thumb);

    /**
     * @return thumb offset in pixels
     */
    int getOffset();

    /**
     * @param offset thumb offset in pixels
     * @return self
     */
    IThumb setOffset(int offset);

    /**
     * @return the range drawable
     */
    Drawable getRange();

    /**
     * Set the range drawable
     *
     * @param range
     * @return self
     */
    IThumb setRange(Drawable range);

    /**
     * @return true is the thumb is enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Sets enable of the thume
     *
     * @param enabled
     */
    IThumb setEnabled(boolean enabled);

    /**
     * @return true is the thumb is invisible, false otherwise
     */
    boolean isInvisible();

    /**
     * Sets thumb's visibility
     *
     * @param invisible
     * @return self
     */
    IThumb setInvisible(boolean invisible);

    /**
     * @return thumb's view binder
     */
    View getViewBinder();

    /**
     * Sets thumb's view binder
     *
     * @param view for binder
     * @return self
     */
    IThumb setViewBinder(final View view);

    /**
     * Update X-axis position for thumb's view binder
     * NOTE: call this function directly, if width of view was changed programmatically
     *
     * @return self
     */
    IThumb updateViewBinderPosition();
}
