package org.example.telemetry;

class Telemetry {

    private static final Telemetry active;
    private static final String buildFingerprint;

    static {
        active = TelemetryFactory.isTelemetryEnabled()
            ? TelemetryFactory.createTelemetry()
            : null;
    }

    static {
        buildFingerprint = TelemetryFactory.computeBuildFingerprint();
    }
}
