package com.sensorsdata.analytics.android.sdk.exposure;

import android.os.Build;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureConfig;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureData;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAModuleProtocol;

import org.json.JSONObject;

public class SAExposureProtocolImpl implements SAModuleProtocol {
    private boolean mEnable = false;
    private SAExposedProcess mExposedProcess;

    @Override
    public void install(SAContextManager contextManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            setModuleState(false);
            return;
        }
        if (!contextManager.getInternalConfigs().saConfigOptions.isDisableSDK()) {
            setModuleState(true);
        }
        init(contextManager.getInternalConfigs().saConfigOptions.getExposureConfig());
    }

    @Override
    public void setModuleState(boolean enable) {
        if (mEnable != enable) {
            mEnable = enable;
        }
    }

    @Override
    public String getModuleName() {
        return Modules.Exposure.MODULE_NAME;
    }

    @Override
    public boolean isEnable() {
        return mEnable;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        switch (methodName) {
            case Modules.Exposure.METHOD_ADD_EXPOSURE_VIEW:
                addExposureView((View) argv[0], (SAExposureData) argv[1]);
                break;
            case Modules.Exposure.METHOD_SET_EXPOSURE_IDENTIFIER:
                setExposureIdentifier((View) argv[0], (String) argv[1]);
                break;
            case Modules.Exposure.METHOD_REMOVE_EXPOSURE_VIEW:
                if (argv.length == 2) {
                    removeExposureView((View) argv[0], (String) argv[1]);
                } else {
                    removeExposureView((View) argv[0], null);
                }
                break;
            case Modules.Exposure.METHOD_UPDATE_EXPOSURE_PROPERTIES:
                mExposedProcess.updateExposureView((View) argv[0], (JSONObject) argv[1]);
                break;
        }
        return null;
    }

    private void init(SAExposureConfig exposureConfig) {
        if (exposureConfig == null) {
            exposureConfig = new SAExposureConfig(0.0f, 0, true);
        }
        //初始化
        mExposedProcess = new SAExposedProcess(exposureConfig);
    }

    private void setExposureIdentifier(View view, String exposureIdentifier) {
        if (mExposedProcess != null) {
            mExposedProcess.setExposureIdentifier(view, exposureIdentifier);
        }
    }

    private void addExposureView(View view, SAExposureData exposureData) {
        if (mExposedProcess != null) {
            mExposedProcess.addExposureView(view, exposureData);
        }
    }

    private void removeExposureView(View view, String identifier) {
        if (mExposedProcess != null) {
            mExposedProcess.removeExposureView(view, identifier);
        }
    }
}
