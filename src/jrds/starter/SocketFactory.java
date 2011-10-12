package jrds.starter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import jrds.PropertiesManager;
import jrds.objects.RdsHost;

public class SocketFactory extends Starter {
	int timeout;
	
    /* (non-Javadoc)
     * @see jrds.starter.Starter#configure(jrds.PropertiesManager)
     */
    @Override
    public void configure(PropertiesManager pm) {
        super.configure(pm);
        timeout = pm.timeout;
    }

    public ServerSocket createServerSocket(int port) throws IOException {
		if(! isStarted())
			return null;
		
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
		if(! isStarted())
			return null;
		
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

	public Socket createSocket(RdsHost host, int port) throws IOException {
		if(! isStarted())
			return null;

		Resolver r = host.find(Resolver.class);
		if(r == null || ! r.isStarted())
			return null;

		Socket s = new Socket(r.getInetAddress(), port) {
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

	@Deprecated
	public static Object makeKey(StarterNode level) {
		return SocketFactory.class;
	}

}
