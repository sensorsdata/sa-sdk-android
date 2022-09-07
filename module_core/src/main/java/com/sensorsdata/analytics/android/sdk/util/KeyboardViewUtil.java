package com.sensorsdata.analytics.android.sdk.util;

import android.view.View;
import android.view.ViewGroup;

import com.sensorsdata.analytics.android.sdk.R;

import java.util.regex.Pattern;

public class KeyboardViewUtil {
    private static final String MATCH_RULE_KEYBOARD = "^([A-Za-z]|[0-9])";
    private static final String TAG_KEYBOARD = "keyboard_tag";
    private static boolean isSensorsCheckKeyboard = true;

    public static boolean isKeyboardView(View view) {
        if (!isSensorsCheckKeyboard || null == view) {
            return false;
        }
        String viewText = SAViewUtils.getViewContent(view);
        if (Pattern.matches(MATCH_RULE_KEYBOARD, viewText)) {
            return getKeyboardSimilarView(view);
        }
        return false;
    }

    private static boolean getKeyboardSimilarView(View view) {
        if (view.getParent() instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view.getParent();
            if (null != viewGroup.getTag(R.id.sensors_analytics_tag_view_keyboard)) {
                return true;
            }
            int currentIndex = viewGroup.indexOfChild(view);//获取点击 view 在 ViewGroup 的位置
            int viewCount = viewGroup.getChildCount();
            if (viewCount > 1) {
                boolean isKeyboardView = false;
                for (int i = 0; i < viewCount; i++) {
                    if (currentIndex != i &&
                            Pattern.matches(MATCH_RULE_KEYBOARD, SAViewUtils.getViewContent(viewGroup.getChildAt(i)))) {
                        isKeyboardView = true;
                        break;
                    }
                }
                if (!isKeyboardView) {//向上查找一层
                    return getKeyboardSimilarFatherView(viewGroup);
                } else {
                    viewGroup.setTag(R.id.sensors_analytics_tag_view_keyboard, TAG_KEYBOARD);
                    return true;
                }
            } else {//再向上查找一层
                return getKeyboardSimilarFatherView(viewGroup);
            }
        } else {//再向上查找一层
            return getKeyboardSimilarFatherView((View) view.getParent());
        }
    }

    private static boolean getKeyboardSimilarFatherView(View viewParent) {
        if (viewParent.getParent() instanceof ViewGroup) {
            ViewGroup viewGroupParent = (ViewGroup) viewParent.getParent();
            if (null != viewGroupParent.getTag(R.id.sensors_analytics_tag_view_keyboard)) {
                return true;
            }
            int viewGroupParentChildCount = viewGroupParent.getChildCount();
            if (viewGroupParentChildCount > 1) {
                int viewGroupIndex = viewGroupParent.indexOfChild(viewParent);
                boolean isKeyboardFatherView = false;
                for (int i = 0; i < viewGroupParentChildCount; i++) {
                    if (viewGroupIndex != i) {
                        View viewTemp = viewGroupParent.getChildAt(i);
                        if (null != viewTemp.getTag(R.id.sensors_analytics_tag_view_keyboard)) {
                            isKeyboardFatherView = true;
                            break;
                        }
                        if (viewTemp instanceof ViewGroup) {
                            ViewGroup viewGroupOther = (ViewGroup) viewTemp;
                            int numOther = viewGroupOther.getChildCount();
                            boolean isKeyBoardSunView = false;
                            for (int n = 0; n < numOther; n++) {
                                if (Pattern.matches(MATCH_RULE_KEYBOARD, SAViewUtils.getViewContent(viewGroupOther.getChildAt(n)))) {
                                    isKeyBoardSunView = true;
                                    break;
                                }
                            }
                            if (isKeyBoardSunView) {
                                viewGroupOther.setTag(R.id.sensors_analytics_tag_view_keyboard, TAG_KEYBOARD);
                                viewGroupParent.setTag(R.id.sensors_analytics_tag_view_keyboard, TAG_KEYBOARD);
                                isKeyboardFatherView = true;
                                break;
                            }
                        } else {
                            if (Pattern.matches(MATCH_RULE_KEYBOARD, SAViewUtils.getViewContent(viewTemp))) {
                                viewTemp.setTag(R.id.sensors_analytics_tag_view_keyboard, TAG_KEYBOARD);
                                viewGroupParent.setTag(R.id.sensors_analytics_tag_view_keyboard, TAG_KEYBOARD);
                                isKeyboardFatherView = true;
                                break;
                            }
                        }
                    }
                }
                return isKeyboardFatherView;
            }
        }
        return false;
    }
}
