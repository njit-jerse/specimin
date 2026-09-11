package org.example.runtime;

@SuppressWarnings("TryWithIdenticalCatches")
public class PlatformDetector {

    public static boolean isDesktopRuntimeAvailable() {
        throw new java.lang.Error();
    }

    public static PlatformServices createPlatformServices() {
        throw new java.lang.Error();
    }
}
