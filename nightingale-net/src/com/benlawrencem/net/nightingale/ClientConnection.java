package com.benlawrencem.net.nightingale;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.benlawrencem.net.nightingale.Packet.CouldNotSendPacketException;
import com.benlawrencem.net.nightingale.Packet.MessageType;
import com.benlawrencem.net.nightingale.Packet.NullPacketException;
import com.benlawrencem.net.nightingale.Packet.CouldNotEncodePacketException;
import com.benlawrencem.net.nightingale.Packet.PacketIOException;
import com.benlawrencem.net.nightingale.Packet.PacketEncodingException;

public class ClientConnection implements PacketReceiver {
	private static final Logger logger = Logger.getLogger(ClientConnection.class.getName());
	private final Object CONNECTION_LOCK = new Object();
	private static final int TIME_BETWEEN_PINGS = 1000;
	private static final int CONNECT_REQUEST_TIMEOUT = 3000;
	private static final int RECEIVE_PACKET_TIMEOUT = 3000;
	private static final String CONNECT_REQUEST_REFUSED = "Connection refused by server.";
	private static final String CONNECT_REQUEST_TIMED_OUT = "Connect request timed out.";
	private static final String CONNECTION_TIMED_OUT = "Connection timed out.";
	private static final String DISCONNECTED_BY_CLIENT = "Disconnect requested by client.";
	private ClientConnectionListener listener;
	private DatagramSocket socket;
	private String serverAddress;
	private InetAddress serverInetAddress;
	private int serverPort;
	private boolean isConnected;
	private boolean isAttemptingToConnect;
	private int clientId;
	private PacketRecorder recorder;
	private PingThread pingThread;
	private TimeoutThread timeoutThread;
	private ReceivePacketThread receivePacketThread;
	private long latency;

	public ClientConnection(ClientConnectionListener listener) {
		this.listener = listener;
		recorder = new PacketRecorder();
		resetVariables();
	}

