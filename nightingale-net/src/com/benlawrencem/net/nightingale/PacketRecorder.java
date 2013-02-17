package com.benlawrencem.net.nightingale;

import java.util.ArrayList;
import java.util.List;

public class PacketRecorder {
	private static final int NUM_RECEIVED_PACKETS_STORED = 64;
	private PacketReceipt[] receivedPackets;
	private int receivedPacketHistoryInt;
	private int lastReceivedPacketIndex;
	private int lastReceivedPacketSequenceNumber;

	private static final int NUM_SENT_PACKETS_STORED = 64;
	private PacketReceipt[] sentPackets;
	private int lastSentPacketIndex;
	private int lastSentPacketSequenceNumber;

	private int lastSentPacketCheckedForDelivery;

	public PacketRecorder() {
		receivedPackets = new PacketReceipt[PacketRecorder.NUM_RECEIVED_PACKETS_STORED];
		sentPackets = new PacketReceipt[PacketRecorder.NUM_SENT_PACKETS_STORED];
		reset();
	}

	public synchronized boolean hasRecordedIncomingPacket(Packet packet) {
		return (packet == null ? false : hasReceivedPacketWithSequenceNumber(packet.getSequenceNumber()));
	}

	public synchronized boolean hasRecordedDuplicateOfIncomingPacket(Packet packet) {
		return (packet == null ? false : hasReceivedPacketWithSequenceNumber(packet.getDuplicateSequenceNumber()));
	}

	public synchronized void recordIncomingPacket(Packet packet) {
		//if the packet is null or if it doesn't have a sequence number, we can't record it
		if(packet == null || !packet.hasSequenceNumber())
			return;

		//if we've never received a packet before, our job is easy
		if(lastReceivedPacketSequenceNumber == Packet.SEQUENCE_NUMBER_NOT_APPLICABLE) {
			receivedPackets[0] = new PacketReceipt(packet);
			receivedPacketHistoryInt = 0;
			lastReceivedPacketIndex = 0;
			lastReceivedPacketSequenceNumber = packet.getSequenceNumber();
		}
		else {
			//if our last received packet is really old, our job is almost exactly the same as receiving our first packet
			int delta = Packet.deltaBetweenSequenceNumbers(lastReceivedPacketSequenceNumber, packet.getSequenceNumber());
			if(delta >= PacketRecorder.NUM_RECEIVED_PACKETS_STORED) {
				receivedPackets[0] = new PacketReceipt(packet);
				for(int i = 1; i < receivedPackets.length; i++)
					receivedPackets[i] = null;
				receivedPacketHistoryInt = 0;
				lastReceivedPacketIndex = 0;
				lastReceivedPacketSequenceNumber = packet.getSequenceNumber();
			}

			//if the packet is one in the near future, push things over and add it
			else if (delta > 0) {
				//add a 1 to the history int, marking the last received packet as having been received
				receivedPacketHistoryInt = (receivedPacketHistoryInt >> 1) | Integer.MIN_VALUE;

				//add nulls in-between the last received packet and this packet, representing missing packets
				for(int i = 1; i < delta; i++) {
					receivedPackets[(lastReceivedPacketIndex + i) % PacketRecorder.NUM_RECEIVED_PACKETS_STORED] = null;
					receivedPacketHistoryInt = (receivedPacketHistoryInt >> 1) & Integer.MAX_VALUE; //also shift a 0, meaning a missed packet
				}

				//add the packet
				lastReceivedPacketIndex = (lastReceivedPacketIndex + delta) % PacketRecorder.NUM_RECEIVED_PACKETS_STORED;
				receivedPackets[lastReceivedPacketIndex] = new PacketReceipt(packet);
				lastReceivedPacketSequenceNumber = packet.getSequenceNumber();
			}

			//if the packet is one in the recent past, slot it in
			else if(0 > delta && delta > -PacketRecorder.NUM_RECEIVED_PACKETS_STORED) {
				int index = lastReceivedPacketIndex + delta;
				if(index < 0)
					index += PacketRecorder.NUM_RECEIVED_PACKETS_STORED;
				receivedPackets[index] = new PacketReceipt(packet);

				//add a 1 to the correct position in the history int
				if(delta >= -32) {
					//ex: delta = -1  -->  leftmost bit needs to be a 1  -->  mask with 2^31  -->  mask with Integer.MIN_VALUE
					//ex: delta = -32 -->  rightmost bit needs to be a 1 -->  mask with 2^0
					int mask = (delta == -1 ? Integer.MIN_VALUE : (int) Math.pow(2, 32 + delta));
					receivedPacketHistoryInt = receivedPacketHistoryInt & mask;
				}
			}

			//otherwise the packet is really old or delta is 0 and it's the packet we just received--either way, we can ignore it
		}
	}

