package jrds.factories.xml;

import java.io.IOException;
import java.net.URL;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EntityResolver implements org.xml.sax.EntityResolver {
	public InputSource resolveEntity(String publicId, String systemId)
	throws SAXException, IOException {
		URL realSystemId = new URL(systemId);
		if("-//jrds//DTD Graph Description//EN".equals(publicId)) {
			realSystemId = getClass().getResource("/graphdesc.dtd");
		}
		else if("-//jrds//DTD Probe Description//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/probedesc.dtd");
		}
		else if("-//jrds//DTD Filter//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/filter.dtd");
		}

		return new InputSource(realSystemId.openStream());
	}
}
