/*
 * Copyright (C) 2014 Kalin Maldzhanski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apptik.widget.example.multislider;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.apptik.widget.MultiSlider;

public class MultiSliderFragmentRange extends Fragment {


    public MultiSliderFragmentRange() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_multi_slider_range, container, false);
        final TextView min1 = (TextView) v.findViewById(R.id.minValue1);
        final TextView max1 = (TextView) v.findViewById(R.id.maxValue1);
        final TextView min2 = (TextView) v.findViewById(R.id.minValue2);
        final TextView max2 = (TextView) v.findViewById(R.id.maxValue2);
        final TextView min3 = (TextView) v.findViewById(R.id.minValue3);
        final TextView max3 = (TextView) v.findViewById(R.id.maxValue3);
        final TextView min4 = (TextView) v.findViewById(R.id.minValue4);
        final TextView max4 = (TextView) v.findViewById(R.id.maxValue4);
        final TextView min5 = (TextView) v.findViewById(R.id.minValue5);
        final TextView max5 = (TextView) v.findViewById(R.id.maxValue5);
        final TextView min6 = (TextView) v.findViewById(R.id.minValue6);
        final TextView max6 = (TextView) v.findViewById(R.id.maxValue6);
        final TextView min7 = (TextView) v.findViewById(R.id.minValue7);
        final TextView max7 = (TextView) v.findViewById(R.id.maxValue7);


        MultiSlider multiSlider1 = (MultiSlider)v.findViewById(R.id.range_slider1);
        MultiSlider multiSlider2 = (MultiSlider)v.findViewById(R.id.range_slider2);
        MultiSlider multiSlider3 = (MultiSlider)v.findViewById(R.id.range_slider3);
        MultiSlider multiSlider4 = (MultiSlider)v.findViewById(R.id.range_slider4);
        MultiSlider multiSlider5 = (MultiSlider)v.findViewById(R.id.range_slider5);
        MultiSlider multiSlider6 = (MultiSlider)v.findViewById(R.id.range_slider6);
        MultiSlider multiSlider7 = (MultiSlider)v.findViewById(R.id.range_slider7);


        min1.setText(String.valueOf(multiSlider1.getThumb(0).getValue()));
        max1.setText(String.valueOf(multiSlider1.getThumb(1).getValue()));

        min2.setText(String.valueOf(multiSlider2.getThumb(0).getValue()));
        max2.setText(String.valueOf(multiSlider2.getThumb(1).getValue()));

        min3.setText(String.valueOf(multiSlider3.getThumb(0).getValue()));
        max3.setText(String.valueOf(multiSlider3.getThumb(1).getValue()));

        min4.setText(String.valueOf(multiSlider4.getThumb(0).getValue()));
        max4.setText(String.valueOf(multiSlider4.getThumb(1).getValue()));

        min5.setText(String.valueOf(multiSlider5.getThumb(0).getValue()));
        max5.setText(String.valueOf(multiSlider5.getThumb(1).getValue()));

        min6.setText(String.valueOf(multiSlider6.getThumb(0).getValue()));
        max6.setText(String.valueOf(multiSlider6.getThumb(1).getValue()));

        min7.setText(String.valueOf(multiSlider7.getThumb(0).getValue()));
        max7.setText(String.valueOf(multiSlider7.getThumb(1).getValue()));


        multiSlider1.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                if (thumbIndex == 0) {
                    min1.setText(String.valueOf(value));
                } else {
                    max1.setText(String.valueOf(value));
                }
            }
        });

        multiSlider2.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                if (thumbIndex == 0) {
                    min2.setText(String.valueOf(value));
                } else {
                    max2.setText(String.valueOf(value));
                }
            }
        });

        multiSlider3.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                if (thumbIndex == 0) {
                    min3.setText(String.valueOf(value));
                } else {
                    max3.setText(String.valueOf(value));
                }
            }
        });

        multiSlider4.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                if (thumbIndex == 0) {
                    min4.setText(String.valueOf(value));
                } else {
                    max4.setText(String.valueOf(value));
                }
            }
        });

        multiSlider5.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                if (thumbIndex == 0) {
                    min5.setText(String.valueOf(value));
                } else {
                    max5.setText(String.valueOf(value));
                }
            }
        });


        multiSlider6.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                if (thumbIndex == 0) {
                    min6.setText(String.valueOf(value));
                } else {
                    max6.setText(String.valueOf(value));
                }
            }
        });

        multiSlider7.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                if (thumbIndex == 0) {
                    min7.setText(String.valueOf(value));
                } else {
                    max7.setText(String.valueOf(value));
                }
            }
        });

        multiSlider7.getThumb(1).setValue(50).setEnabled(false);

        return v;
    }
}
