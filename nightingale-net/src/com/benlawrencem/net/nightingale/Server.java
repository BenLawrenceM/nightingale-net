package com.benlawrencem.net.nightingale;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.benlawrencem.net.nightingale.Packet.CouldNotEncodePacketException;
import com.benlawrencem.net.nightingale.Packet.CouldNotSendPacketException;
import com.benlawrencem.net.nightingale.Packet.MessageType;
import com.benlawrencem.net.nightingale.Packet.NullPacketException;
import com.benlawrencem.net.nightingale.Packet.PacketEncodingException;
import com.benlawrencem.net.nightingale.Packet.PacketIOException;

public class Server implements PacketReceiver {
	private static final Logger logger = Logger.getLogger(Server.class.getName());
	private final Object CONNECTION_LOCK = new Object();
	private static final int CLIENT_TIMEOUT = 3000;
	private static final String SERVER_STOPPING = "Server stopping.";
	private static final String DISCONNECT_BY_CLIENT = "Disconnect requested by client.";
	private static final String DROPPED_BY_SERVER = "Client cropped by server.";
	private static final String CLIENT_COULD_NOT_CONNECT = "Could not accept client connection.";
	private static final String CLIENT_TIMED_OUT = "Client timed out.";
	private ServerListener listener;
	private DatagramSocket socket;
	private boolean isRunning;
	private ServerTimeoutThread timeoutThread;
	private ReceivePacketThread receivePacketThread;
	private Map<Integer, ClientInfo> clients;
	private int lastConnectedClientId;

	public Server(ServerListener listener) {
		this.listener = listener;
		resetParameters();
	}

