Java bindings for [Alpine.js](https://alpinejs.dev/).

* [Sources](https://github.com/Nasdanika/html/tree/master/alpinejs)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.html/alpinejs)

## Example - chat application

This section explains how to build a simple chat application. 
Source code - [ChatServerCommand](https://github.com/Nasdanika-Demos/cli/blob/main/src/main/java/org/nasdanika/launcher/demo/ai/ChatServerCommand.java):

* [home()](https://github.com/Nasdanika-Demos/cli/blob/main/src/main/java/org/nasdanika/launcher/demo/ai/ChatServerCommand.java#L171) method builds the application - 80 lines of code
* [chat()](https://github.com/Nasdanika-Demos/cli/blob/main/src/main/java/org/nasdanika/launcher/demo/ai/ChatServerCommand.java#L247) method serves chat requests, not specific to the Alpine.js functionality


### home()

```java
BootstrapFactory bootstrapFactory = BootstrapFactory.INSTANCE;
AlpineJsFactory alpineJsFactory = AlpineJsFactory.INSTANCE;

HTMLPage page = bootstrapFactory.bootstrapCdnHTMLPage();        
alpineJsFactory.cdn(page);
Container chatApp = bootstrapFactory.container();
JSONObject appData = alpineJsFactory.from(chatApp.toHTMLElement()).data();
JSONArray messagesArray = new JSONArray();
appData
    .put("messages", messagesArray)
    .put("text", "");

page.body(chatApp);

// Chat message cards
Card messageCard = bootstrapFactory.card();
messageCard.margin().bottom(Breakpoint.DEFAULT, Size.S1);
Tag messageCardHtmlElement = messageCard.toHTMLElement();
alpineJsFactory
    .from(messageCardHtmlElement)
    .bind("class", "'border-' + message.style");
messageCard.border(Color.DEFAULT);      
Tag messageCardBody = messageCard.getBody().toHTMLElement();
messageCardBody.content("Loading...");
alpineJsFactory
    .from(messageCardBody)
    .html("message.content");               
Tag messagesFor = alpineJsFactory._for("message in messages", messageCardHtmlElement);      
chatApp.row().col().content(messagesFor);

// Text area
TextArea textArea = bootstrapFactory.getHTMLFactory().textArea();
textArea
    .name("userInput")
    .placeholder("Ask me anything about TOGAF 10");
InputGroup textAreaInputGroup = bootstrapFactory
    .inputGroup()
    .input(textArea)
    .prepend("Chat");
alpineJsFactory.from(textArea).model("text");
Button submitButton = bootstrapFactory.getHTMLFactory().button("Submit");

String submitHandler = """
    messages.push({
        content: text,
        style: 'primary'
    });
    
    var responseMessage = Alpine.reactive({
        content: 'Processing...',
        style: 'muted'
    });
    messages.push(responseMessage);                     
    
    fetch("chat", {
        method: 'POST',
        body: text
    }).then(response => {
        if (response.ok) {
            response.json().then(responseJson => {
                responseMessage.content = responseJson.content;
                responseMessage.style = responseJson.style;
            });
        } else {
            responseMessage.content = response.status + ": " + response.statusText;
            responseMessage.style = 'danger';
        }
    });
    text = '';
    """;

alpineJsFactory
    .from(submitButton)
    .on("click", submitHandler)
    .bind("disabled", "!text");
org.nasdanika.html.bootstrap.Button<Button> bootstrapSubmitButton = bootstrapFactory.button(submitButton, Color.PRIMARY, false);
textAreaInputGroup.append(bootstrapSubmitButton);
chatApp.row().col().content(textAreaInputGroup);
return page.toString();
```

First, two factories are created - [BootstrapFactory](https://github.com/Nasdanika/html/blob/master/bootstrap/src/main/java/org/nasdanika/html/bootstrap/BootstrapFactory.java) and [AlpineJsFactory](https://github.com/Nasdanika/html/blob/master/alpinejs/src/main/java/org/nasdanika/html/alpinejs/AlpineJsFactory.java).

Then a bootstrap HTML page is created with bootstrap initialization script and stylesheet and Alpine.js script is added to the page.

After that UI elements are built using the Bootstrap factory and dynamic behavior is added to them by wrapping them into [AlpineJs](https://github.com/Nasdanika/html/blob/master/alpinejs/src/main/java/org/nasdanika/html/alpinejs/AlpineJs.java) and then calling one of AlpineJs instance methods.
The ``for`` loop for messages is created by calling ``AlpineJsFactory._for`` - template methods ``_for``, ``_if`` and ``teleport`` are defined in the factory because they wrap HTML element instance into a ``<template>`` tag.


Application Javascript code is just 26 lines and is defined in the code.
Larger Javascript sources can be loaded from classloader resources.




