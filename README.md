# MultiSlider

[JavaDocs](http://apptik.github.io/MultiSlider/)

[![Build Status](https://travis-ci.org/apptik/MultiSlider.svg?branch=master)](https://travis-ci.org/apptik/MultiSlider)
[![Join the chat at https://gitter.im/apptik/MultiSlider](https://badges.gitter.im/apptik/MultiSlider.svg)](https://gitter.im/apptik/MultiSlider?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/maven-central/v/io.apptik.widget/multislider.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.apptik.widget/multislider)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-MultiSlider-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/3976)

MultiSlider is multifunctional and multi-thumb custom view component for Android.
It Can be used as a normal Android seekbar, a range bar and multi-thumb bar.
MultiSlider is extremely easy to use while still very flexible and customizable.

Developer can customize many features from XML layout or programmatically.

## Download

Find [the latest AARs][mvn] or grab via Maven:

    <dependency>
      <groupId>io.apptik.widget</groupId>
      <artifactId>multislider</artifactId>
      <version>1.3</version>
    </dependency>

or Gradle:

```gradle
    implementation 'io.apptik.widget:multislider:1.3'
```

Downloads of the released versions are available in [Sonatype's `releases` repository][release].

### Using snapshots

in **build.gradle**:

```gradle
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }

    repositories {
        maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
    }
```

in **app.gradle**:

```gradle
    dependencies {
        api ('io.apptik.widget:multislider:1.3.1-SNAPSHOT') { changing = true }
    }
```

Snapshots of the development versions are available in [Sonatype's `snapshots` repository][snap].

## Customizable Features

* View Dimensions
* Number of thumbs, from 0 to any. Default is 2
* Thumb offset. default is half the thumb width
* Track drawable
* Global Range drawable
* Separate Range drawables for each thumb
* Global Thumb drawable (supporting selector drawable)
* Separate Thumbs drawables (via XML thumb 1 and 2 can be specified, via code all)
* Global Min and Max scale limits
* Specific Min and Max limits for each thumb
* Values for thumbs (via XML thumb 1 and 2 can be specified, via code all)
* Scale step
* Option to draw thumbs apart, in order to be easier to select thumbs with the same or similar value
* Option to keep thumbs apart a specific number of steps in order not to allow thumbs to have same or similar values


## Usage

### in layout xml file add

```xml
    <io.apptik.widget.MultiSlider
        android:id="@+id/range_slider5"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"

        app:thumbNumber="2"
        app:stepsThumbsApart="5"
        app:drawThumbsApart="true"

        />
```

### in the activity/fragment code add

```java
    MultiSlider multiSlider5 = (MultiSlider)v.findViewById(R.id.range_slider5);

    multiSlider5.setOnThumbValueChangeListener(new MultiSlider.OnThumbValueChangeListener() {
        @Override
        public void onValueChanged(MultiSlider multiSlider,
                                   MultiSlider.Thumb thumb,
                                   int thumbIndex,
                                   int value)
        {
            if (thumbIndex == 0) {
                doSmth(String.valueOf(value));
            } else {
                doSmthElse(String.valueOf(value));
            }
        }
    });
```

### To use the default Material theme, edit res/values/styles.xml, res/values-v21/styles.xml:

```xml
    <style name="AppTheme" parent="...">
        <item name="multiSliderStyle">@style/Widget.MultiSlider</item>
    </style>

    <style name="Widget.MultiSlider" parent="android:Widget">
    </style>
```

### To use the Holo theme, edit res/values/styles.xml, res/values-v21/styles.xml:

```xml
    <style name="AppTheme" parent="...">
        <item name="multiSliderStyle">@style/sliderHoloStyle</item>
    </style>
```

#### and add the holo theme to your project dependencies, example for gradle:

```gradle
    implementation 'io.apptik.widget:multislider-holo:1.3'
```



## Testing
MultiSlider comes with ready testing support for both Espresso and UiAutomator

### Espresso

in build.gradle:

```gradle
    androidTestImplementation 'io.apptik.widget:multislider-espresso:1.3'
```

in test code:

```java
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
```

### UiAutomator

in build.gradle:

```gradle
    androidTestImplementation 'io.apptik.widget:mslider-uiautomator:1.3'
```

in test code:

```java
    UiMultiSlider slider = new UiMultiSlider(new UiCollection(new UiSelector()
            .className(MultiSlider.class)
            .resourceIdMatches(".*multiSlider3.*"))
            .getChild(new UiSelector().textStartsWith("thumb 0:")));

    for (int i = 0; i < 15; i++) {
        slider.moveThumbForward();
    }
    slider.setThumbValue(10);
    for (int i = 0; i < 10; i++) {
        slider.moveThumbBackward();
    }
```

## Example ScreenShots

![ExampleGif](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/art/multislider.gif)

### Simple Slider / Seek bar

![ScreenShot1](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/scrshot1.png)

### Range Bar

![ScreenShot2](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/scrshot2.png)

### Multiple Thumbs

![ScreenShot3](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/scrshot3.png)


## Licence

    Copyright (C) 2016 AppTik Project
    Copyright (C) 2014 Kalin Maldzhanski

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


## Contributions

Anyone is welcome to contribute via GitHub pull requests.

This is a rough outline of what a contributor's workflow looks like:

*    Create a topic branch from where you want to base your work (usually master).
*    Make commits of logical units.
*    Make sure your commit message
*    Push your changes to a topic branch in your fork of the repository.
*    Make sure the tests pass, and add any new tests as appropriate.
*    Submit a pull request to the original repository.

Thanks for your contributions!

[mvn]: http://search.maven.org/#search|ga|1|io.apptik.widget.multislider
[release]: https://oss.sonatype.org/content/repositories/releases/io/apptik/widget/multislider/
[snap]: https://oss.sonatype.org/content/repositories/snapshots/io/apptik/widget/multislider/
 
