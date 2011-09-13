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

import org.apache.log4j.Level;
import org.rrd4j.core.Sample;

/**
 * This abstract class can be used to parse the results of an external command
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public abstract class ExternalCmdProbe extends Probe<String, Number> {

	private String[] cmd;
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#getNewSampleValues()
	 */
	public Map<String, Number> getNewSampleValues() {
		return null;
	}
	
	protected void updateSample(Sample oneSample) {
		Process urlperfps = null;
		InputStream stdout = null;
		try {
			urlperfps = Runtime.getRuntime().exec(getCmd());
			stdout = urlperfps.getInputStream();
			BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
			String perfstring = stdoutReader.readLine();
			if(perfstring != null)
				oneSample.set(perfstring);
		} catch (IOException e) {
			log(Level.ERROR, e, "external command failed : %s", e);
		} finally {
		    try {
                stdout.close();
            } catch (IOException e) {
            }
		}
		
		try {
			if(urlperfps != null) {
				urlperfps.waitFor();
				urlperfps.getInputStream().close();
				urlperfps.getErrorStream().close();
				urlperfps.getOutputStream().close();
			}
		} catch (IOException e) {
			log(Level.ERROR, e, "Exception on close: %s", e);
		} catch (InterruptedException e) {
			log(Level.ERROR, e, "Exception on close: %s", e);
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
