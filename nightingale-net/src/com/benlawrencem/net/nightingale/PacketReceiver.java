package com.benlawrencem.net.nightingale;

public interface PacketReceiver {
	void receivePacket(Packet packet, String address, int port);
}