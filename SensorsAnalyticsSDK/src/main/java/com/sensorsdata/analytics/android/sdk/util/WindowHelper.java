package com.sensorsdata.analytics.android.sdk.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build.VERSION;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TabHost;

import com.sensorsdata.analytics.android.sdk.AppStateManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class WindowHelper {
    private static Object sWindowManger;
    private static Field viewsField;
    private static Class sPhoneWindowClazz;
    private static Class sPopupWindowClazz;
    private static Class<?> sListMenuItemViewClazz;
    private static Method sItemViewGetDataMethod;
    private static boolean sIsInitialized = false;
    private static boolean sArrayListWindowViews = false;
    private static boolean sViewArrayWindowViews = false;
    private static final String sMainWindowPrefix = "/MainWindow";
    private static final String sDialogWindowPrefix = "/DialogWindow";
    private static final String sPopupWindowPrefix = "/PopupWindow";
    private static final String sCustomWindowPrefix = "/CustomWindow";

    public static void init() {
        if (!sIsInitialized) {
            String windowManagerClassName;
            if (VERSION.SDK_INT >= 17) {
                windowManagerClassName = "android.view.WindowManagerGlobal";
            } else {
                windowManagerClassName = "android.view.WindowManagerImpl";
            }

            Class windowManager = null;

            try {
                windowManager = Class.forName(windowManagerClassName);
                String windowManagerString;
                if (VERSION.SDK_INT >= 17) {
                    windowManagerString = "sDefaultWindowManager";
                } else if (VERSION.SDK_INT >= 13) {
                    windowManagerString = "sWindowManager";
                } else {
                    windowManagerString = "mWindowManager";
                }

                viewsField = windowManager.getDeclaredField("mViews");
                Field instanceField = windowManager.getDeclaredField(windowManagerString);
                viewsField.setAccessible(true);
                if (viewsField.getType() == ArrayList.class) {
                    sArrayListWindowViews = true;
                } else if (viewsField.getType() == View[].class) {
                    sViewArrayWindowViews = true;
                }

                instanceField.setAccessible(true);
                sWindowManger = instanceField.get((Object) null);
            } catch (NoSuchFieldException var9) {
                //ignored
            } catch (IllegalAccessException var10) {
                //ignored
            } catch (ClassNotFoundException var11) {
                //ignored
            }

            try {
                sListMenuItemViewClazz = Class.forName("com.android.internal.view.menu.ListMenuItemView");
                Class itemViewInterface = Class.forName("com.android.internal.view.menu.MenuView$ItemView");
                sItemViewGetDataMethod = itemViewInterface.getDeclaredMethod("getItemData");
            } catch (ClassNotFoundException var7) {
                //ignored
            } catch (NoSuchMethodException var8) {
                //ignored
            }

            try {
                if (VERSION.SDK_INT >= 23) {
                    try {
                        sPhoneWindowClazz = Class.forName("com.android.internal.policy.PhoneWindow$DecorView");
                    } catch (ClassNotFoundException var5) {
                        sPhoneWindowClazz = Class.forName("com.android.internal.policy.DecorView");
                    }
                } else {
                    sPhoneWindowClazz = Class.forName("com.android.internal.policy.impl.PhoneWindow$DecorView");
                }
            } catch (ClassNotFoundException var6) {
                //ignored
            }

            try {
                if (VERSION.SDK_INT >= 23) {
                    sPopupWindowClazz = Class.forName("android.widget.PopupWindow$PopupDecorView");
                } else {
                    sPopupWindowClazz = Class.forName("android.widget.PopupWindow$PopupViewContainer");
                }
            } catch (ClassNotFoundException var4) {
                //ignored
            }

            sIsInitialized = true;
        }
    }

    private static View[] getWindowViews() {
        View[] result = new View[0];
        if (sWindowManger == null) {
            Activity current = AppStateManager.getInstance().getForegroundActivity();
            return current != null ? new View[]{current.getWindow().getDecorView()} : result;
        } else {
            try {
                View[] views = null;
                if (sArrayListWindowViews) {
                    views = (View[]) ((ArrayList) viewsField.get(sWindowManger)).toArray(result);
                } else if (sViewArrayWindowViews) {
                    views = ((View[]) viewsField.get(sWindowManger));
                }
                if (views != null) {
                    result = views;
                }
            } catch (Exception var2) {
                //ignored
            }

            return filterNullAndDismissToastView(result);
        }
    }

    public static View[] getSortedWindowViews() {
        View[] views = getWindowViews();
        if (views.length > 1) {
            views = (View[]) Arrays.copyOf(views, views.length);
            Arrays.sort(views, sViewSizeComparator);
        }

        return views;
    }

    private static Comparator<View> sViewSizeComparator = new Comparator<View>() {
        public int compare(View lhs, View rhs) {
            int lhsHashCode = lhs.hashCode();
            int rhsHashCode = rhs.hashCode();
            int currentHashCode = AppStateManager.getInstance().getCurrentRootWindowsHashCode();
            if (lhsHashCode == currentHashCode) {
                return -1;
            } else {
                return rhsHashCode == currentHashCode ? 1 : rhs.getWidth() * rhs.getHeight() - lhs.getWidth() * lhs.getHeight();
            }
        }
    };

    private static View[] filterNullAndDismissToastView(View[] array) {
        List<View> list = new ArrayList(array.length);
        long currentTime = System.currentTimeMillis();
        View[] result = array;
        int length = array.length;

        for (int i = 0; i < length; ++i) {
            View view = result[i];
            if (view != null) {
                list.add(view);
            }
        }

        result = new View[list.size()];
        list.toArray(result);
        return result;
    }

    public static boolean isDecorView(Class rootClass) {
        if (!sIsInitialized) {
            init();
        }
        return rootClass == sPhoneWindowClazz || rootClass == sPopupWindowClazz;
    }

    @SuppressLint({"RestrictedApi"})
    private static Object getMenuItemData(View view) throws InvocationTargetException, IllegalAccessException {
        if (view.getClass() == sListMenuItemViewClazz) {
            return sItemViewGetDataMethod.invoke(view);
        } else if (ViewUtil.instanceOfAndroidXListMenuItemView(view) || ViewUtil.instanceOfSupportListMenuItemView(view) || ViewUtil.instanceOfBottomNavigationItemView(view)) {
            return ViewUtil.getItemData(view);
        }
        return null;
    }

    @SuppressLint("NewApi")
    private static View findMenuItemView(View view, MenuItem item) throws InvocationTargetException, IllegalAccessException {
        // 解决 actionbar 左侧返回按钮全埋点问题
        if (ViewUtil.instanceOfActionMenuItem(item) && item.getItemId() == android.R.id.home && ViewUtil.instanceOfToolbar(view.getParent()) && view instanceof ImageButton) {
            View navButtonView = ReflectUtil.findField(new String[]{"androidx.appcompat.widget.Toolbar", "android.support.v7.widget.Toolbar", "android.widget.Toolbar"}, view.getParent(), "mNavButtonView");
            if (navButtonView != null && navButtonView == view) {
                return view;
            }
        }
        if (getMenuItemData(view) == item) {
            return view;
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View menuView = findMenuItemView(((ViewGroup) view).getChildAt(i), item);
                if (menuView != null) {
                    return menuView;
                }
            }
        }
        return null;
    }

    public static View getClickView(MenuItem menuItem) {
        int i = 0;
        if (menuItem == null) {
            return null;
        }
        WindowHelper.init();
        View[] windows = WindowHelper.getWindowViews();
        try {
            View window;
            View menuView;
            for (View window2 : windows) {
                if (window2.getClass() == sPopupWindowClazz) {
                    if ((menuView = findMenuItemView(window2, menuItem)) != null) {
                        return menuView;
                    }
                }
            }
            int length = windows.length;
            while (i < length) {
                window = windows[i];
                if (window.getClass() != sPopupWindowClazz) {
                    if ((menuView = findMenuItemView(window, menuItem)) != null) {
                        return menuView;
                    }
                }
                i++;
            }
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (IllegalAccessException e2) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static View getClickView(String tabHostTag) {
        int i = 0;
        if (TextUtils.isEmpty(tabHostTag)) {
            return null;
        }
        WindowHelper.init();
        View[] windows = WindowHelper.getWindowViews();
        try {
            View window;
            View tabHostView;
            while (i < windows.length) {
                window = windows[i];
                if (window.getClass() != sPopupWindowClazz) {
                    if ((tabHostView = findTabView(window, tabHostTag)) != null) {
                        return tabHostView;
                    }
                }
                i++;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static View findTabView(View view, String tabHostTag) {
        if (TextUtils.equals(tabHostTag, getTabHostTag(view))) {
            return ReflectUtil.callMethod(view, "getCurrentTabView");
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View tabHostView = findTabView(((ViewGroup) view).getChildAt(i), tabHostTag);
                if (tabHostView != null) {
                    return tabHostView;
                }
            }
        }
        return null;
    }

    private static String getTabHostTag(View view) {
        if (view instanceof TabHost) {
            return ReflectUtil.callMethod(view, "getCurrentTabTag");
        }
        return null;
    }

    public static String getWindowPrefix(View root) {
        if (root.hashCode() == AppStateManager.getInstance().getCurrentRootWindowsHashCode()) {
            return getMainWindowPrefix();
        }
        return getSubWindowPrefix(root);
    }

    public static String getMainWindowPrefix() {
        return sMainWindowPrefix;
    }

    private static String getSubWindowPrefix(View root) {
        ViewGroup.LayoutParams params = root.getLayoutParams();
        if (params != null && (params instanceof WindowManager.LayoutParams)) {
            int type = ((WindowManager.LayoutParams) params).type;
            if (type == 1) {
                return sMainWindowPrefix;
            }
            if (type < 99 && root.getClass() == sPhoneWindowClazz) {
                return sDialogWindowPrefix;
            }
            if (type < 1999 && root.getClass() == sPopupWindowClazz) {
                return sPopupWindowPrefix;
            }
            if (type < 2999) {
                return sCustomWindowPrefix;
            }
        }
        Class rootClazz = root.getClass();
        if (rootClazz == sPhoneWindowClazz) {
            return sDialogWindowPrefix;
        }
        if (rootClazz == sPopupWindowClazz) {
            return sPopupWindowPrefix;
        }
        return sCustomWindowPrefix;
    }

    public static boolean isMainWindow(View root) {
        ViewGroup.LayoutParams params = root.getLayoutParams();
        if ((params instanceof WindowManager.LayoutParams)) {
            return ((WindowManager.LayoutParams) params).type == 1;
        }
        return false;
    }

    public static boolean isDialogOrPopupWindow(View root) {
        String prefix = getSubWindowPrefix(root);
        return TextUtils.equals(sDialogWindowPrefix, prefix) || TextUtils.equals(sPopupWindowPrefix, prefix);
    }

}
