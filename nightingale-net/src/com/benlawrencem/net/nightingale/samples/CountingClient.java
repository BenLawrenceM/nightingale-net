package com.benlawrencem.net.nightingale.samples;

import com.benlawrencem.net.nightingale.ClientConnection;
import com.benlawrencem.net.nightingale.ClientConnection.CouldNotConnectException;
import com.benlawrencem.net.nightingale.ClientConnection.CouldNotSendPacketException;
import com.benlawrencem.net.nightingale.ClientConnectionListener;

public class CountingClient implements ClientConnectionListener {
	public static void main(String[] args) {
		new CountingClient("5.5.5.5", 9876);
	}

	private ClientConnection conn;

	public CountingClient(String address, int port) {
		System.out.println("Connecting to " + address + ":" + port + "...");
		conn = new ClientConnection(this);
		try {
			conn.connect(address, port);
		} catch (CouldNotConnectException e) {
			System.out.println("Could not connect: " + e.getMessage());
		}
	}

	public void onConnected() {
		System.out.println("Connected!");
		System.out.println("Sending 1");
		try {
			conn.send("1");
		} catch (CouldNotSendPacketException e) {
			System.out.println("Could not send packet: " + e.getMessage());
			conn.disconnect();
		}
	}

	public void onCouldNotConnect(String reason) {
		System.out.println("Could not connect: " + reason);
	}

	public void onDisconnected(String reason) {
		System.out.println("Disconnected: " + reason);
	}

	public void onReceive(String message) {
		System.out.println("Received " + message);
		try {
			int x = Integer.parseInt(message) + 1;
			System.out.println("Sending " + x);
			try {
				conn.send("" + x);
			} catch (CouldNotSendPacketException e) {
				System.out.println("Could not send packet: " + e.getMessage());
				conn.disconnect();
			}
		} catch(NumberFormatException e) {
			System.out.println("Could not send packet: Received packet is not an integer.");
			conn.disconnect();
		}
	}

	public void onMessageNotDelivered(int messageId, int resendMessageId, String message) {
		System.out.println("Resending " + message);
		try {
			conn.resend(resendMessageId, message);
		} catch (CouldNotSendPacketException e) {
			System.out.println("Could not resend packet: " + e.getMessage());
			conn.disconnect();
		}
	}
}