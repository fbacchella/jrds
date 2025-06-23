package jrds.probe.jdbc;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.slf4j.event.Level;

import jrds.PropertiesManager;
import jrds.factories.ProbeBean;
import jrds.starter.Connection;
import lombok.Getter;
import lombok.Setter;

@ProbeBean({ "user", "password", "url", "driverClass"})
public class JdbcConnection extends Connection<Statement> {

    private java.sql.Connection con;
    @Setter @Getter
    private String user;
    private String passwd;
    @Getter
    private String driverClass = null;
    @Setter @Getter
    private String url;

    protected static void registerDriver(Class<? extends Driver> jdbcDriver) {
        try {
            DriverManager.registerDriver(jdbcDriver.getConstructor().newInstance());
        } catch (Exception e) {
            throw new RuntimeException("Can't register JDBC driver " + jdbcDriver, e);
        }
    }

    public JdbcConnection() {

    }

    public JdbcConnection(String user, String passwd, String url) {
        this.user = user;
        this.passwd = passwd;
        this.url = url;
        checkDriver(url);
    }

    public JdbcConnection(String user, String passwd, String url, String driverClass) {
        this.user = user;
        this.passwd = passwd;
        this.url = url;
        this.driverClass = driverClass;
    }

    @Override
    public void configure(PropertiesManager pm) {
        url = jrds.Util.parseTemplate(url, this, getLevel());
        checkDriver(url);
        super.configure(pm);
    }

    public Statement getConnection() {
        try {
            return con.createStatement();
        } catch (SQLException e) {
            log(Level.ERROR, "JDBC Statment failed: %s", e);
            return null;
        }
    }

    public void checkDriver(String sqlurl) {
        try {
            if (driverClass != null && !"".equals(driverClass)) {
                Class.forName(driverClass);
            }
            DriverManager.getDriver(sqlurl);
        } catch (SQLException | ClassNotFoundException e) {
            throw new IllegalStateException("Error checking JDBC url " + sqlurl, e);
        }
    }

    @Override
    public long setUptime() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean startConnection() {
        boolean started = false;
        if (getResolver().isStarted()) {
            Properties p = getProperties();
            p.put("user", user);
            p.put("password", passwd);
            try {
                DriverManager.setLoginTimeout(getTimeout());
                con = DriverManager.getConnection(url, p);
                started = true;
            } catch (SQLException e) {
                log(Level.ERROR, e, "Sql error for %s: %s", url, e);
            }
        }
        return started;
    }

    private Properties getProperties() {
        return new Properties();
    }

    @Override
    public void stopConnection() {
        if(con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                log(Level.ERROR, e, "Error with %s: %s", getUrl(), e);
            }
        }
        con = null;
    }

    /**
     * @return the passwd
     */
    public String getPassword() {
        return passwd;
    }

    /**
     * @param passwd the passwd to set
     */
    public void setPassword(String passwd) {
        this.passwd = passwd;
    }

    /**
     * @param driverClass the driverClass to set
     */
    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't find JDBC driver " + driverClass, e);
        }
    }

}
