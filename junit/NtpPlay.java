import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.junit.Test;


public class NtpPlay {
	@Test
	public void play() throws UnknownHostException, IOException {
		NTPUDPClient cl = new NTPUDPClient();
		
		TimeInfo ti = cl.getTime(InetAddress.getByName("0.europe.pool.ntp.org"));
		
		ti.computeDetails();
		
		System.out.println(ti.getMessage().getRootDelay());
	}
}
