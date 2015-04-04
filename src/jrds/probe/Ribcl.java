package jrds.probe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;

import jrds.Probe;
import jrds.Util;
import jrds.factories.ProbeBean;
import jrds.factories.ProbeMeta;
import jrds.starter.SSLStarter;
import jrds.starter.SocketFactory;
import jrds.starter.XmlProvider;

import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@ProbeMeta(
        topStarter = jrds.starter.XmlProvider.class
        )
@ProbeBean({"user", "password", "iloHost", "port"})
public class Ribcl extends Probe<String, Number> implements SSLProbe {
    private String user;
    private String passwd;
    private String iloHost;
    private Integer port = 443;

    static final private String encoding = "ISO-8859-1";
    static final private String eol = "\r\n";
    static final private String xmlHeader = "<?xml version=\"1.0\" ?>" + eol;

    public void configure(String iloHost, int port, String user, String passwd) {
        this.iloHost = iloHost;
        this.user = user;
        this.passwd = passwd;
        this.port = port;
    }

    public void configure(String iloHost, String user, String passwd) {
        this.iloHost = iloHost;
        this.user = user;
        this.passwd = passwd;
    }

    public void configure() {
    }

    @Override
    public Map<String, Number> getNewSampleValues() {
        Socket s;
        try {
            s = connect();
        } catch (Exception e) {
            log(Level.ERROR, e, "SSL connect error %s", e);
            return Collections.emptyMap();
        }

        XmlProvider xmlstarter  = find(XmlProvider.class);
        if(xmlstarter == null) {
            log(Level.ERROR, "XML Provider not found");
            return Collections.emptyMap();
        }

        if(! isCollectRunning())
            return Collections.emptyMap();

        StringBuilder message = new StringBuilder();
        try {
            OutputStream outputSocket = s.getOutputStream();
            InputStream inputSocket = s.getInputStream();

            outputSocket.write(xmlHeader.getBytes(encoding));
            buildQuery(outputSocket, xmlstarter);

            byte[] buffer = new byte[4096];
            int n;
            while((n = inputSocket.read(buffer)) > 0) {
                String messageBuffer = new String(buffer, 0, n, encoding);
                message.append(messageBuffer);
            }
        } catch (IOException e) {
            log(Level.ERROR, e, "SSL socket error %s", e);
        }
        finally {
            try {
                s.close();
            } catch (IOException e) {
                log(Level.ERROR, e, "SSL socket error %s", e);
            }            
        }

        if(message.length() > 0) {
            return parseRibcl(message.toString(), xmlstarter);            
        }
        else {
            return Collections.emptyMap();
        }
    }

    @Override
    public String getSourceType() {
        return "RIBCL";
    }

    Map<String, Number> parseRibcl(String message, XmlProvider xmlstarter) {
        int start = 0;
        int end = 0;
        Map<String, Number> vars = new HashMap<String, Number>();
        while(start >= 0) {
            int nextstart = message.indexOf("<?xml ", end + 2);
            end = (nextstart != - 1 ? nextstart : message.length()) - 1;
            parse(message.substring(start, end), vars, xmlstarter);                    
            start = nextstart;
        }
        return vars;
    }

    protected Document makeDocument(XmlProvider xmlstarter) {
        Document ribclQ = xmlstarter.getDocument();
        Element LOCFG = ribclQ.createElement("LOCFG");
        LOCFG.setAttribute("version", "2.21");
        ribclQ.appendChild(LOCFG);
        Element RIBCL = ribclQ.createElement("RIBCL");
        LOCFG.appendChild(RIBCL);
        RIBCL.setAttribute("VERSION", "2.0");
        Element LOGIN = ribclQ.createElement("LOGIN");
        LOGIN.setAttribute("USER_LOGIN", user);
        LOGIN.setAttribute("PASSWORD", passwd);
        RIBCL.appendChild(LOGIN);
        Element command = ribclQ.createElement(getPd().getSpecific("command"));
        command.setAttribute("MODE", "read");
        LOGIN.appendChild(command);
        Element subcommand = ribclQ.createElement(getPd().getSpecific("subcommand"));
        command.appendChild(subcommand);
        return ribclQ;
    }

    protected void buildQuery(OutputStream out, XmlProvider xmlstarter) {
        Document ribclQ = makeDocument(xmlstarter);
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(OutputKeys.INDENT, "no");
        properties.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        properties.put(OutputKeys.ENCODING, encoding);
        try {
            Util.serialize(ribclQ, out, null, properties);
        } catch (TransformerException e) {
            throw new RuntimeException("Unable to serialize in memory", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize in memory", e);
        }
    }

    private Socket connect() throws NoSuchAlgorithmException, KeyManagementException, UnknownHostException, IOException {
        if(port == 23) {
            SocketFactory ss = find(SocketFactory.class);
            return ss.createSocket(iloHost, port);
        }               

        return find(SSLStarter.class).connect(iloHost, port);
    }

    public void parse(String message, Map<String, Number> vars, XmlProvider xmlstarter) {
        if(message == null ||  "".equals(message))
            return;
        log(Level.TRACE,"new message to parse: ");
        log(Level.TRACE, message);
        //The XML returned from an iLO is buggy, up to ilO2 1.50 
        message = message.replaceAll("<RIBCL VERSION=\"[0-9\\.]+\"/>", "<RIBCL >");
        Document d = xmlstarter.getDocument(new StringReader(message));
        xmlstarter.fileFromXpaths(d, getPd().getCollectStrings().keySet(), vars);
    }

    /* (non-Javadoc)
     * @see jrds.starter.StarterNode#isStarted(java.lang.Object)
     */
    @Override
    public boolean isStarted(Object key) {
        return super.isStarted(key) && find(XmlProvider.class).isStarted() && find(SocketFactory.class).isStarted();
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the passwd
     */
    public String getPassword() {
        return passwd;
    }

    /**
     * @param passwd the passwd to set
     */
    public void setPassword(String passwd) {
        this.passwd = passwd;
    }

    /**
     * @return the iloHost
     */
    public String getIloHost() {
        return iloHost;
    }

    /**
     * @param iloHost the iloHost to set
     */
    public void setIloHost(String iloHost) {
        this.iloHost = iloHost;
    }

    /**
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(Integer port) {
        this.port = port;
    }

}
