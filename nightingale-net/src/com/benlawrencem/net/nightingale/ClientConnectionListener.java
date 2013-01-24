package com.benlawrencem.net.nightingale;

public interface ClientConnectionListener {
	void onConnected();
	void onDisconnected();
	void onReceive(String message);

	/**
	 * Called when the client fails to deliver a message to the server.
	 * 
	 * @param messageId the id of the message that couldn't be delivered, as
	 *  returned by {@link Server.send} and {@link Server.resend}
	 * @param resendMessageId the id of the message that the undelivered
	 *  message duplicates. If this method handles the undelivered message by
	 *  resending it, this should be passed into Server.resend. If the message
	 *  isn't a duplicate, resendMessageId will be equal to messageId
	 * @param message the message that couldn't be delivered
	 */
	void onMessageNotDelivered(int messageId, int resendMessageId, String message);
}