This module provides building blocks for HTTP-based solutions.

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

### Example

Below is an example of an echo chat:

```java
ReflectiveHttpServerRouteBuilder builder = new ReflectiveHttpServerRouteBuilder();
builder.addTargets("/test-chat/", new AbstractChatRoutes() {
    
    @Override
    protected Object getConfigurator() {
        Input text = getBootstrapFactory().getHTMLFactory().input(InputType.text);
        AlpineJs<Input> aText = getAlpineJsFactory().from(text);
        aText.model("config.test");
        return text;
    }
    
    @Override
    protected JSONObject getConfig() {
        JSONObject jsonConfig = super.getConfig();
        jsonConfig.put("test", "123");
        return jsonConfig;
    }

    @Override
    protected Mono<String> chatContent(
            HttpServerRequest request, 
            String chatId, 
            String question,
            JSONObject config, 
            JSONObject context) {
        return Mono.just("Here we go [" + chatId +"]: " + question + " | " + config + " | " + context);
    }
    
});

DisposableServer server = HttpServer
  .create()
  .route(builder::buildRoutes)
  .bindNow();       

URI resolvedUri = new URI("http://localhost:" + server.port() + "/").resolve("/test-chat/chat");            
Desktop.getDesktop().browse(resolvedUri);

try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
    LineReader lineReader = LineReaderBuilder
            .builder()
            .terminal(terminal)
            .build();
    
    String prompt = "http-server>";
    while (true) {
        String line = null;
        line = lineReader.readLine(prompt);
        System.out.println("Got: " + line);
        if ("exit".equals(line)) {
            break;
        }
    }
}
server.dispose();
server.onDispose().block();     
```
    
