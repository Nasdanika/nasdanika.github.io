This module provides building blocks for HTTP-based AI solutions.

* [Sources](https://github.com/Nasdanika/html/tree/main/http)
* [Maven Central](https://central.sonatype.com/artifact/org.nasdanika.html/http)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.html/http)


## Chat

The module provides several classes for building [Bootstrap](../../html/bootstrap/index.html)/[AlpineJS](../../html/alpine-js/index.html) chat Web UIs:

* ``ChatBuilder`` - builds chat UI with an optional configuration dialog. 
* ``AbstractChatRoutes`` - extends ``ChatBuilder`` and provides two HTTP routes:
    * ``GET`` - which builds chat UI
    * ``POST`` - processes chat requests
* ``AbstractTelemetryChatRoutes`` - extends ``AbstractChatRoutes``, takes ``TelemetryFilter`` as a constructor argument for collecting [telemetry](../../core/telemetry/index.html).

The ``AbstractAIChatRoutes`` class in the [AI HTTP](../../ai/ai-http/index.html) module extends the ``AbstractTelemetryChatRoutes`` and takes ``Chat`` as a constructor argument. 
    
