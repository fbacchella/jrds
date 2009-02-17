/*##########################################################################
_##
_##  $Id: BackEndCommiter.java 235 2006-03-01 21:29:48 +0100 (mer., 01 mars 2006) fbacchella $
_##
_##########################################################################*/

package jrds.probe;

import java.util.ArrayList;
import java.util.Date;

import jrds.ProbeDesc;

import org.apache.log4j.Logger;

public class ContainerProbe extends VirtualProbe {
	static final private Logger logger = Logger.getLogger(ContainerProbe.class);

	static final ProbeDesc pd = new ProbeDesc(0);

	//An array list is needed, the introspection is picky
	public ContainerProbe(String name, ArrayList<String> graphList) {
		logger.debug("new container: " + name);
		setName(name);
		setPd(pd);
	}

	@Override
	public Date getLastUpdate() {
		return new Date();
	}
	
	@Override
	public String getSourceType() {
		return "container";
	}
}
