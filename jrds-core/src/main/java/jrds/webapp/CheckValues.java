package jrds.webapp;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.Variable;

import jrds.HostsList;
import jrds.Probe;
import jrds.store.ExtractInfo;
import jrds.store.Extractor;

/**
 * A servlet which returns datastore values from a probe. It can be used in many
 * way : The simplest way is by using a URL of the form : http://<em>server</em>
 * /values/<em>host</em>/<em>probe.</em> It will return all datastores values
 * for this probe. By adding a /<em>datastore</em>, one can choose only one data
 * store.
 * <p>
 * It's possible to refine the query with some arguments, using REST syntax.
 * <p>
 * The argument can be:
 * <ul>
 * <li>dsName: the datastore name</li>
 * <li>period: the time interval in seconds, default to the step value.</li>
 * <li>cf: the consolidated function used.</li>
 * </ul>
 * If there is only one value generated, it's displayed as is. Else the name is
 * also shown as well as the last update value in the form
 * <code>datastore: value</code>
 * 
 * @author Fabrice Bacchella
 */
public final class CheckValues extends JrdsServlet {

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HostsList hl = getHostsList();
        ParamsBean params = new ParamsBean(req, hl, "host", "probe", "dsname", "period", "cf");

        long period = jrds.Util.parseStringNumber(params.getValue("period"), new Long(hl.getStep()));
        String cfName = params.getValue("cf");
        if(cfName == null || cfName.trim().isEmpty())
            cfName = "AVERAGE";
        Variable v;
        switch (cfName.trim().toUpperCase()) {
        case "AVERAGE": v = new Variable.AVERAGE(); break;
        case "FIRST": v = new Variable.FIRST(); break;
        case "LAST": v = new Variable.LAST(); break;
        case "LSLCORREL": v = new Variable.LSLCORREL(); break;
        case "LSLINT": v = new Variable.LSLINT(); break;
        case "LSLSLOPE": v = new Variable.LSLSLOPE(); break;
        case "MAX": v = new Variable.MAX(); break;
        case "MIN": v = new Variable.MIN(); break;
        case "PERCENTILE95": v = new Variable.PERCENTILE(95); break;
        case "STDDEV": v = new Variable.STDDEV(); break;
        case "TOTAL": v = new Variable.TOTAL(); break;
        default:
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "No matching function");
            return;
        }

        Probe<?, ?> p = params.getProbe();
        if (p == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "No matching probe");
            return;
        }

        res.setContentType("text/plain");
        res.addHeader("Cache-Control", "no-cache");
        ServletOutputStream out = res.getOutputStream();

        Date lastupdate = p.getLastUpdate();
        long age = (new Date().getTime() - lastupdate.getTime()) / 1000;
        // It the last update is too old, it fails
        if(age > p.getStep() * 2) {
            out.println("Probe too old: " + age);
            return;
        }
        Date paste = new Date(lastupdate.getTime() - period * 1000);

        Extractor ex = p.fetchData();

        String ds = params.getValue("dsname");

        ExtractInfo ei = ExtractInfo.get().make(paste, lastupdate).make(p.getStep());
        if (ds != null && !"".equals(ds.trim())) {
            String dsName = ds.trim();
            ex.addSource(dsName, dsName);
            DataProcessor dp = ei.getDataProcessor(ex);
            double val = dp.getVariable(dsName, v).value;
            out.print(val);
        } else {
            for(String dsName: p.getPd().getDs()) {
                ex.addSource(dsName, dsName);
            }
            DataProcessor dp = ei.getDataProcessor(ex);
            for(String dsName: ex.getDsNames()) {
                double val = dp.getVariable(dsName, v).value;
                out.println(dsName + ": " + val);
            }
            out.println("Last update: " + p.getLastUpdate());
            out.println("Last update age (ms): " + (new Date().getTime() - p.getLastUpdate().getTime()));
        }
        ex.release();
    }

}
