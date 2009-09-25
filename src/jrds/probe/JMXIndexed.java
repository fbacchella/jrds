package jrds.probe;


public class JMXIndexed extends JMX implements IndexedProbe {
	
	String index;
	
	public boolean configure(String index) {
		this.index = index;
		return configure();
	}

	public String getIndexName() {
		return index;
	}

}
