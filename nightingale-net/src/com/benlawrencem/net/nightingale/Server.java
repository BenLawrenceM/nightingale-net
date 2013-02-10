package com.benlawrencem.net.nightingale;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.benlawrencem.net.nightingale.ClientConnection.NotConnectedException;
import com.benlawrencem.net.nightingale.Packet.CouldNotEncodePacketException;
import com.benlawrencem.net.nightingale.Packet.CouldNotSendPacketException;
import com.benlawrencem.net.nightingale.Packet.MessageType;
import com.benlawrencem.net.nightingale.Packet.NullPacketException;
import com.benlawrencem.net.nightingale.Packet.PacketEncodingException;
import com.benlawrencem.net.nightingale.Packet.PacketIOException;

public class Server implements PacketReceiver {
	private static final Logger logger = Logger.getLogger(Server.class.getName());
	private final Object CONNECTION_LOCK = new Object();
	private static final String SERVER_STOPPING = "Server stopping.";
	private ServerListener listener;
	private DatagramSocket socket;
	private boolean isRunning;
	private ReceivePacketThread receivePacketThread;
	private Map<Integer, ServerClientConnection> clients;
	private int lastConnectedClientId;

	public Server(ServerListener listener) {
		this.listener = listener;
		resetParameters();
	}

	public void startServer(int port) throws CouldNotStartServerException {
		synchronized(CONNECTION_LOCK) {
			if(isRunning)
				throw new ServerAlreadyStartedException();
			try {
				socket = new DatagramSocket(port);
				receivePacketThread = new ReceivePacketThread(this, socket);
				receivePacketThread.start();
			} catch (SocketException e) {
				closeConnection();
				logger.fine("Could not start server due to SocketException: " + e.getMessage());
				throw new CouldNotOpenServerSocketException(e, port);
			}
		}
		if(listener != null)
			listener.onServerStarted();
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void stopServer() {
		boolean wasRunning = false;
		synchronized(CONNECTION_LOCK) {
			wasRunning = isRunning;
			if(isRunning) {
				for(Integer clientId : clients.keySet()) {
					ServerClientConnection client = clients.get(clientId);
					try {
						sendPacket(Packet.createForceDisconnectPacket(clientId, Server.SERVER_STOPPING), client.getInetAddress(), client.getPort());
					} catch (CouldNotSendPacketException e) {
						//no need to report that we couldn't ask the client to disconnect--the server is stopping regardless
					}
					//no need to call onClientDisconnected--the server is stopping, of course all the clients are going to be disconnected
				}
			}
			closeConnection();
		}
		if(wasRunning && listener != null)
			listener.onServerStopped();
	}

	public void dropClient(int clientId, String reason) {
		boolean clientDropped = false;
		synchronized(CONNECTION_LOCK) {
			if(clients.containsKey(clientId)) {
				ServerClientConnection client = clients.get(clientId);
				try {
					sendPacket(Packet.createForceDisconnectPacket(clientId, reason), client.getInetAddress(), client.getPort());
				} catch (CouldNotSendPacketException e) {
					//no need to report that we couldn't ask the client to disconnect--we're dropping the client regardless
				}
				clients.remove(client);
				clientDropped = true;
			}
		}
		if(clientDropped && listener != null)
			listener.onClientDisconnected(clientId);
	}

	public int send(int clientId, String message) throws CouldNotSendPacketException {
		synchronized(CONNECTION_LOCK) {
			//if the client isn't connected then throw an exception
			if(!clients.containsKey(clientId))
				return -1; //TODO throw ClientNotConnectedException

			logger.fine("Sending message to client " + clientId + ": " + message);
			ServerClientConnection client = clients.get(clientId);
			return sendPacket(Packet.createApplicationPacket(clientId, message), client.getInetAddress(), client.getPort());
		}
	}

	public int resend(int clientId, int originalMessageId, String message) throws CouldNotSendPacketException {
		synchronized(CONNECTION_LOCK) {
			//if the client isn't connected then throw an exception
			if(!clients.containsKey(clientId))
				return -1; //TODO throw ClientNotConnectedException

			logger.fine("Sending message to client " + clientId + ": " + message);
			ServerClientConnection client = clients.get(clientId);
			Packet packet = Packet.createApplicationPacket(clientId, message);
			packet.setDuplicateSequenceNumber(originalMessageId);
			return sendPacket(packet, client.getInetAddress(), client.getPort());
		}
	}

	public void receivePacket(Packet packet, String address, int port) {
		if(logger.isLoggable(Level.FINEST))
			logger.finest("Incoming packet from " + address + ":" + port + ":" + (packet == null ? " null" : "  " + packet.toString().replaceAll("\n", "\n  ")));

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

		synchronized(CONNECTION_LOCK) {
			//ignore all packets if the server isn't running
			if(!isRunning) {
				logger.finer("Ignoring packet because the server is not running");
				return;
			}

			if(packet.isAnonymousConnection()) {
				if(packet.getMessageType() == MessageType.CONNECT_REQUEST) {
					listenerAction = 1; //onClientConnected
				}
				else {
					logger.finer("Ignoring " + packet.getMessageType() + " packet because only CONNECT_REQUEST packets are expected");
					return;
				}
			}
			else {
				//ignore packets from clients that aren't connected
				int clientId = packet.getConnectionId();
				if(!clients.containsKey(clientId) || clients.get(clientId) == null) {
					logger.finer("Ignoring packet from client " + clientId + " because client " + clientId + " is not connected");
					return;
				}

				//ignore packets from unexpected sources
				ServerClientConnection client = clients.get(clientId);
				if(!client.matchesAddress(address, port)) {
					logger.finer("Ignoring packet from client " + clientId + " because packet came from " + address + ":" + port + " which does not match the expected " + client.getAddress() + ":" + client.getPort());
					return;
				}

				synchronized(client.getPacketRecorder()) {
					//ignore packets we've received from the client before
					if(client.getPacketRecorder().hasRecordedIncomingPacket(packet)) {
						logger.finer("Ignoring packet that has already been received from client " + clientId + " before");
						return;
					}

					//ignore duplicates of packets we've received from the client before
					if(packet.isDuplicate() && client.getPacketRecorder().hasRecordedDuplicateOfIncomingPacket(packet)) {
						logger.finer("Ignoring duplicate of packet that has already been received from client " + clientId + " before");
						client.getPacketRecorder().recordIncomingPacket(packet); //we still want to record having received it (must be run AFTER hasRecordedDuplicateOfIncomingPacket)
						return;
					}

					//record the packet as having been received
					client.getPacketRecorder().recordIncomingPacket(packet);

					//we expect application messages, pings, and disconnect notifications from the client
					switch(packet.getMessageType()) {
						case APPLICATION:
							logger.fine("Receiving message from client " + clientId +": " + packet.getMessage());
							listenerAction = 2; //onReceive
							resetTimeout(client);
							break;
						case PING:
							try {
								sendPacket(Packet.createPingResponsePacket(clientId), client.getInetAddress(), client.getPort());
							} catch (CouldNotSendPacketException e) {
								//ignore all exceptions--we don't need to report that we had trouble responding to a ping
							}
							resetTimeout(client);
							break;
						case PING_RESPONSE:
							handlePingResponse(client, packet);
							resetTimeout(client);
							break;
						case CLIENT_DISCONNECT:
							logger.fine("Client " + clientId + " disconnected");
							removeClient(client);
							listenerAction = 3; //onClientDisconnected
							break;
						default:
							logger.finer("Ignoring " + packet.getMessageType() + " packet from client " + clientId + " because only APPLICATION, PING, PING_RESPONSE and CLIENT_DISCONNECT packets are expected");
							return;
					}
				}
			}
		}

		//execute listener callback--once again, ugly but shouldn't be synchronized
		if(listener != null) {
			switch(listenerAction) {
				case 1: //onClientConnected
					int clientId = getNextClientId();
					if(listener.onClientConnected(clientId, address ,port)) {
						acceptClient(clientId, address, port);
					}
					else {
						rejectClient(clientId, address, port);
					}
					break;
				case 2: //onReceive
					listener.onReceive(packet.getConnectionId(), packet.getMessage());
					break;
				case 3: //onClientDisconnected
					listener.onClientDisconnected(packet.getConnectionId());
					break;
			}
		}
	}

	private void closeConnection() {
		synchronized(CONNECTION_LOCK) {
			if(receivePacketThread != null)
				receivePacketThread.stopReceiving();
			if(socket != null)
				socket.close();
			resetParameters();
		}
	}

	private void resetParameters() {
		synchronized(CONNECTION_LOCK) {
			socket = null;
			isRunning = false;
			receivePacketThread = null;
			clients = new HashMap<Integer, ServerClientConnection>();
			lastConnectedClientId = Packet.ANONYMOUS_CONNECTION_ID;
		}
	}

	private void acceptClient(int clientId, String address, int port) {
		boolean clientAccepted = false;
		synchronized(CONNECTION_LOCK) {
			try {
				ServerClientConnection client = new ServerClientConnection(clientId, address, port, InetAddress.getByName(address));
				sendPacket(Packet.createConnectionAcceptedPacket(clientId), client.getInetAddress(), client.getPort());
				clientAccepted = true;
				clients.put(clientId, client);
				logger.fine("Client " + clientId + " connected");
			} catch (UnknownHostException e) {
				//we'll tell the listener the client disconnected outside of the synchronized block
				logger.fine("Could not accept client " + clientId + " due to UnknownHostException: " + e.getMessage());
			} catch (CouldNotSendPacketException e) {
				//we'll tell the listener the client disconnected outside of the synchronized block
				logger.fine("Could not accept client " + clientId + " due to CouldNotSendPacketException: " + e.getMessage());
			}
		}
		if(!clientAccepted && listener != null)
			listener.onClientDisconnected(clientId);
	}

	private void rejectClient(int clientId, String address, int port) {
		logger.fine("Client " + clientId + " was refused");
		synchronized(CONNECTION_LOCK) {
			try {
				sendPacket(Packet.createConnectionRefusedPacket(), InetAddress.getByName(address), port);
			} catch (UnknownHostException e) {
				//ignore exceptions--we don't need to report that we had trouble rejecting a connection
			} catch (CouldNotSendPacketException e) {
				//ignore exceptions--we don't need to report that we had trouble rejecting a connection
			}
		}
	}

	private void handlePingResponse(ServerClientConnection client, Packet pingResponse) {
		//TODO implement
	}

	private void removeClient(ServerClientConnection client) {
		//TODO implement
	}

	private int sendPacket(Packet packet, InetAddress inetAddress, int port) throws ServerNotStartedException, NullPacketException, CouldNotEncodePacketException, PacketIOException {
		//TODO implement
		return -1;
	}

	private void resetTimeout(ServerClientConnection client) {
		//TODO implement
	}

	private int getNextClientId() {
		synchronized(CONNECTION_LOCK) {
			if(clients.size() > Packet.MAXIMUM_CONNECTION_ID - Packet.MINIMUM_CONNECTION_ID)
				return Packet.ANONYMOUS_CONNECTION_ID;
			do {
				Packet.nextConnectionId(lastConnectedClientId);
			} while(clients.containsKey(lastConnectedClientId));
			return lastConnectedClientId;
		}
	}

	public static abstract class CouldNotStartServerException extends Exception {
		private static final long serialVersionUID = -6383721472101600079L;

		public CouldNotStartServerException(String message) {
			super(message);
		}
	}

	public class ServerAlreadyStartedException extends CouldNotStartServerException {
		private static final long serialVersionUID = 3824340604720308489L;

		public ServerAlreadyStartedException() {
			super("Could not start server because it is already started.");
		}
	}

	public class CouldNotOpenServerSocketException extends CouldNotStartServerException {
		private static final long serialVersionUID = -6736257520912125766L;
		private SocketException wrappedException;

		public CouldNotOpenServerSocketException(SocketException e, int port) {
			super("Could not open server socket on port " + port + ".");
			wrappedException = e;
		}

		public SocketException getException() {
			return wrappedException;
		}
	}

	public static class ServerNotStartedException extends CouldNotSendPacketException {
		private static final long serialVersionUID = 324615594029936997L;

		public ServerNotStartedException(Packet packet) {
			super("Server is not started.", packet);
		}
	}
}