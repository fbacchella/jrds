package jrds.jsonp.probe;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.event.Level;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import jrds.CollectResolver;
import jrds.Util;
import jrds.factories.ProbeMeta;
import jrds.jsonp.starter.JsonpProvider;
import jrds.probe.HCHttpProbe;

@ProbeMeta(
           topStarter = JsonpProvider.class,
           collectResolver = CollectResolver.StringResolver.class
                )
public class HttpJson extends HCHttpProbe<String> {

    private Map<JsonPath, String> collectKeys = null;
    private JsonPath uptimePointer = null;
    private JsonPath startPointer = null;
    private JsonPath currentTimePointer = null;

    @Override
    public Boolean configure() {
        collectKeys = new HashMap<>(getPd().getCollectMapping().size());
        for(Map.Entry<String, String> e: getPd().getCollectMapping().entrySet()) {
            String solved = Util.parseTemplate(e.getKey(), this);
            JsonPath xpath = JsonPath.compile(solved);
            if (xpath ==  null) {
                log(Level.DEBUG, "unparsed xpath: %s", e.getKey());
                continue;
            } else {
                collectKeys.put(xpath, solved);
            }
        }
        collectKeys = Collections.unmodifiableMap(collectKeys);
        log(Level.TRACE, "collect JSON pointer mapping %s", collectKeys);
        if (getPd().getSpecific("nouptime") == null) {
            String upTimePointer = getPd().getSpecific("upTimePath");
            if (upTimePointer != null && !upTimePointer.isEmpty()) {
                uptimePointer = JsonPath.compile(Util.parseTemplate(upTimePointer, this));
            } else {
                String startTimePath = getPd().getSpecific("startTimePath");
                String currentTimePath = getPd().getSpecific("currentTimePath");
                String timePattern = getPd().getSpecific("timePattern");
                if(startTimePath != null && currentTimePath != null && timePattern != null) {
                    startPointer = JsonPath.compile(Util.parseTemplate(startTimePath, this));
                    currentTimePointer = JsonPath.compile(Util.parseTemplate(startTimePath, this));
                } else {
                    timePattern = null;
                }
            }
        }
        return super.configure();
    }

    @Override
    protected Map<String, Number> parseStream(InputStream stream) {
        Configuration c = find(JsonpProvider.class).getConfiguration();
        DocumentContext ctx = JsonPath.parse(stream, c);

        setUptime(findUptime(ctx));
        Map<String, Number> vars = new HashMap<>(collectKeys.size());

        for (Map.Entry<JsonPath, String> e: collectKeys.entrySet()) {
            try {
                JSONArray v = ctx.read(e.getKey(), JSONArray.class);
                if (v.isEmpty()) {
                    log(Level.ERROR, "Failed to collect data '%s': not found", e.getValue());
                    break;
                }
                vars.put(e.getValue(), valueToNum(v.get(0)));
            } catch (PathNotFoundException ex) {
                log(Level.ERROR, "Failed to collect data '%s': not found", e.getValue());
            } catch (JSONException ex) {
                log(Level.ERROR, "Failed to collect data '%s': %s", e.getValue(), ex);
            }
        }
        return vars;
    }

    private Number valueToNum(Object o) {
        if (o instanceof Number) {
            return (Number) o;
        } else if (o instanceof String) {
            String val = (String)o;
            return Util.parseStringNumber(val, Double.NaN);
        } else {
            return Double.NaN;
        }
    }

    protected long findUptime(DocumentContext ctx) {
        try {
            JSONArray v;
            long returned;
            if (uptimePointer != null) {
                v = ctx.read(uptimePointer, JSONArray.class);
                if (v == null || v.isEmpty()) {
                    return 0;
                }
                returned = valueToNum(v.get(0)).longValue();
            } else if (startPointer != null) {
                v = ctx.read(startPointer, JSONArray.class);
                if (v == null || v.isEmpty()) {
                    return 0;
                }
                long start = valueToNum(v.get(0)).longValue();
                v = ctx.read(currentTimePointer, JSONArray.class);
                if (v == null || v.isEmpty()) {
                    return 0;
                }
                long current = valueToNum(v.get(0)).longValue();
                returned = current - start;
            } else {
                returned = Long.MAX_VALUE;
            }
            return returned;
        } catch (JSONException | PathNotFoundException e) {
            log(Level.ERROR, "Failed checking uptime: %s", e);
            return 0;
        }
    }

}
