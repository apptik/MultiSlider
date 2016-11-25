package io.apptik.widget.example.multislider;


import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiCollection;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.apptik.widget.MultiSlider;
import io.apptik.widget.UiMultiSlider;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static io.apptik.widget.MultiSliderActions.moveThumbBackward;
import static io.apptik.widget.MultiSliderActions.moveThumbForward;
import static io.apptik.widget.MultiSliderActions.setThumbValue;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class ExampleTest {


    @Rule
    public ActivityTestRule<MyActivity> mActivityRule = new ActivityTestRule<>(
            MyActivity.class);

    private UiDevice mDevice;

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    }

    @Test
    public void moveSingle() throws Exception {
        UiObject slider = new UiCollection(new UiSelector()
                .className(MultiSlider.class)
                .resourceIdMatches(".*multiSlider2.*")
                )
                .getChild(new UiSelector().textStartsWith("thumb 0:"));

        UiMultiSlider ms2 = new UiMultiSlider(slider);

        for (int i = 0; i < 15; i++) {
            ms2.moveThumbForward();
        }

        ms2.setThumbValue(10);

        for (int i = 0; i < 10; i++) {
            ms2.moveThumbBackward();
        }

        ms2.moveThumbBackward();

        for (int i = 0; i < 90; i++) {
            onView(ViewMatchers.withId(R.id.multiSlider3))
                    .perform(moveThumbForward(0));
        }
        onView(ViewMatchers.withId(R.id.multiSlider3))
                .perform(setThumbValue(0, 50));
        for (int i = 0; i < 15; i++) {
            onView(ViewMatchers.withId(R.id.multiSlider3))
                    .perform(moveThumbBackward(0));
        }
        onView(ViewMatchers.withId(R.id.multiSlider3))
                .perform(click());
    }
}
