package jrds.probe.jdbc;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.event.Level;

import jrds.ProbeConnected;
import jrds.ProbeDesc;
import jrds.Util;
import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;

public class GenericJdbcProbe extends ProbeConnected<String, Number, JdbcConnection> implements UrlProbe, IndexedProbe {

    String query = null;
    String keyColumn = null;
    String index = "";
    String uptimeRow = null;
    String uptimeQuery = null;

    public GenericJdbcProbe() {
        super(JdbcConnection.class.getName());
    }

    /**
     * 
     * @see jrds.ProbeConnected#configure()
     */
    public Boolean configure(List<?> args) {
        if (super.configure()) {
            ProbeDesc<String> pd = getPd();
            query = jrds.Util.parseTemplate(pd.getSpecific("query"), this, getHost(), args);
            keyColumn = jrds.Util.parseTemplate(pd.getSpecific("key"), this, getHost(), args);
            uptimeQuery = jrds.Util.parseTemplate(pd.getSpecific("uptimeQuery"), this, getHost(), args);
            uptimeRow = jrds.Util.parseTemplate(pd.getSpecific("uptimeRow"), this, getHost(), args);
            String indexTemplate = pd.getSpecific("index");
            if (indexTemplate != null && !"".equals(indexTemplate)) {
                index = jrds.Util.parseTemplate(indexTemplate, this, getHost(), args);
            }
            setName(jrds.Util.parseTemplate(pd.getProbeName(), this, getHost(), args));
            return true;
        }
        return false;
    }

    public Boolean configure() {
        return configure(Collections.emptyList());
    }

    public Boolean configure(String args) {
        return configure(Collections.singletonList(args));
    }

    public Boolean configure(String... args) {
        return configure(Arrays.asList(args));
    }

    @Override
    public Map<String, Number> getNewSampleValuesConnected(JdbcConnection cnx) {
        Map<String, Number> values = null;
        Statement stmt = cnx.getConnection();
        if (stmt == null) {
            return Collections.emptyMap();
        }
        if(uptimeQuery != null && !"".equals(uptimeQuery) && !doUptimeQuery(stmt)) {
            closeStatement(stmt);
            return Collections.emptyMap();
        }
        try {
            ResultSet rs = null;
            try {
                log(Level.DEBUG, "sql query used: %s", query);
                if (stmt.execute(query)) {
                    rs = stmt.getResultSet();
                    Set<String> collectKeys = new HashSet<>(getPd().getCollectMapping().keySet());
                    if (uptimeQuery == null && uptimeRow != null)
                        collectKeys.add(uptimeRow);
                    values = getValuesFromRS(rs, collectKeys);
                    if (uptimeRow != null && values.containsKey(uptimeRow)) {
                        setUptime(values.get(uptimeRow).longValue());
                        values.remove(uptimeRow);
                    }
                }
            } finally {
                closeResultSet(rs);
            }
            return values;
        } catch (SQLException e) {
            log(Level.ERROR, e, "SQL exception while getting values: %s", e);
        } finally {
            closeStatement(stmt);
        }
        return Collections.emptyMap();
    }

    private void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log(Level.ERROR, e, "SQL exception while closing statement: %s", e);
            }
        }
    }

    private void closeResultSet(ResultSet rs) {
        if(rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log(Level.ERROR, e, "SQL exception while closing results: %s", e);
            }
        }
    }

    private boolean doUptimeQuery(Statement stmt) {
        ResultSet rs = null;
        try {
            stmt.execute(uptimeQuery);
            rs = stmt.getResultSet();
            Map<String, Number> values = getValuesFromRS(rs, Collections.singleton(uptimeRow));
            if(uptimeRow != null && values.containsKey(uptimeRow)) {
                setUptime(values.get(uptimeRow).longValue());
                values.remove(uptimeRow);
            }
            return true;
        } catch (SQLException e) {
            log(Level.ERROR, e, "SQL exception while getting uptime: %s", e);
            return false;
        } finally {
            closeResultSet(rs);
        }
    }

    protected Map<String, Number> getValuesFromRS(ResultSet rs, Set<String> collectKeys) {
        Map<String, Number> values = null;
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            values = new HashMap<>(columnCount);
            while (rs.next()) {
                String keyValue = "";
                if(keyColumn != null) {
                    keyValue = rs.getString(keyColumn) + ".";
                    log(Level.TRACE, "found a row with key %s", rs.getString(keyColumn));
                }

                for(int i = 1; i <= columnCount; i++) {
                    String key = keyValue + meta.getColumnLabel(i);
                    if(!collectKeys.contains(key))
                        continue;
                    Number value;
                    Object oValue = rs.getObject(i);
                    log(Level.TRACE, "type info for %s: type %d, %s = %s", key, meta.getColumnType(i), oValue.getClass(), oValue);
                    if (oValue instanceof Number) {
                        value = (Number) oValue;
                        values.put(key, value);
                    } else {
                        int type = meta.getColumnType(i);
                        value = switch (type) {
                            case Types.DATE -> rs.getDate(i).getTime() / 1000;
                            case Types.TIME -> rs.getTime(i).getTime() / 1000;
                            case Types.VARCHAR -> Util.parseStringNumber(rs.getString(i), Double.NaN);
                            case Types.TIMESTAMP -> rs.getTimestamp(i).getTime() / 1000;
                            default -> Double.NaN;
                        };
                        values.put(key, value);
                    }
                }
            }
        } catch (SQLException e) {
            log(Level.ERROR, e, "SQL exception while getting values: ", e);
        }
        log(Level.TRACE, "values found: %s", values);
        return values;
    }

    @Override
    public String getSourceType() {
        return "JDBC";
    }

    @Override
    public Integer getPort() {
        return 0;
    }

    @Override
    public URL getUrl() {
        URL newurl = null;
        try {
            newurl = new URL(getUrlAsString());
        } catch (MalformedURLException e) {
            log(Level.ERROR, e, "Invalid jdbc url: " + getUrlAsString());
        }
        return newurl;
    }

    @Override
    public String getUrlAsString() {
        return getConnection().getUrl();
    }

    @Override
    public String getIndexName() {
        return index;
    }

}
