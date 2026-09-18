package org.example.config;

class ConfigLoader {

    static boolean isRemoteConfigEnabled() {
        return System.getProperty("remote.config") != null;
    }

    static ConfigCenter loadRemoteConfig() {
        return new ConfigCenter();
    }

    static void logConfigSource() {
        System.out.println("loading config from environment");
    }
}
