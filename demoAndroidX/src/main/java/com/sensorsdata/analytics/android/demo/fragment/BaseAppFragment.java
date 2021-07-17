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


import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.sensorsdata.analytics.android.demo.R;

public class BaseAppFragment extends Fragment {
    private static final String TAG = "nice ";//过滤关键字
    private static final String TAG_2 = " ---> :  ";
    public BaseAppFragment() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        view.setTag(R.id.fragment_root_view, this);
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG + getClass().getSimpleName() + TAG_2, "onCreateView @");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_base_app, container, false);
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