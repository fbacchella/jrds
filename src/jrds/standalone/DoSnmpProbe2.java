package jrds.standalone;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import org.snmp4j.smi.OID;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fr.jrds.snmpcodec.OIDFormatter;
import fr.jrds.snmpcodec.parsing.MibLoader;
import jrds.ProbeDesc;
import jrds.factories.xml.EntityResolver;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

public class DoSnmpProbe2 extends CommandStarterImpl {

    static final Pattern oidPattern = Pattern.compile("^(.\\d+)+$");
    static final Pattern namePattern = Pattern.compile("^(.+)\\s+OBJECT-TYPE$");
    static final Pattern syntaxPattern = Pattern.compile(".*SYNTAX\\s+([a-zA-Z0-9]+).*");

    private final DocumentBuilderFactory instance;
    private final DocumentBuilder dbuilder;


    public DoSnmpProbe2() {
        super();
        try {
            instance = DocumentBuilderFactory.newInstance();
            // Focus on content, not structure
            instance.setIgnoringComments(true);
            instance.setValidating(true);
            instance.setIgnoringElementContentWhitespace(true);
            instance.setCoalescing(true);
            instance.setExpandEntityReferences(true);

            dbuilder = instance.newDocumentBuilder();
            dbuilder.setEntityResolver(new EntityResolver());
            dbuilder.setErrorHandler(new ErrorHandler() {
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void start(String[] args) throws Exception {
        ProbeDesc<OID> pd = new ProbeDesc<>();
        pd.setProbeClass(jrds.probe.snmp.RdsSnmpSimple.class);
        List<String> xmlPaths = new ArrayList<>();
        MibLoader loader = new MibLoader();

        for(int i = 0; i < args.length; i++) {
            String cmd = args[i];
            if("--mibs".equals(cmd.toLowerCase())) {
                loader.load(Paths.get(args[++i]));
            } else {
                xmlPaths.add(args[i]);
            }
        }
        OIDFormatter.register(loader.buildTree());

        for (String xmlpath: xmlPaths) {
            JrdsDocument n;
            try (InputStream fis = new FileInputStream(xmlpath)) {
                n = new JrdsDocument(dbuilder.parse(fis));
            }
            transform((JrdsElement) n.getRootElement().findByPath("/probedesc/specific[@name='indexOid']"));
            transform((JrdsElement) n.getRootElement().findByPath("/probedesc/specific[@name='labelOid']"));
            transform((JrdsElement) n.getRootElement().findByPath("/probedesc/specific[@name='existOid']"));
            transform((JrdsElement) n.getRootElement().findByPath("/probedesc/specific[@name='uptimeOid']"));
            for (String xp: new String[] {"/probedesc/ds/oid", "/probedesc/ds/oidhigh", "/probedesc/ds/oidlow"}) {
                JrdsElement oidNode = n.getRootElement().findByPath(xp);
                transform(oidNode);
            }
            Map<String, String> prop = new HashMap<String, String>();
            prop.put(OutputKeys.INDENT, "yes");
            prop.put(OutputKeys.DOCTYPE_PUBLIC, "-//jrds//DTD Probe Description//EN");
            prop.put(OutputKeys.DOCTYPE_SYSTEM, "urn:jrds:probedesc");
            prop.put(OutputKeys.INDENT, "yes");
            prop.put("{http://xml.apache.org/xslt}indent-amount", "4");
            prop.put(OutputKeys.ENCODING, "UTF-8");
            try (OutputStream fos = new FileOutputStream(xmlpath)) {
                jrds.Util.serialize(n.getOwnerDocument(), fos, null, prop);
            }
        }
    }

    private void transform(JrdsElement e) {
        if (e == null) {
            return;
        }
        String oidstring = e.getTextContent();
        if (oidstring.length() > 1 && oidstring.charAt(0) == '.') {
            oidstring = oidstring.substring(1);
        } else if (oidstring.length() <= 1){
            System.out.println("Bad OID " + oidstring);
            return;
        }
        try {
            String formatted = new OID(oidstring).format();
            // Ensure that SNMP4J can parse the content back
            @SuppressWarnings("unused")
            OID voidoid = new OID(formatted);
            e.setTextContent(formatted);
        } catch (Exception ex) {
            System.out.println("can't manage " + oidstring);
        }
    }
}
