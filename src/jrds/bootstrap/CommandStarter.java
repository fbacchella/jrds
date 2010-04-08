package jrds.bootstrap;

import java.util.Properties;

public interface CommandStarter {
	public void configure(Properties configuration);
	public void start() throws Exception;
}