	public void connect(String address, int port) throws CouldNotConnectException {
		logger.fine("Connecting to " + address + ":" + port + "...");
		boolean disconnected = false;
		try {
			synchronized(CONNECTION_LOCK) {
				if(isConnected) {
					logger.fine("Disconnecting from " + serverAddress + ":" + serverPort + " so client can connect to " + address + ":" + port);
					disconnectQuietly();
					disconnected = true;
				}
				else if(isAttemptingToConnect) {
					logger.fine("Cancelling connect request to " + serverAddress + ":" + serverPort + " so client can connect to " + address + ":" + port);
					closeConnection();
				}
				isAttemptingToConnect = true;
				serverAddress = address;
				serverInetAddress = InetAddress.getByName(serverAddress);
				serverPort = port;
				socket = new DatagramSocket();
				receivePacketThread = new ReceivePacketThread(this, socket);
				receivePacketThread.start();
				timeoutThread = new TimeoutThread(this, ClientConnection.CONNECT_REQUEST_TIMEOUT);
				timeoutThread.start();
				logger.finer("Sending connect request packet");
				sendPacket(Packet.createConnectRequestPacket());
			}
		} catch (UnknownHostException e) {
			closeConnection();
			logger.fine("Could not connect due to UnknownHostException: " + e.getMessage());
			throw new ServerNotFoundException(address, port); //wrapped so callers can just catch CouldNotConnectException
		} catch (SocketException e) {
			closeConnection();
			logger.fine("Could not connect due to SocketException: " + e.getMessage());
			throw new CouldNotOpenSocketToServerException(e);
		} catch (CouldNotSendPacketException e) {
			closeConnection();
			logger.fine("Could not connect due to CouldNotSendPacketException while sending connect request: " + e.getMessage());
			throw new CouldNotSendConnectRequestException(e);
		}
		finally {
			if(disconnected && listener != null) {
				listener.onDisconnected(ClientConnection.DISCONNECTED_BY_CLIENT);
			}
		}
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void disconnect() {
		boolean disconnected = false;
		synchronized(CONNECTION_LOCK) {
			if(isConnected) {
				logger.fine("Disconnecting from " + serverAddress + ":" + serverPort);
				disconnectQuietly();
				disconnected = true;
			}
			else if(isAttemptingToConnect) {
				logger.fine("Cancelling connect request to " + serverAddress + ":" + serverPort);
				closeConnection();
			}
		}
		if(disconnected && listener != null)
			listener.onDisconnected(ClientConnection.DISCONNECTED_BY_CLIENT);
	}

	public int send(String message) throws CouldNotSendPacketException {
		synchronized(CONNECTION_LOCK) {
			logger.fine("Sending message:   " + message);
			return sendPacket(Packet.createApplicationPacket(clientId, message));
		}
	}

	public int resend(int originalMessageId, String message) throws CouldNotSendPacketException {
		synchronized(CONNECTION_LOCK) {
			logger.fine("Resending message: " + message);
			Packet packet = Packet.createApplicationPacket(clientId, message);
			packet.setDuplicateSequenceNumber(originalMessageId);
			return sendPacket(packet);
		}
	}

	public void receivePacket(Packet packet, String address, int port) {
		if(logger.isLoggable(Level.FINEST))
			logger.finest("Incoming packet:" + (packet == null ? " null" : "\n  " + packet.toString().replaceAll("\n", "\n  ")));

		//ignore null packets
		if(packet == null) {
			logger.finer("Ignoring null packet");
			return;
		}

		//ignore packets with invalid protocol bytes
		if(!packet.isValidProtocol()) {
			logger.finer("Ignoring packet with invalid protocol");
			return;
		}

		//ugly, but I don't want the listener callbacks to be in a synchronized block
		int listenerAction = -1;
		List<Packet> undeliveredPackets = null;
		String disconnectReason = null;

		synchronized(CONNECTION_LOCK) {
			//ignore all packets if the client isn't connected or isn't attempting to connect to any server
			if(!isAttemptingToConnect && !isConnected) {
				logger.finer("Ignoring packet because the client is not connected to any server");
				return;
			}

			//ignore packets that don't match the server address and port we have on record
			if((serverAddress == null && address != null) || (serverAddress != null && !serverAddress.equals(address)) || serverPort != port) {
				logger.finer("Ignoring packet from " + address + ":" + port + " because it is not from the server at " + serverAddress + ":" + serverPort);
				return;
			}

			synchronized(recorder) {
				//ignore packets we've received before
				if(recorder.hasRecordedIncomingPacket(packet)) {
					logger.finer("Ignoring packet that has already been received before");
					undeliveredPackets = recorder.getUndeliveredPackets();
				}

				//ignore duplicates of packets we've received before
				else if(packet.isDuplicate() && recorder.hasRecordedDuplicateOfIncomingPacket(packet)) {
					logger.finer("Ignoring duplicate of packet that has already been received before");
					recorder.recordIncomingPacket(packet); //we still want to record having received it (must be run AFTER hasRecordedDuplicateOfIncomingPacket)
					undeliveredPackets = recorder.getUndeliveredPackets();
				}

				else {
					//record the packet as having been received
					recorder.recordIncomingPacket(packet);
					undeliveredPackets = recorder.getUndeliveredPackets();
	
					//when attempting to connect we expect to receive either a connection refused or connection accepted packet
					if(isAttemptingToConnect) {
						switch(packet.getMessageType()) {
							case CONNECTION_ACCEPTED:
								acceptConnection(packet);
								listenerAction = 1; //onConnected
								break;
							case CONNECTION_REFUSED:
								logger.fine("Connection refused");
								closeConnection();
								listenerAction = 2; //onCouldNotConnect
								break;
							default:
								logger.finer("Ignoring " + packet.getMessageType() + " packet because only CONNECTION_ACCEPTED and CONNECTION_REFUSED packets are expected");
								return;
						}
					}
	
					//when already connected we expect application messages, pings, and disconnect notifications
					else if(isConnected) {
						switch(packet.getMessageType()) {
							case APPLICATION:
								logger.fine("Receiving message: " + packet.getMessage());
								listenerAction = 3; //onReceive
								timeoutThread.resetTimeout();
								break;
							case PING_RESPONSE:
								handlePingResponse(packet);
								timeoutThread.resetTimeout();
								break;
							case FORCE_DISCONNECT:
								logger.fine("Disconnected by server: " + packet.getMessage());
								closeConnection();
								disconnectReason = packet.getMessage();
								listenerAction = 4; //onDisconnected
								break;
							default:
								logger.finer("Ignoring " + packet.getMessageType() + " packet because only APPLICATION, PING_RESPONSE and FORCE_DISCONNECT packets are expected");
								return;
						}
					}
				}
			}
		}

		//execute listener callback--once again, ugly but shouldn't be synchronized
		if(listener != null) {
			switch(listenerAction) {
				case 1: //onConnected
					listener.onConnected();
					break;
				case 2: //onCouldNotConnect
					listener.onCouldNotConnect(ClientConnection.CONNECT_REQUEST_REFUSED);
					break;
				case 3: //onReceive
					listener.onReceive(packet.getMessage());
					break;
				case 4: //onDisconnected
					listener.onDisconnected(disconnectReason);
					break;
			}

			//inform the listener of any undelivered application messages
			if(undeliveredPackets != null) {
				for(Packet undeliveredPacket : undeliveredPackets) {
					if(undeliveredPacket.getMessageType() == MessageType.APPLICATION) {
						listener.onMessageNotDelivered(
								undeliveredPacket.getSequenceNumber(),
								(packet.isDuplicate() ? undeliveredPacket.getDuplicateSequenceNumber() : undeliveredPacket.getSequenceNumber()),
								undeliveredPacket.getMessage());
					}
				}
			}
		}
	}

	private void ping() {
		synchronized(CONNECTION_LOCK) {
			if(isConnected) {
				try {
					long latency = getLatency();
					sendPacket(Packet.createPingPacket(clientId, latency));
					logger.finest("Pinging server [" + latency + "ms]");
				} catch (CouldNotSendPacketException e) {
					//ignore all exceptions--who cares if we had trouble sending a ping?
					logger.finest("Could not ping server: " + e.getMessage());
				}
			}
		}
	}

	private void acceptConnection(Packet packet) {
		synchronized(CONNECTION_LOCK) {
			//the packet contains the client id we'll use for all future communications with the server
			logger.fine("Connected to " + serverAddress + ":" + serverPort +" as client " + clientId + "!");
			clientId = packet.getConnectionId();
			isAttemptingToConnect = false;
			isConnected = true; //we are now officially connected!
			timeoutThread.stopTimeout();
			timeoutThread = new TimeoutThread(this, ClientConnection.RECEIVE_PACKET_TIMEOUT);
			timeoutThread.start();
			pingThread = new PingThread(this, ClientConnection.TIME_BETWEEN_PINGS);
			pingThread.start();
		}
	}

	private void timeOut() {
		boolean timedOutBeforeConnecting = false;
		boolean timedOutAfterConnecting = false;
		synchronized(CONNECTION_LOCK) {
			if(isAttemptingToConnect) {
				logger.fine("Connection to " + serverAddress + ":" + serverPort + " timed out");
				closeConnection();
				timedOutBeforeConnecting = true;
			}
			else if(isConnected) {
				logger.fine("Connect request to " + serverAddress + ":" + serverPort + " timed out");
				disconnectQuietly();
				timedOutAfterConnecting = true;
			}
			//it should be impossible for timedOut() to get called if the
			// ClientConnection is neither connected nor attempting to connect,
			// but we'll check for both separately and not assume anything
		}

		//once again, exactly one of these could be true, but we're making sure
		// both don't get run and not assuming one not happening implies the
		// other happening
		if(listener != null) {
			if(timedOutBeforeConnecting) {
				listener.onCouldNotConnect(ClientConnection.CONNECT_REQUEST_TIMED_OUT);
			}
			else if(timedOutAfterConnecting) {
				listener.onDisconnected(ClientConnection.CONNECTION_TIMED_OUT);
			}
		}
	}

	private void disconnectQuietly() {
		synchronized(CONNECTION_LOCK) {
			try {
				//inform the server of the client's intent to disconnect
				logger.finer("Sending disconnect packet");
				sendPacket(Packet.createClientDisconnectPacket(clientId));
			}
			catch(CouldNotSendPacketException e) {
				//ignore all exceptions--disconnecting gracefully isn't worth maintaining the connection
			}
			closeConnection();
		}
	}

	private void closeConnection() {
		logger.finer("Closing connection to server");
		synchronized(CONNECTION_LOCK) {
			if(receivePacketThread != null)
				receivePacketThread.stopReceiving();
			if(timeoutThread != null)
				timeoutThread.stopTimeout();
			if(pingThread != null)
				pingThread.stopPinging();
			if(socket != null)
				socket.close();
			resetVariables();
		}
	}

	private void resetVariables() {
		synchronized(CONNECTION_LOCK) {
			socket = null;
			serverAddress = null;
			serverInetAddress = null;
			serverPort = -1;
			isConnected = false;
			isAttemptingToConnect = false;
			clientId = Packet.ANONYMOUS_CONNECTION_ID;
			pingThread = null;
			timeoutThread = null;
			receivePacketThread = null;
			recorder.reset();
			latency = -1;
		}
	}

	private int sendPacket(Packet packet) throws NotConnectedException, NullPacketException, CouldNotEncodePacketException, PacketIOException {
		int sequenceNumber = -1;
		synchronized(CONNECTION_LOCK) {
			//regardless of whether the packet is valid, if the client is not connected then throw a NotConnectedException
			if(!isConnected && !isAttemptingToConnect) {
				logger.finest("Outgoing packet: could not send because client is not connected");
				throw new NotConnectedException(packet);
			}

			//there's no point in sending null packets, so throw a NullPacketException
			if(packet == null) {
				logger.finest("Outgoing packet: could not send because packet is null");
				throw new NullPacketException();
			}

			synchronized(recorder) {
				//add the sequenceNumber, lastReceivedSequenceNumber, and receivedPacketHistory to the packet which we've been
				// recording with our PacketRecorder--also simultaneously record this packet as getting sent
				recorder.recordAndAddSequenceNumberToOutgoingPacket(packet);
				recorder.addReceivedPacketHistoryToOutgoingPacket(packet);
				sequenceNumber = packet.getSequenceNumber();

				try {
					//attempt to send the packet
					byte[] bytes = packet.toByteArray();
					DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, serverInetAddress, serverPort);
					socket.send(datagramPacket);
					if(logger.isLoggable(Level.FINEST))
						logger.finest("Outgoing packet:\n  " + packet.toString().replaceAll("\n", "\n  "));
				} catch (PacketEncodingException e) {
					//encoding issues result when the packet contains connection id or sequence numbers that are out of range.
					// we would not expect these to occur if everything is functioning as normal
					recorder.recordPreviousOutgoingPacketNotSent();
					logger.finest("Outgoing packet: could not send due to PacketEncodingException \"" + e.getMessage() + "\"");
					throw new CouldNotEncodePacketException(e, packet);
				}
				catch (IOException e) {
					//wrapping any IOException the socket might throw so calling send() only throws CouldNotSendPacketExceptions
					recorder.recordPreviousOutgoingPacketNotSent();
					logger.finest("Outgoing packet: could not send due to IOException \"" + e.getMessage() + "\"");
					throw new PacketIOException(e, packet);
				}
			}
		}

		//return the sequence number of the packet that we sent
		return sequenceNumber;
	}

