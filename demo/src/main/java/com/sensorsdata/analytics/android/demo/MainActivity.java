/**
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved.
 */

package com.sensorsdata.analytics.android.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.sensorsdata.analytics.android.demo.databinding.ActivityMainBinding;

import androidx.databinding.DataBindingUtil;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setHandlers(this);

        initLambdaButton();
        initButton();
    }

    public void onViewClick(View view) {

    }

    private void initLambdaButton() {
        Button button = (Button) findViewById(R.id.lambdaButton);
        button.setOnClickListener(v -> {

        });
    }

    private void initButton() {
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
}
