/*
 * Created by dengshiwei on 2022/07/13.
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

package com.sensorsdata.analytics.android.autotrack.aop;

import static org.junit.Assert.fail;

import android.app.Application;
import android.app.Fragment;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.autotrack.core.autotrack.SAFragmentLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.dialog.SchemeActivity;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class FragmentTrackHelperTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    private static Fragment mFragment = new Fragment();
    private SAFragmentLifecycleCallbacks mFragmentListener;
    SchemeActivity mActivity = Robolectric.setupActivity(SchemeActivity.class);

    @Test
    public void testFragment() {
        onFragmentViewCreated();
        trackFragmentResume();
        trackFragmentPause();
        trackFragmentSetUserVisibleHint();
        trackOnHiddenChanged();
    }

    public void onFragmentViewCreated() {
        mFragmentListener = new SAFragmentLifecycleCallbacks() {
            @Override
            public void onCreate(Object object) {
                fail();
            }

            @Override
            public void onViewCreated(Object object, View rootView, Bundle bundle) {
                Assert.assertTrue(true);
            }

            @Override
            public void onStart(Object object) {
                fail();
            }

            @Override
            public void onResume(Object object) {
                fail();
            }

            @Override
            public void onPause(Object object) {
                fail();
            }

            @Override
            public void onStop(Object object) {
                fail();
            }

            @Override
            public void onHiddenChanged(Object object, boolean hidden) {
                fail();
            }

            @Override
            public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
                fail();
            }
        };
        FragmentTrackHelper.addFragmentCallbacks(mFragmentListener);
        FragmentTrackHelper.onFragmentViewCreated(mFragment, new LinearLayout(mActivity), null);
        FragmentTrackHelper.removeFragmentCallbacks(mFragmentListener);
    }

    public void trackFragmentResume() {
        mFragmentListener = new SAFragmentLifecycleCallbacks() {
            @Override
            public void onCreate(Object object) {
                fail();
            }

            @Override
            public void onViewCreated(Object object, View rootView, Bundle bundle) {
                fail();
            }

            @Override
            public void onStart(Object object) {
                fail();
            }

            @Override
            public void onResume(Object object) {
                Assert.assertTrue(true);
            }

            @Override
            public void onPause(Object object) {
                fail();
            }

            @Override
            public void onStop(Object object) {
                fail();
            }

            @Override
            public void onHiddenChanged(Object object, boolean hidden) {
                fail();
            }

            @Override
            public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
                fail();
            }
        };
        FragmentTrackHelper.addFragmentCallbacks(mFragmentListener);
        FragmentTrackHelper.trackFragmentResume(mFragment);
        FragmentTrackHelper.removeFragmentCallbacks(mFragmentListener);
    }

    public void trackFragmentPause() {
        mFragmentListener = new SAFragmentLifecycleCallbacks() {
            @Override
            public void onCreate(Object object) {
                fail();
            }

            @Override
            public void onViewCreated(Object object, View rootView, Bundle bundle) {
                fail();
            }

            @Override
            public void onStart(Object object) {
                fail();
            }

            @Override
            public void onResume(Object object) {
                fail();
            }

            @Override
            public void onPause(Object object) {
                Assert.assertTrue(true);
            }

            @Override
            public void onStop(Object object) {
                fail();
            }

            @Override
            public void onHiddenChanged(Object object, boolean hidden) {
                fail();
            }

            @Override
            public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
                fail();
            }
        };
        FragmentTrackHelper.addFragmentCallbacks(mFragmentListener);
        FragmentTrackHelper.trackFragmentPause(mFragment);
        FragmentTrackHelper.removeFragmentCallbacks(mFragmentListener);
    }

    public void trackFragmentSetUserVisibleHint() {
        mFragmentListener = new SAFragmentLifecycleCallbacks() {
            @Override
            public void onCreate(Object object) {
                fail();
            }

            @Override
            public void onViewCreated(Object object, View rootView, Bundle bundle) {
                fail();
            }

            @Override
            public void onStart(Object object) {
                fail();
            }

            @Override
            public void onResume(Object object) {
                fail();
            }

            @Override
            public void onPause(Object object) {
                fail();
            }

            @Override
            public void onStop(Object object) {
                fail();
            }

            @Override
            public void onHiddenChanged(Object object, boolean hidden) {
                fail();
            }

            @Override
            public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
                Assert.assertTrue(true);
            }
        };
        FragmentTrackHelper.addFragmentCallbacks(mFragmentListener);
        FragmentTrackHelper.trackFragmentSetUserVisibleHint(mFragment, true);
        FragmentTrackHelper.removeFragmentCallbacks(mFragmentListener);
    }

    public void trackOnHiddenChanged() {
        mFragmentListener = new SAFragmentLifecycleCallbacks() {
            @Override
            public void onCreate(Object object) {
                fail();
            }

            @Override
            public void onViewCreated(Object object, View rootView, Bundle bundle) {
                fail();
            }

            @Override
            public void onStart(Object object) {
                fail();
            }

            @Override
            public void onResume(Object object) {
                fail();
            }

            @Override
            public void onPause(Object object) {
                fail();
            }

            @Override
            public void onStop(Object object) {
                fail();
            }

            @Override
            public void onHiddenChanged(Object object, boolean hidden) {
                Assert.assertTrue(true);
            }

            @Override
            public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
                fail();
            }
        };
        FragmentTrackHelper.addFragmentCallbacks(mFragmentListener);
        FragmentTrackHelper.trackOnHiddenChanged(mFragment, true);
        FragmentTrackHelper.removeFragmentCallbacks(mFragmentListener);
    }
}