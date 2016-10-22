# stomp-pubsub-service
A simple pubsub service using STOMP over WebSocket

The pubsub service uses Spring Framework's [WebSocket](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/socket/config/annotation/WebSocketConfigurer.html) to handle WebSocket connections.

[STOMP messages](https://stomp.github.io/index.html) are parsed from [TextMessage](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/socket/TextMessage.html).

For simplicity, only CONNECT, SUBSCRIBE and SEND are handled.

A web page is provided to act as a client to publish and subscribe.

### Compile with:
  `mvn clean install`
  
### Run with:
*  `java -jar target/stomp-pubsub-service-0.0.1-SNAPSHOT.war`
*  Open multiple browser windows to `http://localhost:8080/index.html`
*  `Connect`, type a message and `Publish`
*  All windows will show the published message

Alternatively, to start service in tomcat, copy `target/stomp-pubsub-service-0.0.1-SNAPSHOT.war` to tomcat webapp directory
