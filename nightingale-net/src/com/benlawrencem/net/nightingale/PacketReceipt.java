package com.benlawrencem.net.nightingale;

public class PacketReceipt {
	private Packet packet;
	private long time;

	public PacketReceipt(Packet packet) {
		this.packet = packet;
		time = System.currentTimeMillis();
	}

	public PacketReceipt(Packet packet, long time) {
		this.packet = packet;
		this.time = time;
	}

	public Packet getPacket() {
		return packet;
	}

	public long getTime() {
		return time;
	}
}