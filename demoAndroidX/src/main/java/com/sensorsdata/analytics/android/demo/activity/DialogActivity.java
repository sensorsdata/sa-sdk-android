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

package com.sensorsdata.analytics.android.demo.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.utils.DialogUtil;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class DialogActivity extends BaseActivity implements View.OnClickListener {
    int yourChoice;
    int yourChoiceLambda;
    ArrayList<Integer> yourChoices = new ArrayList<>();
    ArrayList<Integer> yourChoicesLambda = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        initView();
    }

    private void initView() {
        findViewById(R.id.dialog_normal).setOnClickListener(this);
        findViewById(R.id.dialog_single_choice).setOnClickListener(this);
        findViewById(R.id.dialog_multi_button).setOnClickListener(this);
        findViewById(R.id.dialog_multi_choice).setOnClickListener(this);
        findViewById(R.id.dialog_list).setOnClickListener(this);
        findViewById(R.id.dialog_waiting).setOnClickListener(this);
        findViewById(R.id.dialog_progress).setOnClickListener(this);
        findViewById(R.id.dialog_input).setOnClickListener(this);
        findViewById(R.id.dialog_custom).setOnClickListener(this);
        findViewById(R.id.dialog_utils).setOnClickListener(this);
        findViewById(R.id.dialog_utils_relogin).setOnClickListener(this);
        findViewById(R.id.dialog_normal_lambda).setOnClickListener(this);
        findViewById(R.id.dialog_single_choice_lambda).setOnClickListener(this);
        findViewById(R.id.dialog_multi_button_lambda).setOnClickListener(this);
        findViewById(R.id.dialog_multi_choice_lambda).setOnClickListener(this);
        findViewById(R.id.dialog_list_lambda).setOnClickListener(this);
        findViewById(R.id.dialog_input_lambda).setOnClickListener(this);
        findViewById(R.id.dialog_custom_lambda).setOnClickListener(this);
        findViewById(R.id.dialog_fragment).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dialog_normal:
                showNormalDialog();
                break;
            case R.id.dialog_single_choice:
                showSingleChoiceDialog();
                break;
            case R.id.dialog_multi_button:
                showMultiBtnDialog();
                break;
            case R.id.dialog_multi_choice:
                showMultiChoiceDialog();
                break;
            case R.id.dialog_list:
                showListDialog();
                break;
            case R.id.dialog_waiting:
                showWaitingDialog();
                break;
            case R.id.dialog_progress:
                showProgressDialog();
                break;
            case R.id.dialog_input:
                showInputDialog();
                break;
            case R.id.dialog_custom:
                showCustomizeDialog();
                break;
            case R.id.dialog_utils:
                DialogUtil.showModalDialog(DialogActivity.this, "title", "content", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                break;
            case R.id.dialog_utils_relogin:
                DialogUtil.showNeedReLoginDialog(DialogActivity.this);
                break;
            case R.id.dialog_normal_lambda:
                showNormalDialogLambda();
                break;
            case R.id.dialog_single_choice_lambda:
                showSingleChoiceDialogLambda();
                break;
            case R.id.dialog_multi_button_lambda:
                showMultiBtnDialogLambda();
                break;
            case R.id.dialog_multi_choice_lambda:
                showMultiChoiceDialogLambda();
                break;
            case R.id.dialog_list_lambda:
                showListDialogLambda();
                break;
            case R.id.dialog_input_lambda:
                showInputDialogLambda();
                break;
            case R.id.dialog_custom_lambda:
                showCustomizeDialogLambda();
                break;
            case R.id.dialog_fragment:
                ShareDialog dialog = new ShareDialog();
                dialog.show(getSupportFragmentManager().beginTransaction(), "hello");
                break;
        }
    }

    private void showNormalDialog() {

        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(DialogActivity.this);
        normalDialog.setIcon(R.mipmap.ic_launcher);
        normalDialog.setTitle("我是一个普通Dialog");
        normalDialog.setMessage("你要点击哪一个按钮呢?");
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        normalDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        // 显示
        normalDialog.show();
    }

    private void showNormalDialogLambda() {

        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(DialogActivity.this);
        normalDialog.setIcon(R.mipmap.ic_launcher);
        normalDialog.setTitle("我是一个普通Dialog");
        normalDialog.setMessage("你要点击哪一个按钮呢?");
        normalDialog.setPositiveButton("确定",
                (dialog, which) -> {

                });
        normalDialog.setNegativeButton("关闭",
                (dialog, which) -> {

                });
        // 显示
        normalDialog.show();
    }

    private void showMultiBtnDialog() {
        AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(DialogActivity.this);
        normalDialog.setIcon(R.mipmap.ic_launcher);
        normalDialog.setTitle("我是一个普通Dialog").setMessage("你要点击哪一个按钮呢?");
        normalDialog.setPositiveButton("按钮1",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        normalDialog.setNeutralButton("按钮2",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        normalDialog.setNegativeButton("按钮3", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        // 创建实例并显示
        normalDialog.show();
    }

    private void showMultiBtnDialogLambda() {
        AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(DialogActivity.this);
        normalDialog.setIcon(R.mipmap.ic_launcher);
        normalDialog.setTitle("我是一个普通Dialog").setMessage("你要点击哪一个按钮呢?");
        normalDialog.setPositiveButton("按钮1",
                (dialog, which) -> {

                });
        normalDialog.setNeutralButton("按钮2",
                (dialog, which) -> {

                });
        normalDialog.setNegativeButton("按钮3", (dialog, which) -> {

        });
        // 创建实例并显示
        normalDialog.show();
    }

    private void showListDialog() {
        final String[] items = {"我是1", "我是2", "我是3", "我是4"};
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(DialogActivity.this);
        listDialog.setTitle("我是一个列表Dialog");
        listDialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // which 下标从0开始

                Toast.makeText(DialogActivity.this,
                        "你点击了" + items[which],
                        Toast.LENGTH_SHORT).show();
            }
        });
        listDialog.show();
    }

    private void showListDialogLambda() {
        final String[] items = {"我是1", "我是2", "我是3", "我是4"};
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(DialogActivity.this);
        listDialog.setTitle("我是一个列表Dialog");
        listDialog.setItems(items, (dialog, which) -> {
            // which 下标从0开始

            Toast.makeText(DialogActivity.this,
                    "你点击了" + items[which],
                    Toast.LENGTH_SHORT).show();
        });
        listDialog.show();
    }

    private void showSingleChoiceDialog() {
        final String[] items = {"我是1", "我是2", "我是3", "我是4"};
        yourChoice = -1;
        AlertDialog.Builder singleChoiceDialog =
                new AlertDialog.Builder(DialogActivity.this);
        singleChoiceDialog.setTitle("我是一个单选Dialog");
        // 第二个参数是默认选项，此处设置为0
        singleChoiceDialog.setSingleChoiceItems(items, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        yourChoice = which;
                    }
                });
        singleChoiceDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (yourChoice != -1) {
                            Toast.makeText(DialogActivity.this,
                                    "你选择了" + items[yourChoice],
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        singleChoiceDialog.show();
    }

    private void showSingleChoiceDialogLambda() {
        final String[] items = {"我是1", "我是2", "我是3", "我是4"};
        yourChoice = -1;
        AlertDialog.Builder singleChoiceDialog =
                new AlertDialog.Builder(DialogActivity.this);
        singleChoiceDialog.setTitle("我是一个单选Dialog");
        // 第二个参数是默认选项，此处设置为0
        singleChoiceDialog.setSingleChoiceItems(items, 0,
                (dialog, which) -> yourChoiceLambda = which);
        singleChoiceDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    if (yourChoiceLambda != -1) {
                        Toast.makeText(DialogActivity.this,
                                "你选择了" + items[yourChoiceLambda],
                                Toast.LENGTH_SHORT).show();
                    }
                });
        singleChoiceDialog.show();
    }

    private void showMultiChoiceDialog() {
        final String[] items = {"我是1", "我是2", "我是3", "我是4"};
        // 设置默认选中的选项，全为false默认均未选中
        final boolean initChoiceSets[] = {false, false, false, false};
        yourChoices.clear();
        AlertDialog.Builder multiChoiceDialog =
                new AlertDialog.Builder(DialogActivity.this);
        multiChoiceDialog.setTitle("我是一个多选Dialog");
        multiChoiceDialog.setMultiChoiceItems(items, initChoiceSets,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which,
                                        boolean isChecked) {
                        if (isChecked) {
                            yourChoices.add(which);
                        } else {
                            yourChoices.remove(which);
                        }
                    }
                });
        multiChoiceDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int size = yourChoices.size();
                        String str = "";
                        for (int i = 0; i < size; i++) {
                            str += items[yourChoices.get(i)] + " ";
                        }
                        Toast.makeText(DialogActivity.this,
                                "你选中了" + str,
                                Toast.LENGTH_SHORT).show();
                    }
                });
        multiChoiceDialog.show();
    }

    private void showMultiChoiceDialogLambda() {
        final String[] items = {"我是1", "我是2", "我是3", "我是4"};
        // 设置默认选中的选项，全为false默认均未选中
        final boolean initChoiceSets[] = {false, false, false, false};
        yourChoicesLambda.clear();
        AlertDialog.Builder multiChoiceDialog =
                new AlertDialog.Builder(DialogActivity.this);
        multiChoiceDialog.setTitle("我是一个多选Dialog");
        multiChoiceDialog.setMultiChoiceItems(items, initChoiceSets,
                (dialog, which, isChecked) -> {
                    if (isChecked) {
                        yourChoicesLambda.add(which);
                    } else {
                        yourChoicesLambda.remove(which);
                    }
                });
        multiChoiceDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int size = yourChoicesLambda.size();
                        String str = "";
                        for (int i = 0; i < size; i++) {
                            str += items[yourChoicesLambda.get(i)] + " ";
                        }
                        Toast.makeText(DialogActivity.this,
                                "你选中了" + str,
                                Toast.LENGTH_SHORT).show();
                    }
                });
        multiChoiceDialog.show();
    }

    private void showWaitingDialog() {
        /* 等待Dialog具有屏蔽其他控件的交互能力
         * @setCancelable 为使屏幕不可点击，设置为不可取消(false)
         * 下载等事件完成后，主动调用函数关闭该Dialog
         */
        ProgressDialog waitingDialog =
                new ProgressDialog(DialogActivity.this);
        waitingDialog.setTitle("我是一个等待Dialog");
        waitingDialog.setMessage("等待中...");
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(true);
        waitingDialog.show();
    }

    private void showProgressDialog() {
        /* @setProgress 设置初始进度
         * @setProgressStyle 设置样式（水平进度条）
         * @setMax 设置进度最大值
         */
        final int MAX_PROGRESS = 100;
        final ProgressDialog progressDialog =
                new ProgressDialog(DialogActivity.this);
        progressDialog.setProgress(0);
        progressDialog.setTitle("我是一个进度条Dialog");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(MAX_PROGRESS);
        progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Positive_Btn",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Negative_Btn",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        progressDialog.show();
    }

    private void showInputDialog() {
        /*@setView 装入一个EditView
         */
        final EditText editText = new EditText(DialogActivity.this);
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(DialogActivity.this);
        inputDialog.setTitle("我是一个输入Dialog").setView(editText);
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(DialogActivity.this,
                                editText.getText().toString(),
                                Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void showInputDialogLambda() {
        /*@setView 装入一个EditView
         */
        final EditText editText = new EditText(DialogActivity.this);
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(DialogActivity.this);
        inputDialog.setTitle("我是一个输入Dialog").setView(editText);
        inputDialog.setPositiveButton("确定",
                (dialog, which) -> Toast.makeText(DialogActivity.this,
                        editText.getText().toString(),
                        Toast.LENGTH_SHORT).show()).show();
    }

    private void showCustomizeDialog() {
        /* @setView 装入自定义View ==> R.layout.dialog_customize
         * 由于dialog_customize.xml只放置了一个EditView，因此和图8一样
         * dialog_customize.xml可自定义更复杂的View
         */
        AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(DialogActivity.this);
        final View dialogView = LayoutInflater.from(DialogActivity.this)
                .inflate(R.layout.dialog_custom, null);
        customizeDialog.setTitle("我是一个自定义Dialog");
        customizeDialog.setView(dialogView);
        ((RadioButton) dialogView.findViewById(R.id.radiobutton1))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    }
                });
        ((RadioButton) dialogView.findViewById(R.id.radiobutton2))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    }
                });
        ((CheckBox) dialogView.findViewById(R.id.checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });
        customizeDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 获取EditView中的输入内容
                        EditText edit_text =
                                (EditText) dialogView.findViewById(R.id.edit_text);
                        Toast.makeText(DialogActivity.this,
                                edit_text.getText().toString(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
        customizeDialog.show();
    }

    private void showCustomizeDialogLambda() {
        /* @setView 装入自定义View ==> R.layout.dialog_customize
         * 由于dialog_customize.xml只放置了一个EditView，因此和图8一样
         * dialog_customize.xml可自定义更复杂的View
         */
        AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(DialogActivity.this);
        final View dialogView = LayoutInflater.from(DialogActivity.this)
                .inflate(R.layout.dialog_custom, null);
        ((RadioButton) dialogView.findViewById(R.id.radiobutton1))
                .setOnCheckedChangeListener((buttonView, isChecked) -> {

                });
        ((RadioButton) dialogView.findViewById(R.id.radiobutton2))
                .setOnCheckedChangeListener((buttonView, isChecked) -> {

                });
        ((CheckBox) dialogView.findViewById(R.id.checkbox))
                .setOnCheckedChangeListener((buttonView, isChecked) -> {

                });

        customizeDialog.setTitle("我是一个自定义Dialog");
        customizeDialog.setView(dialogView);
        customizeDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    // 获取EditView中的输入内容
                    EditText edit_text =
                            (EditText) dialogView.findViewById(R.id.edit_text);
                    Toast.makeText(DialogActivity.this,
                            edit_text.getText().toString(),
                            Toast.LENGTH_SHORT).show();
                });
        customizeDialog.show();
    }
}
