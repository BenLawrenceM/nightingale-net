package com.benlawrencem.net.nightingale.samples;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import com.benlawrencem.net.nightingale.ClientConnection;
import com.benlawrencem.net.nightingale.Packet.CouldNotSendPacketException;

public class CountingClient extends ClientConnection {
	public static void main(String[] args) {
		String address = "198.144.185.152";
		if(args != null && args.length > 0 && args[0] != null && !args[0].equals("") && !args[0].equals("-") && !args[0].equalsIgnoreCase("default"))
			address = "198.144.185.152";
		int port = 9876;
		if(args != null && args.length > 1 && args[1] != null && !args[1].equals("") && !args[1].equals("-") && !args[1].equalsIgnoreCase("default")) {
			try {
				port = Integer.parseInt(args[1]);
			} catch(NumberFormatException e) {
				port = 9876;
			}
		}
		Level logLevel = Level.OFF;
		if(args != null && args.length > 2 && args[2] != null && !args[2].equals("") && !args[2].equals("-") && !args[2].equalsIgnoreCase("default") && !args[2].equalsIgnoreCase("off")) {
			if(args[2].equalsIgnoreCase("fine"))
				logLevel = Level.FINE;
			else if(args[2].equalsIgnoreCase("finer"))
				logLevel = Level.FINER;
			else if(args[2].equalsIgnoreCase("finest"))
				logLevel = Level.FINEST;
			else if(args[2].equalsIgnoreCase("all"))
				logLevel = Level.ALL;
			else
				logLevel = Level.OFF;
		}
		Logger.getLogger("").addHandler(new StreamHandler() { public void publish(LogRecord record) { System.out.println(record.getLoggerName() + "  " + record.getMessage()); } });
		Logger.getLogger("").setLevel(logLevel);

		CountingClient client = new CountingClient();
		client.connect(address, port);
	}


	public CountingClient() {}
	
	public void connect(String address, int port) {
		System.out.println("Connecting to " + address + ":" + port);
		try {
			super.connect(address, port);
		} catch (CouldNotConnectException e) {
			System.out.println("Could not connect: " + e.getMessage());
		}
	}

	protected void onConnected() {
		System.out.println("Connected!");
		System.out.println("Sending 1");
		try {
			send("1");
		} catch (CouldNotSendPacketException e) {
			System.out.println("Could not send packet: " + e.getMessage());
			disconnect();
		}
	}

	protected void onCouldNotConnect(String reason) {
		System.out.println("Could not connect: " + reason);
	}

	protected void onDisconnected(String reason) {
		System.out.println("Disconnected: " + reason);
	}

	protected void onReceive(String message) {
		System.out.println("Received " + message + (getLatency() > -1 ? " [" + getLatency() + "ms]" : ""));
		try {
			int x = Integer.parseInt(message) + 1;
			System.out.println("Sending  " + x + (getLatency() > -1 ? " [" + getLatency() + "ms]" : ""));
			try {
				send("" + x);
			} catch (CouldNotSendPacketException e) {
				System.out.println("Could not send " + x + ": " + e.getMessage());
				disconnect();
			}
		} catch(NumberFormatException e) {
			System.out.println("Received non-integer");
			disconnect();
		}
	}

	protected void onMessageNotDelivered(int messageId, int resendMessageId, String message) {
		System.out.println("Resending " + message);
		try {
			resend(resendMessageId, message);
		} catch (CouldNotSendPacketException e) {
			System.out.println("Could not resend packet: " + e.getMessage());
			disconnect();
		}
	}
}