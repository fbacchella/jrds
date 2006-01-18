/*-------------------------------------------------------------
 * $Id: $
 */
package jrds.standalone;

import java.io.IOException;
import java.net.InetAddress;

import jrds.probe.rstat.rstatClient;
import jrds.probe.rstat.statstime;
import jrds.probe.rstat.statsvar;

import org.acplt.oncrpc.OncRpcClient;
import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.OncRpcProgramNotRegisteredException;
import org.acplt.oncrpc.OncRpcProtocols;

/**
 * TODO what this type is good for ?
 * @author bacchell
 * @version $Revision: $
 */
public class RstatTest {
	public static void main(String[] args) {
		OncRpcClient client;
		 try {
		 	rstatClient c = new rstatClient(InetAddress.getByName("avalon.logres.fr"), OncRpcProtocols.ONCRPC_UDP);
		     client = c.getClient(); /*OncRpcClient.newOncRpcClient(
		         InetAddress.getByName("avalon.logres.fr"),
		         0x49678, 1,
		         OncRpcProtocols.ONCRPC_UDP);*/
		     //stats s = c.RSTATPROC_STATS_1();
		     //statsswtch sw = c.RSTATPROC_STATS_2();
		     statstime st = c.RSTATPROC_STATS_3();
		     statsvar sv = c.RSTATPROC_STATS_4();
		 } catch ( OncRpcProgramNotRegisteredException e ) {
		     System.out.println("ONC/RPC program server not found");
		     System.exit(0);
		 } catch ( OncRpcException e ) {
		     System.out.println("Could not contact portmapper:");
		     e.printStackTrace(System.out);
		     System.exit(0);
		 } catch ( IOException e ) {
		     System.out.println("Could not contact portmapper:");
		     e.printStackTrace(System.out);
		     System.exit(0);
		 }
	}
}
