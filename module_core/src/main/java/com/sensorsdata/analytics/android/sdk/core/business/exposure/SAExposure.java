package com.sensorsdata.analytics.android.sdk.core.business.exposure;

import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;

public class SAExposure {

    public static void setExposureIdentifier(View view, String exposureIdentifier) {
        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.EXPOSURE_NAME)) {
            try {
                SAModuleManager.getInstance().getExposureModuleService().setExposureIdentifier(view, exposureIdentifier);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    public static void addExposureView(View view, SAExposureData exposureData) {
        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.EXPOSURE_NAME)) {
            try {
                SAModuleManager.getInstance().getExposureModuleService().addExposureView(view, exposureData);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    public static void removeExposureView(View view, String identifier) {
        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.EXPOSURE_NAME)) {
            try {
                SAModuleManager.getInstance().getExposureModuleService().removeExposureView(view, identifier);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

}
