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


public class MultiSliderFragmentSingle extends Fragment {


    public MultiSliderFragmentSingle() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_multi_slider_single, container, false);
        final TextView min1 = (TextView) v.findViewById(R.id.minValue1);
        final TextView min2 = (TextView) v.findViewById(R.id.minValue2);
        final TextView min3 = (TextView) v.findViewById(R.id.minValue3);
        final TextView min4 = (TextView) v.findViewById(R.id.minValue4);
        final TextView min5 = (TextView) v.findViewById(R.id.minValue5);
        final TextView min6 = (TextView) v.findViewById(R.id.minValue6);
        final TextView min7 = (TextView) v.findViewById(R.id.minValue7);

        MultiSlider multiSlider1 = (MultiSlider) v.findViewById(R.id.multiSlider1);
        MultiSlider multiSlider2 = (MultiSlider) v.findViewById(R.id.multiSlider2);
        MultiSlider multiSlider3 = (MultiSlider) v.findViewById(R.id.multiSlider3);
        MultiSlider multiSlider4 = (MultiSlider) v.findViewById(R.id.multiSlider4);
        multiSlider4.getThumb(1).setInvisibleThumb(true);
        MultiSlider multiSlider5 = (MultiSlider) v.findViewById(R.id.multiSlider5);
        MultiSlider multiSlider6 = (MultiSlider) v.findViewById(R.id.multiSlider6);
        MultiSlider multiSlider7 = (MultiSlider) v.findViewById(R.id.multiSlider7);

        min1.setText(String.valueOf(multiSlider1.getThumb(0).getValue()));
        min2.setText(String.valueOf(multiSlider2.getThumb(0).getValue()));
        min3.setText(String.valueOf(multiSlider3.getThumb(0).getValue()));
        min4.setText(String.valueOf(multiSlider4.getThumb(0).getValue()));
        min5.setText(String.valueOf(multiSlider5.getThumb(0).getValue()));
        min6.setText(String.valueOf(multiSlider6.getThumb(0).getValue()));
        min6.setText(String.valueOf(multiSlider7.getThumb(0).getValue()));

        multiSlider1.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                min1.setText(String.valueOf(value));
            }
        });


        multiSlider2.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                min2.setText(String.valueOf(value));
            }
        });

        multiSlider3.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                min3.setText(String.valueOf(value));
            }
        });

        multiSlider4.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                min4.setText(String.valueOf(value));
            }
        });

        multiSlider5.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                min5.setText(String.valueOf(value));
            }
        });

        multiSlider6.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                min6.setText(String.valueOf(value));
            }
        });

        multiSlider7.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                min7.setText(String.valueOf(value));
            }
        });

        return v;
    }
}
