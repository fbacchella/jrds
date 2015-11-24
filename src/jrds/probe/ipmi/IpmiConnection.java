package jrds.probe.ipmi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Level;

import com.veraxsystems.vxipmi.api.async.ConnectionHandle;
import com.veraxsystems.vxipmi.api.sync.IpmiConnector;
import com.veraxsystems.vxipmi.coding.commands.PrivilegeLevel;
import com.veraxsystems.vxipmi.coding.security.CipherSuite;

import jrds.JuliToLog4jHandler;
import jrds.PropertiesManager;
import jrds.factories.ProbeBean;
import jrds.starter.Connection;
import jrds.starter.Resolver;

@ProbeBean({"bmcname", "user", "password"})
public class IpmiConnection extends Connection<Handle> {

    static {
        //If not already configured, we filter it
        JuliToLog4jHandler.catchLogger("com.veraxsystems.vxipmi", Level.FATAL);
    }

    String bmcname;
    String user;
    String password;
    Object starterKey;
    Handle jrdsHandle;

    @Override
    public void configure(PropertiesManager pm) {
        super.configure(pm);
        starterKey = getLevel().getParent().registerStarter(new Resolver(bmcname)).getKey();
    }

    @Override
    public Handle getConnection() {
        return jrdsHandle;
    }

    @Override
    public boolean startConnection() {
        Resolver resolver = getLevel().find(Resolver.class, starterKey);
        if(!resolver.isStarted()) {
            return false;
        }

        IpmiConnectorStarter mainStarter = getLevel().find(IpmiConnectorStarter.class);
        if(!mainStarter.isStarted()) {
            return false;
        }

        try {
            IpmiConnector connector = mainStarter.connector;
            ConnectionHandle handle = connector.createConnection(resolver.getInetAddress());
            connector.setTimeout(handle, getTimeout() * 1000);

            CipherSuite cs;
            // Get cipher suites supported by the remote host
            List<CipherSuite> suites = connector.getAllCipherSuites(handle);
            if (suites.size() > 3) {
                cs = suites.get(3);
            } else if (suites.size() > 2) {
                cs = suites.get(2);
            } else if (suites.size() > 1) {
                cs = suites.get(1);
            } else {
                cs = suites.get(0);
            }
            connector.getChannelAuthenticationCapabilities(handle, cs, PrivilegeLevel.User);
            connector.openSession(handle, user, password, "".getBytes());
            jrdsHandle = new Handle(mainStarter.connector, handle);
        } catch (InterruptedException e) {
            return false;
        } catch (FileNotFoundException e) {
            log(Level.ERROR, "invalid IPMI connection: %s", e.getMessage());
            return false;
        } catch (IOException e) {
            log(Level.ERROR, "IPMI network error: %s", e.getMessage());
            return false;
        } catch (Exception e) {
            log(Level.ERROR, "invalid IPMI connection: %s", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public void stopConnection() {
        IpmiConnectorStarter mainStarter = getLevel().find(IpmiConnectorStarter.class);
        if(mainStarter != null && mainStarter.connector != null) {
            jrdsHandle.closeConnection();
        }
        jrdsHandle = null;
    }

    @Override
    public long setUptime() {
        return Long.MAX_VALUE;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBmcname() {
        return bmcname;
    }

    public void setBmcname(String bmcname) {
        this.bmcname = bmcname;
    }

}
