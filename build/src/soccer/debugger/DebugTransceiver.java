package soccer.debugger;

import soccer.common.Packet;
import soccer.common.Player;
import soccer.common.SeeData;
import soccer.common.Transceiver;
import soccer.common.Vector2d;

/**
 *  
 * This class replaces Transceiver in the debug mode when
 * no communication over network is taking place.
 * Rather, data exchange is done via a public data object declared
 * in class DebugMain.
 * One or more instances of this class belong to each stand alone 
 * application in the TOS bundle (in the debug mode they all are glued 
 * together in a monolithic debugger application).
 * 
 * @author vkyrylov
 *
 */

public class DebugTransceiver extends Transceiver {
	
	  private boolean isServer;
	  private DebuggerRun debugger;	

	  private int myport;				

	  /**
	   * This constructor creates a dummy transceiver that 
	   * does not communicate over the network. Therefore, it does 
	   * not need an InetAddress parameter at all.
	   * Rather, it places the received and sent packets in the
	   * debugger application.
	   *
	   * @param isServer 
	   * @param port is the client thread ID on the server
	   * @param debugger is a reference to the debugger application
	   */
	  public DebugTransceiver(boolean isServer, int port, DebuggerRun debugger) {
		  this.isServer = isServer;
		  this.debugger = debugger;
		  this.myport = port;
	  }
	 
		/**
		 * This method emulates sending a data packet by Transceiver.
		 * It adds the arrived packet to the pool.
		 * @param p
		 */
		public void send(Packet p) {
			if (p.packetType == Packet.SEE) {
				// round coordinates (this affects displaying them by the viewer)
				SeeData sd = (SeeData)p.data;
				roundSeeCoordinates(sd);
			}
			// add packet to the exchange buffer
			debugger.getPackets().add(p);
		}

		/**
		 * This method emulates receiving a data packet by Transceiver.
		 * It uses senderID to properly set the port ID for the server.
		 * The address field is not used.
		 * This method removes the packet, if any, from the pool and 
		 * returns it as the output. 
		 */
		public Packet receive() {
			Packet p = null;
			// read packets in the inverse order (first in, last out)
			for (int i=0; i<debugger.getPackets().size(); i++) {
				Packet p2 = debugger.getPackets().elementAt(i);
				// I receive packets sent to my port only
				if (p2.port == myport) {
					// this packet is for me; got it
					if (isServer) {
						// port is used to distinguish different clients
						p2.port = p2.senderIDdebug;
					}
					p = p2;
					debugger.getPackets().remove(p2);
					break;
			//<-----
				}
			}
			return p;
		}
 
		/**
		 * this stub just overrides method in the super class
		 */
		public void setTimeout(int timeout) { }
	 
		/**
		 * This method rounds xy-coordinates contained on the SEE data object
		 * 
		 * @param sd
		 */
		private void roundSeeCoordinates(SeeData sd) {
			for (int i=0; i< sd.leftTeam.size(); i++) {
				Player plr = sd.leftTeam.elementAt(i);
				double x = ((int)(100*plr.getPosition().getX() + 0.5))/100.0;
				double y = ((int)(100*plr.getPosition().getY() + 0.5))/100.0;
				plr.setPosition(new Vector2d(x, y));
			}
			for (int i=0; i< sd.rightTeam.size(); i++) {
				Player plr = sd.rightTeam.elementAt(i);
				double x = ((int)(100*plr.getPosition().getX() + 0.5))/100.0;
				double y = ((int)(100*plr.getPosition().getY() + 0.5))/100.0;
				plr.setPosition(new Vector2d(x, y));
			}
			double x = ((int)(100*sd.ball.getPosition().getX() + 0.5))/100.0;
			double y = ((int)(100*sd.ball.getPosition().getY() + 0.5))/100.0;
			sd.ball.setPosition(new Vector2d(x, y));						
		}
		
}
