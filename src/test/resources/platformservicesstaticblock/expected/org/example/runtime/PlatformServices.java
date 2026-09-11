package org.example.runtime;

public abstract class PlatformServices {

    private static final PlatformServices instance;

    static {
        instance = PlatformDetector.isDesktopRuntimeAvailable()
            ? PlatformDetector.createPlatformServices()
            : null;
    }
}
