package jrds.bootstrap;

import org.junit.Assert;
import org.junit.Test;

public class TestBootStrap {

    @Test
    public void testLoadClasses() {
        for(String cmdClass: BootStrap.cmdClasses.values()) {
            try {
                @SuppressWarnings("unchecked")
                Class<CommandStarter> cmdStarter = (Class<CommandStarter>) getClass().getClassLoader().loadClass(cmdClass);
                @SuppressWarnings("unused")
                CommandStarter cmd = cmdStarter.newInstance();
            } catch (ClassNotFoundException e) {
                Assert.fail("command class " + cmdClass + " not found");
            } catch (Exception e) {
                throw new RuntimeException("command class '" + cmdClass + "' invalid", e);
            }
        }
    }

}
