package com.github.alei121.stomp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/*
 * 
 * Note for WebSocket:
 * If input comes as WebSocket text type, (WS RFC says Text is UTF-8)
 * server side handling code like Spring TextMessage may convert the bytes to String as UTF-8
 * which maybe the wrong encoding as STOMP message itself can use other encoding.
 * 
 * e.g. A particular encoding may have bytes: FF FF FF FF FF FF FF FF FF FF... 10, that is completely out of range for Unicode.
 * 
 * Unless STOMP message body is also UTF-8, STOMP messages must be sent as binary
 */
public class StompMessage {
	StompCommand command;
	Map<String, String> attributes = new HashMap<>();
	byte[] content;
	
	public StompCommand getCommand() {
		return command;
	}
	
	public void setCommand(StompCommand command) {
		this.command = command;
	}
	
	public String getAttribute(String name) {
		return attributes.get(name);
	}
	
	public void setAttribute(String name, String value) {
		attributes.put(name, value);
	}
	
	public void setContent(byte[] content) {
		this.content = content;
	}
	
	public void write(OutputStream out) throws IOException {
		out.write(command.name().getBytes());
		out.write('\n');
		for (String name : attributes.keySet()) {
			out.write(name.getBytes());
			out.write(':');
			out.write(attributes.get(name).getBytes());
			out.write('\n');
		}
		out.write('\n');
		if (content != null) {
			out.write(content);
		}
		out.write(0);
	}
	
	private static String readLine(InputStream in) throws IOException {
		byte[] line = new byte[1024];
		int index = 0;
		// TODO line too long!!!
		while (index < 1024) {
			int b = in.read();
			if (b != -1) {
				if (b == '\n') break;
				if (b != '\r') {
					line[index] = (byte)b;
					index++;
				}
			}
			else {
				return null;
			}
		}
		return new String(line, 0, index);			
	}
	
	public static StompMessage parse(String message) throws IOException {
		return parse(new ByteArrayInputStream(message.getBytes()));
	}

	/*
	 * Using InputStream instead of Reader because
	 * content-length is octet count instead of character count
	 */
	public static StompMessage parse(InputStream reader) throws IOException {
		StompMessage stomp = new StompMessage();
		
		String line = readLine(reader);
		StompCommand command = StompCommand.valueOf(line);
		stomp.setCommand(command);
		
		// Attributes
		int contentLength = -1;
		while ((line = readLine(reader)) != null) {
			if (line.equals("")) break;
			int colon = line.indexOf(':');
			String name = line.substring(0, colon);
			String value = line.substring(colon + 1);
			stomp.setAttribute(name, value);
			if (name.equals("content-length")) {
				contentLength = Integer.parseInt(value);
			}
		}
		
		// Content
		if (contentLength != -1) {
			// content-length is octet
			byte[] content = new byte[contentLength];
			reader.read(content);
			stomp.setContent(content);
			// TODO check last byte
			reader.read();
		}
		else {
			byte[] buffer = new byte[1024];
			int length = 0;
			while (length < 1024) {
				int b = reader.read();
				if (b == -1) {
					// TODO premature end of stream
					break;
				}
				if (b == 0) break;
				buffer[length] = (byte)b;
				length++;
			}
			if (length > 0) {
				byte[] content = new byte[length];
				System.arraycopy(buffer, 0, content, 0, length);
				stomp.setContent(content);
			}
		}
		return stomp;
	}
}
