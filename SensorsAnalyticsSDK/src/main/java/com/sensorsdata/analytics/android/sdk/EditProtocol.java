/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EditProtocol {

    public static class BadInstructionsException extends Exception {
        private static final long serialVersionUID = -4062004792184145311L;

        public BadInstructionsException(String message) {
            super(message);
        }

        public BadInstructionsException(String message, Throwable e) {
            super(message, e);
        }
    }

    public EditProtocol(ResourceIds resourceIds) {
        mResourceIds = resourceIds;
    }

    public ViewSnapshot readSnapshotConfig(JSONObject source) throws BadInstructionsException {
        final List<PropertyDescription> properties = new ArrayList<PropertyDescription>();

        try {
            final JSONObject config = source.getJSONObject("config");
            final JSONArray classes = config.getJSONArray("classes");
            for (int classIx = 0; classIx < classes.length(); classIx++) {
                final JSONObject classDesc = classes.getJSONObject(classIx);
                final String targetClassName = classDesc.getString("name");
                final Class<?> targetClass = Class.forName(targetClassName);

                final JSONArray propertyDescs = classDesc.getJSONArray("properties");
                for (int i = 0; i < propertyDescs.length(); i++) {
                    final JSONObject propertyDesc = propertyDescs.getJSONObject(i);
                    final PropertyDescription desc = readPropertyDescription(targetClass, propertyDesc);
                    properties.add(desc);
                }
            }
            return new ViewSnapshot(properties, mResourceIds);
        } catch (JSONException e) {
            throw new BadInstructionsException("Can't read snapshot configuration", e);
        } catch (final ClassNotFoundException e) {
            throw new BadInstructionsException("Can't resolve types for snapshot configuration", e);
        }
    }

    private PropertyDescription readPropertyDescription(Class<?> targetClass, JSONObject propertyDesc)
            throws BadInstructionsException {
        try {
            final String propName = propertyDesc.getString("name");

            Caller accessor = null;
            if (propertyDesc.has("get")) {
                final JSONObject accessorConfig = propertyDesc.getJSONObject("get");
                final String accessorName = accessorConfig.getString("selector");
                final String accessorResultTypeName =
                        accessorConfig.getJSONObject("result").getString("type");
                final Class<?> accessorResultType = Class.forName(accessorResultTypeName);
                accessor = new Caller(targetClass, accessorName, NO_PARAMS, accessorResultType);
            }

            final String mutatorName;
            if (propertyDesc.has("set")) {
                final JSONObject mutatorConfig = propertyDesc.getJSONObject("set");
                mutatorName = mutatorConfig.getString("selector");
            } else {
                mutatorName = null;
            }

            return new PropertyDescription(propName, targetClass, accessor, mutatorName);
        } catch (final NoSuchMethodException e) {
            throw new BadInstructionsException("Can't create property reader", e);
        } catch (final JSONException e) {
            throw new BadInstructionsException("Can't read property JSON", e);
        } catch (final ClassNotFoundException e) {
            throw new BadInstructionsException(
                    "Can't read property JSON, relevant arg/return class not found", e);
        }
    }

    private final ResourceIds mResourceIds;

    private static final Class<?>[] NO_PARAMS = new Class[0];

    private static final String TAG = "SA.EProtocol";
} // EditProtocol
