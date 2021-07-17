/*
 * Created by chenru on 2019/06/20.
 * Copyright 2015－2021 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.demo.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sensorsdata.analytics.android.demo.R;

public class BaseAndroidXFragment extends Fragment {

    private static final String TAG = "nice ";//过滤关键字 nice
    private static final String TAG_2 = " ---> :  ";

    public BaseAndroidXFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG + getClass().getSimpleName() + TAG_2, "onCreateView @");
        return inflater.inflate(R.layout.fragment_base_androidx, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setTag(R.id.fragment_root_view, this);
        Log.i(TAG + getClass().getSimpleName() + TAG_2, "onViewCreated");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG + getClass().getSimpleName() + TAG_2, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG + getClass().getSimpleName() + TAG_2, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG + getClass().getSimpleName() + TAG_2, "onStop");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.i(TAG + getClass().getSimpleName() + TAG_2, "onHiddenChanged = " + hidden);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG + getClass().getSimpleName() + TAG_2, "setUserVisibleHint = " + isVisibleToUser);
    }
}