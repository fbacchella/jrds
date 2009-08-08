package jrds.bootstrap;

import java.util.Map;

public interface CommandStarter {
	public void configure(Map<String, String> configuration);
	public void start() throws Exception;
}
