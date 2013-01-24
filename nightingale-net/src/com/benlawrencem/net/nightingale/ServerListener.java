package com.benlawrencem.net.nightingale;

public interface ServerListener {
	void onServerStarted();
	void onServerStopped();
	boolean onClientConnected(int clientId, String address, int port);
	void onClientDisconnectd(int clientId);
	void onReceive(int clientId, String message);
	void onMessageNotDelivered(int messageId, int clientId, String message);
}