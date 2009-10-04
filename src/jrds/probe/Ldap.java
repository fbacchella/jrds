package jrds.probe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;

import jrds.ProbeConnected;

public class Ldap extends ProbeConnected<String, Number, LdapConnection> {
	static final private Logger logger = Logger.getLogger(Ldap.class);

	public Ldap() {
		super(LdapConnection.class.getName());
	}

	@Override
	public Map<String, Number> getNewSampleValuesConnected(LdapConnection cnx) {
		String base = this.getPd().getSpecific("base");
		String filter = this.getPd().getSpecific("filter");

		DirContext dctx = cnx.getConnection();

		Set<String> fieldSet = new HashSet<String>();
		for(String collect : getCollectMapping().keySet()) {
			String[] parsed = collect.toString().split("\\?");
			String field = parsed[0];
			if(parsed.length == 2)
				field = parsed[1];
			fieldSet.add(field);
		}
		logger.trace("Attributes to collect "  + fieldSet);
		
		SearchControls sc = new SearchControls();
		String[] attributeFilter = fieldSet.toArray(new String[]{});
		sc.setReturningAttributes(attributeFilter);
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		sc.setReturningObjFlag(false);

		Map<String, Number> retValues = new HashMap<String, Number>();
		Map<String, String> collectMap = getPd().getCollectStrings();
		try {
			NamingEnumeration<?> results = dctx.search(base, filter, sc);
			while (results.hasMore()) {
				SearchResult sr = (SearchResult) results.next();
				String dn = sr.getName();
				for(Attribute a: jrds.Util.iterate(sr.getAttributes().getAll())) {
					String collectName = a.getID();
					if(! "".equals(dn))
						collectName = dn + "?" + collectName;
					logger.trace(collectName);
					String ds = collectMap.get(collectName);
					if(ds != null) {
						double val = jrds.Util.parseStringNumber(a.get().toString(), Double.class, Double.NaN).doubleValue();
						retValues.put(ds, val);
					}
				}
			}
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.trace(retValues);
		return null;
	}

	@Override
	public String getSourceType() {
		return "LDAP";
	}

}
