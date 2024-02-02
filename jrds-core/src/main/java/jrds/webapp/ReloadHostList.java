package jrds.webapp;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jrds.Configuration;

/**
 * This servlet reload the host list file
 * 
 * @author Fabrice Bacchella
 * @version $Revision$
 */
@ServletSecurity
public class ReloadHostList extends JrdsServlet {

    static final Semaphore reloading = new Semaphore(1);

    /*
     * (non-Javadoc)
     * 
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.
     * HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

        // only one reload allowed to run, just ignore synchronous reload
        if(!reloading.tryAcquire()) {
            res.sendRedirect(req.getContextPath() + "/");
            return;
        }

        ParamsBean params = new ParamsBean(req, getHostsList());

        // Check permissions
        if(!allowed(params, getPropertiesManager().adminACL, req, res)) {
            return;
        }

        final ServletContext ctxt = getServletContext();
        Thread configthread = new Thread("jrds-new-config") {
            @Override
            public void run() {
                StartListener sl = (StartListener) ctxt.getAttribute(StartListener.class.getName());
                Properties p = sl.readProperties(ctxt);
                Configuration.switchConf(p);
                reloading.release();
            }
        };
        if(params.getValue("sync") != null) {
            configthread.run();
        } else {
            configthread.start();
        }
        res.sendRedirect(req.getContextPath() + "/");
    }
}
