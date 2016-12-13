package com.github.alei121.stomp;

import org.springframework.web.socket.WebSocketSession;

public class StompSubscription {
	private String id;
	private String topic;
	private WebSocketSession session;

	public StompSubscription(StompFrame stomp, WebSocketSession session) {
		this.session = session;
		id = stomp.getHeader("id");
		topic = stomp.getHeader("destination");
	}
	
	public String getId() {
		return id;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public WebSocketSession getSession() {
		return session;
	}
}