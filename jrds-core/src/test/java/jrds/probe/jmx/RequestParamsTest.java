package jrds.probe.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Test;

public class RequestParamsTest {

    @Test
    public void testParsing() throws MalformedObjectNameException {
        test("java.lang:type=MemoryPool,name=Compressed Class Space/Usage/used",
             "java.lang:type=MemoryPool,name=Compressed Class Space", "Usage", "used");
        test("Catalina:type=GlobalRequestProcessor,name=a\\/b/bytesReceived",
                "Catalina:type=GlobalRequestProcessor,name=a/b", "bytesReceived", null);
        test("\"Catalina:type=GlobalRequestProcessor,name=a/b\"/bytesReceived",
                "Catalina:type=GlobalRequestProcessor,name=a/b", "bytesReceived", null);
        test("SUN_OPENSSO_SERVER_MIB_SsoServerInstance:name=com.sun.identity.monitoring.SsoServerAuthSvc/AuthenticationFailureCount",
                "SUN_OPENSSO_SERVER_MIB_SsoServerInstance:name=com.sun.identity.monitoring.SsoServerAuthSvc", "AuthenticationFailureCount", null);
        test("java.lang:type=MemoryPool,name=CodeHeap 'non-nmethods'/Usage/used",
                "java.lang:type=MemoryPool,name=CodeHeap 'non-nmethods'", "Usage", "used");
    }

    private void test(String path, String on, String attribute, String jmxPath)
            throws MalformedObjectNameException {
        RequestParams rp = new RequestParams(path);
        Assert.assertEquals(new ObjectName(on), rp.mbeanName);
        Assert.assertEquals(attribute, rp.attributeName);
        Assert.assertEquals(jmxPath, rp.jmxPath);
    }

}