	public void startServer(int port) throws CouldNotStartServerException {
		logger.fine("Starting server on port " + port + "...");
		synchronized(CONNECTION_LOCK) {
			if(isRunning) {
				logger.fine("Server is already started!");
				throw new ServerAlreadyStartedException();
			}
			try {
				socket = new DatagramSocket(port);
				receivePacketThread = new ReceivePacketThread(this, socket);
				receivePacketThread.start();
				timeoutThread = new ServerTimeoutThread(this, Server.CLIENT_TIMEOUT);
				timeoutThread.start();
				isRunning = true;
				logger.fine("Server is open and receiving connections!");
			} catch (SocketException e) {
				closeConnection();
				logger.fine("Could not start server due to SocketException: " + e.getMessage());
				throw new CouldNotOpenServerSocketException(e, port);
			}
		}
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void stopServer() {
		logger.fine("Stopping server...");
		boolean wasRunning = false;
		synchronized(CONNECTION_LOCK) {
			wasRunning = isRunning;
			if(isRunning) {
				logger.finer("Sending disconnect packets to all clients");
				for(Integer clientId : clients.keySet()) {
					ClientInfo client = clients.get(clientId);
					try {
						sendPacket(Packet.createForceDisconnectPacket(clientId, Server.SERVER_STOPPING), client);
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
		logger.fine("Server stopped");
	}

	public List<Integer> getClientIds() {
		synchronized(CONNECTION_LOCK) {
			List<Integer> clientIds = new ArrayList<Integer>();
			for(Integer clientId: clients.keySet()) {
				if(clients.get(clientId) != null)
					clientIds.add(clientId);
			}
			return clientIds;
		}
	}

	public long getLatency(int clientId) {
		synchronized(CONNECTION_LOCK) {
			ClientInfo client = clients.get(clientId);
			if(client != null)
				return client.getLatency();
			return -1;
		}
	}

	public void dropClient(int clientId, String reason) {
		logger.finer("Dropping client " + clientId + ": " + reason);
		boolean clientDropped = false;
		synchronized(CONNECTION_LOCK) {
			if(clients.containsKey(clientId)) {
				ClientInfo client = clients.get(clientId);
				try {
					sendPacket(Packet.createForceDisconnectPacket(clientId, reason), client);
				} catch (CouldNotSendPacketException e) {
					//no need to report that we couldn't ask the client to disconnect--we're dropping the client regardless
				}
				clients.remove(clientId);
				clientDropped = true;
			}
			else {
				logger.finer("Client " + clientId + " could not be dropped: Client not connected.");
			}
		}
		if(clientDropped && listener != null)
			listener.onClientDisconnected(clientId, Server.DROPPED_BY_SERVER);
	}

	public int send(int clientId, String message) throws CouldNotSendPacketException {
		synchronized(CONNECTION_LOCK) {
			Packet packet = Packet.createApplicationPacket(clientId, message);

			//if the client isn't connected then throw an exception
			if(!clients.containsKey(clientId)) {
				logger.fine("Could not send message to client " + clientId + ": Client not connected.");
				throw new ClientNotConnectedException(clientId, packet);
			}

			logger.fine("Sending message to client " + clientId + ": " + message);
			ClientInfo client = clients.get(clientId);
			return sendPacket(packet, client);
		}
	}

	public int resend(int clientId, int originalMessageId, String message) throws CouldNotSendPacketException {
		synchronized(CONNECTION_LOCK) {
			Packet packet = Packet.createApplicationPacket(clientId, message);
			packet.setDuplicateSequenceNumber(originalMessageId);

			//if the client isn't connected then throw an exception
			if(!clients.containsKey(clientId)) {
				logger.fine("Could not resend message to client " + clientId + ": Client not connected.");
				throw new ClientNotConnectedException(clientId, packet);
			}

			logger.fine("Resending message to client " + clientId + ": " + message);
			ClientInfo client = clients.get(clientId);
			return sendPacket(packet, client);
		}
	}

	public void receivePacket(Packet packet, String address, int port) {
		if(logger.isLoggable(Level.FINEST))
			logger.finest("Incoming packet from " + address + ":" + port + ":" + (packet == null ? " null" : "\n  " + packet.toString().replaceAll("\n", "\n  ")));

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

		synchronized(CONNECTION_LOCK) {
			//ignore all packets if the server isn't running
			if(!isRunning) {
				logger.finer("Ignoring packet because the server is not running");
				return;
			}

			if(packet.isAnonymousConnection()) {
				if(packet.getMessageType() == MessageType.CONNECT_REQUEST) {
					logger.finest("Client is requesting connection");
					listenerAction = 1; //accept/reject connection
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
				ClientInfo client = clients.get(clientId);
				if(!client.matchesAddress(address, port)) {
					logger.finer("Ignoring packet from client " + clientId + " because packet came from " + address + ":" + port + " which does not match the expected " + client.getAddress() + ":" + client.getPort());
					return;
				}

				synchronized(client.getPacketRecorder()) {

					//ignore packets we've received from the client before
					if(client.getPacketRecorder().hasRecordedIncomingPacket(packet)) {
						logger.finer("Ignoring packet that has already been received from client " + clientId + " before");
						undeliveredPackets = client.getPacketRecorder().getUndeliveredPackets();
					}

					//ignore duplicates of packets we've received from the client before
					else if(packet.isDuplicate() && client.getPacketRecorder().hasRecordedDuplicateOfIncomingPacket(packet)) {
						logger.finer("Ignoring duplicate of packet that has already been received from client " + clientId + " before");
						client.getPacketRecorder().recordIncomingPacket(packet); //we still want to record having received it (must be run AFTER hasRecordedDuplicateOfIncomingPacket)
						undeliveredPackets = client.getPacketRecorder().getUndeliveredPackets();
					}

					else {
						//record the packet as having been received
						client.getPacketRecorder().recordIncomingPacket(packet);
						undeliveredPackets = client.getPacketRecorder().getUndeliveredPackets();
	
						//we expect application messages, pings, and disconnect notifications from the client
						switch(packet.getMessageType()) {
							case APPLICATION:
								logger.fine("Receiving message from client " + clientId +": " + packet.getMessage());
								listenerAction = 2; //onReceive
								client.resetTimeout();
								break;
							case PING:
								try {
									if(packet.getMessage() != null) {
										try {
											client.setLatency(Long.parseLong(packet.getMessage()));
										}
										catch(NumberFormatException e) {
											//ignore--just don't modify latency
										}
									}
									sendPacket(Packet.createPingResponsePacket(clientId), client);
								} catch (CouldNotSendPacketException e) {
									//ignore all exceptions--we don't need to report that we had trouble responding to a ping
								}
								client.resetTimeout();
								break;
							case CLIENT_DISCONNECT:
								logger.fine("Client " + clientId + " disconnected");
								clients.remove(client.getClientId());
								listenerAction = 3; //onClientDisconnected
								break;
							default:
								logger.finer("Ignoring " + packet.getMessageType() + " packet from client " + clientId + " because only APPLICATION, PING and CLIENT_DISCONNECT packets are expected");
								return;
						}
					}
				}
			}
		}

		//execute listener callback--once again, ugly but shouldn't be synchronized
		if(listener != null) {
			switch(listenerAction) {
				case 1: //onClientConnected
					int clientId = getNextClientId();
					logger.finest("Client " + clientId + " asking for permission to connect to server");
					if(listener.onClientConnected(clientId, address ,port)) {
						logger.finest("Permission to connect granted to client " + clientId);
						acceptClient(clientId, address, port);
					}
					else {
						logger.finest("Permission to connect refused for client " + clientId);
						rejectClient(clientId, address, port);
					}
					break;
				case 2: //onReceive
					listener.onReceive(packet.getConnectionId(), packet.getMessage());
					break;
				case 3: //onClientDisconnected
					listener.onClientDisconnected(packet.getConnectionId(), Server.DISCONNECT_BY_CLIENT);
					break;
			}

			//inform the listener of any undelivered application messages
			if(undeliveredPackets != null) {
				for(Packet undeliveredPacket : undeliveredPackets) {
					if(undeliveredPacket.getMessageType() == MessageType.APPLICATION) {
						listener.onMessageNotDelivered(
								undeliveredPacket.getSequenceNumber(),
								(packet.isDuplicate() ? undeliveredPacket.getDuplicateSequenceNumber() : undeliveredPacket.getSequenceNumber()),
								undeliveredPacket.getConnectionId(),
								undeliveredPacket.getMessage());
					}
				}
			}
		}
	}

	private void closeConnection() {
		logger.finer("Closing server connection");
		synchronized(CONNECTION_LOCK) {
			if(receivePacketThread != null)
				receivePacketThread.stopReceiving();
			if(timeoutThread != null)
				timeoutThread.stopCheckingForTimeouts();
			if(socket != null)
				socket.close();
			resetParameters();
		}
	}

	private void resetParameters() {
		synchronized(CONNECTION_LOCK) {
			socket = null;
			isRunning = false;
			timeoutThread = null;
			receivePacketThread = null;
			clients = new HashMap<Integer, ClientInfo>();
			lastConnectedClientId = Packet.ANONYMOUS_CONNECTION_ID;
		}
	}

	private void acceptClient(int clientId, String address, int port) {
		boolean clientAccepted = false;
		synchronized(CONNECTION_LOCK) {
			try {
				ClientInfo client = new ClientInfo(clientId, address, port, InetAddress.getByName(address));
				sendPacket(Packet.createConnectionAcceptedPacket(clientId), client);
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
			listener.onClientDisconnected(clientId, Server.CLIENT_COULD_NOT_CONNECT);
	}

	private void rejectClient(int clientId, String address, int port) {
		logger.fine("Client " + clientId + " was refused");
		synchronized(CONNECTION_LOCK) {
			try {
				sendPacket(Packet.createConnectionRefusedPacket(), new ClientInfo(clientId, address, port, InetAddress.getByName(address)));
			} catch (UnknownHostException e) {
				//ignore exceptions--we don't need to report that we had trouble rejecting a connection
			} catch (CouldNotSendPacketException e) {
				//ignore exceptions--we don't need to report that we had trouble rejecting a connection
			}
		}
	}

	private int sendPacket(Packet packet, ClientInfo client) throws ServerNotStartedException, NullPacketException, CouldNotEncodePacketException, PacketIOException {
		int sequenceNumber = -1;
		synchronized(CONNECTION_LOCK) {
			//regardless of whether the packet is valid, if the client is not connected then throw a NotConnectedException
			if(!isRunning) {
				logger.finest("Outgoing packet: could not send because server is not running");
				throw new ServerNotStartedException(packet);
			}

			//there's no point in sending null packets, so throw a NullPacketException
			if(packet == null) {
				logger.finest("Outgoing packet: could not send because packet is null");
				throw new NullPacketException();
			}

			synchronized(client.getPacketRecorder()) {
				//add the sequenceNumber, lastReceivedSequenceNumber, and receivedPacketHistory to the packet which we've been
				// recording with our PacketRecorder--also simultaneously record this packet as getting sent
				client.getPacketRecorder().recordAndAddSequenceNumberToOutgoingPacket(packet);
				client.getPacketRecorder().addReceivedPacketHistoryToOutgoingPacket(packet);
				sequenceNumber = packet.getSequenceNumber();

				try {
					//attempt to send the packet
					byte[] bytes = packet.toByteArray();
					DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, client.getInetAddress(), client.getPort());
					socket.send(datagramPacket);
					if(logger.isLoggable(Level.FINEST))
						logger.finest("Outgoing packet to " + client.getAddress() + ":" + client.getPort() + ":\n  " + packet.toString().replaceAll("\n", "\n  "));
				} catch (PacketEncodingException e) {
					//encoding issues result when the packet contains connection id or sequence numbers that are out of range.
					// we would not expect these to occur if everything is functioning as normal
					client.getPacketRecorder().recordPreviousOutgoingPacketNotSent();
					logger.finest("Outgoing packet: could not send due to PacketEncodingException \"" + e.getMessage() + "\"");
					throw new CouldNotEncodePacketException(e, packet);
				}
				catch (IOException e) {
					//wrapping any IOException the socket might throw so calling send() only throws CouldNotSendPacketExceptions
					client.getPacketRecorder().recordPreviousOutgoingPacketNotSent();
					logger.finest("Outgoing packet: could not send due to IOException \"" + e.getMessage() + "\"");
					throw new PacketIOException(e, packet);
				}
			}
		}

		//return the sequence number of the packet that we sent
		return sequenceNumber;
	}

	private int getNextClientId() {
		logger.finest("Getting next client id...");
		synchronized(CONNECTION_LOCK) {
			if(clients.size() > Packet.MAXIMUM_CONNECTION_ID - Packet.MINIMUM_CONNECTION_ID) {
				logger.finer("Assigning anonymous connection id of " + Packet.ANONYMOUS_CONNECTION_ID + " to client because server has maximum of " + (Packet.MAXIMUM_CONNECTION_ID - Packet.MINIMUM_CONNECTION_ID) + " connections");
				return Packet.ANONYMOUS_CONNECTION_ID;
			}
			do {
				lastConnectedClientId = Packet.nextConnectionId(lastConnectedClientId);
			} while(clients.containsKey(lastConnectedClientId));
			logger.finest("Next client id is " + lastConnectedClientId);
			return lastConnectedClientId;
		}
	}

	private long checkClientTimeouts(int timeout) {
		//we may need to notify the listener of disconnected clients
		Set<Integer> disconnectedClientIds = new HashSet<Integer>();
		long oldestClientCommunicationTime = -1;

		synchronized(CONNECTION_LOCK) {
			//if the server isn't running just return a sentinel value
			if(!isRunning)
				return -1;

			//check to see if any client has timed out
			long now = System.currentTimeMillis();
			oldestClientCommunicationTime = now; //now is a good default value as it will make the timeout thread wait the full timeout if no clients are connected
			for(Iterator<Integer> iter = clients.keySet().iterator(); iter.hasNext();) {
				int clientId = iter.next();
				ClientInfo client = clients.get(clientId);

				//if the client has timed out then remove it from the list of clients
				if(client.getTimeOfLastCommunication() + timeout <= now) {
					iter.remove(); //removing client ids from the key set DOES remove clients from the map
					disconnectedClientIds.add(clientId); 
				}

				//otherwise this client may be the client closest to timing out
				else if(client.getTimeOfLastCommunication() < oldestClientCommunicationTime)
					oldestClientCommunicationTime = client.getTimeOfLastCommunication();
			}
		}

		//inform the listener of any clients that timed out
		if(listener != null) {
			for(int clientId : disconnectedClientIds) {
				logger.fine("Client " + clientId + " timed out");
				listener.onClientDisconnected(clientId, Server.CLIENT_TIMED_OUT);
			}
		}

		//return the time of last communication of the client who is closest to timing out
		return oldestClientCommunicationTime;
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

	public static class ClientNotConnectedException extends CouldNotSendPacketException {
		private static final long serialVersionUID = -4168333419680498496L;

		public ClientNotConnectedException(int clientId, Packet packet) {
			super("Client " + clientId + " is not connected.", packet);
		}
	}

	private static class ServerTimeoutThread extends Thread {
		private Server server;
		private int timeout;
		private boolean isCheckingForTimeouts;

		public ServerTimeoutThread(Server server, int timeoutInMilliseconds) {
			super();
			this.server = server;
			timeout = timeoutInMilliseconds;
			isCheckingForTimeouts = false;
		}

		public void run() {
			isCheckingForTimeouts = true;
			while(isCheckingForTimeouts) {
				//tell the server to check for clients that have timed out
				long oldestCommunication = server.checkClientTimeouts(timeout);

				//sleep until the next client is expected to time out
				long now = System.currentTimeMillis();
				try {
					Thread.sleep(Math.max(50, timeout - now + oldestCommunication));
				} catch (InterruptedException e) {}
			}
		}

		public void stopCheckingForTimeouts() {
			isCheckingForTimeouts = false;
		}
	}
}