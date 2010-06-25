package jrds.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

/**
 * Servlet implementation class JSonQueryParams
 */
public class JSonQueryParams extends JrdsServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ParamsBean params = getParamsBean(request);
		JrdsJSONWriter w = new JrdsJSONWriter(response);
		try {
			w.object();
			doVariable(w, "pid", request.getParameter("pid"));
			doVariable(w, "id", request.getParameter("id"));
			doVariable(w, "gid", request.getParameter("gid"));
			doVariable(w, "sort", request.getParameter("sort"));
			doVariable(w, "host", request.getParameter("host"));
			doVariable(w, "filter", request.getParameter("filter"));
			doVariable(w, "begin", params.getStringBegin());
			doVariable(w, "end", params.getStringEnd());
			doVariable(w, "min", params.getMinStr());
			doVariable(w, "max", params.getMaxStr());
			doVariable(w, "dsName", request.getParameter("dsName"));
			int scale = params.getScale();
			if(scale > 0) {
				doVariable(w, "autoperiod", "" + scale);
			}
			else {
				doVariable(w, "autoperiod", "0");
			}
			w.endObject();
			w.newLine();
			w.flush();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private final void doVariable(JrdsJSONWriter w, String key, String value) throws JSONException {
		if(value != null && ! "".equals(value)) {
			value = value.replace("'", " ").replace("\"", " ");
			w.key(key).value(value);
		}
	}

}
