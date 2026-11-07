package org.example.config;

class ConfigCenter {

    private static final ConfigCenter active;

    static {
        active = ConfigLoader.isRemoteConfigEnabled()
            ? ConfigLoader.loadRemoteConfig()
            : null;
    }
}
