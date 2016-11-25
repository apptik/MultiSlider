package io.apptik.widget;

import android.os.Bundle;
import android.support.test.uiautomator.Configurator;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.view.accessibility.AccessibilityNodeInfo;

import static io.apptik.widget.MultiSlider.VirtualTreeProvider.ACT_SET_PROGRESS;

public class UiMultiSlider extends UiScrollable {

    /**
     * Constructor.
     *
     * @param container a {@link UiSelector} selector to identify the scrollable
     *                  layout element.
     * @since API Level 16
     */
    public UiMultiSlider(UiSelector container) {
        super(container);
        setAsHorizontalList();
    }

    public UiMultiSlider(UiObject uiObject) {
        this(uiObject.getSelector());
    }

    public boolean moveThumbForward() throws UiObjectNotFoundException {
        AccessibilityNodeInfo ani =
                findAccessibilityNodeInfo(Configurator.getInstance().getWaitForSelectorTimeout());
        if (ani == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return ani.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }

    public boolean moveThumbBackward() throws UiObjectNotFoundException {
        AccessibilityNodeInfo ani =
                findAccessibilityNodeInfo(Configurator.getInstance().getWaitForSelectorTimeout());
        if (ani == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return ani.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    public boolean setThumbValue(int value) throws UiObjectNotFoundException {
        AccessibilityNodeInfo ani =
                findAccessibilityNodeInfo(Configurator.getInstance().getWaitForSelectorTimeout());
        if (ani == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Bundle args = new Bundle();
        args.putInt("value", value);
        return ani.performAction(ACT_SET_PROGRESS, args);
    }
}
