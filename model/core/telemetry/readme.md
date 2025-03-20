TODO - OpenTelemetry capability, autoconfiguraiton with properties/env vars

## Quick start

Create a VM, enable HTTP(S) traffic on ports 3000, 4317, 4318

sudo bash
apt install podman
podman pull docker.io/grafana/otel-lgtm



https://hub.docker.com/r/grafana/otel-lgtm


-Dotel.metrics.exporter=otlp -Dotel.logs.exporter=otlp -Dotel.traces.exporter=otlp -Dotel.exporter.otlp.endpoint=http://34.60.154.10:4317 -Dotel.service.name=<class name>

-ea -Dotel.metrics.exporter=otlp -Dotel.logs.exporter=otlp -Dotel.traces.exporter=otlp -Dotel.exporter.otlp.endpoint=http://34.60.154.10:4317 -Dotel.service.name=org.nasdanika.telemetry.tests.TestTelemetry

TODO - diagram, in an organization - shared collector.

```java
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
CapabilityLoader capabilityLoader = new CapabilityLoader();
try {
    Requirement<Object, OpenTelemetry> requirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(requirement, progressMonitor);
    
    Meter meter = openTelemetry.getMeter(getClass().getModule().getName());
    meter
        .counterBuilder("my-counter")
        .setDescription("My test counter")
        .setUnit("my-unit")
        .build()
        .add(25);
    
    Tracer tracer = openTelemetry.getTracer(getClass().getModule().getName());
    Span span = tracer
        .spanBuilder("something important")
        .setAttribute("importance", 33)
        .setAttribute("service_name", "telemetry-test")
        .startSpan();
    
    try {
        Thread.sleep(500);
    } finally {
        span.end();
    }
    
    Logger logger = openTelemetry.getLogsBridge().get("my-logger");
    
    logger          
        .logRecordBuilder()
        .setSeverity(Severity.ERROR)
        .setBody("My log message")
        .setAttribute("my-attribute", 88)
        .emit();
    
    
    Thread.sleep(5000);
} finally {             
    capabilityLoader.close(progressMonitor);
}
```