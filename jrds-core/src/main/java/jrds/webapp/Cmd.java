package jrds.webapp;

import java.io.IOException;
import java.util.Properties;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Configuration;
import jrds.HostsList;
import jrds.starter.Timer;

/**
 * Servlet implementation class Cmd
 */
@ServletSecurity
public class Cmd extends JrdsServlet {
    static final private Logger logger = LoggerFactory.getLogger(Cmd.class);

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        ParamsBean params = new ParamsBean(req, getHostsList(), "command", "arg");

        String command = params.getValue("command");
        if(command == null || "".equals(command)) {
            command = req.getServletPath().substring(1);
        }
        logger.debug("Command found: {}", command);

        if(!allowed(params, getPropertiesManager().adminACL, req, res))
            return;

        if("reload".equalsIgnoreCase(command)) {
            ServletContext ctxt = getServletContext();
            // only one reload allowed to run, just ignore synchronous reload
            if(ReloadHostList.reloading.tryAcquire()) {
                reload(ctxt);
            }
            res.sendRedirect(req.getContextPath() + "/");
        } else if("pause".equalsIgnoreCase(command)) {
            ServletContext ctxt = getServletContext();
            pause(ctxt, params.getValue("arg"));
            res.sendRedirect(req.getContextPath() + "/");
        }
    }

    private void reload(final ServletContext ctxt) {
        Thread configthread = new Thread("jrds-new-config") {
            @Override
            public void run() {
                StartListener sl = (StartListener) ctxt.getAttribute(StartListener.class.getName());
                Properties p = sl.readProperties(ctxt);
                Configuration.switchConf(p);
                ReloadHostList.reloading.release();
                logger.info("Configuration rescaned");
            }
        };
        configthread.start();
    }

    private void pause(final ServletContext ctxt, final String arg) {
        Thread configthread = new Thread("jrds-pause") {
            @Override
            public void run() {
                HostsList hl = Configuration.get().getHostsList();
                try {
                    for(Timer t: hl.getTimers()) {
                        t.lockCollect();
                    }
                    Thread.sleep(jrds.Util.parseStringNumber(arg, 1) * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for(Timer t: hl.getTimers()) {
                    t.releaseCollect();
                }
                logger.info("collect restarted");
            }
        };
        configthread.start();
    }

}
