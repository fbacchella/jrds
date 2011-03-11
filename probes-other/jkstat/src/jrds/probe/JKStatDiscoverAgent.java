package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.webapp.DiscoverAgent;

import org.apache.log4j.Level;
import org.w3c.dom.Element;

import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;
import uk.co.petertribble.jkstat.client.RemoteJKstat;

public class JKStatDiscoverAgent extends DiscoverAgent {
    static final int DEFAULTPORT = 3000;

    public JKStatDiscoverAgent() {
        super("JKStat");
    }

    @Override
    public void discover(String hostname, Element hostElement,
            Collection<JrdsNode> probdescs, HttpServletRequest request) {
        int port = jrds.Util.parseStringNumber(request.getParameter("discoverJKStatPort"), new Integer(DEFAULTPORT));
        try {
            String hostName = hostname;
            URL remoteUrl = new URL("http", hostName, port, "/");
            JKstat remoteJk = new RemoteJKstat(remoteUrl.toString());
            List<String> argsTypes = Collections.emptyList();
            List<String> argsValues = Collections.emptyList();

            if(port != DEFAULTPORT) {
                argsTypes = Collections.singletonList("Integer");
                argsValues = Collections.singletonList(Integer.toString(port).toString());
            }
            addConnexion(hostElement, KstatConnection.class.getName(), argsTypes, argsValues);

            ClassLoader cl = getClass().getClassLoader();
            Class<?> kstatClass = jrds.probe.KstatProbe.class;
            for(JrdsNode e: probdescs) {
                String probe = e.evaluate(CompiledXPath.get("/probedesc/name"));
                String probeClass = e.evaluate(CompiledXPath.get("/probedesc/probeClass"));
                Class<?> c = cl.loadClass(probeClass);
                String module = e.evaluate(CompiledXPath.get("/probedesc/specific[@name='module']"));
                String name = e.evaluate(CompiledXPath.get("/probedesc/specific[@name='name']"));
                String instanceVal = e.evaluate(CompiledXPath.get("/probedesc/specific[@name='index']"));
                int instance = jrds.Util.parseStringNumber(instanceVal, new Integer(0));
                if(module != null && ! "".equals(module) &&  kstatClass.isAssignableFrom(c)) {
                    Kstat active  = remoteJk.getKstat(module, instance, name);
                    if(active != null) {
                        log(Level.DEBUG, "probe found: %s:%d:%s", module, instance,name);
                        addProbe(hostElement, probe, null, null);
                    }
                }
            }
        } catch (MalformedURLException e) {
            this.log(Level.ERROR, "Malformed URL http://%s:%d/", hostname, port);
        } catch (ClassNotFoundException e) {
            this.log(Level.ERROR, e.getMessage());
        }
    }

    @Override
    public List<FieldInfo> getFields() {
        FieldInfo fi = new FieldInfo();
        fi.dojoType = DojoType.TextBox;
        fi.id = "discoverJKStatPort";
        fi.label = " JKstat listening port";
        return Collections.singletonList(fi);
    }

}
