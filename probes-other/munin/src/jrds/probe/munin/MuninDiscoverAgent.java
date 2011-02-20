package jrds.probe.munin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.probe.IndexedProbe;
import jrds.webapp.DiscoverAgent;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MuninDiscoverAgent extends DiscoverAgent {
    static final private Logger logger = Logger.getLogger(MuninDiscoverAgent.class);

    @Override
    public void discover(String hostName, Document hostDom, Collection<JrdsNode> probdescs, HttpServletRequest request) {
        try {
            Socket muninSocket = null;
            try {
                muninSocket = new Socket(hostName, 4949);
            } catch (IOException e) {
                logger.info("Munin not running on " + hostName);
                return;
            }
            muninSocket.setTcpNoDelay(true);

            PrintWriter out = null;
            BufferedReader in = null;
            out = new PrintWriter(muninSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(muninSocket.getInputStream()));
            //Drop the  welcome line
            in.readLine();
            out.println("list");
            Set<String> muninProbes = new HashSet<String>();
            for(String p: in.readLine().split(" ")) {
                muninProbes.add(p);
                if(p.lastIndexOf("_")>0 )
                    muninProbes.add(p.substring(0, p.lastIndexOf("_") +1 ));
                logger.trace("Munin probe found :" + p);
            }

            logger.trace(muninProbes);

            Element cnxElement = hostDom.createElement("connection");
            cnxElement.setAttribute("type", "jrds.probe.munin.MuninConnection");
            hostDom.getDocumentElement().appendChild(cnxElement);

            ClassLoader cl = getClass().getClassLoader();
            Class<?> muninClass = cl.loadClass("jrds.probe.munin.Munin");
            for(JrdsNode e: probdescs) {
                String probe = e.evaluate(CompiledXPath.get("/probedesc/name"));
                String probeClass = e.evaluate(CompiledXPath.get("/probedesc/probeClass"));
                Class<?> c = cl.loadClass(probeClass);
                String fetch = e.evaluate(CompiledXPath.get("/probedesc/specific[@name='fetch']"));
                if(fetch != null && ! "".equals(fetch) &&  muninClass.isAssignableFrom(c)) {
                    if( muninProbes.contains(fetch) ) {
                        Element rrdElem = hostDom.createElement("probe");
                        rrdElem.setAttribute("type", probe);
                        hostDom.getDocumentElement().appendChild(rrdElem);
                        muninProbes.remove(fetch);
                        
                    }
                    else if(IndexedProbe.class.isAssignableFrom(c)) {
                        Pattern indexedFetch = Pattern.compile(fetch + "_(.*)");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Generation Failed: ",e);
        }


    }

}
