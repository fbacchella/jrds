package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.Util;

/**
 * 
 * This class needs select privilege, so remember to set up something like that
 * : GRANT USAGE ON *.* TO monitor@'%' IDENTIFIED BY 'password';
 * 
 * @author Fabrice Bacchella
 *
 */
public class MysqlStatus extends Mysql {

    @Override
    public List<String> getQueries() {
        return Collections.singletonList("SHOW /*!50002 GLOBAL */ STATUS");
    }

    @Override
    public Map<String, Number> parseRs(ResultSet rs) throws SQLException {
        Map<String, Number> retValues = new HashMap<String, Number>(getPd().getSize());
        Set<String> toCollect = getPd().getCollectMapping().keySet();
        for(Map<String, Object> m: parseRsVerticaly(rs, false)) {
            for(Map.Entry<String, Object> e: m.entrySet()) {
                Double d = Double.NaN;
                // We only keep the data in data stores list
                if(toCollect.contains(e.getKey())) {
                    if(e.getValue() instanceof String)
                        d = Util.parseStringNumber((String) e.getValue(), Double.NaN);
                    retValues.put(e.getKey(), d);
                }
            }
        }
        return retValues;
    }
}
