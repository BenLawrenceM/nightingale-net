package com.benlawrencem.net.nightingale;

import java.net.InetAddress;

public class ClientInfo {
	private int connectionId;
	private String clientAddress;
	private int clientPort;
	private InetAddress clientInetAddress;
	private PacketRecorder recorder;
	private long timeOfLastCommunication;
	private long latency;

	public ClientInfo(int connectionId, String address, int port, InetAddress inetAddress) {
		this.connectionId = connectionId;
		clientAddress = address;
		clientPort = port;
		clientInetAddress = inetAddress;
		recorder = new PacketRecorder();
		timeOfLastCommunication = System.currentTimeMillis();
		latency = -1;
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

	public long getLatency() {
		return latency;
	}

	public void setLatency(long latency) {
		this.latency = latency;
	}
}