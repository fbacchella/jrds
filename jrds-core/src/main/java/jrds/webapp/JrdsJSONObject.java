package jrds.webapp;

import java.util.Iterator;

import org.json.JSONObject;

public class JrdsJSONObject extends JSONObject implements Iterable<String> {

    public JrdsJSONObject() {
        super();
    }

    public JrdsJSONObject(String arg0) {
        super(arg0);
    }

    public Iterator<String> iterator() {
        return keys();
    }

}
