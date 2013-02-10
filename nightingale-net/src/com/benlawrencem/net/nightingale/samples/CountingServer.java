package com.benlawrencem.net.nightingale.samples;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import com.benlawrencem.net.nightingale.Server;
import com.benlawrencem.net.nightingale.Packet.CouldNotSendPacketException;
import com.benlawrencem.net.nightingale.Server.CouldNotStartServerException;
import com.benlawrencem.net.nightingale.ServerListener;

public class CountingServer implements ServerListener {
	public static void main(String[] args) {
		int port = 9876;
		if(args != null && args.length > 0 && args[0] != null && !args[0].equals("") && !args[0].equals("-") && !args[0].equalsIgnoreCase("default")) {
			try {
				port = Integer.parseInt(args[0]);
			} catch(NumberFormatException e) {
				port = 9876;
			}
		}
		Level logLevel = Level.OFF;
		if(args != null && args.length > 1 && args[1] != null && !args[1].equals("") && !args[1].equals("-") && !args[1].equalsIgnoreCase("default") && !args[1].equalsIgnoreCase("off")) {
			if(args[1].equalsIgnoreCase("fine"))
				logLevel = Level.FINE;
			else if(args[1].equalsIgnoreCase("finer"))
				logLevel = Level.FINER;
			else if(args[1].equalsIgnoreCase("finest"))
				logLevel = Level.FINEST;
			else if(args[1].equalsIgnoreCase("all"))
				logLevel = Level.ALL;
			else
				logLevel = Level.OFF;
		}
		Logger.getLogger("").addHandler(new StreamHandler() { public void publish(LogRecord record) { System.out.println(record.getLoggerName() + "  " + record.getMessage()); } });
		Logger.getLogger("").setLevel(logLevel);

		CountingServer server = new CountingServer();
		server.startServer(port);
	}


	private Server server;

	public CountingServer() {
		server = new Server(this);
	}

	public void startServer(int port) {
		System.out.println("Starting server on port " + port);
		try {
			server.startServer(port);
		} catch (CouldNotStartServerException e) {
			System.out.println("Could not start server: " + e.getMessage());
		}
	}

	public void onServerStopped() {
		System.out.println("Server stopped");
	}

	public boolean onClientConnected(int clientId, String address, int port) {
		System.out.println("Client " + clientId + " connected from " + address + ":" + port);
		return true;
	}

	public void onClientDisconnected(int clientId, String reason) {
		System.out.println("Client " + clientId + " disconnected: " + reason);
	}

	public void onReceive(int clientId, String message) {
		System.out.println("Received " + message + " from client " + clientId);
		try {
			int x = Integer.parseInt(message) + 1;
			System.out.println("Sending  " + x + " to client " + clientId);
			try {
				server.send(clientId, "" + x);
			} catch (CouldNotSendPacketException e) {
				System.out.println("Could not send " + x + " to client " + clientId + ": " + e.getMessage());
			}
		} catch(NumberFormatException e) {
			server.dropClient(clientId, "Sent non-integer packet.");
			System.out.println("Dropped client " + clientId + " for sending non-integer");
		}
	}

	public void onMessageNotDelivered(int messageId, int resendMessageId, int clientId, String message) {
		System.out.println("Resending " + message + " to client " + clientId);
		try {
			server.resend(clientId, resendMessageId, message);
		} catch (CouldNotSendPacketException e) {
			System.out.println("Could not resend " + message + " to client " + clientId + ": " + e.getMessage());
		}
	}
}