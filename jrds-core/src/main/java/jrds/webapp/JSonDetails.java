package jrds.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.GraphNode;
import jrds.Probe;
import jrds.Util;
import jrds.probe.IndexedProbe;

public class JSonDetails extends JrdsServlet {

    static final private Logger logger = LoggerFactory.getLogger(JSonDetails.class);

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            ParamsBean params = getParamsBean(request);
            Probe<?, ?> p = params.getProbe();

            JrdsJSONWriter w = new JrdsJSONWriter(response);
            w.object();
            if (p != null) {
                Optional.ofNullable(p.getQualifiedName()).ifPresent( i -> w.key("probequalifiedname").value(i));
                Optional.ofNullable(p.getName()).ifPresent( i-> w.key("probeinstancename").value(i));
                Optional.ofNullable(p.getPd().getName()).ifPresent( i-> w.key("probename").value(i));
                Optional.ofNullable(p.getHost().getName()).ifPresent( i-> w.key("hostname").value(i));
                Optional.ofNullable(params.getPid()).ifPresent( i-> w.key("pid").value(i));
                if(p instanceof IndexedProbe) {
                    Optional.ofNullable(((IndexedProbe) p).getIndexName()).ifPresent( i-> w.key("index").value(i));
                }
                w.key("datastores");
                w.array();
                List<String> dsNames = new ArrayList<>();
                dsNames.addAll(p.getPd().getDs());
                Collections.sort(dsNames, Util.nodeComparator);
                for(String datasource: dsNames) {
                    w.object();
                    w.key("id").value(datasource.hashCode());
                    w.key("name").value(datasource);
                    w.endObject();
                }
                w.endArray();
                w.key("graphs");
                w.array();
                for(GraphNode gn: p.getGraphList()) {
                    w.object();
                    w.key("id").value(gn.getQualifiedName().hashCode());
                    w.key("name").value(gn.getQualifiedName());
                    w.endObject();
                }
                w.endArray();
            }
            w.endObject();
            w.flush();
        } catch (Exception e) {
            logger.warn("Failed request: " + request.getRequestURI() + "?" + request.getQueryString() + ": " + e, e);
        }
    }

}
