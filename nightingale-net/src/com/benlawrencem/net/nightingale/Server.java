package com.benlawrencem.net.nightingale;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Logger;

public class Server implements PacketReceiver {
	private static final Logger logger = Logger.getLogger(Server.class.getName());
	private final Object CONNECTION_LOCK = new Object();
	private ServerListener listener;
	private DatagramSocket socket;
	private boolean isRunning;
	private ReceivePacketThread receivePacketThread;

	public Server(ServerListener listener) {
		this.listener = listener;
		resetParameters();
	}

	public void startServer(int port) {
		synchronized(CONNECTION_LOCK) {
			if(isRunning)
				return; //TODO throw new ServerAlreadyStartedException();
			try {
				socket = new DatagramSocket(port);
				receivePacketThread = new ReceivePacketThread(this, socket);
				receivePacketThread.start();
			} catch (SocketException e) {
				closeConnection();
				logger.fine("Could not start server due to SocketException: " + e.getMessage());
				return; //TODO throw new CouldNotOpenSocketException(e);
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
				//TODO send  disconnect packet to each client
			}
			closeConnection();
		}
		if(wasRunning && listener != null)
			listener.onServerStopped();
	}

	public int[] getClientIds() {
		//TODO implement
		return null;
	}

	public void dropClient(int clientId) {
		//TODO implement
	}

	public int send(int clientId, String message) {
		//TODO implement
		return -1;
	}

	public int resend(int clientId, int originalMessageId, String message) {
		//TODO implement
		return -1;
	}

	public void receivePacket(Packet packet, String address, int port) {
		//TODO implement
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
		}
	}
}