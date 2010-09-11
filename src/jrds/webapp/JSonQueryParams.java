package jrds.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.HostsList;
import jrds.Tab;

import org.json.JSONArray;
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
		HostsList root = getHostsList();

		JrdsJSONWriter w = new JrdsJSONWriter(response);
		try {
			w.object();
			doVariable(w, "pid", params.getValue("pid"));
			doVariable(w, "id", params.getValue("id"));
			doVariable(w, "gid", params.getValue("gid"));
			doVariable(w, "sort", params.getValue("sort"));
			String pathString = params.getValue("path");
			if(pathString != null && ! "".equals(pathString)) {
				doVariable(w, "path", new JSONArray(pathString));
			}
			String choiceType = params.getChoiceType();
			String choiceValue = params.getChoiceValue();
			if(choiceType != null && choiceValue != null)
				doVariable(w, choiceType, choiceValue);

			//			doVariable(w, "tree", params.getValue("tree"));
			//			doVariable(w, "tab", params.getValue("tab"));
			//			Filter f = params.getFilter();
			//			if(f != null) {
			//				if(f instanceof FilterHost) {
			//					doVariable(w, "host", f.getName());
			//				}
			//				else {
			//					doVariable(w, "filter", f.getName());
			//				}
			//			}
			doVariable(w, "min", params.getMinStr());
			doVariable(w, "max", params.getMaxStr());
			doVariable(w, "dsName", params.getValue("dsName"));
			int scale = params.getScale();
			if(scale > 0) {
				doVariable(w, "autoperiod", "" + scale);
			}
			else {
				doVariable(w, "begin", params.getStringBegin());
				doVariable(w, "end", params.getStringEnd());
				doVariable(w, "autoperiod", "0");
			}

			//Add the list of tabs
			w.key("tabslist");
			w.array();
			for(String id: root.getTabsId()) {
				w.object();
				Tab tab = root.getTab(id);
				w.key("id").value(id);
				w.key("label").value(tab.getName());
				w.key("isFilters").value(tab.isFilters());
				if(ParamsBean.DEFAULTTAB.equals(id))
					w.key("selected").value("true");
				w.endObject();
			}
			w.endArray();

			w.endObject();
			w.newLine();
			w.flush();
		} catch (JSONException e) {
		}
	}

	private final void doVariable(JrdsJSONWriter w, String key, Object value) throws JSONException {
		if(value == null) 
			return;
		if(value instanceof String && "".equals(value.toString().trim())) {
			return;
		}
		//		if(value != null && ! "".equals(value)) {
		//			value = value.replace("'", " ");//.replace("\"", " ");
		//		}
		w.key(key).value(value);
	}

}
