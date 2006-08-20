package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class MysqlStatus extends Mysql {
	static final private org.apache.log4j.Logger logger = Logger.getLogger(MysqlStatus.class);

	public MysqlStatus(int port, String user, String passwd) {
		super(port, user, passwd);
	}

	public MysqlStatus(String user, String passwd) {
		super(user, passwd);
	}

	@Override
	public List<String> getQueries() {
		return Collections.singletonList("SHOW STATUS");
	}

	@Override
	public Map<String, Number> parseRs(ResultSet rs) throws SQLException {
		Map<String, Number> retValues = new HashMap<String, Number>(getPd().getSize());
		Set<String> toCollect = getPd().getProbesNamesMap().keySet();
		for(Map<String, Object> m: parseRsVerticaly(rs, false)) {
			for(Map.Entry<String, Object> e: m.entrySet()) {
				Double d = Double.NaN;
				//We only keep the data in data stores list
				if(toCollect.contains(e.getKey())) {
					try {
						if(e.getValue() instanceof String)
							d = Double.parseDouble((String)e.getValue());
					} catch (NumberFormatException ex) {
						logger.error("Conversion problem with : " + e.getValue() + " for variable " + e.getKey() + " on probe " + this);
					}
					retValues.put(e.getKey(), d);
				}

			}
		}
		return retValues;
	}


}
