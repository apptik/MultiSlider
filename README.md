# MultiSlider


MultiSlider is multifunctional and multi thumb custom view component for Android.
It Can be used as a normal Android seekbar, a range bar and multi-thumb bar.
MultiSlider is extremely easy to use while still very flexible and customizable.

Developer can customize many features from XML layout or programmatically.

##Download

Sonatype Repo: <https://oss.sonatype.org/content/repositories/releases/io/apptik/widget/multislider/1.2/multislider-1.2.aar>

Maven:

    <dependency>
      <groupId>io.apptik.widget</groupId>
      <artifactId>multislider</artifactId>
      <version>1.2.0</version>
    </dependency>

Gradle:

    compile 'io.apptik.widget:multislider:1.2.0'



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


##Usage

###in layout xml file add

        <org.djodjo.widget.MultiSlider
            android:id="@+id/range_slider5"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"

            app:thumbNumber="2"
            app:range1="@drawable/multislider_scrubber_track_holo_light"
            app:stepsThumbsApart="5"
            app:drawThumbsApart="true"

            />

###in the activity/fragment code add

        MultiSlider multiSlider5 = (MultiSlider)v.findViewById(R.id.range_slider5);

        multiSlider5.setOnThumbValueChangeListener(new MultiSlider.OnThumbValueChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                if (thumbIndex == 0) {
                    doSmth(String.valueOf(value));
                } else {
                    doSmthElse(String.valueOf(value));
                }
            }
        });



## Example ScreenShots


### Simple Slider / Seek bar

![ScreenShot1](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/scrshot1.png)

### Range Bar

![ScreenShot2](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/scrshot2.png)

### Multiple Thumbs

![ScreenShot3](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/scrshot3.png)


## Licence

    Copyright (C) 2015 AppTik Project
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
*    Make sure your commit messages are in the proper format:  <type> (<scope>) : <summary>, where type can be [fix, feat(ure), test, docs]
*    Push your changes to a topic branch in your fork of the repository.
*    Make sure the tests pass, and add any new tests as appropriate.
*    Submit a pull request to the original repository.

Thanks for your contributions!
