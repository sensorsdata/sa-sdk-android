package com.sensorsdata.analytics.android.sdk.exposure;

import android.view.View;

import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureConfig;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureData;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.exposure.SAExposureProtocol;

public class SAExposureProtocolImpl implements SAExposureProtocol {
    private boolean mEnable = false;
    private SAExposedProcess mExposedProcess;

    @Override
    public void install(SAContextManager contextManager) {
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
        return ModuleConstants.ModuleName.EXPOSURE_NAME;
    }

    @Override
    public boolean isEnable() {
        return mEnable;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    private void init(SAExposureConfig exposureConfig) {
        if (exposureConfig == null) {
            exposureConfig = new SAExposureConfig(0.0f, 0, true);
        }
        //初始化
        mExposedProcess = new SAExposedProcess(exposureConfig);
    }

    @Override
    public void setExposureIdentifier(View view, String exposureIdentifier) {
        if (mExposedProcess != null) {
            mExposedProcess.setExposureIdentifier(view, exposureIdentifier);
        }
    }

    @Override
    public void addExposureView(View view, SAExposureData exposureData) {
        if (mExposedProcess != null) {
            mExposedProcess.addExposureView(view, exposureData);
        }
    }

    @Override
    public void removeExposureView(View view, String identifier) {
        if (mExposedProcess != null) {
            mExposedProcess.removeExposureView(view, identifier);
        }
    }

    @Override
    public void removeExposureView(View view) {
        removeExposureView(view, null);
    }
}
