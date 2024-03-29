package jrds.webapp;

import java.io.IOException;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Period;
import jrds.Probe;
import jrds.store.ExtractInfo;

/**
 * A servlet which show the last update values and time
 * 
 * @author Fabrice Bacchella
 */
public final class ProbeSummary extends JrdsServlet {
    static final private Logger logger = LoggerFactory.getLogger(ProbeSummary.class);

    /**
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/plain");
        res.addHeader("Cache-Control", "no-cache");
        ServletOutputStream out = res.getOutputStream();

        ParamsBean params = getParamsBean(req);

        Probe<?, ?> probe = params.getProbe();
        if(probe != null) {
            Period p = params.getPeriod();

            ExtractInfo ei = ExtractInfo.builder().interval(p.getBegin(), p.getEnd()).build();
            DataProcessor dp = probe.extract(ei);
            for(String dsName: probe.getPd().getDs()) {
                try {
                    out.print(dsName + " ");
                    out.println(dp.getVariable(dsName, new Variable.AVERAGE()).value + " ");
                    out.println(dp.getVariable(dsName, new Variable.MIN()).value + " ");
                    out.println(dp.getVariable(dsName, new Variable.MAX()).value);
                } catch (IOException e) {
                    logger.error("Probe " + probe + "unusable: " + e);
                }
            }
        } else {
            logger.error("Probe id provided " + params.getId() + " invalid");
        }
    }

}
