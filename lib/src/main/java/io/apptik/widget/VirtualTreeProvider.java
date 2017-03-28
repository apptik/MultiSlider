package io.apptik.widget;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD;

class VirtualTreeProvider extends AccessibilityNodeProvider {

    // parent MultiSlider instance
    private final MultiSlider mMultiSlider;

    public static final int ACT_SET_PROGRESS = 16908349;

    private final AccessibilityNodeInfo.AccessibilityAction ACTION_SET_PROGRESS;

    VirtualTreeProvider(MultiSlider multiSlider) {
        mMultiSlider = multiSlider;
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
            info = AccessibilityNodeInfo.obtain(mMultiSlider);
            mMultiSlider.onInitializeAccessibilityNodeInfo(info);
            // Add the virtual children of the root View.
            final int childCount = mMultiSlider.getThumbsCount();
            for (int i = 0; i < childCount; i++) {
                info.addChild(mMultiSlider, i);
            }
            if (mMultiSlider.getThumbsCount() == 1) {
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
            IThumb thumb = mMultiSlider.getThumb(thumbId);
            if (thumb == null) {
                return null;
            }
            // Obtain and initialize an AccessibilityNodeInfo with
            // information about the virtual view.
            info = AccessibilityNodeInfo.obtain(mMultiSlider, thumbId);
            info.setClassName(thumb.getClass().getName());
            info.setParent(mMultiSlider);
            info.setSource(mMultiSlider, thumbId);
            info.setContentDescription("Multi-Slider thumb no:" + thumbId);

            if (Build.VERSION.SDK_INT >= 21) {
                info.addAction(ACTION_SET_PROGRESS);
                if (thumb.getPossibleMax() > thumb.getValue()) {
                    info.addAction(ACTION_SCROLL_BACKWARD);
                }
                if (thumb.getPossibleMax() > thumb.getValue()) {
                    info.addAction(ACTION_SCROLL_FORWARD);
                }

            } else {
                if (thumb.getPossibleMin() > thumb.getValue()) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
                if (thumb.getPossibleMax() > thumb.getValue()) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                }
            }


            if (thumb.getThumb() != null) {
                int[] loc = new int[2];
                mMultiSlider.getLocationOnScreen(loc);
                Rect rect = thumb.getThumb().copyBounds();
                rect.top += loc[1];
                rect.left += loc[0];
                rect.right += loc[0];
                rect.bottom += loc[1];
                info.setBoundsInScreen(rect);
                //TODO somehow this resuls in [0,0][0,0]. wonder check why
                //info.setBoundsInParent(rect);

            }

            info.setText(thumb.getTag() + ": " + thumb.getValue());
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
            final int childCount = mMultiSlider.getThumbsCount();
            for (int i = 0; i < childCount; i++) {
                IThumb child = mMultiSlider.getThumb(i);
                String textToLowerCase = child.getTag().toLowerCase();
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
            IThumb virtualView = mMultiSlider.getThumb(virtualViewId);
            if (virtualView != null) {
                String textToLowerCase = virtualView.getTag().toLowerCase();
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
            if (virtualViewId >= mMultiSlider.getThumbsCount()) return false;
            IThumb thumb = mMultiSlider.getThumb(virtualViewId);
            if (thumb == null) return false;

            switch (action) {
                case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
                    thumb.setValue(thumb.getValue() + mMultiSlider.getStep());
                    return true;

                case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
                    thumb.setValue(thumb.getValue() - mMultiSlider.getStep());
                    return true;

                case ACT_SET_PROGRESS:
                    thumb.setValue(arguments.getInt("value"));
                    return true;
            }
        }

        return false;
    }
}
