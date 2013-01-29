package com.benlawrencem.net.nightingale;

public interface PacketReceiver {
	void receive(Packet packet, String address, int port);
}