/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import jrds.Probe;

import org.apache.log4j.Logger;
import org.rrd4j.core.Sample;


/**
 * This abstract class can be used to parse the results of an external cmd
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public abstract class ExternalCmdProbe extends Probe {
	static final private Logger logger = Logger.getLogger(ExternalCmdProbe.class);

	private String[] cmd;
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#getNewSampleValues()
	 */
	public Map<?, ?> getNewSampleValues() {
		return null;
	}
	
	protected void updateSample(Sample oneSample) {
		Process urlperfps = null;
		try {
			urlperfps = Runtime.getRuntime().exec(getCmd());
			InputStream stdout = urlperfps.getInputStream();
			BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
			String perfstring = stdoutReader.readLine();
			if(perfstring != null)
				oneSample.set(perfstring);
		} catch (IOException e) {
			logger.warn("external command failed : " + e);
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
	@Override
	public String getSourceType() {
		return "external command";
	}
}
