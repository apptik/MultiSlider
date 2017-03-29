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

public class MultiSliderFragmentViewBinder extends Fragment {


    public MultiSliderFragmentViewBinder() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_multi_slider_viewbinder, container, false);

        final TextView bindSlider1_view = (TextView) v.findViewById(R.id.bindSlider1_view);
        final TextView bindSlider2_viewMin = (TextView) v.findViewById(R.id.bindSlider2_viewMin);
        final TextView bindSlider2_viewMax = (TextView) v.findViewById(R.id.bindSlider2_viewMax);
        final TextView bindSlider3_viewMin = (TextView) v.findViewById(R.id.bindSlider3_viewMin);
        final TextView bindSlider3_viewMax = (TextView) v.findViewById(R.id.bindSlider3_viewMax);

        MultiSlider bindSlider1 = (MultiSlider) v.findViewById(R.id.bindSlider1);
        MultiSlider bindSlider2 = (MultiSlider) v.findViewById(R.id.bindSlider2);
        MultiSlider bindSlider3 = (MultiSlider) v.findViewById(R.id.bindSlider3);

        // bind view
        bindSlider1.getThumb(0).setViewBinder(bindSlider1_view);

        bindSlider2.getThumb(0).setViewBinder(bindSlider2_viewMin);
        bindSlider2.getThumb(1).setViewBinder(bindSlider2_viewMax);

        bindSlider3.getThumb(0).setViewBinder(bindSlider3_viewMin);
        bindSlider3.getThumb(1).setViewBinder(bindSlider3_viewMax);


        // initialize value
        bindSlider1_view.setText(String.valueOf(bindSlider1.getThumb(0).getValue()));

        bindSlider2_viewMin.setText(String.valueOf(bindSlider2.getThumb(0).getValue()));
        bindSlider2_viewMax.setText(String.valueOf(bindSlider2.getThumb(1).getValue()));

        bindSlider3_viewMin.setText(String.valueOf(bindSlider3.getThumb(0).getValue()));
        bindSlider3_viewMax.setText(String.valueOf(bindSlider3.getThumb(1).getValue()));

        bindSlider1.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int
                    thumbIndex, int value) {
                bindSlider1_view.setText(String.valueOf(value));
            }
        });

        bindSlider2.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int
                    thumbIndex, int value) {
                final String text = String.valueOf(value);
                switch (thumbIndex) {
                    case 0:
                        bindSlider2_viewMin.setText(text);
                        break;
                    case 1:
                        bindSlider2_viewMax.setText(text);
                        break;
                }
            }
        });

        bindSlider3.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int
                    thumbIndex, int value) {
                final String text = String.valueOf(value);
                switch (thumbIndex) {
                    case 0:
                        bindSlider3_viewMin.setText(text);
                        break;
                    case 1:
                        bindSlider3_viewMax.setText(text);
                        break;
                }
            }
        });

        return v;
    }
}
