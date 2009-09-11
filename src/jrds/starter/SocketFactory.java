package jrds.starter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class SocketFactory extends Starter {
	int timeout;
	
	public ServerSocket createServerSocket(int port) throws IOException {
		ServerSocket s = new ServerSocket(port) {

			/* (non-Javadoc)
			 * @see java.net.ServerSocket#accept()
			 */
			@Override
			public Socket accept() throws IOException {
				Socket accepted = super.accept();
				accepted.setTcpNoDelay(true);
				return accepted;
			}
			
		};
		s.setSoTimeout(timeout * 1000);
		return s;
	}

	public Socket createSocket(String host, int port) throws IOException {
		Socket s = new Socket(host, port) {
			public void connect(SocketAddress endpoint) throws IOException {
				super.connect(endpoint, timeout * 1000);
			}

			/* (non-Javadoc)
			 * @see java.net.Socket#connect(java.net.SocketAddress, int)
			 */
			public void connect(SocketAddress endpoint, int timeout) throws IOException {
				super.connect(endpoint, timeout);
			}
		};
		s.setSoTimeout(timeout * 1000);
		s.setTcpNoDelay(true);
		return s;
	}

	/**
	 * @return the timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout the timeout to set
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public static Object makeKey(StarterNode node) {
		return SocketFactory.class;
	}

	/* (non-Javadoc)
	 * @see jrds.starter.Starter#getKey()
	 */
	@Override
	public Object getKey() {
		return SocketFactory.class;
	}

}
