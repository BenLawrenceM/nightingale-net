package com.benlawrencem.net.nightingale;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.benlawrencem.net.nightingale.Packet.MalformedPacketException;

public class ReceivePacketThread extends Thread {
	private PacketReceiver receiver;
	private DatagramSocket socket;
	private boolean isReceiving;

	public ReceivePacketThread(PacketReceiver receiver, DatagramSocket socket) {
		this.receiver = receiver;
		this.socket = socket;
		isReceiving = false;
	}

	public void run() {
		isReceiving = true;
		while(isReceiving) {
			byte[] bytes = new byte[Packet.MAXIMUM_PACKET_SIZE];
			DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
			try {
				socket.receive(datagramPacket);
				try {
					Packet packet = Packet.parsePacket(datagramPacket.getData(), datagramPacket.getLength());
					receiver.receivePacket(packet, datagramPacket.getAddress().getHostAddress(), datagramPacket.getPort());
				} catch (MalformedPacketException e) {
					//it might be valuable to inform the server that it's receiving invalid packets from a client
					receiver.receivePacket(null, datagramPacket.getAddress().getHostAddress(), datagramPacket.getPort());
				}
			} catch (IOException e) {
				//if the packet is having trouble receiving, the best thing to
				// do is keep trying. If the problem persists the connection
				// will time out
			}
		}
	}

	public void stopReceiving() {
		isReceiving = false;
	}
}