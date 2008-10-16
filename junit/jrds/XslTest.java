package jrds;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import jrds.webapp.ProbeDetails;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;



public class XslTest extends JrdsTester {
	static final private Logger logger = Logger.getLogger(XslTest.class);

	@Test public void probeDetails() {
		OutputStream bufferOs = new ByteArrayOutputStream();
		ProbeDetails pd = new ProbeDetails();
		pd.dump(GetMoke.getProbe(), bufferOs);
		logger.debug(bufferOs);
		String buffer = bufferOs.toString();
		Assert.assertTrue(buffer.contains("<?xml version=" + '"' + "1.0" + '"' +" encoding=" + '"' + "UTF-8" + '"' + "?>"));
		Assert.assertTrue(buffer.contains("<!DOCTYPE html PUBLIC " + '"' + "//W3C//DTD XHTML 1.0 Strict//EN" + '"' + " " + '"' + "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd" + '"' + ">"));
	}

	@BeforeClass static public void configure() {
		JrdsTester.configure();
		Logger.getLogger(XslTest.class).setLevel(Level.DEBUG);
	}

}
