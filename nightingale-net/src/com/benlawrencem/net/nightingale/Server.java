package com.benlawrencem.net.nightingale;

public class Server {
	public void startServer(int port) {
		//TODO implement
	}

	public boolean isRunning() {
		//TODO implement
		return false;
	}

	public void stopServer() {
		//TODO implement
	}

	public int[] getClientIds() {
		//TODO implement
		return null;
	}

	public void dropClient(int clientId) {
		//TODO implement
	}

	public int send(int clientId, String message) {
		//TODO implement
		return -1;
	}

	public int resend(int originalMessageId, int clientId, String message) {
		//TODO implement
		return -1;
	}
}