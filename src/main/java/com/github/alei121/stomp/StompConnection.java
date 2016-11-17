package com.github.alei121.stomp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StompConnection {
	private static String supportedVersionString = "1.0,1.1,1.2";
	private static Set<String> supportedVersions = new HashSet<>(Arrays.asList(supportedVersionString.split(",")));
	
	String useVersion;
	
	public StompConnection(StompMessage message) {
		String acceptVersion = message.getAttribute("accept-version");
		if (acceptVersion != null) {
			String[] versions = acceptVersion.split(",");

			useVersion = null;
			for (String version : versions) {
				if (supportedVersions.contains(version)) {
					if (useVersion == null) {
						useVersion = version;
					} else {
						// Use the max version
						if (version.compareTo(useVersion) > 0) {
							useVersion = version;
						}
					}
				}
			}
		} else {
			useVersion = "1.0";
		}
	}
	
	public StompMessage getResponse() {
		StompMessage message = new StompMessage();
		if (useVersion != null) {
			message.setCommand(StompMessage.Command.CONNECTED);
			message.setAttribute("version", useVersion);
			// TODO heartbeat!!!
			message.setAttribute("heart-beat", "0,0");
		}
		else {
			message.setCommand(StompMessage.Command.ERROR);
			message.setAttribute("version", supportedVersionString);
			String text = "Supported protocol versions are " + supportedVersionString;
			message.setContent(text.getBytes());
		}
		return message;
	}
}

