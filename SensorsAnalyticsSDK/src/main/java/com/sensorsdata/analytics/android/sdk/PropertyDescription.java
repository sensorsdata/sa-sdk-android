package com.sensorsdata.analytics.android.sdk;

/* package */ class PropertyDescription {

    public PropertyDescription(String name, Class<?> targetClass, Caller accessor,
                               String mutatorName) {
        this.name = name;
        this.targetClass = targetClass;
        this.accessor = accessor;

        mMutatorName = mutatorName;
    }

    @Override
    public String toString() {
        return "[PropertyDescription " + name + "," + targetClass + ", " + accessor + "/" + mMutatorName
                + "]";
    }

    public final String name;
    public final Class<?> targetClass;
    public final Caller accessor;

    private final String mMutatorName;
}
