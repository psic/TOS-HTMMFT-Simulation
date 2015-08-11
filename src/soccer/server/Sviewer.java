/**
 *  Sviewer.java  
 *  
 *  This class represents a viewer client as registered by the server 
 *  
 *    Copyright (C) 2001  Yu Zhang
 *    Modifications by Vadim Kyrylov
 *    2012
 *    
 */


package soccer.server;

import java.net.*;

import soccer.common.RWLock;

public class Sviewer {

	  // networking properties
	  public InetAddress address;
	  public int         port;
	
	  public int viewerId;		// own ID
	  public int playerID;		// user controlled player ID (if any)
	  public char playerSide;	// user controlled player side (if any)
	
	  public boolean coach = false;
	  
	  private RWLock lastTimeLock = null;
	  private int lastTime; // last time when viewer is active
	    
	  public Sviewer(InetAddress address, int port, int viewerId, int lastTime, boolean coach) {
	    this.address = address;
	    this.port = port;
	    this.viewerId = viewerId;
	    this.lastTime = lastTime;
		this.coach = coach;
		this.lastTimeLock = new RWLock();	
	  }
	    

	/**
	 * @return
	 */
	public int getLastTime() {
		try{
			lastTimeLock.lockRead();
			return lastTime;
		}
		finally {
			lastTimeLock.unlock();
		}	
	}
	
	/**
	 * @param i
	 */
	public void setLastTime(int i) {
		try {
			lastTimeLock.lockWrite();
			lastTime = i;
		}    	
		finally {
			lastTimeLock.unlock();
		}	
	}

}
