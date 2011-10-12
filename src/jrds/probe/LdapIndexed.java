package jrds.probe;

import jrds.objects.probe.IndexedProbe;


public class LdapIndexed extends Ldap implements IndexedProbe {
	String index;
	
	public boolean configure(String index) {
		this.index = index;
		return true;
	}
	public String getIndexName() {
		return index;
	}
}
