package com.benlawrencem.net.nightingale.test;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.benlawrencem.net.nightingale.Packet;
import com.benlawrencem.net.nightingale.Packet.MalformedPacketException;
import com.benlawrencem.net.nightingale.Packet.MessageType;
import com.benlawrencem.net.nightingale.Packet.PacketEncodingException;

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
				0, 0, 0, 0,			//Protocol Id:     0 -- INVALID
				0,					//Connection Id:   ANONYMOUS
				0, 0,				//Sequence Number: N/A
				0, 0,				//Duplicate Of:    N/A
				0, 0,				//Last Received:   N/A
				0, 0, 0, 0,			//Packet History:  0
				0,					//Packet Flags:    NOT IMMEDIATE
				0					//Message Type:    INVALID
									//Message:         null
		});
		protocolValidPacket = Packet.parsePacket(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				-117,				//Connection Id:   139
				43, 81,				//Sequence Number: 11089
				0, 20,				//Duplicate Of:    20
				-119, 119,			//Last Received:   35191
				42, -78, 0, -11,	//Packet History:  716308725
				-128,				//Packet Flags:    IMMEDIATE
				-127				//Message Type:    PING
									//Message:         null
		});
		protocolInvalidPacket = Packet.parsePacket(new byte[] {
				-50, 2, 5, 81,		//Protocol Id:     -838728367 -- INVALID
				24,					//Connection Id:   24
				-110, 26,			//Sequence Number: 37402
				-50, -104,			//Duplicate Of:    52888
				26, -47,			//Last Received:   6865
				12, 15, 108, -82,	//Packet History:  202337454
				0,					//Packet Flags:    NOT IMMEDIATE
				0					//Message Type:    INVALID
									//Message:         null
		});
		applicationPacketHelloWorld = Packet.parsePacket(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				0,					//Connection Id:   ANONYMOUS
				0, 81,				//Sequence Number: 81
				-1, -1,				//Duplicate Of:    65535
				0, 27,				//Last Received:   27
				2, 16, -121, 124,	//Packet History:  34637692
				0,					//Packet Flags:    NOT IMMEDIATE
				-128,				//Message Type:    APPLICATION
									//Message:         "Hello world!"
				72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33
		});
		clientDisconnectPacket = Packet.parsePacket(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				1,					//Connection Id:   1
				37, 85,				//Sequence Number: 9557
				-1, -2,				//Duplicate Of:    65534
				0, 8,				//Last Received:   8
				4, -103, 2, -103,	//Packet History:  77136537
				-128,				//Packet Flags:    IMMEDIATE
				-121				//Message Type:    CLIENT_DISCONNECT
									//Message:         null
		});
		connectionAcceptedPacket = Packet.parsePacket(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				82, 				//Connection Id:   82
				10, -52,			//Sequence Number: 2764
				0, 1,				//Duplicate Of:    1
				15, -96,			//Last Received:   4000
				2, 110, 91, -128,	//Packet History:  40786816
				0,					//Packet Flags:    NOT IMMEDIATE
				-124				//Message Type:    CONNECTION_ACCEPTED
									//Message:         null
		});
		connectionRefusedPacket = Packet.parsePacket(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				0,					//Connection Id:   ANONYMOUS
				-1, -2,				//Sequence Number: 65534
				0, 0,				//Duplicate Of:    N/A
				0, 0,				//Last Received:   N/A
				6, -41, 36, 42,		//Packet History:  114762794
				0,					//Packet Flags:    NOT IMMEDIATE
				-123				//Message Type:    CONNECTION_REFUSED
									//Message:         null
		});
		connectRequestPacket = Packet.parsePacket(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				0,					//Connection Id:   ANONYMOUS
				-1, -1,				//Sequence Number: 65535
				0, 52,				//Duplicate Of:    52
				-1, -2,				//Last Received:   65534
				10, -67, -13, -78,	//Packet History:  180220850
				0,					//Packet Flags:    NOT IMMEDIATE
				-125				//Message Type:    CONNECT_REQUEST
									//Message:         null
		});
		forceDisconnectPacket = Packet.parsePacket(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				-56,				//Connection Id:   200
				0, 0,				//Sequence Number: N/A
				0, 0,				//Duplicate Of:    N/A
				-1, -1,				//Last Received:   65535
				3, -85, 85, 119,	//Packet History:  61560183
				0,					//Packet Flags:    NOT IMMEDIATE
				-122				//Message Type:    FORCE_DISCONNECT
									//Message:         null
		});
		pingPacket = Packet.parsePacket(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				-2,					//Connection Id:   254
				0, 1,				//Sequence Number: 1
				0, 0,				//Duplicate Of:    N/A
				0, 1,				//Last Received:   1
				10, 54, 121, 31,	//Packet History:  171342111
				0,					//Packet Flags:    NOT IMMEDIATE
				-127				//Message Type:    PING
									//Message:         null
		});
		pingResponsePacket = Packet.parsePacket(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				-1,					//Connection Id:   255
				0, 4,				//Sequence Number: 4
				11, -72,			//Duplicate Of:    3000
				0, 0,				//Last Received:   N/A
				0, 125, 74, 30,		//Packet History:  8210974
				-128,				//Packet Flags:    IMMEDIATE
				-126				//Message Type:    PING_RESPONSE
									//Message:         null
		});
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
		assertEquals(139, protocolValidPacket.getConnectionId());
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
		assertEquals(37402, protocolInvalidPacket.getSequenceNumber());
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
		assertEquals(52888, protocolInvalidPacket.getDuplicateSequenceNumber());
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
		allZeroesPacket.setDuplicateSequenceNumber(2000);
		protocolValidPacket.setDuplicateSequenceNumber(52);
		protocolInvalidPacket.setDuplicateSequenceNumber(908);
		applicationPacketHelloWorld.setDuplicateSequenceNumber(14);
		clientDisconnectPacket.setDuplicateSequenceNumber(0);
		connectionAcceptedPacket.setDuplicateSequenceNumber(1);
		connectionRefusedPacket.setDuplicateSequenceNumber(Packet.SEQUENCE_NUMBER_NOT_APPLICABLE);
		connectRequestPacket.setDuplicateSequenceNumber(65534);
		forceDisconnectPacket.setDuplicateSequenceNumber(Packet.MINIMUM_CONNECTION_ID);
		pingPacket.setDuplicateSequenceNumber(65535);
		pingResponsePacket.setDuplicateSequenceNumber(Packet.MAXIMUM_SEQUENCE_NUMBER);

		assertEquals(2000, allZeroesPacket.getDuplicateSequenceNumber());
		assertEquals(52, protocolValidPacket.getDuplicateSequenceNumber());
		assertEquals(908, protocolInvalidPacket.getDuplicateSequenceNumber());
		assertEquals(14, applicationPacketHelloWorld.getDuplicateSequenceNumber());
		assertEquals(0, clientDisconnectPacket.getDuplicateSequenceNumber());
		assertEquals(1, connectionAcceptedPacket.getDuplicateSequenceNumber());
		assertEquals(Packet.SEQUENCE_NUMBER_NOT_APPLICABLE, connectionRefusedPacket.getDuplicateSequenceNumber());
		assertEquals(65534, connectRequestPacket.getDuplicateSequenceNumber());
		assertEquals(Packet.MINIMUM_CONNECTION_ID, forceDisconnectPacket.getDuplicateSequenceNumber());
		assertEquals(65535, pingPacket.getDuplicateSequenceNumber());
		assertEquals(Packet.MAXIMUM_SEQUENCE_NUMBER, pingResponsePacket.getDuplicateSequenceNumber());

		connectRequestPacket.setDuplicateSequenceNumber(32);
		assertEquals(32, connectRequestPacket.getDuplicateSequenceNumber());

		connectRequestPacket.setDuplicateSequenceNumber(32);
		assertEquals(32, connectRequestPacket.getDuplicateSequenceNumber());

		connectRequestPacket.setDuplicateSequenceNumber(0);
		assertEquals(0, connectRequestPacket.getDuplicateSequenceNumber());

		connectRequestPacket.setDuplicateSequenceNumber(5000);
		assertEquals(5000, connectRequestPacket.getDuplicateSequenceNumber());
	}

	@Test
	public void testIsDuplicate() {
		assertFalse(allZeroesPacket.isDuplicate());
		assertTrue(protocolValidPacket.isDuplicate());
		assertTrue(protocolInvalidPacket.isDuplicate());
		assertTrue(applicationPacketHelloWorld.isDuplicate());
		assertTrue(clientDisconnectPacket.isDuplicate());
		assertTrue(connectionAcceptedPacket.isDuplicate());
		assertFalse(connectionRefusedPacket.isDuplicate());
		assertTrue(connectRequestPacket.isDuplicate());
		assertFalse(forceDisconnectPacket.isDuplicate());
		assertFalse(pingPacket.isDuplicate());
		assertTrue(pingResponsePacket.isDuplicate());

		pingPacket.setDuplicateSequenceNumber(10);
		assertTrue(pingPacket.isDuplicate());

		pingPacket.setDuplicateSequenceNumber(Packet.SEQUENCE_NUMBER_NOT_APPLICABLE);
		assertFalse(pingPacket.isDuplicate());

		pingPacket.setDuplicateSequenceNumber(5);
		assertTrue(pingPacket.isDuplicate());
	}

	@Test
	public void testGetLastReceivedSequenceNumber() {
		assertEquals(0, allZeroesPacket.getLastReceivedSequenceNumber());
		assertEquals(35191, protocolValidPacket.getLastReceivedSequenceNumber());
		assertEquals(6865, protocolInvalidPacket.getLastReceivedSequenceNumber());
		assertEquals(27, applicationPacketHelloWorld.getLastReceivedSequenceNumber());
		assertEquals(8, clientDisconnectPacket.getLastReceivedSequenceNumber());
		assertEquals(4000, connectionAcceptedPacket.getLastReceivedSequenceNumber());
		assertEquals(0, connectionRefusedPacket.getLastReceivedSequenceNumber());
		assertEquals(65534, connectRequestPacket.getLastReceivedSequenceNumber());
		assertEquals(65535, forceDisconnectPacket.getLastReceivedSequenceNumber());
		assertEquals(1, pingPacket.getLastReceivedSequenceNumber());
		assertEquals(0, pingResponsePacket.getLastReceivedSequenceNumber());
	}

	@Test
	public void testSetLastReceivedSequenceNumber() {
		allZeroesPacket.setLastReceivedSequenceNumber(62);
		protocolValidPacket.setLastReceivedSequenceNumber(1);
		protocolInvalidPacket.setLastReceivedSequenceNumber(0);
		applicationPacketHelloWorld.setLastReceivedSequenceNumber(65535);
		clientDisconnectPacket.setLastReceivedSequenceNumber(65534);
		connectionAcceptedPacket.setLastReceivedSequenceNumber(86);
		connectionRefusedPacket.setLastReceivedSequenceNumber(40000);
		connectRequestPacket.setLastReceivedSequenceNumber(Packet.MINIMUM_SEQUENCE_NUMBER);
		forceDisconnectPacket.setLastReceivedSequenceNumber(Packet.MAXIMUM_SEQUENCE_NUMBER);
		pingPacket.setLastReceivedSequenceNumber(87);
		pingResponsePacket.setLastReceivedSequenceNumber(Packet.SEQUENCE_NUMBER_NOT_APPLICABLE);

		assertEquals(62, allZeroesPacket.getLastReceivedSequenceNumber());
		assertEquals(1, protocolValidPacket.getLastReceivedSequenceNumber());
		assertEquals(0, protocolInvalidPacket.getLastReceivedSequenceNumber());
		assertEquals(65535, applicationPacketHelloWorld.getLastReceivedSequenceNumber());
		assertEquals(65534, clientDisconnectPacket.getLastReceivedSequenceNumber());
		assertEquals(86, connectionAcceptedPacket.getLastReceivedSequenceNumber());
		assertEquals(40000, connectionRefusedPacket.getLastReceivedSequenceNumber());
		assertEquals(Packet.MINIMUM_SEQUENCE_NUMBER, connectRequestPacket.getLastReceivedSequenceNumber());
		assertEquals(Packet.MAXIMUM_SEQUENCE_NUMBER, forceDisconnectPacket.getLastReceivedSequenceNumber());
		assertEquals(87, pingPacket.getLastReceivedSequenceNumber());
		assertEquals(Packet.SEQUENCE_NUMBER_NOT_APPLICABLE, pingResponsePacket.getLastReceivedSequenceNumber());

		pingPacket.setLastReceivedSequenceNumber(1000);
		assertEquals(1000, pingPacket.getLastReceivedSequenceNumber());

		pingPacket.setLastReceivedSequenceNumber(1000);
		assertEquals(1000, pingPacket.getLastReceivedSequenceNumber());

		pingPacket.setLastReceivedSequenceNumber(0);
		assertEquals(0, pingPacket.getLastReceivedSequenceNumber());

		pingPacket.setLastReceivedSequenceNumber(25);
		assertEquals(25, pingPacket.getLastReceivedSequenceNumber());
	}

	@Test
	public void testHasReceivedPacketHistory() {
		assertFalse(allZeroesPacket.hasReceivedPacketHistory());
		assertTrue(protocolValidPacket.hasReceivedPacketHistory());
		assertTrue(protocolInvalidPacket.hasReceivedPacketHistory());
		assertTrue(applicationPacketHelloWorld.hasReceivedPacketHistory());
		assertTrue(clientDisconnectPacket.hasReceivedPacketHistory());
		assertTrue(connectionAcceptedPacket.hasReceivedPacketHistory());
		assertFalse(connectionRefusedPacket.hasReceivedPacketHistory());
		assertTrue(connectRequestPacket.hasReceivedPacketHistory());
		assertTrue(forceDisconnectPacket.hasReceivedPacketHistory());
		assertTrue(pingPacket.hasReceivedPacketHistory());
		assertFalse(pingResponsePacket.hasReceivedPacketHistory());

		pingResponsePacket.setLastReceivedSequenceNumber(Packet.SEQUENCE_NUMBER_NOT_APPLICABLE);
		assertFalse(pingResponsePacket.hasReceivedPacketHistory());

		pingResponsePacket.setLastReceivedSequenceNumber(3);
		assertTrue(pingResponsePacket.hasReceivedPacketHistory());
	}

	@Test
	public void testGetReceivedPacketHistory() {
		assertEquals(0, allZeroesPacket.getReceivedPacketHistory());
		assertEquals(716308725, protocolValidPacket.getReceivedPacketHistory());
		assertEquals(202337454, protocolInvalidPacket.getReceivedPacketHistory());
		assertEquals(34637692, applicationPacketHelloWorld.getReceivedPacketHistory());
		assertEquals(77136537, clientDisconnectPacket.getReceivedPacketHistory());
		assertEquals(40786816, connectionAcceptedPacket.getReceivedPacketHistory());
		assertEquals(114762794, connectionRefusedPacket.getReceivedPacketHistory());
		assertEquals(180220850, connectRequestPacket.getReceivedPacketHistory());
		assertEquals(61560183, forceDisconnectPacket.getReceivedPacketHistory());
		assertEquals(171342111, pingPacket.getReceivedPacketHistory());
		assertEquals(8210974, pingResponsePacket.getReceivedPacketHistory());
	}

	@Test
	public void testSetReceivedPacketHistory() {
		allZeroesPacket.setReceivedPacketHistory(2147483647);
		protocolValidPacket.setReceivedPacketHistory(-2147483648);
		protocolInvalidPacket.setReceivedPacketHistory(0);
		applicationPacketHelloWorld.setReceivedPacketHistory(82266084);
		clientDisconnectPacket.setReceivedPacketHistory(79112450);
		connectionAcceptedPacket.setReceivedPacketHistory(-137004667);
		connectionRefusedPacket.setReceivedPacketHistory(156227865);
		connectRequestPacket.setReceivedPacketHistory(185700919);
		forceDisconnectPacket.setReceivedPacketHistory(-14970660);
		pingPacket.setReceivedPacketHistory(134470887);
		pingResponsePacket.setReceivedPacketHistory(54348759);

		assertEquals(2147483647, allZeroesPacket.getReceivedPacketHistory());
		assertEquals(-2147483648, protocolValidPacket.getReceivedPacketHistory());
		assertEquals(0, protocolInvalidPacket.getReceivedPacketHistory());
		assertEquals(82266084, applicationPacketHelloWorld.getReceivedPacketHistory());
		assertEquals(79112450, clientDisconnectPacket.getReceivedPacketHistory());
		assertEquals(-137004667, connectionAcceptedPacket.getReceivedPacketHistory());
		assertEquals(156227865, connectionRefusedPacket.getReceivedPacketHistory());
		assertEquals(185700919, connectRequestPacket.getReceivedPacketHistory());
		assertEquals(-14970660, forceDisconnectPacket.getReceivedPacketHistory());
		assertEquals(134470887, pingPacket.getReceivedPacketHistory());
		assertEquals(54348759, pingResponsePacket.getReceivedPacketHistory());

		connectRequestPacket.setReceivedPacketHistory(0);
		assertEquals(0, connectRequestPacket.getReceivedPacketHistory());

		connectRequestPacket.setReceivedPacketHistory(5);
		assertEquals(5, connectRequestPacket.getReceivedPacketHistory());

		connectRequestPacket.setReceivedPacketHistory(5);
		assertEquals(5, connectRequestPacket.getReceivedPacketHistory());
	}

	@Test
	public void testIsImmediateResponse() {
		assertFalse(allZeroesPacket.isImmediateResponse());
		assertTrue(protocolValidPacket.isImmediateResponse());
		assertFalse(protocolInvalidPacket.isImmediateResponse());
		assertFalse(applicationPacketHelloWorld.isImmediateResponse());
		assertTrue(clientDisconnectPacket.isImmediateResponse());
		assertFalse(connectionAcceptedPacket.isImmediateResponse());
		assertFalse(connectionRefusedPacket.isImmediateResponse());
		assertFalse(connectRequestPacket.isImmediateResponse());
		assertFalse(forceDisconnectPacket.isImmediateResponse());
		assertFalse(pingPacket.isImmediateResponse());
		assertTrue(pingResponsePacket.isImmediateResponse());
	}

	@Test
	public void testSetIsImmediateResponse() {
		allZeroesPacket.setIsImmediateResponse(true);
		protocolValidPacket.setIsImmediateResponse(false);
		protocolInvalidPacket.setIsImmediateResponse(true);
		applicationPacketHelloWorld.setIsImmediateResponse(false);
		clientDisconnectPacket.setIsImmediateResponse(true);
		connectionAcceptedPacket.setIsImmediateResponse(false);
		connectionRefusedPacket.setIsImmediateResponse(true);
		connectRequestPacket.setIsImmediateResponse(false);
		forceDisconnectPacket.setIsImmediateResponse(true);
		pingPacket.setIsImmediateResponse(false);
		pingResponsePacket.setIsImmediateResponse(true);
		
		assertTrue(allZeroesPacket.isImmediateResponse());
		assertFalse(protocolValidPacket.isImmediateResponse());
		assertTrue(protocolInvalidPacket.isImmediateResponse());
		assertFalse(applicationPacketHelloWorld.isImmediateResponse());
		assertTrue(clientDisconnectPacket.isImmediateResponse());
		assertFalse(connectionAcceptedPacket.isImmediateResponse());
		assertTrue(connectionRefusedPacket.isImmediateResponse());
		assertFalse(connectRequestPacket.isImmediateResponse());
		assertTrue(forceDisconnectPacket.isImmediateResponse());
		assertFalse(pingPacket.isImmediateResponse());
		assertTrue(pingResponsePacket.isImmediateResponse());

	}

	@Test
	public void testGetMessageType() {
		assertEquals(MessageType.INVALID, allZeroesPacket.getMessageType());
		assertEquals(MessageType.PING, protocolValidPacket.getMessageType());
		assertEquals(MessageType.INVALID, protocolInvalidPacket.getMessageType());
		assertEquals(MessageType.APPLICATION, applicationPacketHelloWorld.getMessageType());
		assertEquals(MessageType.CLIENT_DISCONNECT, clientDisconnectPacket.getMessageType());
		assertEquals(MessageType.CONNECTION_ACCEPTED, connectionAcceptedPacket.getMessageType());
		assertEquals(MessageType.CONNECTION_REFUSED, connectionRefusedPacket.getMessageType());
		assertEquals(MessageType.CONNECT_REQUEST, connectRequestPacket.getMessageType());
		assertEquals(MessageType.FORCE_DISCONNECT, forceDisconnectPacket.getMessageType());
		assertEquals(MessageType.PING, pingPacket.getMessageType());
		assertEquals(MessageType.PING_RESPONSE, pingResponsePacket.getMessageType());
	}

	@Test
	public void testSetMessageType() {
		protocolValidPacket.setMessageType(MessageType.INVALID);
		assertEquals(MessageType.INVALID, protocolValidPacket.getMessageType());

		protocolValidPacket.setMessageType(MessageType.PING);
		assertEquals(MessageType.PING, protocolValidPacket.getMessageType());

		protocolValidPacket.setMessageType(MessageType.APPLICATION);
		assertEquals(MessageType.APPLICATION, protocolValidPacket.getMessageType());

		protocolValidPacket.setMessageType(MessageType.CLIENT_DISCONNECT);
		assertEquals(MessageType.CLIENT_DISCONNECT, protocolValidPacket.getMessageType());

		protocolValidPacket.setMessageType(MessageType.CONNECTION_REFUSED);
		assertEquals(MessageType.CONNECTION_REFUSED, protocolValidPacket.getMessageType());

		protocolValidPacket.setMessageType(MessageType.CONNECT_REQUEST);
		assertEquals(MessageType.CONNECT_REQUEST, protocolValidPacket.getMessageType());

		protocolValidPacket.setMessageType(MessageType.FORCE_DISCONNECT);
		assertEquals(MessageType.FORCE_DISCONNECT, protocolValidPacket.getMessageType());

		protocolValidPacket.setMessageType(MessageType.PING);
		assertEquals(MessageType.PING, protocolValidPacket.getMessageType());

		protocolValidPacket.setMessageType(MessageType.PING_RESPONSE);
		assertEquals(MessageType.PING_RESPONSE, protocolValidPacket.getMessageType());
	}

	@Test
	public void testGetMessage() {
		assertNull(allZeroesPacket.getMessage());
		assertNull(protocolValidPacket.getMessage());
		assertNull(protocolInvalidPacket.getMessage());
		assertEquals("Hello world!", applicationPacketHelloWorld.getMessage());
		assertNull(clientDisconnectPacket.getMessage());
		assertNull(connectionAcceptedPacket.getMessage());
		assertNull(connectionRefusedPacket.getMessage());
		assertNull(connectRequestPacket.getMessage());
		assertNull(forceDisconnectPacket.getMessage());
		assertNull(pingPacket.getMessage());
		assertNull(pingResponsePacket.getMessage());

		assertNull(Packet.createApplicationPacket(0, "").getMessage());
		assertEquals("bar", Packet.createApplicationPacket(0, "bar").getMessage());
		assertEquals("~!@#$%^&*()_+`1234567890-=[]\\{}|;':\",/<>?", Packet.createApplicationPacket(0, "~!@#$%^&*()_+`1234567890-=[]\\{}|;':\",/<>?").getMessage());
	}

	@Test
	public void testSetMessage() {
		allZeroesPacket.setMessage("");
		protocolValidPacket.setMessage("1");
		protocolInvalidPacket.setMessage(null);
		applicationPacketHelloWorld.setMessage("abcdef");
		clientDisconnectPacket.setMessage("123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "1234567890");
		connectionAcceptedPacket.setMessage("~!@#$%^&*()_+`1234567890-={}|\\[]:\";',./<>?ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
		connectionRefusedPacket.setMessage("a");

		assertNull(allZeroesPacket.getMessage());
		assertEquals("1", protocolValidPacket.getMessage());
		assertNull(protocolInvalidPacket.getMessage());
		assertEquals("abcdef", applicationPacketHelloWorld.getMessage());
		assertEquals("123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
				+ "1234567890", clientDisconnectPacket.getMessage());
		assertEquals("~!@#$%^&*()_+`1234567890-={}|\\[]:\";',./<>?ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", connectionAcceptedPacket.getMessage());
		assertEquals("a", connectionRefusedPacket.getMessage());

		connectionRefusedPacket.setMessage("b");
		assertEquals("b", connectionRefusedPacket.getMessage());

		connectionRefusedPacket.setMessage("b");
		assertEquals("b", connectionRefusedPacket.getMessage());

		connectionRefusedPacket.setMessage(null);
		assertNull(connectionRefusedPacket.getMessage());

		connectionRefusedPacket.setMessage("c");
		assertEquals("c", connectionRefusedPacket.getMessage());
	}

	@Test
	public void testToByteArray() {
		try {
			Assert.assertArrayEquals(new byte[] {
					0, 0, 0, 0,			//Protocol Id:     0 -- INVALID
					0,					//Connection Id:   ANONYMOUS
					0, 0,				//Sequence Number: N/A
					0, 0,				//Duplicate Of:    N/A
					0, 0,				//Last Received:   N/A
					0, 0, 0, 0,			//Packet History:  0
					0,					//Packet Flags:    NOT IMMEDIATE
					0					//Message Type:    INVALID
										//Message:         null
			}, allZeroesPacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				-117,				//Connection Id:   139
				43, 81,				//Sequence Number: 11089
				0, 20,				//Duplicate Of:    20
				-119, 119,			//Last Received:   35191
				42, -78, 0, -11,	//Packet History:  716308725
				-128,				//Packet Flags:    IMMEDIATE
				-127				//Message Type:    PING
									//Message:         null
			}, protocolValidPacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				-50, 2, 5, 81,		//Protocol Id:     -838728367 -- INVALID
				24,					//Connection Id:   24
				-110, 26,			//Sequence Number: 37402
				-50, -104,			//Duplicate Of:    52888
				26, -47,			//Last Received:   6865
				12, 15, 108, -82,	//Packet History:  202337454
				0,					//Packet Flags:    NOT IMMEDIATE
				0					//Message Type:    INVALID
									//Message:         null
			}, protocolInvalidPacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				0,					//Connection Id:   ANONYMOUS
				0, 81,				//Sequence Number: 81
				-1, -1,				//Duplicate Of:    65535
				0, 27,				//Last Received:   27
				2, 16, -121, 124,	//Packet History:  34637692
				0,					//Packet Flags:    NOT IMMEDIATE
				-128,				//Message Type:    APPLICATION
									//Message:         "Hello world!"
				72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33
			}, applicationPacketHelloWorld.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				1,					//Connection Id:   1
				37, 85,				//Sequence Number: 9557
				-1, -2,				//Duplicate Of:    65534
				0, 8,				//Last Received:   8
				4, -103, 2, -103,	//Packet History:  77136537
				-128,				//Packet Flags:    IMMEDIATE
				-121				//Message Type:    CLIENT_DISCONNECT
									//Message:         null
			}, clientDisconnectPacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				82, 				//Connection Id:   82
				10, -52,			//Sequence Number: 2764
				0, 1,				//Duplicate Of:    1
				15, -96,			//Last Received:   4000
				2, 110, 91, -128,	//Packet History:  40786816
				0,					//Packet Flags:    NOT IMMEDIATE
				-124				//Message Type:    CONNECTION_ACCEPTED
									//Message:         null
			}, connectionAcceptedPacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				0,					//Connection Id:   ANONYMOUS
				-1, -2,				//Sequence Number: 65534
				0, 0,				//Duplicate Of:    N/A
				0, 0,				//Last Received:   N/A
				6, -41, 36, 42,		//Packet History:  114762794
				0,					//Packet Flags:    NOT IMMEDIATE
				-123				//Message Type:    CONNECTION_REFUSED
									//Message:         null
			}, connectionRefusedPacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				0,					//Connection Id:   ANONYMOUS
				-1, -1,				//Sequence Number: 65535
				0, 52,				//Duplicate Of:    52
				-1, -2,				//Last Received:   65534
				10, -67, -13, -78,	//Packet History:  180220850
				0,					//Packet Flags:    NOT IMMEDIATE
				-125				//Message Type:    CONNECT_REQUEST
									//Message:         null
			}, connectRequestPacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				-56,				//Connection Id:   200
				0, 0,				//Sequence Number: N/A
				0, 0,				//Duplicate Of:    N/A
				-1, -1,				//Last Received:   65535
				3, -85, 85, 119,	//Packet History:  61560183
				0,					//Packet Flags:    NOT IMMEDIATE
				-122				//Message Type:    FORCE_DISCONNECT
									//Message:         null
			}, forceDisconnectPacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				-2,					//Connection Id:   254
				0, 1,				//Sequence Number: 1
				0, 0,				//Duplicate Of:    N/A
				0, 1,				//Last Received:   1
				10, 54, 121, 31,	//Packet History:  171342111
				0,					//Packet Flags:    NOT IMMEDIATE
				-127				//Message Type:    PING
									//Message:         null
			}, pingPacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				-1,					//Connection Id:   255
				0, 4,				//Sequence Number: 4
				11, -72,			//Duplicate Of:    3000
				0, 0,				//Last Received:   N/A
				0, 125, 74, 30,		//Packet History:  8210974
				-128,				//Packet Flags:    IMMEDIATE
				-126				//Message Type:    PING_RESPONSE
									//Message:         null
			}, pingResponsePacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		pingResponsePacket.setConnectionId(4);
		pingResponsePacket.setSequenceNumber(18);
		pingResponsePacket.setDuplicateSequenceNumber(42);
		pingResponsePacket.setLastReceivedSequenceNumber(6);
		pingResponsePacket.setReceivedPacketHistory(-2);
		pingResponsePacket.setIsImmediateResponse(false);
		pingResponsePacket.setMessageType(MessageType.CLIENT_DISCONNECT);
		pingResponsePacket.setMessage("bar");
		try {
			Assert.assertArrayEquals(new byte[] {
				6, 45, -9, 59,		//Protocol Id:     103675707
				4,					//Connection Id:   4
				0, 18,				//Sequence Number: 4
				0, 42,				//Duplicate Of:    42
				0, 6,				//Last Received:   6
				-1, -1, -1, -2,		//Packet History:  -2
				0,					//Packet Flags:    NOT IMMEDIATE
				-121,				//Message Type:    CLIENT_DISCONNECT
				98, 97, 114			//Message:         "bar"
			}, pingResponsePacket.toByteArray());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			forceDisconnectPacket.setConnectionId(Packet.MINIMUM_CONNECTION_ID - 2);
			forceDisconnectPacket.toByteArray();
			fail("Expected PacketEncodingException");
		}  catch (PacketEncodingException e) {
			//expected
		}

		try {
			forceDisconnectPacket.setConnectionId(Packet.MAXIMUM_CONNECTION_ID + 1);
			forceDisconnectPacket.toByteArray();
			fail("Expected PacketEncodingException");
		}  catch (PacketEncodingException e) {
			//expected
		}

		try {
			applicationPacketHelloWorld.setSequenceNumber(Packet.MINIMUM_SEQUENCE_NUMBER - 2);
			applicationPacketHelloWorld.toByteArray();
			fail("Expected PacketEncodingException");
		}  catch (PacketEncodingException e) {
			//expected
		}

		try {
			applicationPacketHelloWorld.setSequenceNumber(Packet.MAXIMUM_SEQUENCE_NUMBER + 1);
			applicationPacketHelloWorld.toByteArray();
			fail("Expected PacketEncodingException");
		}  catch (PacketEncodingException e) {
			//expected
		}

		try {
			connectionRefusedPacket.setDuplicateSequenceNumber(Packet.MINIMUM_SEQUENCE_NUMBER - 2);
			connectionRefusedPacket.toByteArray();
			fail("Expected PacketEncodingException");
		}  catch (PacketEncodingException e) {
			//expected
		}

		try {
			connectionRefusedPacket.setDuplicateSequenceNumber(Packet.MAXIMUM_SEQUENCE_NUMBER + 1);
			connectionRefusedPacket.toByteArray();
			fail("Expected PacketEncodingException");
		}  catch (PacketEncodingException e) {
			//expected
		}

		try {
			connectionAcceptedPacket.setLastReceivedSequenceNumber(Packet.MINIMUM_SEQUENCE_NUMBER - 2);
			connectionAcceptedPacket.toByteArray();
			fail("Expected PacketEncodingException");
		}  catch (PacketEncodingException e) {
			//expected
		}

		try {
			connectionAcceptedPacket.setLastReceivedSequenceNumber(Packet.MAXIMUM_SEQUENCE_NUMBER + 1);
			connectionAcceptedPacket.toByteArray();
			fail("Expected PacketEncodingException");
		}  catch (PacketEncodingException e) {
			//expected
		}
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
		try {
			assertPacketsEqual(allZeroesPacket, Packet.parsePacket(allZeroesPacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(protocolValidPacket, Packet.parsePacket(protocolValidPacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(protocolInvalidPacket, Packet.parsePacket(protocolInvalidPacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(applicationPacketHelloWorld, Packet.parsePacket(applicationPacketHelloWorld.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(clientDisconnectPacket, Packet.parsePacket(clientDisconnectPacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(connectionAcceptedPacket, Packet.parsePacket(connectionAcceptedPacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(connectionRefusedPacket, Packet.parsePacket(connectionRefusedPacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(connectRequestPacket, Packet.parsePacket(connectRequestPacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(forceDisconnectPacket, Packet.parsePacket(forceDisconnectPacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(pingPacket, Packet.parsePacket(pingPacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			assertPacketsEqual(pingResponsePacket, Packet.parsePacket(pingResponsePacket.toByteArray()));
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testParsePacketByteArrayInt() {
		try {
			Packet packet = Packet.parsePacket(applicationPacketHelloWorld.toByteArray(), 17);
			assertNotNull(packet);
			assertNull(packet.getMessage());
			packet = Packet.parsePacket(applicationPacketHelloWorld.toByteArray(), 18);
			assertNotNull(packet);
			assertEquals(packet.getMessage(), "H");
			packet = Packet.parsePacket(applicationPacketHelloWorld.toByteArray(), 20);
			assertNotNull(packet);
			assertEquals(packet.getMessage(), "Hel");
			packet = Packet.parsePacket(applicationPacketHelloWorld.toByteArray(), 100);
			assertNotNull(packet);
			assertEquals(packet.getMessage(), "Hello world!");
		} catch (MalformedPacketException e) {
			fail(e.getMessage());
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}

		try {
			Packet packet = Packet.parsePacket(applicationPacketHelloWorld.toByteArray(), 3);
			assertNotNull(packet);
			assertNull(packet.getMessage());
			fail("Expected MalformedPacketException");
		}
		catch (MalformedPacketException e) {
			//expected
		} catch (PacketEncodingException e) {
			fail(e.getMessage());
		}
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
		assertEquals(100, Packet.createApplicationPacket(100, "Hello world!").getConnectionId());
		assertEquals(MessageType.APPLICATION, Packet.createApplicationPacket(100, "Hello world!").getMessageType());
		assertEquals("Hello world!", Packet.createApplicationPacket(100, "Hello world!").getMessage());
	}

	@Test
	public void testCreatePingPacket() {
		assertNotNull(pingPacket);
		assertNotNull(Packet.createPingPacket(Packet.ANONYMOUS_CONNECTION_ID, 100));
		assertNotNull(Packet.createPingPacket(Packet.MINIMUM_CONNECTION_ID, 0));
		assertNotNull(Packet.createPingPacket(Packet.MAXIMUM_CONNECTION_ID, 3));
		assertNotNull(Packet.createPingPacket(0, 6));
		assertNotNull(Packet.createPingPacket(1, 3));
		assertNotNull(Packet.createPingPacket(100, 38565764));
		assertNotNull(Packet.createPingPacket(254, -1));
		assertNotNull(Packet.createPingPacket(255, 7));
		assertEquals(100, Packet.createPingPacket(100, 4).getConnectionId());
		assertEquals(MessageType.PING, Packet.createPingPacket(100, 8).getMessageType());
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
		assertEquals(100, Packet.createPingResponsePacket(100).getConnectionId());
		assertEquals(MessageType.PING_RESPONSE, Packet.createPingResponsePacket(100).getMessageType());
	}

	@Test
	public void testCreateConnectRequestPacket() {
		assertNotNull(connectRequestPacket);
		assertNotNull(Packet.createConnectRequestPacket());
		assertEquals(Packet.ANONYMOUS_CONNECTION_ID, Packet.createConnectRequestPacket().getConnectionId());
		assertEquals(MessageType.CONNECT_REQUEST, Packet.createConnectRequestPacket().getMessageType());
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
		assertEquals(100, Packet.createConnectionAcceptedPacket(100).getConnectionId());
		assertEquals(MessageType.CONNECTION_ACCEPTED, Packet.createConnectionAcceptedPacket(100).getMessageType());
	}

	@Test
	public void testCreateConnectionRefusedPacket() {
		assertNotNull(connectionRefusedPacket);
		assertNotNull(Packet.createConnectionRefusedPacket());
		assertEquals(Packet.ANONYMOUS_CONNECTION_ID, Packet.createConnectionRefusedPacket().getConnectionId());
		assertEquals(MessageType.CONNECTION_REFUSED, Packet.createConnectionRefusedPacket().getMessageType());
	}

	@Test
	public void testCreateForceDisconnectPacket() {
		assertNotNull(forceDisconnectPacket);
		assertNotNull(Packet.createForceDisconnectPacket(Packet.ANONYMOUS_CONNECTION_ID, "REASON"));
		assertNotNull(Packet.createForceDisconnectPacket(Packet.MINIMUM_CONNECTION_ID, "REASON"));
		assertNotNull(Packet.createForceDisconnectPacket(Packet.MAXIMUM_CONNECTION_ID, "REASON"));
		assertNotNull(Packet.createForceDisconnectPacket(0, "REASON"));
		assertNotNull(Packet.createForceDisconnectPacket(1, "REASON"));
		assertNotNull(Packet.createForceDisconnectPacket(100, "REASON"));
		assertNotNull(Packet.createForceDisconnectPacket(101, null));
		assertNotNull(Packet.createForceDisconnectPacket(102, "`1234567890-~!@#$%^&*()_+[]\\{}|;\":'<>?,./"));
		assertNotNull(Packet.createForceDisconnectPacket(103, ""));
		assertNotNull(Packet.createForceDisconnectPacket(254, "REASON"));
		assertNotNull(Packet.createForceDisconnectPacket(255, "REASON"));
		assertEquals(100, Packet.createForceDisconnectPacket(100, "REASON").getConnectionId());
		assertEquals(MessageType.FORCE_DISCONNECT, Packet.createForceDisconnectPacket(100, "REASON").getMessageType());
		assertEquals("REASON", Packet.createForceDisconnectPacket(100, "REASON").getMessage());
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
		assertEquals(100, Packet.createClientDisconnectPacket(100).getConnectionId());
		assertEquals(MessageType.CLIENT_DISCONNECT, Packet.createClientDisconnectPacket(100).getMessageType());
	}

	private void assertPacketsEqual(Packet packet1, Packet packet2) {
		if(packet1 == null)
			assertNull(packet2);
		else {
			assertNotNull(packet2);
			assertEquals(packet1.isValidProtocol(), packet2.isValidProtocol());
			assertEquals(packet1.getConnectionId(), packet2.getConnectionId());
			assertEquals(packet1.getSequenceNumber(), packet2.getSequenceNumber());
			assertEquals(packet1.getDuplicateSequenceNumber(), packet2.getDuplicateSequenceNumber());
			assertEquals(packet1.getLastReceivedSequenceNumber(), packet2.getLastReceivedSequenceNumber());
			assertEquals(packet1.getReceivedPacketHistory(), packet2.getReceivedPacketHistory());
			assertEquals(packet1.isImmediateResponse(), packet2.isImmediateResponse());
			assertEquals(packet1.getMessageType(), packet2.getMessageType());
			if(packet1.getMessage() == null)
				assertNull(packet2.getMessage());
			else {
				assertNotNull(packet2.getMessage());
				assertEquals(packet1.getMessage(), packet2.getMessage());
			}
			
		}
	}
}