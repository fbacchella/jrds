/*
 * Created on 24 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import jrds.JrdsLogger;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdException;
import org.jrobin.core.Sample;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class ExternalCmdProbe extends Probe {
	static final private Logger logger = JrdsLogger.getLogger(ExternalCmdProbe.class);

	static Runtime rt = Runtime.getRuntime();

	/**
	 * @param thehost
	 */
	public ExternalCmdProbe(RdsHost thehost, ProbeDesc pd) {
		super(thehost, pd);
	}
	
	private String[] cmd;
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#getNewSampleValues()
	 */
	public Map getNewSampleValues() {
		return null;
	}
	
	protected void updateSample(Sample oneSample) {
		Process urlperfps = null;
		try {
			urlperfps = rt.exec(getCmd());
			InputStream stdout = urlperfps.getInputStream();
			BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
			String perfstring = stdoutReader.readLine();
			if(perfstring != null)
				oneSample.set(perfstring);
		} catch (IOException e) {
			logger.warn("external command failed : " + e.getLocalizedMessage());
		} catch (RrdException e) {
			logger.warn("unable to store result for probe " + this.getName() + ": " + e.getLocalizedMessage());
		}
		try {
			if(urlperfps != null) {
				urlperfps.waitFor();
				urlperfps.getInputStream().close();
				urlperfps.getErrorStream().close();
				urlperfps.getOutputStream().close();
			}
		} catch (IOException e1) {
			logger.warn("Exception on close", e1);
		} catch (InterruptedException e) {
			logger.warn("Exception on close", e);
		}
	}
	/**
	 * @return Returns the cmd.
	 */
	public String[] getCmd() {
		return cmd;
	}
	/**
	 * @param cmd The cmd to set.
	 */
	public void setCmd(String[] cmd) {
		this.cmd = cmd;
	}
}
