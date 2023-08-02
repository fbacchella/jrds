package jrds.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Period;

/**
 * Servlet implementation class JSonPeriod
 */
public class JSonPeriod extends JrdsServlet {
    static final private Logger logger = LoggerFactory.getLogger(JSonPeriod.class);

    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ParamsBean params = getParamsBean(request);

        try {
            JrdsJSONWriter w = new JrdsJSONWriter(response);
            w.object();

            Period p = params.getPeriod();

            w.key("begin").value(params.getStringBegin());
            w.key("end").value(params.getStringEnd());

            Period.Scale scale = p.getScale();
            if(scale != Period.Scale.MANUAL) {
                w.key("scale").value(scale.ordinal());
            }
            w.endObject();
            w.newLine();
            w.flush();
        } catch (JSONException e) {
            logger.warn("Failed request: " + request.getRequestURI() + "?" + request.getQueryString() + ": " + e, e);
        }
    }

}
