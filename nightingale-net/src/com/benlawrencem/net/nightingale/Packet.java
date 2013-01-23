package com.benlawrencem.net.nightingale;

public class Packet {
	private static final int PROTOCOL_ID = 103675707;
	public static final int ANONYMOUS_CONNECTION_ID = 0, MINIMUM_CONNECTION_ID = 1, MAXIMUM_CONNECTION_ID = 255;
	public static final int SEQUENCE_NUMBER_NOT_APPLICALBE = 0, MINIMUM_SEQUENCE_NUMBER = 1, MAXIMUM_SEQUENCE_NUMBER = 65535;
	private static final byte MESSAGE_TYPE_INVALID = 0, MESSAGE_TYPE_APPLICATION = -128, MESSAGE_TYPE_PING = -127,
			MESSAGE_TYPE_PING_RESPONSE = -126, MESSAGE_TYPE_CONNECT_REQUEST = -125, MESSAGE_TYPE_CONNECTION_ACCEPTED = -124,
			MESSAGE_TYPE_CONNECTION_REFUSED = -123, MESSAGE_TYPE_FORCE_DISCONNECT = -122, MESSAGE_TYPE_CLIENT_DISCONNECT = -121;
	public static enum MessageType {
		INVALID, APPLICATION, PING, PING_RESPONSE, CONNECT_REQUEST, CONNECTION_ACCEPTED,
		CONNECTION_REFUSED, FORCE_DISCONNECT, CLIENT_DISCONNECT
	};

	private int protocolId, connectionId, sequenceNumber, duplicateSequenceNumber, lastReceivedSequenceNumber, receivedPacketHistory;
	private boolean isImmediateResponse;
	private MessageType messageType;
	private String message;

	/* Packet structure:
	 	int		4 bytes	protocolId
	 	byte	1 byte	connectionId
	 	short	2 bytes	sequenceNumber
	 	short	2 bytes	duplicateSequenceNumber
	 	short	2 bytes	lastReceivedSequenceNumber
	 	int		4 bytes	receivedPacketHistory
	 	byte	1 byte	packetFlags
	 	byte	1 byte	messageType
	 	String	n bytes	message
	 */

	private Packet() {
		protocolId = Packet.PROTOCOL_ID;
		connectionId = Packet.ANONYMOUS_CONNECTION_ID;
		sequenceNumber = Packet.SEQUENCE_NUMBER_NOT_APPLICALBE;
		duplicateSequenceNumber = Packet.SEQUENCE_NUMBER_NOT_APPLICALBE;
		lastReceivedSequenceNumber = Packet.SEQUENCE_NUMBER_NOT_APPLICALBE;
		receivedPacketHistory = 0;
		isImmediateResponse = false;
		messageType = MessageType.INVALID;
		message = null;
	}

	public boolean isValidProtocol() {
		return protocolId == Packet.PROTOCOL_ID;
	}

	public int getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(int connectionId) {
		this.connectionId = connectionId;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public boolean hasSequenceNumber() {
		return sequenceNumber != Packet.SEQUENCE_NUMBER_NOT_APPLICALBE;
	}

	public int getDuplicateSequenceNumber() {
		return duplicateSequenceNumber;
	}

	public void setDuplicateSequenceNumber(int duplicateSequenceNumber) {
		this.duplicateSequenceNumber = duplicateSequenceNumber;
	}

	public boolean isDuplicate() {
		return duplicateSequenceNumber != Packet.SEQUENCE_NUMBER_NOT_APPLICALBE;
	}

	public int getLastReceivedSequenceNumber() {
		return lastReceivedSequenceNumber;
	}

	public void setLastReceivedSequenceNumber(int lastReceivedSequenceNumber) {
		this.lastReceivedSequenceNumber = lastReceivedSequenceNumber;
	}

	public boolean hasReceivedPacketHistory() {
		return lastReceivedSequenceNumber != Packet.SEQUENCE_NUMBER_NOT_APPLICALBE;
	}

	public int getReceivedPacketHistory() {
		return receivedPacketHistory;
	}

	public void setReceivedPacketHistory(int receivedPacketHistory) {
		this.receivedPacketHistory = receivedPacketHistory;
	}

	public boolean isImmediateResponse() {
		return isImmediateResponse;
	}

	public void setIsImmediateResponse(boolean isImmediateResponse) {
		this.isImmediateResponse = isImmediateResponse;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public byte[] toByteArray() {
		//TODO implement
		return null;
	}

	//TODO finish reimplementing methods
}