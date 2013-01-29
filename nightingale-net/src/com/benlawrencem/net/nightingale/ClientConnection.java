package com.benlawrencem.net.nightingale;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import com.benlawrencem.net.nightingale.Packet.PacketEncodingException;

public class ClientConnection implements PacketReceiver {
	private final Logger logger = Logger.getLogger(ClientConnection.class.getName());
	private final Object CONNECTION_LOCK = new Object();
	private ClientConnectionListener listener;
	private DatagramSocket socket;
	private String serverAddress;
	private InetAddress serverInetAddress;
	private int serverPort;
	private boolean isConnected;
	private boolean isAttemptingToConnect;
	private int clientId;
	private PacketRecorder recorder;

	public ClientConnection() {
		this(null);
	}

	public ClientConnection(ClientConnectionListener listener) {
		this.listener = listener;
		recorder = new PacketRecorder();
		reset();
	}

	public void connect(String address, int port) throws ServerNotFoundException, CouldNotOpenSocketToServerException, CouldNotSendConnectRequestException {
		boolean disconnected = false;
		try {
			synchronized(CONNECTION_LOCK) {
				if(isConnected) {
					disconnectQuietly();
					disconnected = true;
				}
				else if(isAttemptingToConnect) {
					closeConnection();
				}
				isAttemptingToConnect = true;
				serverAddress = address;
				serverInetAddress = InetAddress.getByName(serverAddress);
				serverPort = port;
				socket = new DatagramSocket();
				//TODO start receive thread
				sendPacket(Packet.createConnectRequestPacket());
			}
		} catch (UnknownHostException e) {
			closeConnection();
			throw new ServerNotFoundException(address, port);
		} catch (SocketException e) {
			closeConnection();
			throw new CouldNotOpenSocketToServerException(e);
		} catch (CouldNotSendPacketException e) {
			closeConnection();
			throw new CouldNotSendConnectRequestException(e);
		}
		finally {
			if(disconnected && listener != null)
				listener.onDisconnected();
		}
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void disconnect() {
		boolean disconnected = false;
		synchronized(CONNECTION_LOCK) {
			if(isConnected) {
				disconnectQuietly();
				disconnected = true;
			}
			else if(isAttemptingToConnect) {
				closeConnection();
			}
		}
		if(disconnected && listener != null)
			listener.onDisconnected();
	}

	public int send(String message) throws NotConnectedException, NullPacketException, CouldNotEncodePacketException, PacketIOException {
		synchronized(CONNECTION_LOCK) {
			return sendPacket(Packet.createApplicationPacket(clientId, message));
		}
	}

	public void receive(Packet packet, String address, int port) {
		//ignore null packets or packets with invalid protocols
		if(packet == null || !packet.isValidProtocol())
			return;

		//ugly, but I don't want the listener callbacks to be in a synchronized block--might consider refactoring
		int listenerAction = -1;

		synchronized(CONNECTION_LOCK) {
			//only process packets that match the server address and port we have on record
			if((serverAddress == null ? address == null : serverAddress.equals(address)) && serverPort == port
					&& (isAttemptingToConnect || isConnected)) {
				synchronized(recorder) {
					//only process packets we haven't received before
					if(!recorder.hasRecordedIncomingPacket(packet)) {
						//don't process packets if we've received duplicates of the packet before
						if(!packet.isDuplicate() || !recorder.hasRecordedDuplicateOfIncomingPacket(packet)) {
							//when attempting to connect we expect to receive either a connection refused or connection accepted packet
							if(isAttemptingToConnect) {
								switch(packet.getMessageType()) {
									case CONNECTION_ACCEPTED:
										//the packet contains the client id we'll use for all future communications with the server
										clientId = packet.getConnectionId();
										isAttemptingToConnect = false;
										isConnected = true; //we are now officially connected!
										listenerAction = 1; //onConnected
										break;
									case CONNECTION_REFUSED:
										closeConnection();
										listenerAction = 2; //onCouldNotConnect
										break;
								}
							}
		
							//when already connected we expect application messages, pings, and disconnect notifications
							else if(isConnected) {
								switch(packet.getMessageType()) {
									case APPLICATION:
										listenerAction = 3; //onReceive
										break;
									case PING:
										//respond to PINGs with PING_RESPONSEs
										try {
											sendPacket(Packet.createPingResponsePacket(clientId));
										} catch (CouldNotSendPacketException e) {
											//ignore all exceptions--the user doesn't need to know that we had trouble responding to a ping
										}
										break;
									case PING_RESPONSE:
										handlePingResponse(packet);
										break;
									case FORCE_DISCONNECT:
										closeConnection();
										listenerAction = 4; //onDisconnected
										break;
								}
							}
						}
	
						//record the packet as having been received (even if we've received a duplicate of it before)
						recorder.recordIncomingPacket(packet);
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
					listener.onCouldNotConnect();
					break;
				case 3: //onReceive
					listener.onReceive(packet.getMessage());
					break;
				case 4: //onDisconnected
					listener.onDisconnected();
					break;
			}
		}
	}

	public int resend(int originalMessageId, String message) throws NotConnectedException, NullPacketException, CouldNotEncodePacketException, PacketIOException {
		synchronized(CONNECTION_LOCK) {
			Packet packet = Packet.createApplicationPacket(clientId, message);
			packet.setDuplicateSequenceNumber(originalMessageId);
			return sendPacket(packet);
		}
	}

	private void disconnectQuietly() {
		synchronized(CONNECTION_LOCK) {
			try {
				//inform the server of the client's intent to disconnect
				sendPacket(Packet.createClientDisconnectPacket(clientId));
			}
			catch(CouldNotSendPacketException e) {
				//ignore all exceptions--disconnecting gracefully isn't worth maintaining the connection
			}
			closeConnection();
		}
	}

	private void closeConnection() {
		synchronized(CONNECTION_LOCK) {
			//TODO stop receive thread
			if(socket != null)
				socket.close();
			reset();
		}
	}

	private void reset() {
		synchronized(CONNECTION_LOCK) {
			socket = null;
			serverAddress = null;
			serverInetAddress = null;
			serverPort = -1;
			isConnected = false;
			isAttemptingToConnect = false;
			clientId = Packet.ANONYMOUS_CONNECTION_ID;
			recorder.reset();
		}
	}

	private int sendPacket(Packet packet) throws NotConnectedException, NullPacketException, CouldNotEncodePacketException, PacketIOException {
		int sequenceNumber = -1;

		synchronized(CONNECTION_LOCK) {
			//regardless of whether the packet is valid, if the client is not connected then throw a NotConnectedException
			if(!isConnected && !isAttemptingToConnect)
				throw new NotConnectedException(packet);

			//there's no point in sending null packets, so throw a NullPacketException
			if(packet == null)
				throw new NullPacketException();

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
				} catch (PacketEncodingException e) {
					//encoding issues result when the packet contains connection id or sequence numbers that are out of range.
					// we would not expect these to occur if everything is functioning as normal
					recorder.recordPreviousOutgoingPacketNotSent();
					throw new CouldNotEncodePacketException(e, packet);
				}
				catch (IOException e) {
					//wrapping any IOException the socket might throw so calling send() only throws CouldNotSendPacketExceptions
					recorder.recordPreviousOutgoingPacketNotSent();
					throw new PacketIOException(e, packet);
				}
			}
		}

		//return the sequence number of the packet that we sent
		return sequenceNumber;
	}

	private void handlePingResponse(Packet pingResponse) {
		//TODO implement
	}

	public static abstract class CouldNotConnectException extends Exception {
		private static final long serialVersionUID = -8997925597566127340L;

		public CouldNotConnectException(String message) {
			super("Could not connect to server: " + message);
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

	public static class NotConnectedException extends CouldNotSendPacketException {
		private static final long serialVersionUID = 7207922501617442210L;

		public NotConnectedException(Packet packet) {
			super("Client is not connected to server.", packet);
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

		private PacketIOException(IOException e, Packet packet) {
			super("Issue sending packet to server" + (e == null ? "." : "--" + e.getMessage()), packet);
			wrappedException = e;
		}

		public IOException getException() {
			return wrappedException;
		}
	}
}