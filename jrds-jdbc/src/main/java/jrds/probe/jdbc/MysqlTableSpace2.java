package jrds.probe.jdbc;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.slf4j.event.Level;

import jrds.Util;
import jrds.factories.ProbeBean;
import lombok.Getter;
import lombok.Setter;

@ProbeBean({"table"})
public class MysqlTableSpace2 extends GenericJdbcProbe {

    @Getter @Setter
    String table;

    @Override
    public Map<String, Number> getValuesFromRS(ResultSet rs, Set<String> collectKeys) {
        try {
            Map<String, Number> collected = new HashMap<>(collectKeys.size());
            collectKeys.forEach(k -> collected.put(k, 0d));
            BiFunction<String, Number, Double> recompute = (k, v) -> recompute(k, v, rs);
            while (rs.next()) {
                collectKeys.forEach(k -> collected.computeIfPresent(k, recompute));
            }
            return collected;
        } catch (SQLException | UndeclaredThrowableException ex) {
            log(Level.ERROR, ex,"Failed to parse sql result: " + Util.resolveThrowableException(ex));
            return Collections.emptyMap();
        }
    }

    private Double recompute(String k, Number v, ResultSet rs) {
        try {
            return v.doubleValue() + rs.getDouble(k);
        } catch (SQLException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @Override
    public String getUrlAsString() {
        return super.getUrlAsString() + "/" + table;
    }

}
