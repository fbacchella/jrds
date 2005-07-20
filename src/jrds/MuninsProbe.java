/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class MuninsProbe extends Probe {
	static final private Logger logger = JrdsLogger.getLogger(MuninsProbe.class);
	private Collection muninsName = null;
	protected Map nameMap;

	/**
	 * @param monitoredHost
	 * @param pd
	 */
	public MuninsProbe(RdsHost monitoredHost, ProbeDesc pd) {
		super(monitoredHost, pd);
	}
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.MuninsProbe#initMuninsName()
	 */
	protected Collection initMuninsName() {
		return getPd().getNamedProbesNames();
	}


	public Collection getMuninsName()
	{
		if(muninsName == null)
			muninsName = initMuninsName();
		return muninsName;
	}
	
	
	public Map getNewSampleValues()
	{
		Map retValue = new HashMap();
		Socket muninsSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		try {
			muninsSocket = new Socket(getHost().getName(), 4949);
			out = new PrintWriter(muninsSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(muninsSocket.getInputStream()));
			for(Iterator i = getMuninsName().iterator() ; i.hasNext() ; ) {
				String currentProbe = (String) i.next();
				try {
					out.println("fetch " + currentProbe);
					String lastLine;
					boolean dotFound = false;
					while(! dotFound && ( lastLine = in.readLine()) != null ) {
						lastLine.replaceFirst("#.*", "");
						if(".".equals(lastLine)) {
							dotFound = true;
						}
						else {
							String[] kvp = lastLine.split(" ");
							if(kvp.length == 2) {
								String name = kvp[0].trim();
								Double value = new Double(kvp[1].trim());
								if(name != null && value != null)
									retValue.put(name, value);
							};
						};
					}
				}
				catch (IOException e) {
					logger.error("Error with munins probe " + this + ": " + e);
				}
			}
		} catch (IOException e) {
			logger.error("Unable to connect to munins because: " + e);
		}
		
		if(out != null) {
			out.close();
		}
		try {
			if(in != null) 
				in.close();
		} catch (IOException e1) {}
		
		try {
			if(muninsSocket != null)
				muninsSocket.close();
		} catch (IOException e2) {};
		return retValue;
	}
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.MuninsProbe#initNameMap()
	 */
	protected Map initNameMap() {
		return getPd().getProbesNamesMap();
	}


}
