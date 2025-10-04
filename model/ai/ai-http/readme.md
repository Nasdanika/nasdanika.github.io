This module provides building blocks for HTTP-based AI solutions.

* [Sources](https://github.com/Nasdanika/ai/tree/main/http)
* [Maven Central](https://central.sonatype.com/artifact/org.nasdanika.ai/http)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.ai/http)


## Chat

The model provides several classes for building [Bootstrap](../../html/bootstrap/index.html)/[AlpineJS](../../html/alpine-js/index.html) chat Web UIs:

* ``ChatBuilder`` - builds chat UI with an optional configuration dialog. 
* ``AbstractChatRoutes`` - extends ``ChatBuilder`` and provides two HTTP routes:
    * ``GET`` - which builds chat UI
    * ``POST`` - processes chat requests
* ``AbstractTelemetryChatRoutes`` - extends ``AbstractChatRoutes``, takes ``TelemetryFilter`` as a constructor argument for collecting [telemetry](../../core/telemetry/index.html).
* ``AbstractAIChatRoutes`` extends ``AbstractTelemetryChatRoutes`` and takes ``Chat`` as a constructor argument. Subclasses shall implement two abstract methods:
    * ``Mono<List<Chat.Message>> generateChatRequestMessages(String chatId, String question, JSONObject config)``
    * ``Mono<String> generateResponseContent(String chatId, String question, List<? extends Chat.ResponseMessage> responses, JSONObject config)``
    
Below is a code snippet of a server with an echo chat:

```java    
ReflectiveHttpServerRouteBuilder builder = new ReflectiveHttpServerRouteBuilder();
builder.addTargets("/test-chat/", new AbstractAIChatRoutes(null, Chat.ECHO) {
    
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
    protected Mono<List<org.nasdanika.ai.Chat.Message>> generateChatRequestMessages(
            String chatId,
            String question,
            JSONObject config) {
        return Mono.just(List.of(Chat.Role.user.createMessage(question)));
    }

    @Override
    protected Mono<String> generateResponseContent(
            String chatId, 
            String question,
            List<? extends ResponseMessage> responses, 
            JSONObject config) {
        return Mono.just("Here we go [" + chatId +"]: " + question + " | " + responses.get(0).getContent() + " | " + config);
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
    
