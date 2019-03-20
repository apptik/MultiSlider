package io.apptik.widget;

import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.uiautomator.Configurator;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import static io.apptik.widget.VirtualTreeProvider.ACT_SET_PROGRESS;

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
