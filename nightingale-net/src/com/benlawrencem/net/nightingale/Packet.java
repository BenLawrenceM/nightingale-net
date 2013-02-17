package com.benlawrencem.net.nightingale;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Packet {
	private static final int HEADER_SIZE = 17;
	private static final int PROTOCOL_ID = 103675707;
	public static final int ANONYMOUS_CONNECTION_ID = 0;
	public static final int MINIMUM_CONNECTION_ID = 1;
	public static final int MAXIMUM_CONNECTION_ID = 255;
	public static final int SEQUENCE_NUMBER_NOT_APPLICABLE = 0;
	public static final int MINIMUM_SEQUENCE_NUMBER = 1;
	public static final int MAXIMUM_SEQUENCE_NUMBER = 65535;
	public static final int MAXIMUM_PACKET_SIZE = 512;

	public static enum MessageType {
		INVALID, APPLICATION, PING, PING_RESPONSE, CONNECT_REQUEST,
		CONNECTION_ACCEPTED, CONNECTION_REFUSED, FORCE_DISCONNECT,
		CLIENT_DISCONNECT
	};
	private static final byte MESSAGE_TYPE_INVALID = 0;
	private static final byte MESSAGE_TYPE_APPLICATION = -128;
	private static final byte MESSAGE_TYPE_PING = -127;
	private static final byte MESSAGE_TYPE_PING_RESPONSE = -126;
	private static final byte MESSAGE_TYPE_CONNECT_REQUEST = -125;
	private static final byte MESSAGE_TYPE_CONNECTION_ACCEPTED = -124;
	private static final byte MESSAGE_TYPE_CONNECTION_REFUSED = -123;
	private static final byte MESSAGE_TYPE_FORCE_DISCONNECT = -122;
	private static final byte MESSAGE_TYPE_CLIENT_DISCONNECT = -121;

	private int protocolId;
	private int connectionId;
	private int sequenceNumber;
	private int duplicateSequenceNumber;
	private int lastReceivedSequenceNumber;
	private int receivedPacketHistory;
	private boolean isImmediateResponse;
	private MessageType messageType;
	private String message;

	/* Packet structure:
	 	int		4 bytes	protocol id
	 	byte	1 byte	connection id
	 	short	2 bytes	sequence number
	 	short	2 bytes	duplicate sequence number
	 	short	2 bytes	last received sequence number
	 	int		4 bytes	received packet history
	 	byte	1 byte	packet flags
	 	byte	1 byte	message type
	 	String	n bytes	message
	 */

	private Packet() {
		protocolId = Packet.PROTOCOL_ID;
		connectionId = Packet.ANONYMOUS_CONNECTION_ID;
		sequenceNumber = Packet.SEQUENCE_NUMBER_NOT_APPLICABLE;
		duplicateSequenceNumber = Packet.SEQUENCE_NUMBER_NOT_APPLICABLE;
		lastReceivedSequenceNumber = Packet.SEQUENCE_NUMBER_NOT_APPLICABLE;
		receivedPacketHistory = 0;
		isImmediateResponse = false;
		messageType = MessageType.INVALID;
		message = null;
	}

	private Packet(int connectionId, MessageType messageType, String message) {
		this();
		this.connectionId = connectionId;
		this.messageType = messageType;
		this.message = message;
		if(this.message != null && this.message.equals(""))
			this.message = null;
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

	public boolean isAnonymousConnection() {
		return connectionId == Packet.ANONYMOUS_CONNECTION_ID;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public boolean hasSequenceNumber() {
		return sequenceNumber != Packet.SEQUENCE_NUMBER_NOT_APPLICABLE;
	}

	public int getDuplicateSequenceNumber() {
		return duplicateSequenceNumber;
	}

	public void setDuplicateSequenceNumber(int duplicateSequenceNumber) {
		this.duplicateSequenceNumber = duplicateSequenceNumber;
	}

	public boolean isDuplicate() {
		return duplicateSequenceNumber != Packet.SEQUENCE_NUMBER_NOT_APPLICABLE;
	}

	public int getLastReceivedSequenceNumber() {
		return lastReceivedSequenceNumber;
	}

	public void setLastReceivedSequenceNumber(int lastReceivedSequenceNumber) {
		this.lastReceivedSequenceNumber = lastReceivedSequenceNumber;
	}

	public boolean hasReceivedPacketHistory() {
		return lastReceivedSequenceNumber != Packet.SEQUENCE_NUMBER_NOT_APPLICABLE;
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
		if(this.message != null && this.message.equals(""))
			this.message = null;
	}

	public byte[] toByteArray() throws PacketEncodingException {
		ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + (message == null ? 0 : message.length()));
		buffer.putInt(protocolId);
		buffer.put(Packet.encodeConnectionId(connectionId));
		buffer.putShort(Packet.encodeSequenceNumber(sequenceNumber));
		buffer.putShort(Packet.encodeSequenceNumber(duplicateSequenceNumber));
		buffer.putShort(Packet.encodeSequenceNumber(lastReceivedSequenceNumber));
		buffer.putInt(receivedPacketHistory);
		buffer.put((isImmediateResponse ? Byte.MIN_VALUE : 0));
		buffer.put(Packet.encodeMessageType(messageType));
		if(message != null)
			buffer.put(message.getBytes());
		return buffer.array();
	}

	public String toString() {
		int columnSize = 20;
		byte[] bytes;
		try {
			bytes = toByteArray();
			if(bytes == null)
				bytes = new byte[0];
		} catch(PacketEncodingException e) {
			bytes = new byte[0];
		}
		String s = "Packet Size:     " + bytes.length + (bytes.length == 1 ? " byte" : " bytes");

		s += "\nProtocol Id:     " + col((isValidProtocol() ? "VALID" : " -- INVALID"), columnSize);
		for(int i = 0; i < 4 && i < bytes.length; i++)
			s += toByteString(bytes[i]) + " ";

		s += "\nConnection Id:   " + col((isAnonymousConnection() ? "ANONYMOUS" : "" + connectionId), columnSize);
		for(int i = 4; i < 5 && i < bytes.length; i++)
			s += toByteString(bytes[i]) + " ";

		s += "\nSequence Number: " + col((hasSequenceNumber() ? "" + sequenceNumber : "N/A"), columnSize);
		for(int i = 5; i < 7 && i < bytes.length; i++)
			s += toByteString(bytes[i]) + " ";

		s += "\nDuplicate Of:    " + col((isDuplicate() ? "" + duplicateSequenceNumber : "N/A"), columnSize);
		for(int i = 7; i < 9 && i < bytes.length; i++)
			s += toByteString(bytes[i]) + " ";

		s += "\nLast Received:   " + col((hasReceivedPacketHistory() ? "" + lastReceivedSequenceNumber : "N/A"), columnSize);
		for(int i = 9; i < 11 && i < bytes.length; i++)
			s += toByteString(bytes[i]) + " ";

		s += "\nPacket History:  " + col("" + receivedPacketHistory, columnSize);
		for(int i = 11; i < 15 && i < bytes.length; i++)
			s += toByteString(bytes[i]) + " ";

		s += "\nPacket Flags:    " + col((isImmediateResponse() ? "IMMEDIATE" : "NOT IMMEDIATE"), columnSize);
		for(int i = 15; i < 16 && i < bytes.length; i++)
			s += toByteString(bytes[i]) + " ";

		s += "\nMessage Type:    " + col("" + messageType, columnSize);
		for(int i = 16; i < 17 && i < bytes.length; i++)
			s += toByteString(bytes[i]) + " ";

		if(message != null && message.length() > columnSize - 3) {
			s += "\nMessage:         " + (message == null ? "null" : "\"" + message + "\"");
			s += "\n                 ";
		}
		else
			s += "\nMessage:         " + col((message == null ? "null" : "\"" + message + "\""), columnSize);
		for(int i = 17; i < bytes.length; i++)
			s += toByteString(bytes[i]) + " ";
		return s;
	}

	private String toByteString(byte b) {
		String s = "" + Integer.toBinaryString(b & 0xFF);
		while(s.length() < 8)
			s = "0" + s;
		return s;
	}

	private String col(String s, int width) {
		if(s == null)
			s = "";
		if(s.length() >= width)
			s = s.substring(0, width - 1);
		while(s.length() < width)
			s = s + " ";
		return s;
	}

	private static byte encodeConnectionId(int connectionId) throws ConnectionIdOutOfRangeException {
		if(connectionId != Packet.ANONYMOUS_CONNECTION_ID && (connectionId < Packet.MINIMUM_CONNECTION_ID || connectionId > Packet.MAXIMUM_CONNECTION_ID))
			throw new ConnectionIdOutOfRangeException(connectionId);
		return (byte) (connectionId > Byte.MAX_VALUE ? connectionId + 2*Byte.MIN_VALUE : connectionId);
	}

	private static int decodeConnectionId(byte connectionId) {
		return (int) (connectionId < 0 ? connectionId - 2*Byte.MIN_VALUE : connectionId);
	}

	private static short encodeSequenceNumber(int sequenceNumber) throws SequenceNumberOutOfRangeException {
		if(sequenceNumber != Packet.SEQUENCE_NUMBER_NOT_APPLICABLE && (sequenceNumber < Packet.MINIMUM_SEQUENCE_NUMBER || sequenceNumber > Packet.MAXIMUM_SEQUENCE_NUMBER))
			throw new SequenceNumberOutOfRangeException(sequenceNumber);
		return (short) (sequenceNumber > Short.MAX_VALUE ? sequenceNumber + 2*Short.MIN_VALUE : sequenceNumber);
	}

	private static int decodeSequenceNumber(short sequenceNumber) {
		return (int) (sequenceNumber < 0 ? sequenceNumber - 2*Short.MIN_VALUE : sequenceNumber);
	}

	private static byte encodeMessageType(MessageType messageType) {
		switch(messageType) {
			case APPLICATION:
				return Packet.MESSAGE_TYPE_APPLICATION;
			case PING:
				return Packet.MESSAGE_TYPE_PING;
			case PING_RESPONSE:
				return Packet.MESSAGE_TYPE_PING_RESPONSE;
			case CONNECT_REQUEST:
				return Packet.MESSAGE_TYPE_CONNECT_REQUEST;
			case CONNECTION_ACCEPTED:
				return Packet.MESSAGE_TYPE_CONNECTION_ACCEPTED;
			case CONNECTION_REFUSED:
				return Packet.MESSAGE_TYPE_CONNECTION_REFUSED;
			case FORCE_DISCONNECT:
				return Packet.MESSAGE_TYPE_FORCE_DISCONNECT;
			case CLIENT_DISCONNECT:
				return Packet.MESSAGE_TYPE_CLIENT_DISCONNECT;
			default:
				return Packet.MESSAGE_TYPE_INVALID;
		}
	}

	private static MessageType decodeMessageType(byte messageType) {
		switch(messageType) {
			case Packet.MESSAGE_TYPE_APPLICATION:
				return MessageType.APPLICATION;
			case Packet.MESSAGE_TYPE_PING:
				return MessageType.PING;
			case Packet.MESSAGE_TYPE_PING_RESPONSE:
				return MessageType.PING_RESPONSE;
			case Packet.MESSAGE_TYPE_CONNECT_REQUEST:
				return MessageType.CONNECT_REQUEST;
			case Packet.MESSAGE_TYPE_CONNECTION_ACCEPTED:
				return MessageType.CONNECTION_ACCEPTED;
			case Packet.MESSAGE_TYPE_CONNECTION_REFUSED:
				return MessageType.CONNECTION_REFUSED;
			case Packet.MESSAGE_TYPE_FORCE_DISCONNECT:
				return MessageType.FORCE_DISCONNECT;
			case Packet.MESSAGE_TYPE_CLIENT_DISCONNECT:
				return MessageType.CLIENT_DISCONNECT;
			default:
				return MessageType.INVALID;
		}
	}

	public static int nextConnectionId(int connectionId) {
		if(connectionId == Packet.ANONYMOUS_CONNECTION_ID || connectionId == Packet.MAXIMUM_CONNECTION_ID)
			return Packet.MINIMUM_CONNECTION_ID;
		return connectionId + 1;
	}

	public static int nextSequenceNumber(int sequenceNumber) {
		if(sequenceNumber == Packet.SEQUENCE_NUMBER_NOT_APPLICABLE || sequenceNumber == Packet.MAXIMUM_SEQUENCE_NUMBER)
			return Packet.MINIMUM_SEQUENCE_NUMBER;
		return sequenceNumber + 1;
	}

	public static int deltaBetweenSequenceNumbers(int earlierSequenceNumber, int laterSequenceNumber) {
		int delta = laterSequenceNumber - earlierSequenceNumber;
		if(delta < (Packet.MINIMUM_SEQUENCE_NUMBER - Packet.MAXIMUM_SEQUENCE_NUMBER)/2)
			delta += Packet.MAXIMUM_SEQUENCE_NUMBER - Packet.MINIMUM_SEQUENCE_NUMBER + 1;
		else if(delta > (Packet.MAXIMUM_SEQUENCE_NUMBER - Packet.MINIMUM_SEQUENCE_NUMBER)/2)
			delta -= Packet.MAXIMUM_SEQUENCE_NUMBER - Packet.MINIMUM_SEQUENCE_NUMBER + 1;
		return delta;
	}

	public static Packet parsePacket(byte[] bytes) throws MalformedPacketException {
		return parsePacket(bytes, (bytes == null ? 0 : bytes.length));
	}

	public static Packet parsePacket(byte[] bytes, int length) throws MalformedPacketException {
		if(bytes == null)
			throw new NullByteArrayException();
		if(length > bytes.length)
			length = bytes.length;
		if(length < Packet.HEADER_SIZE)
			throw new NotEnoughBytesException(length);

		ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
		buffer.put(bytes);
		buffer.flip();
		Packet packet = new Packet();
		packet.protocolId = buffer.getInt();
		packet.connectionId = decodeConnectionId(buffer.get());
		packet.sequenceNumber = decodeSequenceNumber(buffer.getShort());
		packet.duplicateSequenceNumber = decodeSequenceNumber(buffer.getShort());
		packet.lastReceivedSequenceNumber = decodeSequenceNumber(buffer.getShort());
		packet.receivedPacketHistory = buffer.getInt();
		byte packetFlagsByte = buffer.get();
		if((packetFlagsByte & Byte.MIN_VALUE) != 0)
			packet.isImmediateResponse = true;
		packet.messageType = decodeMessageType(buffer.get());
		packet.message = new String(bytes).substring(Packet.HEADER_SIZE, length);
		if(packet.message != null && packet.message.equals(""))
			packet.message = null;
		return packet;
	}

	public static Packet createApplicationPacket(int connectionId, String message) {
		return new Packet(connectionId, MessageType.APPLICATION, message);
	}

	public static Packet createPingPacket(int connectionId, long latency) {
		return new Packet(connectionId, MessageType.PING, "" + latency);
	}

	public static Packet createPingResponsePacket(int connectionId) {
		Packet packet = new Packet(connectionId, MessageType.PING_RESPONSE, null);
		packet.setIsImmediateResponse(true);
		return packet;
	}

	public static Packet createConnectRequestPacket() {
		return new Packet(Packet.ANONYMOUS_CONNECTION_ID, MessageType.CONNECT_REQUEST, null);
	}

	public static Packet createConnectionAcceptedPacket(int connectionId) {
		return new Packet(connectionId, MessageType.CONNECTION_ACCEPTED, null);
	}

	public static Packet createConnectionRefusedPacket() {
		return new Packet(Packet.ANONYMOUS_CONNECTION_ID, MessageType.CONNECTION_REFUSED, null);
	}

	public static Packet createForceDisconnectPacket(int connectionId, String message) {
		return new Packet(connectionId, MessageType.FORCE_DISCONNECT, message);
	}

	public static Packet createClientDisconnectPacket(int connectionId) {
		return new Packet(connectionId, MessageType.CLIENT_DISCONNECT, null);
	}

	public static abstract class MalformedPacketException extends Exception {
		private static final long serialVersionUID = 2230088823308942874L;

		public MalformedPacketException(String message) {
			super(message);
		}
	}

	public static class NullByteArrayException extends MalformedPacketException {
		private static final long serialVersionUID = 3525212843629482165L;

		public NullByteArrayException() {
			super("Cannot create packet from null byte array.");
		}
	}

	public static class NotEnoughBytesException extends MalformedPacketException {
		private static final long serialVersionUID = 4729357764475446835L;

		public NotEnoughBytesException(int numBytes) {
			super("Packet construction requires minimum of " + Packet.HEADER_SIZE + " bytes. Only " + numBytes + (numBytes == 1 ? " byte" : " bytes") + " given.");
		}
	}

	public static abstract class PacketEncodingException extends Exception {
		private static final long serialVersionUID = -8101834090041580202L;

		public PacketEncodingException(String message) {
			super(message);
		}
	}

	public static class SequenceNumberOutOfRangeException extends PacketEncodingException {
		private static final long serialVersionUID = -2990542520153350384L;

		public SequenceNumberOutOfRangeException(int sequenceNumber) {
			super("Sequence numbers must either be " + Packet.SEQUENCE_NUMBER_NOT_APPLICABLE + " or between " + Packet.MINIMUM_SEQUENCE_NUMBER + " and " + Packet.MAXIMUM_SEQUENCE_NUMBER + ". " + sequenceNumber + " given.");
		}
	}

	public static class ConnectionIdOutOfRangeException extends PacketEncodingException {
		private static final long serialVersionUID = 7448986015839545309L;

		public ConnectionIdOutOfRangeException(int connectionId) {
			super("Connection ID must either be " + Packet.ANONYMOUS_CONNECTION_ID + " or between " + Packet.MINIMUM_CONNECTION_ID + " and " + Packet.MAXIMUM_CONNECTION_ID + ". " + connectionId + " given.");
		}
	}

	public static abstract class CouldNotSendPacketException extends Exception {
		private static final long serialVersionUID = 4469495505607428313L;
		private Packet packet;

		public CouldNotSendPacketException(String message, Packet packet) {
			super("Could not send packet: " + message);
			this.packet = packet;
		}

		public Packet getPacket() {
			return packet;
		}
	}

	public static class NullPacketException extends CouldNotSendPacketException {
		private static final long serialVersionUID = 245600094593694576L;

		public NullPacketException() {
			super("Packet is null.", null);
		}
	}

	public static class CouldNotEncodePacketException extends CouldNotSendPacketException {
		private static final long serialVersionUID = 217976733885663032L;
		private PacketEncodingException wrappedException;

		public CouldNotEncodePacketException(PacketEncodingException e, Packet packet) {
			super("Packet not encodable" + (e == null ? "." : "--" + e.getMessage()), packet);
			wrappedException = e;
		}

		public PacketEncodingException getException() {
			return wrappedException;
		}
	}

	public static class PacketIOException extends CouldNotSendPacketException {
		private static final long serialVersionUID = 3176188125504255759L;
		private IOException wrappedException;

		public PacketIOException(IOException e, Packet packet) {
			super("IOException sending packet" + (e == null ? "." : "--" + e.getMessage()), packet);
			wrappedException = e;
		}

		public IOException getException() {
			return wrappedException;
		}
	}
}