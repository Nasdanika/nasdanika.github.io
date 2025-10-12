This module provides building blocks for HTTP-based AI solutions.

* [Sources](https://github.com/Nasdanika/ai/tree/main/http)
* [Maven Central](https://central.sonatype.com/artifact/org.nasdanika.ai/http)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.ai/http)


## Chat

The module provides a class for building [Bootstrap](../../html/bootstrap/index.html)/[AlpineJS](../../html/alpine-js/index.html) AI chat Web UIs - ``AbstractAIChatRoutes``.
The class extends ``AbstractTelemetryChatRoutes`` from the [HTML/HTTP](../../html/html-http/index.html) module and takes ``Chat`` as a constructor argument. 
Subclasses shall implement two abstract methods:
    * ``Mono<List<Chat.Message>> generateChatRequestMessages(String chatId, String question, JSONObject config, JSONObject context)``
    * ``Mono<String> generateResponseContent(String chatId, String question, List<? extends Chat.ResponseMessage> responses, JSONObject config, JSONObject context)``
    
Below is a code snippet of a server with with gpt-5 chat:

```java    
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
Tracer tracer = openTelemetry.getTracer(TestAI.class.getName() + ".testChatServerWithTelemetry");
TelemetryFilter telemetryFilter = new TelemetryFilter(
        tracer, 
        openTelemetry.getPropagators().getTextMapPropagator(), 
        (k, v) -> System.out.println(k + ": " + v), 
        true);

Chat.Requirement cReq = new Chat.Requirement("OpenAI", "gpt-5", null);
Requirement<Chat.Requirement, Chat> chatRequirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);           
Chat chat = capabilityLoader.loadOne(chatRequirement, progressMonitor);
        
ReflectiveHttpServerRouteBuilder builder = new ReflectiveHttpServerRouteBuilder();
builder.addTargets("/test-chat/", new AbstractAIChatRoutes(telemetryFilter, chat) {
    
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
        jsonConfig.put("chat-provider", chat.getProvider());
        jsonConfig.put("chat-model", chat.getName());
        return jsonConfig;
    }

    @Override
    protected Mono<List<org.nasdanika.ai.Chat.Message>> generateChatRequestMessages(
            String chatId,
            String question,
            JSONObject config,
            JSONObject context) {
        return Mono.just(List.of(
            Chat.Role.system.createMessage("You are a helpful assistant. You you will be provided a user question. Answer in Markdown format with references to resources you used."),
            Chat.Role.user.createMessage(question))
        );
    }

    @Override
    protected Mono<String> generateResponseContent(
            String chatId, 
            String question,
            List<? extends ResponseMessage> responses, 
            JSONObject config,
            JSONObject context) {
        String responseContent = responses.get(0).getContent();
        return Mono.just(MarkdownHelper.INSTANCE.markdownToHtml(responseContent));
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

* [Response screenshot](../../demos/ai/chat-response.png)
* [Telemetry screenshot](../../demos/ai/chat-telemetry.png)

The above example is hardcoded for GPT-5. It can be modified to collect all available chat models
to allow user to select a model to use in the configuration dialog.
It can also be modified to provide contextual information in ``generateChatRequestMessages``.
Such information can include data from the context object sent as part of the chat request.
This approach allows to generate multiple static chat pages with "baked-in" context and use a shared 
chat route. 

One application would be chat pages for generated documentation from models and other sources with 
a chat page per model element, say, [Internet Banking System](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/index.html). 
Context may include a "narration" of the model element and related model elements.r
