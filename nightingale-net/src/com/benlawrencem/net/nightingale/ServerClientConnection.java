package com.benlawrencem.net.nightingale;

import java.net.InetAddress;

public class ServerClientConnection {
	private int connectionId;
	private String clientAddress;
	private int clientPort;
	private InetAddress clientInetAddress;
	private PacketRecorder recorder;
	private long timeOfLastCommunication;

	public ServerClientConnection(int connectionId, String address, int port, InetAddress inetAddress) {
		this.connectionId = connectionId;
		clientAddress = address;
		clientPort = port;
		clientInetAddress = inetAddress;
		recorder = new PacketRecorder();
		timeOfLastCommunication = System.currentTimeMillis();
	}

	public int getClientId() {
		return connectionId;
	}

	public String getAddress() {
		return clientAddress;
	}

	public InetAddress getInetAddress() {
		return clientInetAddress;
	}

	public int getPort() {
		return clientPort;
	}

	public PacketRecorder getPacketRecorder() {
		return recorder;
	}

	public boolean matchesAddress(String address, int port) {
		if(address == null)
			return (clientAddress == null && port == clientPort);
		return (address.equals(clientAddress) && port == clientPort);
	}

	public long getTimeOfLastCommunication() {
		return timeOfLastCommunication;
	}

	public void resetTimeout() {
		timeOfLastCommunication = System.currentTimeMillis();
	}
}