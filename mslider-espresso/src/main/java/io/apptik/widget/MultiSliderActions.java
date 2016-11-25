package io.apptik.widget;


import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.ViewActions;

public class MultiSliderActions {

    private MultiSliderActions() {}

    public static ViewAction moveThumbForward(int thumbId) {
        return ViewActions.actionWithAssertions(new SetThumbValueAction(thumbId, Integer.MAX_VALUE));
    }

    public static ViewAction moveThumbBackward(int thumbId) {
        return ViewActions.actionWithAssertions(new SetThumbValueAction(thumbId, Integer.MIN_VALUE));
    }

    public static ViewAction setThumbValue(int thumbId, int value) {
        return ViewActions.actionWithAssertions(new SetThumbValueAction(thumbId, value));
    }
}