	private void handlePingResponse(Packet pingResponse) {
		//ignore if the ping response is null
		if(pingResponse == null)
			return;

		//if the ping response doesn't have a sequence number of the ping it's responding to then we can't do anything
		if(pingResponse.getLastReceivedSequenceNumber() == Packet.SEQUENCE_NUMBER_NOT_APPLICABLE)
			return;

		synchronized(CONNECTION_LOCK) {
			//if we have no record of when the ping was sent then we can't do anything
			PacketReceipt pingReceipt = recorder.getSentPacketWithSequenceNumber(pingResponse.getLastReceivedSequenceNumber());
			if(pingReceipt == null)
				return;

			//record the latency
			long timeOfPing = pingReceipt.getTime();
			long now = System.currentTimeMillis();
			long timeSincePing = now - timeOfPing;
			latency = (latency == -1 ? timeSincePing : (latency + timeSincePing) / 2);
		}
	}

	public long getLatency() {
		return latency;
	}

	public static abstract class CouldNotConnectException extends Exception {
		private static final long serialVersionUID = -8997925597566127340L;

		public CouldNotConnectException(String message) {
			super(message);
		}
	}

	public static class ServerNotFoundException extends CouldNotConnectException {
		private static final long serialVersionUID = -5142054741364067962L;

