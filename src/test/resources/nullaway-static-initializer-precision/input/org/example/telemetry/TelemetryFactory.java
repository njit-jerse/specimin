package org.example.telemetry;

class TelemetryFactory {

    static boolean isTelemetryEnabled() {
        return Boolean.getBoolean("telemetry.enabled");
    }

    static Telemetry createTelemetry() {
        return new Telemetry();
    }

    static String computeBuildFingerprint() {
        return "build-" + System.currentTimeMillis();
    }
}
