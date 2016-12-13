package com.github.alei121.stomp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StompConnection {
	private static String supportedVersionString = "1.0,1.1,1.2";
	private static Set<String> supportedVersions = new HashSet<>(Arrays.asList(supportedVersionString.split(",")));
	
	String useVersion;
	
	public StompConnection(StompFrame message) {
		String acceptVersion = message.getHeader("accept-version");
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
	
	public StompFrame getResponse() {
		StompFrame message = new StompFrame();
		if (useVersion != null) {
			message.setCommand(StompFrame.Command.CONNECTED);
			message.setHeader("version", useVersion);
			// TODO heartbeat!!!
			message.setHeader("heart-beat", "0,0");
		}
		else {
			message.setCommand(StompFrame.Command.ERROR);
			message.setHeader("version", supportedVersionString);
			String text = "Supported protocol versions are " + supportedVersionString;
			message.setContent(text.getBytes());
		}
		return message;
	}
}