		public ServerNotFoundException(String address, int port) {
			super("Server not found at " + address + ":" + port + ".");
		}
	}

	public static class CouldNotOpenSocketToServerException extends CouldNotConnectException {
		private static final long serialVersionUID = 7033616636602581976L;
		private SocketException wrappedException;

		public CouldNotOpenSocketToServerException(SocketException e) {
			super("Could not open socket to server" + (e == null ? "." : "--" + e.getMessage()));
			wrappedException = e;
		}

		public SocketException getException() {
			return wrappedException;
		}
	}

	public static class CouldNotSendConnectRequestException extends CouldNotConnectException {
		private static final long serialVersionUID = 7818109175569068967L;
		private CouldNotSendPacketException wrappedException;

		public CouldNotSendConnectRequestException(CouldNotSendPacketException e) {
			super("Could not send connect request" + (e == null ? "." : "--" + e.getMessage()));
			wrappedException = e;
		}

		public CouldNotSendPacketException getException() {
			return wrappedException;
		}
	}

	public static class NotConnectedException extends CouldNotSendPacketException {
		private static final long serialVersionUID = 7207922501617442210L;

		public NotConnectedException(Packet packet) {
			super("Client is not connected to server.", packet);
		}
	}

	/**
	 * Responsible for informing the ClientConnection when it should time out.
	 * 
	 * Why is it a private class? So the {@link ClientConnection.timeOut}
	 * method wouldn't affect the class's signature. The loss in readability to
	 * developers is made up for in not polluting the public namespace for
	 * anyone using this class.
	 * 
	 * Why is is a Thread? Ruminating on other possibilities.
	 */
	private static class TimeoutThread extends Thread {
		private ClientConnection client;
		private int timeout;
		private boolean isWaitingToTimeOut;
		private long timeOfLastReset;

