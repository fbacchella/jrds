package jrds.probe.jmx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import lombok.Data;

@Data
class RequestParams {

    private static final Pattern JMX_PATH_PATTERN = Pattern.compile("(\"(?:.*?)(?<!\")\\\"|(?:.*?)(?<!\\\\))/(.*)");

    protected final ObjectName mbeanName;
    protected final String attributeName;
    protected final String jmxPath;
    protected final String jmxCollectPath;
    protected RequestParams(String jmxCollectPath) throws MalformedObjectNameException {
        Matcher m = JMX_PATH_PATTERN.matcher(jmxCollectPath);
        if (m.matches()) {
            String tryObjectName = m.group(1).replaceAll("\\\\/", "/");
            if (tryObjectName.startsWith("\"")) {
                tryObjectName = ObjectName.unquote(tryObjectName);
            }
            mbeanName = new ObjectName(tryObjectName);
            String attributeNameParam = m.group(2);
            int pathSplit = attributeNameParam.indexOf('/');
            if (pathSplit > 0) {
                attributeName = attributeNameParam.substring(0, pathSplit);
                jmxPath = attributeNameParam.substring(pathSplit + 1);
            } else {
                attributeName = attributeNameParam;
                jmxPath = null;
            }
            this.jmxCollectPath = jmxCollectPath;
        } else {
            throw new MalformedObjectNameException("Invalid JMX path \"%s\"".formatted(jmxCollectPath));
        }
    }

}
