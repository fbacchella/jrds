package jrds.probe.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

class RequestParams {
    protected final ObjectName mbeanName;
    protected final String attributeName;
    protected final String jmxPath;
    protected final String jmxCollectPath;
    protected RequestParams(String jmxCollectPath) throws MalformedObjectNameException {
        int nsSplit = jmxCollectPath.indexOf(':');
        int attrSplit = jmxCollectPath.indexOf('/', nsSplit);
        int pathSplit = jmxCollectPath.indexOf('/', attrSplit + 1);
        mbeanName = new ObjectName(jmxCollectPath.substring(0, attrSplit));
        if (pathSplit > 0) {
            attributeName = jmxCollectPath.substring(attrSplit + 1, pathSplit);
            jmxPath = jmxCollectPath.substring(pathSplit + 1);
        } else {
            attributeName = jmxCollectPath.substring(attrSplit + 1, jmxCollectPath.length());
            jmxPath = null;
        }
        this.jmxCollectPath = jmxCollectPath;
    }

}
