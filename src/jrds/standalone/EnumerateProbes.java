package jrds.standalone;

import java.io.File;

import jrds.HostsList;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.StoreOpener;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;

public class EnumerateProbes {
	static final private Logger logger = Logger.getLogger(EnumerateProbes.class);

	public static void main(String[] args) throws Exception {
		jrds.JrdsLoggerConfiguration.initLog4J();

		PropertiesManager pm = new PropertiesManager(new File("jrds.properties"));
		jrds.JrdsLoggerConfiguration.configure(pm);

		System.getProperties().setProperty("java.awt.headless","true");
		System.getProperties().putAll(pm);
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod, pm.timeout, pm.rrdbackend);

		HostsList hl = HostsList.getRootGroup();
		hl.configure(pm);

		OutputFormat of = new OutputFormat("XML","UTF-8",true);
		of.setIndent(1);
		of.setIndenting(true);

		for(RdsHost host: hl.getHosts()) {
			for(final Probe p: host.getProbes()) {
				Document probeDoc = p.dumpAsXml();
				XMLSerializer serializer = new XMLSerializer(System.out,of);
				serializer.asDOMSerializer();
				serializer.serialize(probeDoc.getDocumentElement() );
			}
		}
		StoreOpener.stop();
	}

}
