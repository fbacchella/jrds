package jrds.probe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;

import jrds.HostsList;
import jrds.Probe;
import jrds.RdsHost;
import jrds.Util;
import jrds.XmlProvider;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Ribcl extends Probe {
	private String user;
	private String passwd;
	private String iloHost;
	private int port = 443;

	static final private String encoding = "ISO-8859-1";
	static final private String eol = "\r\n";
	static final private String xmlHeader = "<?xml version=\"1.0\" ?>" + eol;

	static final private Logger logger = Logger.getLogger(Ribcl.class);

	XmlProvider xmlstarter = null;

	public void configure(String iloHost, int port, String user, String passwd) {
		this.iloHost = iloHost;
		this.user = user;
		this.passwd = passwd;
	}

	public void configure(String iloHost, String user, String passwd) {
		this.iloHost = iloHost;
		this.user = user;
		this.passwd = passwd;
	}

	/* (non-Javadoc)
	 * @see jrds.probe.HttpProbe#setHost(jrds.RdsHost)
	 */
	@Override
	public void setHost(RdsHost monitoredHost) {
		super.setHost(monitoredHost);
		xmlstarter = new XmlProvider(monitoredHost);
		xmlstarter = (XmlProvider) xmlstarter.register(monitoredHost);
	}

	@Override
	public Map<?, ?> getNewSampleValues() {
		Map<String, Number> vars = new HashMap<String, Number>();
		Socket s = null;
		try {
			s = connect();
		} catch (Exception e) {
			logger.error("SSL connect error for " + this + " " + e);
			return java.util.Collections.emptyMap();
		}

		try {
			OutputStream outputSocket = s.getOutputStream();
			InputStream inputSocket = s.getInputStream();

			outputSocket.write(xmlHeader.getBytes(encoding));
			buildQuery(outputSocket);

			byte[] buffer = new byte[4096];
			int n;
			StringBuffer message = null;
			while((n = inputSocket.read(buffer)) > 0) {
				String messageBuffer = new String(buffer, 0, n, encoding);
				if(messageBuffer.startsWith("<?xml version=\"1.0\"?>")) {
					if(message != null)
						parse(message.toString(), vars);
					message = new StringBuffer(messageBuffer);
				}
				else {
					message.append(messageBuffer);
				}
			}
			s.close();
		} catch (IOException e) {
			logger.error("SSL socket error for " + this + " :" + e);
		}

		return vars;
	}

	@Override
	public String getSourceType() {
		return "RIBCL";
	}

	private void buildQuery(OutputStream out) {
		Document ribclQ = xmlstarter.getDocument();
		Element LOCFG = ribclQ.createElement("LOCFG");
		LOCFG.setAttribute("version", "2.21");
		ribclQ.appendChild(LOCFG);
		Element RIBCL = ribclQ.createElement("RIBCL");
		LOCFG.appendChild(RIBCL);
		RIBCL.setAttribute("VERSION", "2.0");
		Element LOGIN = ribclQ.createElement("LOGIN");
		LOGIN.setAttribute("USER_LOGIN", user);
		LOGIN.setAttribute("PASSWORD", passwd);
		RIBCL.appendChild(LOGIN);
		Element command = ribclQ.createElement(getPd().getSpecific("command"));
		command.setAttribute("MODE", "read");
		LOGIN.appendChild(command);
		Element subcommand = ribclQ.createElement(getPd().getSpecific("subcommand"));
		command.appendChild(subcommand);

		Map<String, String> properties = new HashMap<String, String>();
		properties.put(OutputKeys.INDENT, "no");
		properties.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
		try {
			Util.serialize(ribclQ, out, null, properties);
		} catch (TransformerException e) {
		} catch (IOException e) {
			logger.fatal("Unable to serialize in memory");
			throw new Error(e);
		}
	}

	private Socket connect() throws NoSuchAlgorithmException, KeyManagementException, UnknownHostException, IOException {
		final int timeout = HostsList.getRootGroup().getTimeout();
		Socket s = new Socket(iloHost, port) {
			public void connect(SocketAddress endpoint) throws IOException {
				super.connect(endpoint, timeout * 1000);
			}
		};
		s.setSoTimeout(timeout * 1000);
		if(port == 23) {
			return 	s;
		}		
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
					public void checkClientTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}
					public void checkServerTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}
				}
		};

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());

		SSLSocketFactory ssf = sc.getSocketFactory();
		s = ssf.createSocket(s, iloHost, port, true);
		logger.debug("done SSL handshake for " + iloHost);

		return s;
	}

	public void parse(String message, Map<String, Number> vars) {
		if(message == null ||  "".equals(message))
			return;
		logger.trace("new message to parse: ");
		logger.trace(message);
		//The XML returned from an iLO is buggy, up to ilO2 1.50 
		message = message.replaceAll("<RIBCL VERSION=\"[0-9\\.]+\"/>", "<RIBCL >");
		Document d = this.xmlstarter.getDocument(new StringReader(message));
		xmlstarter.fileFromXpaths(d, getPd().getCollectStrings().keySet(), vars);
		return;
	}

}
