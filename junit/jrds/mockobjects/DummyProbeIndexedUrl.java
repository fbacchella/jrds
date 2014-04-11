package jrds.mockobjects;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import jrds.Probe;
import jrds.probe.UrlProbe;

public class DummyProbeIndexedUrl extends DummyProbeIndexed implements UrlProbe {
    Class<? extends Probe<?,?>> originalProbe;
    URL url;

    public void configure () throws InvocationTargetException {
        super.configure();
        try {
            url = new URL("http://localhost/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public Integer getPort() {
        return url.getPort();
    }

    public URL getUrl() {
        return url;
    }

    public String getUrlAsString() {
        return url.toString();
    }

}
