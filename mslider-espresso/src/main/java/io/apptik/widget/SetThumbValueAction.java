package io.apptik.widget;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.view.View;

import org.hamcrest.Matcher;


public class SetThumbValueAction implements ViewAction {

    private int value;
    private MultiSlider.Thumb thumb;
    private int thumbId = -1;

    public SetThumbValueAction(MultiSlider.Thumb thumb, int value) {
        this.value = value;
    }

    public SetThumbValueAction(int thumbId, int value) {
        this.value = value;
        this.thumbId = thumbId;
    }

    private String getThumbDesc() {
        if (thumbId < 0) {
            return thumb.getTag();
        } else {
            return "thumb Id: " + thumbId;
        }
    }

    private MultiSlider.Thumb getThumb(MultiSlider ms) {
        if (this.thumbId < 0) {
            return thumb;
        } else {
            return ms.getThumb(thumbId);
        }
    }

    @Override
    public Matcher<View> getConstraints() {
        return ViewMatchers.isAssignableFrom(MultiSlider.class);
    }

    @Override
    public String getDescription() {
        if (value == Integer.MAX_VALUE) {
            return String.format("Move forward thumb: %s", getThumbDesc());
        } else if (value == Integer.MIN_VALUE) {
            return String.format("Move backward thumb: %s", getThumbDesc());
        } else return String.format("Set value (%s) for thumb: %s", value, getThumbDesc());
    }

    @Override
    public void perform(UiController uiController, View view) {
        if (value == Integer.MAX_VALUE) {
            value = getThumb((MultiSlider) view).getValue() + ((MultiSlider) view).getStep();
        } else if (value == Integer.MIN_VALUE) {
            value = getThumb((MultiSlider) view).getValue() - ((MultiSlider) view).getStep();
        }
        getThumb((MultiSlider) view).setValue(value);
    }
}
