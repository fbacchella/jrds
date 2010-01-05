package jrds.probe.munins;

import java.io.IOException;
import java.net.Socket;

import jrds.starter.Connection;
import jrds.starter.SocketFactory;

public class MuninConnection extends Connection<Socket> {
	Socket muninsSocket = null;
	int port = 4949;

	public MuninConnection(Integer port) {
		super();
		this.port = port;
	}

	@Override
	public Socket getConnection() {
		return muninsSocket;
	}

	@Override
	public long setUptime() {
		return Long.MAX_VALUE;
	}

	@Override
	public boolean startConnection() {
		SocketFactory ss = (SocketFactory) getLevel().find(SocketFactory.makeKey(getParent())); 
		try {
			muninsSocket = ss.createSocket(getHostName(), port);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public void stopConnection() {
		try {
			muninsSocket.close();
		} catch (IOException e) {
		}		
	}

}
