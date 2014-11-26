# MultiSlider


MultiSlider is multifunctional and multi thumb custom view component for Android.
It Can be used as a normal Android seekbar, a range bar and multi-thumb bar.
MultiSlider is extremely easy to use while still very flexible and customizable.

Developer can customize many features from XML layout or programmatically.

##Download

Sonatype Repo: <https://oss.sonatype.org/content/repositories/releases/org/djodjo/widget/MultiSliderWidget/1.0/MultiSliderWidget-1.0.aar>

Maven:

    <dependency>
      <groupId>org.djodjo.widget</groupId>
      <artifactId>MultiSliderWidget</artifactId>
      <version>1.0.0</version>
    </dependency>

Gradle:

    compile 'org.djodjo.widget:MultiSliderWidget:1.0.0'



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


## Example ScreenShots


### Simple Slider / Seek bar

![ScreenShot1](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/scrshot1.png)

### Range Bar

![ScreenShot2](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/scrshot2.png)

### Multiple Thumbs

![ScreenShot3](https://raw.githubusercontent.com/djodjoni/MultiSlider/master/scrshot3.png)


## Licence

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
