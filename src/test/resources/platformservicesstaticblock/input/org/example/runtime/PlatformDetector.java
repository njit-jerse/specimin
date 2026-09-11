package org.example.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("TryWithIdenticalCatches")
public class PlatformDetector {

    public static boolean isDesktopRuntimeAvailable() {

        try {
            Class<?> toolkitClass = Class.forName("java.awt.Toolkit");
            Method getDefaultToolkit = toolkitClass.getDeclaredMethod("getDefaultToolkit");
            Object toolkit = getDefaultToolkit.invoke(null);
            return toolkit != null;
        }
        catch (ClassNotFoundException ignored) {}
        catch (NoSuchMethodException ignored) {}
        catch (IllegalAccessException ignored) {}
        catch (InvocationTargetException ignored) {}

        return false;
    }

    private static final String PLATFORM_SERVICES_IMPLEMENTATION_CLASS_NAME = "org.example.runtime.PlatformServicesImpl";

    public static boolean isPlatformOverrideConfigured() {

        try {
            Class.forName(PLATFORM_SERVICES_IMPLEMENTATION_CLASS_NAME);
            return true;
        }
        catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public static PlatformServices createPlatformServices() {

        try {
            Class<?> impl = Class.forName(PLATFORM_SERVICES_IMPLEMENTATION_CLASS_NAME);
            return (PlatformServices) impl.getConstructor().newInstance();
        }
        catch (Throwable ex) {
            return null;
        }
    }

    public static String captureBootDiagnostics() {
        return "boot-ok";
    }
}
