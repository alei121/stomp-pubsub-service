package com.github.alei121.stomp;


public class StompSubscription<T> {
	private String id;
	private String destination;
	private T connection;

	public StompSubscription(StompMessage stomp, T connection) {
		this.connection = connection;
		id = stomp.getAttribute("id");
		destination = stomp.getAttribute("destination");
	}
	
	public String getId() {
		return id;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public T getConnection() {
		return connection;
	}
}