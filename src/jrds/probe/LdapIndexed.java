package jrds.probe;


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
