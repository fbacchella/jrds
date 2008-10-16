package jrds.probe;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.JavaTypeMapper;
import org.codehaus.jackson.map.JsonNode;
import org.codehaus.jackson.map.JsonTypeMapper;
import org.codehaus.jackson.map.impl.ArrayNode;
import org.codehaus.jackson.map.impl.ObjectNode;

public abstract class json extends Probe {
	static final private Logger logger = Logger.getLogger(json.class);

	static final private JsonFactory jfactory = new JsonFactory();

	URL url;

	public json(RdsHost monitoredHost, ProbeDesc pd) {
		super(monitoredHost, pd);
		// TODO Auto-generated constructor stub
	}

	public json(URL url) {
		this.url = url;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Map getNewSampleValues() {
		try {
			JsonParser jp = jfactory.createJsonParser(url);
			JavaTypeMapper mapper = new  JavaTypeMapper();
			Object jn = null;
			while((jn = mapper.read(jp)) != null){
				descendJava(jn, "");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceType() {
		return "JSON";
	}

	public Map<String, JsonNode> mapObject(JsonNode o) {
		Map<String, JsonNode> m = new HashMap<String, JsonNode>(o.size());
		for(Iterator<String> i = o.getFieldNames(); i.hasNext();) {
			String key = i.next();
			m.put(key, o.getFieldValue(key));
		}
		return m;
	}
	
	public List<JsonNode> mapArray(JsonNode o) {
		List<JsonNode> a = new ArrayList<JsonNode>(o.size());
		
		for(Iterator<JsonNode> i = o.getElements(); i.hasNext();) {
			JsonNode e = i.next();
			a.add(e);
		}
		return a;
	}
	
	public void trace(Collection<JsonNode> c) {
		for(JsonNode n: c) {
			logger.trace(n);
		}
	}
	
	public void descend(JsonNode n) {
		if(n.isObject()) {
			Map<String, JsonNode> m = mapObject(n);
			trace(m.values());
			for(JsonNode sn: m.values())
				descend(sn);
		}
		else if(n.isArray()) {
			List<JsonNode> a = mapArray(n);
			trace(a);
			for(JsonNode sn: a)
				descend(sn);
		}
	}
	public void descendJava(Object o, String path) {
		String separator = ".";
		if(path == null ||  "".contentEquals(path)) {
			separator = "";
		}
		//logger.trace(o.getClass());
		if(o instanceof Map) {
			Map<?, ?> m = (Map<?, ?>) o;
			//logger.trace(m.keySet());
			for(Map.Entry<?, ?> e: m.entrySet())
				descendJava(e.getValue(), path + separator + e.getKey());
		}
		else if(o instanceof Collection) {
			Collection<?> c = (Collection<?>) o;
			//logger.trace("[]=" + c.size() );
			int i = 0;
			for (Object v: c)
				descendJava(v, path + separator + "[" + i++ + "]");
		}
		else {
			logger.trace(path + separator + o);
		}
	}
}
