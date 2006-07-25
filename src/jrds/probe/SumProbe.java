/*##########################################################################
_##
_##  $Id: BackEndCommiter.java 235 2006-03-01 21:29:48 +0100 (mer., 01 mars 2006) fbacchella $
_##
_##########################################################################*/

package jrds.probe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.Sum;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

public class SumProbe extends VirtualProbe {
	static private final Logger logger = Logger.getLogger(SumProbe.class);
	static final ProbeDesc pd = new ProbeDesc(0);
	static {
		pd.setGraphClasses(new Object[] {Sum.class});
	}
	public static final RdsHost sumhost =  new RdsHost("SumHost") {
		@Override
		protected void finalize() throws Throwable {
			logger.debug("Sum host destroyed");
			super.finalize();
		}
		
	};

	private String graphName;
	Collection graphList;

	public SumProbe(RdsHost thehost, String name, Collection graphList) {
		super(thehost, pd);
		this.setName(name);
		this.graphList = graphList;
	}

	/**
	 * @return Returns the probeList.
	 */
	public Collection getProbeList() {
		return graphList;
	}

	/**
	 * @return Returns the probeName.
	 */
	public String getGraphName() {
		return graphName;
	}

	public Date getLastUpdate() {
		return new Date();
	}
	public static void addDigester(Digester digester) {
//		digester.register("-//jrds//DTD View//EN", digester.getClass().getResource("/view.dtd").toString());
		digester.addRule("sum/", new Rule() {
			public void begin (String namespace, String name, Attributes attributes) {
				String sumName = attributes.getValue("name");
				digester.push(sumName);
				digester.push(new ArrayList());
			}
			public void end(String namespace, String name) throws Exception {
				List l = (List) digester.pop();
				String sumName = (String) digester.pop();
				Probe newRdsRrd = new SumProbe(sumhost, sumName, l);
				sumhost.addProbe(newRdsRrd);
				logger.debug("adding sum " + newRdsRrd );
			}
		});
		digester.addRule("sum/element/", new Rule() {
			@SuppressWarnings("unchecked")
			public void begin (String namespace, String name, Attributes attributes) {
				String elementName = attributes.getValue("name");
				List<String> l = (List<String>) digester.peek();
				l.add(elementName);
			}
		});
	}
}