		public TimeoutThread(ClientConnection client, int timeoutInMilliseconds) {
			super();
			this.client = client;
			timeout = timeoutInMilliseconds;
			isWaitingToTimeOut = false;
		}

		public void run() {
			isWaitingToTimeOut = true;
			resetTimeout();
			while(isWaitingToTimeOut) {
				//if the thread was last reset over [timeout] milliseconds ago then the client has timed out
				long now = System.currentTimeMillis();
				if(now >= timeOfLastReset + timeout) {
					isWaitingToTimeOut = false;
					if(client != null)
						client.timeOut();
				}

				//wait an amount of time until thread should have timed out
				try {
					Thread.sleep(Math.max(50, timeout - now + timeOfLastReset));
				} catch (InterruptedException e) {}
			}
		}

		public void resetTimeout() {
			timeOfLastReset = System.currentTimeMillis();
		}

		public void stopTimeout() {
			isWaitingToTimeOut = false;
		}
	}

	public class PingThread extends Thread {
		private ClientConnection client;
		private int timeout;
		private boolean isPinging;

		public PingThread(ClientConnection client, int millisecondsBetweenPings) {
			this.client = client;
			timeout = millisecondsBetweenPings;
			isPinging = false;
		}

		public void run() {
			isPinging = true;
			while(isPinging) {
				client.ping();
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {}
			}
		}

		public void stopPinging() {
			isPinging = false;
		}
	}
}