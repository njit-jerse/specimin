package org.greenrobot.eventbus.android;

public abstract class AndroidComponents {

    private static final AndroidComponents implementation;

    static {
        implementation = AndroidDependenciesDetector.isAndroidSDKAvailable()
            ? AndroidDependenciesDetector.instantiateAndroidComponents()
            : null;
    }
}