	public synchronized List<Packet> getUndeliveredPackets() {
		List<Packet> undeliveredPackets = new ArrayList<Packet>();

		//if we've never received a packet then we have no way to tell if the packets we've sent have been delivered
		if(lastReceivedPacketSequenceNumber == Packet.SEQUENCE_NUMBER_NOT_APPLICABLE)
			return undeliveredPackets;

		//if we received null packets (shouldn't be possible) then once again there's no way to tell which packets have been delivered
		if(receivedPackets[lastReceivedPacketIndex] == null)
			return undeliveredPackets;
		Packet packet = receivedPackets[lastReceivedPacketIndex].getPacket();
		if(packet == null)
			return undeliveredPackets;

		//if the last received packet doesn't have history information then there's no information on which packets were delivered
		if(!packet.hasReceivedPacketHistory())
			return undeliveredPackets;

		//if the the last received packet has older history information than when we last checked then we gain no new information
		int packetVerifiedAsDelivered = packet.getLastReceivedSequenceNumber();
		if(lastSentPacketCheckedForDelivery != Packet.SEQUENCE_NUMBER_NOT_APPLICABLE
				&& Packet.deltaBetweenSequenceNumbers(lastSentPacketCheckedForDelivery, packetVerifiedAsDelivered) <= 0)
			return undeliveredPackets;

		//if we've never checked packet history before then we set an anchor here and don't do any checking yet
		if(lastSentPacketCheckedForDelivery == Packet.SEQUENCE_NUMBER_NOT_APPLICABLE) {
			lastSentPacketCheckedForDelivery = packetVerifiedAsDelivered;
			return undeliveredPackets;
		}

		//check each packet between the last packet checked for delivery and the new packet verified as delivered
		lastSentPacketCheckedForDelivery = Packet.nextSequenceNumber(lastSentPacketCheckedForDelivery);
		while(lastSentPacketCheckedForDelivery != packetVerifiedAsDelivered) {
			int delta = Packet.deltaBetweenSequenceNumbers(lastSentPacketCheckedForDelivery, packetVerifiedAsDelivered);
			if(delta <= 32) {
				//if the most recent packet has 0s in it that means packet weren't delivered
				//ex: delta = 32 --> rightmost bit represents delivery --> mask with 2^0
				//ex: delta = 1  --> leftmost bit represents delivery  --> mask with 2^31 --> mask with Integer.MIN_VALUE
				int mask = (delta == 1 ? Integer.MIN_VALUE : (int) Math.pow(2, 32 - delta));
				if((packet.getReceivedPacketHistory() & mask) == 0) {
					int index = lastSentPacketIndex - delta;
					if(index < 0)
						index += PacketRecorder.NUM_SENT_PACKETS_STORED;
					if(sentPackets[index] != null && sentPackets[index].getPacket() != null)
						undeliveredPackets.add(sentPackets[index].getPacket());
				}
			}
			lastSentPacketCheckedForDelivery = Packet.nextSequenceNumber(lastSentPacketCheckedForDelivery);
		}
		return undeliveredPackets;
	}
	
