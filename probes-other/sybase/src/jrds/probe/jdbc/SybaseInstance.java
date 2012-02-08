package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rrd4j.DsType;

import jrds.ProbeDesc;


/**
 * @author bacchell
 *
 * TODO 
 */
public class SybaseInstance extends Sybase {
    private static final ProbeDesc pd = new ProbeDesc(5);
    static {
        pd.add("process", DsType.GAUGE);
        pd.add("transactions", DsType.GAUGE);
        pd.addGraph("sybaseinstance");
    }

    /**
     * 
     */
    public void configure(String user, String passwd) {
        super.configure("master", user, passwd);
        setName("sybase-" + getPort());
    }

    /**
     * 
     */
    public void configure(Integer port, String user, String passwd) {
        super.configure(port,  "master", user, passwd);
        setName("sybase-" + getPort());
    }

    @Override
    public List<String> getQueries() {
        List<String> retValues = new ArrayList<String>(2);
        retValues.add("select count(*) transactions from master..syslogshold");
        retValues.add("select count(*) process from master..sysprocesses where suid > 0");
        return retValues;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Number> parseRs(ResultSet rs) throws SQLException {
        return (Map<String, Number>) this.parseRsHorizontaly(rs, true);
    }

}
