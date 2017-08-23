package com.sensorsdata.analytics.android.sdk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by 王灼洲 on 2017/1/5
 */

@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SensorsDataAutoTrackAppViewScreenUrl {
    String url() default "";
}
