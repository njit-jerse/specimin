package org.example.runtime;

import org.example.runtime.spi.Logger;
import org.example.runtime.spi.ThreadBridge;

public abstract class PlatformServices {

    private static final PlatformServices instance;
    private static final String bootDiagnostics;

    static {
        instance = PlatformDetector.isDesktopRuntimeAvailable()
            ? PlatformDetector.createPlatformServices()
            : null;
    }

    static {
        bootDiagnostics = PlatformDetector.captureBootDiagnostics();
    }

    public static boolean isReady() {
        return instance != null;
    }

    public static PlatformServices current() {
        return instance;
    }

    public final Logger logger;
    public final ThreadBridge threadBridge;

    public PlatformServices(Logger logger, ThreadBridge threadBridge) {
        this.logger = logger;
        this.threadBridge = threadBridge;
    }
}
