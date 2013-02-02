package com.benlawrencem.net.nightingale;

public interface ClientConnectionListener {
	void onConnected();
	void onCouldNotConnect(String reason);
	void onDisconnected(String reason);
	void onReceive(String message);

	/**
	 * Called when a message to the server gets lost or fails to be delivered.
	 * The message could actually still be delivered after this method is
	 * called if it arrives at the server out of order. Resending the message
	 * using the {@link ClientConnection.resend} method will guarantee that if
	 * both the original message and the resent message are receiving, only
	 * one will be processed.
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