package com.github.alei121.pubsub;

import java.io.ByteArrayInputStream;
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

import com.github.alei121.stomp.StompConnection;
import com.github.alei121.stomp.StompFrame;
import com.github.alei121.stomp.StompSubscription;

// Handles CONNECT, STOMP, SEND and SUBSCRIBE frames
public class StompHandler extends TextWebSocketHandler {
	private static Map<String, Queue<StompSubscription>> mapOfTopicToSubscriptions = new ConcurrentHashMap<>();
	
	public void connect(WebSocketSession session, StompFrame stomp) throws IOException {
		StompConnection connection = new StompConnection(stomp);
		StompFrame out = connection.getResponse();
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		out.write(ba);
		TextMessage message = new TextMessage(ba.toByteArray());
		session.sendMessage(message);
		if (out.getCommand() == StompFrame.Command.ERROR) {
			session.close(CloseStatus.NOT_ACCEPTABLE);
		}
	}
	
	public void subscribe(WebSocketSession session, StompFrame stomp) {
		StompSubscription subscription = new StompSubscription(stomp, session);
		Queue<StompSubscription> subscriptions = mapOfTopicToSubscriptions.get(subscription.getTopic());
		if (subscriptions == null) {
			subscriptions = new ConcurrentLinkedQueue<>();
			mapOfTopicToSubscriptions.put(subscription.getTopic(), subscriptions);
		}
		subscriptions.add(subscription);
	}
	
	private static AtomicInteger currentMessageID = new AtomicInteger();
	public void send(StompFrame stomp) throws IOException {
		String dest = stomp.getHeader("destination");
		Queue<StompSubscription> subscriptions = mapOfTopicToSubscriptions.get(dest);
		if (subscriptions != null) {
			stomp.setCommand(StompFrame.Command.MESSAGE);
			stomp.setHeader("message-id", Integer.toString(currentMessageID.getAndIncrement()));
			for (StompSubscription subscription : subscriptions) {
				stomp.setHeader("subscription", subscription.getId());
				ByteArrayOutputStream ba = new ByteArrayOutputStream();
				stomp.write(ba);
				TextMessage message = new TextMessage(ba.toByteArray());
				subscription.getSession().sendMessage(message);
			}
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {		
		StompFrame stomp = StompFrame.parse(new ByteArrayInputStream(message.getPayload().getBytes()));
		switch (stomp.getCommand()) {
		case CONNECT:
		case STOMP:
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
		for (Queue<StompSubscription> subscriptions : mapOfTopicToSubscriptions.values()) {
			Iterator<StompSubscription> i = subscriptions.iterator();
			while (i.hasNext()) {
				StompSubscription subscription = i.next();
				if (subscription.getSession() == session) {
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
