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
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import io.apptik.widget.MultiSlider;


public class MultiSliderFragmentMany extends Fragment {


    public MultiSliderFragmentMany() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_multi_slider_many, container, false);
        final ArrayList<TextView> vals = new ArrayList<TextView>();
        vals.add((TextView) v.findViewById(R.id.value1));
        vals.add((TextView) v.findViewById(R.id.value2));
        vals.add((TextView) v.findViewById(R.id.value3));
        vals.add((TextView) v.findViewById(R.id.value4));
        vals.add((TextView) v.findViewById(R.id.value5));
        vals.add((TextView) v.findViewById(R.id.value6));
        vals.add((TextView) v.findViewById(R.id.value7));

        MultiSlider multiSlider = (MultiSlider)v.findViewById(R.id.multiSlider);

        for(int i=0;i<7;i++) {
            vals.get(i).setText(String.valueOf(multiSlider.getThumb(i).getValue()));
        }

        multiSlider.setOnThumbValueChangeListener(new MultiSlider.SimpleChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                vals.get(thumbIndex).setText(String.valueOf(value));
            }

        });

        //different ranges example
        multiSlider.getThumb(1).setRange( new ColorDrawable(0xFF000000));
        multiSlider.getThumb(2).setRange( new ColorDrawable(0xFFFF0000));
        multiSlider.getThumb(3).setRange( new ColorDrawable(0xFF00FF00));
        multiSlider.getThumb(4).setRange( new ColorDrawable(0xFF0000FF));
        multiSlider.getThumb(5).setRange( new ColorDrawable(0xFFFFFFFF));
        multiSlider.getThumb(6).setRange( new ColorDrawable(0x66FF0000));

        return v;
    }
}
