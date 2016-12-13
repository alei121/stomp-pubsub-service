package com.github.alei121.stomp;


public class StompSubscription<T> {
	private String id;
	private String destination;
	private T connection;

	public StompSubscription(StompFrame stomp, T connection) {
		this.connection = connection;
		id = stomp.getHeader("id");
		destination = stomp.getHeader("destination");
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