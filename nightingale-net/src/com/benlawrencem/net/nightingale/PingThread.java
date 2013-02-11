package com.benlawrencem.net.nightingale;

public class PingThread extends Thread {
	private Pinger pinger;
	private int timeout;
	private boolean isPinging;

	public PingThread(Pinger pinger, int millisecondsBetweenPings) {
		this.pinger = pinger;
		timeout = millisecondsBetweenPings;
		isPinging = false;
	}

	public void run() {
		isPinging = true;
		while(isPinging) {
			pinger.ping();
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {}
		}
	}

	public void stopPinging() {
		isPinging = false;
	}
}