	public synchronized void addReceivedPacketHistoryToOutgoingPacket(Packet packet) {
		if(packet != null) {
			packet.setLastReceivedSequenceNumber(lastReceivedPacketSequenceNumber);
			packet.setReceivedPacketHistory(receivedPacketHistoryInt);
		}
	}

	public synchronized void recordAndAddSequenceNumberToOutgoingPacket(Packet packet) {
		//ignore null packets
		if(packet == null)
			return;

		//add sequence number to packet
		lastSentPacketSequenceNumber = Packet.nextSequenceNumber(lastSentPacketSequenceNumber);
		packet.setSequenceNumber(lastSentPacketSequenceNumber);

		//add packet to array of sent packets
		lastSentPacketIndex++;
		if(lastSentPacketIndex == PacketRecorder.NUM_SENT_PACKETS_STORED)
			lastSentPacketIndex = 0;
		sentPackets[lastSentPacketIndex] = new PacketReceipt(packet);
	}

	public synchronized void recordPreviousOutgoingPacketNotSent() {
		//no-op--the receiving party will recognize the packet has not been received and request a duplicate
	}

	public synchronized PacketReceipt getSentPacketWithSequenceNumber(int sequenceNumber) {
		//ignore N/A sequence numbers
		if(sequenceNumber == Packet.SEQUENCE_NUMBER_NOT_APPLICABLE)
			return null;

		//if the sequence number is for a packet in the future then we haven't sent it yet
		int delta = Packet.deltaBetweenSequenceNumbers(sequenceNumber, lastSentPacketSequenceNumber);
		if(delta < 0)
			return null;

		//if the sequence number is too far in the past then we won't have a record of it
		if(delta >= PacketRecorder.NUM_SENT_PACKETS_STORED)
			return null;

		//return the sent packet
		int index = (lastSentPacketSequenceNumber - delta) % PacketRecorder.NUM_SENT_PACKETS_STORED;
		if(index < 0)
			index += PacketRecorder.NUM_SENT_PACKETS_STORED;
		return sentPackets[index];
	}

	public synchronized void reset() {
		for(int i = 0; i < receivedPackets.length; i++)
			receivedPackets[i] = null;
		receivedPacketHistoryInt = 0;
		lastReceivedPacketIndex = -1;
		lastReceivedPacketSequenceNumber = Packet.SEQUENCE_NUMBER_NOT_APPLICABLE;

		for(int i = 0; i < sentPackets.length; i++)
			sentPackets[i] = null;
		lastSentPacketIndex = -1;
		lastSentPacketSequenceNumber = Packet.SEQUENCE_NUMBER_NOT_APPLICABLE;

		lastSentPacketCheckedForDelivery = Packet.SEQUENCE_NUMBER_NOT_APPLICABLE;
	}

	private synchronized boolean hasReceivedPacketWithSequenceNumber(int sequenceNumber) {
		//if the sequence number isn't specified then we can't tell whether we've received it before--assume we haven't
		if(sequenceNumber == Packet.SEQUENCE_NUMBER_NOT_APPLICABLE)
			return false;

		//if we've NEVER received a packet ever before then there's no way we could have received this one
		if(lastReceivedPacketSequenceNumber == Packet.SEQUENCE_NUMBER_NOT_APPLICABLE)
			return false;

		//if we JUST received the packet then obviously we've received it before
		if(lastReceivedPacketSequenceNumber == sequenceNumber)
			return true;

		//if the packet is from the future (possibly the next packet) then we haven't received it before
		int delta = Packet.deltaBetweenSequenceNumbers(lastReceivedPacketSequenceNumber, sequenceNumber);
		if(delta > 0)
			return false;

		//if the packet is really old then assume we've received it before
		if(delta <= -NUM_RECEIVED_PACKETS_STORED)
			return true;

		//otherwise find the index of the packet in the receivedPackets array and if it's null we haven't received it before
		int index = (lastReceivedPacketIndex + delta);
		if(index < 0)
			index += PacketRecorder.NUM_RECEIVED_PACKETS_STORED;
		return receivedPackets[index] != null;
	}
}