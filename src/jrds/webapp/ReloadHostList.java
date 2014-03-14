package jrds.webapp;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.Configuration;

/**
 * This servlet reload the host list file
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class ReloadHostList extends JrdsServlet {

    static final Semaphore reloading = new Semaphore(1);

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        //only one reload allowed to run, just ignore synchronous reload
        if (! reloading.tryAcquire()) {
            res.sendRedirect(req.getContextPath() + "/");
            return;
        }

        ParamsBean params = new ParamsBean(req, getHostsList());

        //Check permissions
        if(! allowed(params, getPropertiesManager().adminACL, req, res)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
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
        }
        else {
            configthread.start();            
        }
        res.sendRedirect(req.getContextPath() + "/");
    }
}
