package jrds.webapp;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.HostsList;
import jrds.Tab;

/**
 * Servlet implementation class JSonQueryParams
 */
public class JSonQueryParams extends JrdsServlet {
    private static final long serialVersionUID = 1L;
    static final private Logger logger = LoggerFactory.getLogger(JSonQueryParams.class);

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ParamsBean params = getParamsBean(request);
        HostsList root = getHostsList();

        JrdsJSONWriter w = new JrdsJSONWriter(response);
        try {
            w.object();
            doVariable(w, "pid", params.getValue("pid"));
            doVariable(w, "id", params.getValue("id"));
            doVariable(w, "gid", params.getValue("gid"));
            doVariable(w, "sort", params.getValue("sort"));
            String pathString = params.getValue("path");
            if(pathString != null && !"".equals(pathString)) {
                doVariable(w, "path", new JSONArray(pathString));
            }
            String choiceType = params.getChoiceType();
            String choiceValue = params.getChoiceValue();
            if(choiceType != null && choiceValue != null)
                doVariable(w, choiceType, choiceValue);

            doVariable(w, "min", params.getMinStr());
            doVariable(w, "max", params.getMaxStr());
            doVariable(w, "dsName", params.getValue("dsName"));
            doVariable(w, "begin", params.getBegin());
            doVariable(w, "end", params.getEnd());
            doVariable(w, "autoperiod", params.getScale().ordinal());

            // Add the list of tabs
            w.key("tabslist");
            w.object();
            for(String id: root.getTabsId()) {
                Tab tab = root.getTab(id);
                w.key(id);
                w.object();
                w.key("id").value(id);
                w.key("label").value(tab.getName());
                w.key("isFilters").value(tab.isFilters());
                w.key("callback").value(tab.getJSCallback());
                w.endObject();
            }
            w.endObject();

            w.endObject();
            w.newLine();
            w.flush();
        } catch (JSONException e) {
            logger.error("{}", e.getMessage(), e);
        }
    }

    private void doVariable(JrdsJSONWriter w, String key, Object value) {
        logger.trace("resolving {} with {}", key, value);
        if(value == null)
            return;
        if(value instanceof String && "".equals(value.toString().trim())) {
            return;
        }
        w.key(key).value(value);
    }

}
