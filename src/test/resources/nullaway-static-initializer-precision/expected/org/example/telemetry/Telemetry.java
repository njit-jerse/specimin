package org.example.telemetry;

class Telemetry {

    private static final Telemetry active;

    static {
        active = TelemetryFactory.isTelemetryEnabled()
            ? TelemetryFactory.createTelemetry()
            : null;
    }
}
