package jrds.factories.xml;

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EntityResolver implements org.xml.sax.EntityResolver {
	static final Logger logger = Logger.getLogger(EntityResolver.class);
	public InputSource resolveEntity(String publicId, String systemId)
	throws SAXException, IOException {
		URL realSystemId;
		if("-//jrds//DTD Graph Description//EN".equals(publicId)) {
			realSystemId = getClass().getResource("/graphdesc.dtd");
		}
		else if("-//jrds//DTD Probe Description//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/probedesc.dtd");
		}
		else if("-//jrds//DTD Filter//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/filter.dtd");
		}
		else if("-//W3C//DTD XHTML 1.0 Strict//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/ressources/xhtml1-strict.dtd");
		}
		else if("-//W3C//ENTITIES Latin 1 for XHTML//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/ressources/xhtml-lat1.ent");
		}
		else if("-//W3C//ENTITIES Symbols for XHTML//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/ressources/xhtml-symbol.ent");
		}
		else if("-//W3C//ENTITIES Special for XHTML//EN".equals(publicId)) {
			realSystemId =  getClass().getResource("/ressources/xhtml-special.ent");
		}
		else {
			realSystemId = new URL(systemId);
		}

		logger.trace("Resolving \"" + publicId + "\" \"" + systemId + "\" to "  + realSystemId);
		if(realSystemId == null) {
			logger.equals("Failed to resolve " + publicId + " " + systemId);
		}
		return new InputSource(realSystemId.openStream());
	}
}
