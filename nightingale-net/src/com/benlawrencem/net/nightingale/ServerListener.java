package com.benlawrencem.net.nightingale;

public interface ServerListener {
	void onServerStopped();
	boolean onClientConnected(int clientId, String address, int port);
	void onClientDisconnected(int clientId, String reason);
	void onReceive(int clientId, String message);

	/**
	 * Called when the server fails to deliver a message to a client.
	 * 
	 * @param messageId the id of the message that couldn't be delivered, as
	 *  returned by {@link Server.send} and {@link Server.resend}
	 * @param resendMessageId the id of the message that the undelivered
	 *  message duplicates. If this method handles the undelivered message by
	 *  resending it, this should be passed into Server.resend. If the message
	 *  isn't a duplicate, resendMessageId will be equal to messageId
	 * @param clientId the id of the client that this message was intended for
	 * @param message the message that couldn't be delivered
	 */
	void onMessageNotDelivered(int messageId, int resendMessageId, int clientId, String message);
}