package com.benlawrencem.net.nightingale;

public interface ClientConnectionListener {
	void onConnected();
	void onDisconnected();
	void onReceive(String message);
	void onMessageNotDelivered(int messageId, String message);
}