package com.benlawrencem.net.nightingale.test;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.benlawrencem.net.nightingale.Packet;

import junit.framework.TestCase;

public class PacketTest extends TestCase {
	private Packet allZeroesPacket;
	private Packet protocolValidPacket;
	private Packet protocolInvalidPacket;
	private Packet applicationPacketHelloWorld;
	private Packet clientDisconnectPacket;
	private Packet connectionAcceptedPacket;
	private Packet connectionRefusedPacket;
	private Packet connectRequestPacket;
	private Packet forceDisconnectPacket;
	private Packet pingPacket;
	private Packet pingResponsePacket;

	@Before
	public void setUp() throws Exception {
		//parse packets from byte arrays
		allZeroesPacket = Packet.parsePacket(new byte[] {
				0, 0, 0, 0, //protocol id (INVALID)
				0, //connection id (0)
				0, 0, //sequence number (0)
				0, 0, //duplicate sequence number (0)
				0, 0,
				0, 0, 0, 0,
				0,
				0
		});
		protocolValidPacket = Packet.parsePacket(new byte[] {
				6, 45, -9, 59, //protocol id
				-117, //connection id (137)
				43, 81, //sequence number (11089)
				0, 20, //duplicate sequence number (20)
				0, 0,
				0, 0, 0, 0,
				0,
				0
		});
		protocolInvalidPacket = Packet.parsePacket(new byte[] {
				-50, 2, 5, 81, //protocol id (INVALID)
				24, //connection id (24)
				-110, 26, //sequence number (37400)
				-50, -104, //duplicate sequence number (52886)
				0, 0,
				0, 0, 0, 0,
				0,
				0
		});

		//create packets with different message types
		applicationPacketHelloWorld = Packet.createApplicationPacket(0, "Hello world!");
		connectionAcceptedPacket = Packet.createConnectionAcceptedPacket(82);
		clientDisconnectPacket = Packet.createClientDisconnectPacket(1);
		connectionRefusedPacket = Packet.createConnectionRefusedPacket();
		connectRequestPacket = Packet.createConnectRequestPacket();
		forceDisconnectPacket = Packet.createForceDisconnectPacket(200);
		pingPacket = Packet.createPingPacket(254);
		pingResponsePacket = Packet.createPingResponsePacket(255);

		//add sequence numbers
		applicationPacketHelloWorld.setSequenceNumber(81);
		clientDisconnectPacket.setSequenceNumber(9557);
		connectionAcceptedPacket.setSequenceNumber(2764);
		connectionRefusedPacket.setSequenceNumber(65534);
		connectRequestPacket.setSequenceNumber(65535);
		forceDisconnectPacket.setSequenceNumber(0);
		pingPacket.setSequenceNumber(1);
		pingResponsePacket.setSequenceNumber(4);

		//add duplicate sequence numbers
		applicationPacketHelloWorld.setDuplicateSequenceNumber(65535);
		clientDisconnectPacket.setDuplicateSequenceNumber(65534);
		connectionAcceptedPacket.setDuplicateSequenceNumber(1);
		connectionRefusedPacket.setDuplicateSequenceNumber(0);
		connectRequestPacket.setDuplicateSequenceNumber(52);
		forceDisconnectPacket.setDuplicateSequenceNumber(0);
		pingPacket.setDuplicateSequenceNumber(0);
		pingResponsePacket.setDuplicateSequenceNumber(3000);
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void testIsValidProtocol() {
		assertFalse(allZeroesPacket.isValidProtocol());
		assertTrue(protocolValidPacket.isValidProtocol());
		assertFalse(protocolInvalidPacket.isValidProtocol());
		assertTrue(applicationPacketHelloWorld.isValidProtocol());
		assertTrue(clientDisconnectPacket.isValidProtocol());
		assertTrue(connectionAcceptedPacket.isValidProtocol());
		assertTrue(connectionRefusedPacket.isValidProtocol());
		assertTrue(connectRequestPacket.isValidProtocol());
		assertTrue(forceDisconnectPacket.isValidProtocol());
		assertTrue(pingPacket.isValidProtocol());
		assertTrue(pingResponsePacket.isValidProtocol());
	}

	@Test
	public void testGetConnectionId() {
		assertEquals(0, allZeroesPacket.getConnectionId());
		assertEquals(137, protocolValidPacket.getConnectionId());
		assertEquals(24, protocolInvalidPacket.getConnectionId());
		assertEquals(0, applicationPacketHelloWorld.getConnectionId());
		assertEquals(1, clientDisconnectPacket.getConnectionId());
		assertEquals(82, connectionAcceptedPacket.getConnectionId());
		assertEquals(Packet.ANONYMOUS_CONNECTION_ID, connectionRefusedPacket.getConnectionId());
		assertEquals(Packet.ANONYMOUS_CONNECTION_ID, connectRequestPacket.getConnectionId());
		assertEquals(200, forceDisconnectPacket.getConnectionId());
		assertEquals(254, pingPacket.getConnectionId());
		assertEquals(255, pingResponsePacket.getConnectionId());
	}

	@Test
	public void testSetConnectionId() {
		allZeroesPacket.setConnectionId(7);
		protocolValidPacket.setConnectionId(1);
		protocolInvalidPacket.setConnectionId(Packet.MINIMUM_CONNECTION_ID);
		applicationPacketHelloWorld.setConnectionId(81);
		clientDisconnectPacket.setConnectionId(9557);
		connectionAcceptedPacket.setConnectionId(2764);
		connectionRefusedPacket.setConnectionId(65534);
		connectRequestPacket.setConnectionId(65535);
		forceDisconnectPacket.setConnectionId(0);
		pingPacket.setConnectionId(Packet.MAXIMUM_CONNECTION_ID);
		pingResponsePacket.setConnectionId(Packet.ANONYMOUS_CONNECTION_ID);

		assertEquals(7, allZeroesPacket.getConnectionId());
		assertEquals(1, protocolValidPacket.getConnectionId());
		assertEquals(Packet.MINIMUM_CONNECTION_ID, protocolInvalidPacket.getConnectionId());
		assertEquals(81, applicationPacketHelloWorld.getConnectionId());
		assertEquals(9557, clientDisconnectPacket.getConnectionId());
		assertEquals(2764, connectionAcceptedPacket.getConnectionId());
		assertEquals(65534, connectionRefusedPacket.getConnectionId());
		assertEquals(65535, connectRequestPacket.getConnectionId());
		assertEquals(0, forceDisconnectPacket.getConnectionId());
		assertEquals(Packet.MAXIMUM_CONNECTION_ID, pingPacket.getConnectionId());
		assertEquals(Packet.ANONYMOUS_CONNECTION_ID, pingResponsePacket.getConnectionId());

		clientDisconnectPacket.setConnectionId(5);
		assertEquals(5, clientDisconnectPacket.getConnectionId());

		clientDisconnectPacket.setConnectionId(5);
		assertEquals(5, clientDisconnectPacket.getConnectionId());

		clientDisconnectPacket.setConnectionId(Packet.ANONYMOUS_CONNECTION_ID);
		assertEquals(Packet.ANONYMOUS_CONNECTION_ID, clientDisconnectPacket.getConnectionId());

		clientDisconnectPacket.setConnectionId(237);
		assertEquals(237, clientDisconnectPacket.getConnectionId());
	}

	@Test
	public void testIsAnonymousConnection() {
		assertTrue(allZeroesPacket.isAnonymousConnection());
		assertFalse(protocolValidPacket.isAnonymousConnection());
		assertFalse(protocolInvalidPacket.isAnonymousConnection());
		assertTrue(applicationPacketHelloWorld.isAnonymousConnection());
		assertFalse(clientDisconnectPacket.isAnonymousConnection());
		assertFalse(connectionAcceptedPacket.isAnonymousConnection());
		assertTrue(connectionRefusedPacket.isAnonymousConnection());
		assertTrue(connectRequestPacket.isAnonymousConnection());
		assertFalse(forceDisconnectPacket.isAnonymousConnection());
		assertFalse(pingPacket.isAnonymousConnection());
		assertFalse(pingResponsePacket.isAnonymousConnection());

		connectionRefusedPacket.setConnectionId(5);
		assertFalse(connectionRefusedPacket.isAnonymousConnection());

		connectionRefusedPacket.setConnectionId(Packet.ANONYMOUS_CONNECTION_ID);
		assertTrue(connectionRefusedPacket.isAnonymousConnection());

		connectionRefusedPacket.setConnectionId(179);
		assertFalse(connectionRefusedPacket.isAnonymousConnection());

		connectionRefusedPacket.setConnectionId(0);
		assertTrue(connectionRefusedPacket.isAnonymousConnection());
	}

	@Test
	public void testGetSequenceNumber() {
		assertEquals(0, allZeroesPacket.getSequenceNumber());
		assertEquals(11089, protocolValidPacket.getSequenceNumber());
		assertEquals(37400, protocolInvalidPacket.getSequenceNumber());
		assertEquals(81, applicationPacketHelloWorld.getSequenceNumber());
		assertEquals(9557, clientDisconnectPacket.getSequenceNumber());
		assertEquals(2764, connectionAcceptedPacket.getSequenceNumber());
		assertEquals(65534, connectionRefusedPacket.getSequenceNumber());
		assertEquals(65535, connectRequestPacket.getSequenceNumber());
		assertEquals(0, forceDisconnectPacket.getSequenceNumber());
		assertEquals(1, pingPacket.getSequenceNumber());
		assertEquals(4, pingResponsePacket.getSequenceNumber());
	}

	@Test
	public void testSetSequenceNumber() {
		allZeroesPacket.setSequenceNumber(42);
		protocolValidPacket.setSequenceNumber(1);
		protocolInvalidPacket.setSequenceNumber(65534);
		applicationPacketHelloWorld.setSequenceNumber(65535);
		clientDisconnectPacket.setSequenceNumber(0);
		connectionAcceptedPacket.setSequenceNumber(74);
		connectionRefusedPacket.setSequenceNumber(8546);
		connectRequestPacket.setSequenceNumber(Packet.ANONYMOUS_CONNECTION_ID);
		forceDisconnectPacket.setSequenceNumber(Packet.MINIMUM_SEQUENCE_NUMBER);
		pingPacket.setSequenceNumber(Packet.MAXIMUM_SEQUENCE_NUMBER);
		pingResponsePacket.setSequenceNumber(12);

		assertEquals(42, allZeroesPacket.getSequenceNumber());
		assertEquals(1, protocolValidPacket.getSequenceNumber());
		assertEquals(65534, protocolInvalidPacket.getSequenceNumber());
		assertEquals(65535, applicationPacketHelloWorld.getSequenceNumber());
		assertEquals(0, clientDisconnectPacket.getSequenceNumber());
		assertEquals(74, connectionAcceptedPacket.getSequenceNumber());
		assertEquals(8546, connectionRefusedPacket.getSequenceNumber());
		assertEquals(Packet.ANONYMOUS_CONNECTION_ID, connectRequestPacket.getSequenceNumber());
		assertEquals(Packet.MINIMUM_SEQUENCE_NUMBER, forceDisconnectPacket.getSequenceNumber());
		assertEquals(Packet.MAXIMUM_SEQUENCE_NUMBER, pingPacket.getSequenceNumber());
		assertEquals(12, pingResponsePacket.getSequenceNumber());

		protocolInvalidPacket.setSequenceNumber(42);
		assertEquals(42, protocolInvalidPacket.getSequenceNumber());

		protocolInvalidPacket.setSequenceNumber(Packet.ANONYMOUS_CONNECTION_ID);
		assertEquals(Packet.ANONYMOUS_CONNECTION_ID, protocolInvalidPacket.getSequenceNumber());

		protocolInvalidPacket.setSequenceNumber(7425);
		assertEquals(7425, protocolInvalidPacket.getSequenceNumber());

		protocolInvalidPacket.setSequenceNumber(7425);
		assertEquals(7425, protocolInvalidPacket.getSequenceNumber());
	}

	@Test
	public void testHasSequenceNumber() {
		assertFalse(allZeroesPacket.hasSequenceNumber());
		assertTrue(protocolValidPacket.hasSequenceNumber());
		assertTrue(protocolInvalidPacket.hasSequenceNumber());
		assertTrue(applicationPacketHelloWorld.hasSequenceNumber());
		assertTrue(clientDisconnectPacket.hasSequenceNumber());
		assertTrue(connectionAcceptedPacket.hasSequenceNumber());
		assertTrue(connectionRefusedPacket.hasSequenceNumber());
		assertTrue(connectRequestPacket.hasSequenceNumber());
		assertFalse(forceDisconnectPacket.hasSequenceNumber());
		assertTrue(pingPacket.hasSequenceNumber());
		assertTrue(pingResponsePacket.hasSequenceNumber());

		applicationPacketHelloWorld.setSequenceNumber(5);
		assertTrue(applicationPacketHelloWorld.hasSequenceNumber());

		applicationPacketHelloWorld.setSequenceNumber(Packet.SEQUENCE_NUMBER_NOT_APPLICABLE);
		assertFalse(applicationPacketHelloWorld.hasSequenceNumber());

		applicationPacketHelloWorld.setSequenceNumber(2700);
		assertTrue(applicationPacketHelloWorld.hasSequenceNumber());
	}

	@Test
	public void testGetDuplicateSequenceNumber() {
		assertEquals(0, allZeroesPacket.getDuplicateSequenceNumber());
		assertEquals(20, protocolValidPacket.getDuplicateSequenceNumber());
		assertEquals(52886, protocolInvalidPacket.getDuplicateSequenceNumber());
		assertEquals(65535, applicationPacketHelloWorld.getDuplicateSequenceNumber());
		assertEquals(65534, clientDisconnectPacket.getDuplicateSequenceNumber());
		assertEquals(1, connectionAcceptedPacket.getDuplicateSequenceNumber());
		assertEquals(0, connectionRefusedPacket.getDuplicateSequenceNumber());
		assertEquals(52, connectRequestPacket.getDuplicateSequenceNumber());
		assertEquals(0, forceDisconnectPacket.getDuplicateSequenceNumber());
		assertEquals(0, pingPacket.getDuplicateSequenceNumber());
		assertEquals(3000, pingResponsePacket.getDuplicateSequenceNumber());
	}

	@Test
	public void testSetDuplicateSequenceNumber() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsDuplicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetLastReceivedSequenceNumber() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetLastReceivedSequenceNumber() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasReceivedPacketHistory() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetReceivedPacketHistory() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetReceivedPacketHistory() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsImmediateResponse() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetIsImmediateResponse() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMessageType() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetMessageType() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMessage() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetMessage() {
		fail("Not yet implemented");
	}

	@Test
	public void testToByteArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testToString() {
		fail("Not yet implemented");
	}

	@Test
	public void testNextConnectionId() {
		assertEquals(Packet.MINIMUM_CONNECTION_ID, Packet.nextConnectionId(Packet.ANONYMOUS_CONNECTION_ID));
		assertEquals(Packet.MINIMUM_CONNECTION_ID + 1, Packet.nextConnectionId(Packet.MINIMUM_CONNECTION_ID));
		assertEquals(Packet.MINIMUM_CONNECTION_ID, Packet.nextConnectionId(Packet.MAXIMUM_CONNECTION_ID));

		assertEquals(1, Packet.nextConnectionId(0));
		assertEquals(2, Packet.nextConnectionId(1));
		assertEquals(5, Packet.nextConnectionId(4));
		assertEquals(255, Packet.nextConnectionId(254));
		assertEquals(1, Packet.nextConnectionId(255));
	}

	@Test
	public void testNextSequenceNumber() {
		assertEquals(Packet.MINIMUM_SEQUENCE_NUMBER, Packet.nextSequenceNumber(Packet.SEQUENCE_NUMBER_NOT_APPLICABLE));
		assertEquals(Packet.MINIMUM_SEQUENCE_NUMBER + 1, Packet.nextSequenceNumber(Packet.MINIMUM_SEQUENCE_NUMBER));
		assertEquals(Packet.MINIMUM_SEQUENCE_NUMBER, Packet.nextSequenceNumber(Packet.MAXIMUM_SEQUENCE_NUMBER));

		assertEquals(1, Packet.nextSequenceNumber(0));
		assertEquals(2, Packet.nextSequenceNumber(1));
		assertEquals(5, Packet.nextSequenceNumber(4));
		assertEquals(65535, Packet.nextSequenceNumber(65534));
		assertEquals(1, Packet.nextSequenceNumber(65535));
	}

	@Test
	public void testDeltaBetweenSequenceNumbers() {
		int min = Packet.MINIMUM_SEQUENCE_NUMBER;
		int max = Packet.MAXIMUM_SEQUENCE_NUMBER;

		assertEquals(-50, Packet.deltaBetweenSequenceNumbers(55, 5));
		assertEquals(-4, Packet.deltaBetweenSequenceNumbers(9, 5));
		assertEquals(-3, Packet.deltaBetweenSequenceNumbers(8, 5));
		assertEquals(-2, Packet.deltaBetweenSequenceNumbers(7, 5));
		assertEquals(-1, Packet.deltaBetweenSequenceNumbers(6, 5));
		assertEquals(0, Packet.deltaBetweenSequenceNumbers(5, 5));
		assertEquals(1, Packet.deltaBetweenSequenceNumbers(4, 5));
		assertEquals(2, Packet.deltaBetweenSequenceNumbers(3, 5));
		assertEquals(3, Packet.deltaBetweenSequenceNumbers(2, 5));
		assertEquals(4, Packet.deltaBetweenSequenceNumbers(1, 5));
		assertEquals(50, Packet.deltaBetweenSequenceNumbers(5, 55));

		assertEquals(-100, Packet.deltaBetweenSequenceNumbers(3500, 3400));
		assertEquals(0, Packet.deltaBetweenSequenceNumbers(3400, 3400));
		assertEquals(100, Packet.deltaBetweenSequenceNumbers(3300, 3400));

		assertEquals(0, Packet.deltaBetweenSequenceNumbers(min, min));
		assertEquals(0, Packet.deltaBetweenSequenceNumbers(max, max));
		assertEquals(1, Packet.deltaBetweenSequenceNumbers(max, min));
		assertEquals(-1, Packet.deltaBetweenSequenceNumbers(min, max));
		assertEquals(-3, Packet.deltaBetweenSequenceNumbers(min + 1, max - 1));

		int qtr = max/4;
		assertEquals(qtr, Packet.deltaBetweenSequenceNumbers(5, 5 + qtr));
		assertEquals(3 + 1 + qtr, Packet.deltaBetweenSequenceNumbers(max - 3, min + qtr));
	}

	@Test
	public void testParsePacketByteArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testParsePacketByteArrayInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateApplicationPacket() {
		assertNotNull(applicationPacketHelloWorld);
		assertNotNull(Packet.createApplicationPacket(Packet.ANONYMOUS_CONNECTION_ID, "Hello world!"));
		assertNotNull(Packet.createApplicationPacket(Packet.MINIMUM_CONNECTION_ID, null));
		assertNotNull(Packet.createApplicationPacket(Packet.MAXIMUM_CONNECTION_ID, "~!@#$%^&*()`1234567890-=_+[]{};':\"<>?,./\\|"));
		assertNotNull(Packet.createApplicationPacket(0,
				"123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "1234567890"));
		assertNotNull(Packet.createApplicationPacket(1, ""));
		assertNotNull(Packet.createApplicationPacket(100, "1"));
		assertNotNull(Packet.createApplicationPacket(254, null));
		assertNotNull(Packet.createApplicationPacket(255, "<fake-xml>bar</fake-xml>"));
	}

	@Test
	public void testCreatePingPacket() {
		assertNotNull(pingPacket);
		assertNotNull(Packet.createPingPacket(Packet.ANONYMOUS_CONNECTION_ID));
		assertNotNull(Packet.createPingPacket(Packet.MINIMUM_CONNECTION_ID));
		assertNotNull(Packet.createPingPacket(Packet.MAXIMUM_CONNECTION_ID));
		assertNotNull(Packet.createPingPacket(0));
		assertNotNull(Packet.createPingPacket(1));
		assertNotNull(Packet.createPingPacket(100));
		assertNotNull(Packet.createPingPacket(254));
		assertNotNull(Packet.createPingPacket(255));
	}

	@Test
	public void testCreatePingResponsePacket() {
		assertNotNull(pingResponsePacket);
		assertNotNull(Packet.createPingResponsePacket(Packet.ANONYMOUS_CONNECTION_ID));
		assertNotNull(Packet.createPingResponsePacket(Packet.MINIMUM_CONNECTION_ID));
		assertNotNull(Packet.createPingResponsePacket(Packet.MAXIMUM_CONNECTION_ID));
		assertNotNull(Packet.createPingResponsePacket(0));
		assertNotNull(Packet.createPingResponsePacket(1));
		assertNotNull(Packet.createPingResponsePacket(100));
		assertNotNull(Packet.createPingResponsePacket(254));
		assertNotNull(Packet.createPingResponsePacket(255));
	}

	@Test
	public void testCreateConnectRequestPacket() {
		assertNotNull(connectRequestPacket);
		assertNotNull(Packet.createConnectRequestPacket());
	}

	@Test
	public void testCreateConnectionAcceptedPacket() {
		assertNotNull(connectionAcceptedPacket);
		assertNotNull(Packet.createConnectionAcceptedPacket(Packet.ANONYMOUS_CONNECTION_ID));
		assertNotNull(Packet.createConnectionAcceptedPacket(Packet.MINIMUM_CONNECTION_ID));
		assertNotNull(Packet.createConnectionAcceptedPacket(Packet.MAXIMUM_CONNECTION_ID));
		assertNotNull(Packet.createConnectionAcceptedPacket(0));
		assertNotNull(Packet.createConnectionAcceptedPacket(1));
		assertNotNull(Packet.createConnectionAcceptedPacket(100));
		assertNotNull(Packet.createConnectionAcceptedPacket(254));
		assertNotNull(Packet.createConnectionAcceptedPacket(255));
	}

	@Test
	public void testCreateConnectionRefusedPacket() {
		assertNotNull(connectionRefusedPacket);
		assertNotNull(Packet.createConnectionRefusedPacket());
	}

	@Test
	public void testCreateForceDisconnectPacket() {
		assertNotNull(forceDisconnectPacket);
		assertNotNull(Packet.createForceDisconnectPacket(Packet.ANONYMOUS_CONNECTION_ID));
		assertNotNull(Packet.createForceDisconnectPacket(Packet.MINIMUM_CONNECTION_ID));
		assertNotNull(Packet.createForceDisconnectPacket(Packet.MAXIMUM_CONNECTION_ID));
		assertNotNull(Packet.createForceDisconnectPacket(0));
		assertNotNull(Packet.createForceDisconnectPacket(1));
		assertNotNull(Packet.createForceDisconnectPacket(100));
		assertNotNull(Packet.createForceDisconnectPacket(254));
		assertNotNull(Packet.createForceDisconnectPacket(255));
	}

	@Test
	public void testCreateClientDisconnectPacket() {
		assertNotNull(clientDisconnectPacket);
		assertNotNull(Packet.createClientDisconnectPacket(Packet.ANONYMOUS_CONNECTION_ID));
		assertNotNull(Packet.createClientDisconnectPacket(Packet.MINIMUM_CONNECTION_ID));
		assertNotNull(Packet.createClientDisconnectPacket(Packet.MAXIMUM_CONNECTION_ID));
		assertNotNull(Packet.createClientDisconnectPacket(0));
		assertNotNull(Packet.createClientDisconnectPacket(1));
		assertNotNull(Packet.createClientDisconnectPacket(100));
		assertNotNull(Packet.createClientDisconnectPacket(254));
		assertNotNull(Packet.createClientDisconnectPacket(255));
	}
}