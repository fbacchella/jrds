package jrds.factories.xml;

import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EntityResolver implements org.xml.sax.EntityResolver {
	public InputSource resolveEntity(String publicId, String systemId)
	throws SAXException, IOException {
		String realSystemId = systemId;
		if("-//jrds//DTD Graph Description//EN".equals(publicId)) {
			realSystemId = getClass().getResource("/graphdesc.dtd").toString();
		}
		else if("-//jrds//DTD Probe Description//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/probedesc.dtd").toString();
		}
		else if("-//jrds//DTD Filter//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/filter.dtd").toString();
		}

		return new InputSource(realSystemId);
	}


}
