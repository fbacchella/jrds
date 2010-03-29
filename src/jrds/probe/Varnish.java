package jrds.probe;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jrds.Probe;
import jrds.starter.SocketFactory;

import org.apache.log4j.Level;

public class Varnish extends Probe<String, Number> {
	
	static final private Pattern statlinepattern = Pattern.compile("^\\s+(\\d+)\\s+(.*)$");

	private int port = 6081;
	
	public void configure(Integer port) {
		this.port = port;
	}

	public void configure() {
	}

	@Override
	public Map<String, Number> getNewSampleValues() {
		Socket s = null;
		try {
			SocketFactory ss = (SocketFactory) getStarters().find(SocketFactory.makeKey(this)); 
			 s = ss.createSocket(getHost(), port);
		} catch (Exception e) {
			log(Level.ERROR, e, "Connect error %s", e);
			return java.util.Collections.emptyMap();
		}

		try {
			PrintWriter outputSocket =  new PrintWriter(s.getOutputStream());
			BufferedReader inputSocket = new BufferedReader(new InputStreamReader(s.getInputStream()));

			outputSocket.println(getPd().getSpecific("command"));
			outputSocket.flush();
			String statusline  = inputSocket.readLine().trim();
			String[] statusinfo = statusline.split(" ");
			int statuscode = jrds.Util.parseStringNumber(statusinfo[0], Integer.class, -1).intValue();
			int size =  jrds.Util.parseStringNumber(statusinfo[1], Integer.class, -1).intValue();
			if(statuscode != 200 || size < 1) {
				log(Level.ERROR, "communication error, code: %d, byte expected: %d", statuscode, size);
				return Collections.emptyMap();
			}
			
			char[] cbuf= new char[size];
			if( inputSocket.read(cbuf) != size) {
				log(Level.ERROR, "read failed, not enough byte");
				return Collections.emptyMap();
			};
			
			Map<String, Number> vars = new HashMap<String, Number>();
			BufferedReader statsbuffer = new BufferedReader(new CharArrayReader(cbuf));
			while(statsbuffer.ready()) {
				String statsline = statsbuffer.readLine();
				Matcher m = statlinepattern.matcher(statsline);
				if(m.matches()) {
					Integer value = jrds.Util.parseStringNumber(m.group(1), Integer.class, -1).intValue();
					String key = m.group(2);
					vars.put(key, value);
				}
				else {
					log(Level.DEBUG,"Invalid line: %s", statsline);
				}
			}

			s.close();
			return vars;
		} catch (IOException e) {
			log(Level.ERROR, e, "Socket error %s", e);
		}

		return Collections.emptyMap();
	}

	@Override
	public String getSourceType() {
		return "Varnish";
	}

}
