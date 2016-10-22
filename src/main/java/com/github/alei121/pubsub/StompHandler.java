package com.github.alei121.pubsub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.github.alei121.stomp.StompCommand;
import com.github.alei121.stomp.StompConnection;
import com.github.alei121.stomp.StompMessage;
import com.github.alei121.stomp.StompSubscription;

// TODO incomplete!!!
public class StompHandler extends TextWebSocketHandler {
	private static Map<String, Queue<StompSubscription<WebSocketSession>>> mapOfTopicToSubscriptions = new ConcurrentHashMap<>();
	
	public void connect(WebSocketSession session, StompMessage stomp) throws IOException {
		StompConnection connection = new StompConnection(stomp);
		StompMessage out = connection.getResponse();
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		out.write(ba);
		TextMessage message = new TextMessage(ba.toByteArray());
		session.sendMessage(message);
		if (out.getCommand() == StompCommand.ERROR) {
			session.close(CloseStatus.NOT_ACCEPTABLE);
		}
	}
	
	public void subscribe(WebSocketSession session, StompMessage stomp) {
		StompSubscription<WebSocketSession> subscription = new StompSubscription<>(stomp, session);
		Queue<StompSubscription<WebSocketSession>> subscriptions = mapOfTopicToSubscriptions.get(subscription.getDestination());
		if (subscriptions == null) {
			subscriptions = new ConcurrentLinkedQueue<>();
			mapOfTopicToSubscriptions.put(subscription.getDestination(), subscriptions);
		}
		subscriptions.add(subscription);
	}
	
	private static AtomicInteger currentMessageID = new AtomicInteger();
	public void send(StompMessage stomp) throws IOException {
		String dest = stomp.getAttribute("destination");
		Queue<StompSubscription<WebSocketSession>> subscriptions = mapOfTopicToSubscriptions.get(dest);
		if (subscriptions != null) {
			stomp.setCommand(StompCommand.MESSAGE);
			stomp.setAttribute("message-id", Integer.toString(currentMessageID.getAndIncrement()));
			for (StompSubscription<WebSocketSession> subscription : subscriptions) {
				// TODO reconstructing everytime!!!
				stomp.setAttribute("subscription", subscription.getId());
				ByteArrayOutputStream ba = new ByteArrayOutputStream();
				stomp.write(ba);
				TextMessage message = new TextMessage(ba.toByteArray());
				subscription.getConnection().sendMessage(message);
			}
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {		
		StompMessage stomp = StompMessage.parse(message.getPayload());
		switch (stomp.getCommand()) {
		case CONNECT:
			connect(session, stomp);
			break;
		case SUBSCRIBE:
			subscribe(session, stomp);
			break;
		case SEND:
			send(stomp);
			break;
		default:
			break;
		}
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session,	CloseStatus status) throws Exception {
		for (Queue<StompSubscription<WebSocketSession>> subscriptions : mapOfTopicToSubscriptions.values()) {
			Iterator<StompSubscription<WebSocketSession>> i = subscriptions.iterator();
			while (i.hasNext()) {
				StompSubscription<WebSocketSession> subscription = i.next();
				if (subscription.getConnection() == session) {
					i.remove();
				}
			}
		}
	}
	
	@Override
	public boolean supportsPartialMessages() {
		return true;
	}
	
	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception)
			throws Exception {
		session.close(CloseStatus.SERVER_ERROR);
	}
}
