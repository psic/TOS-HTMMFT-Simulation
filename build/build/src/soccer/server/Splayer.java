/* Splayer.java

   Copyright (C) 2001  Yu Zhang

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the 
   Free Software Foundation, Inc., 
   59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

	Modifications by Vadim Kyrylov 
							(2006-2010)
*/

package soccer.server;

import java.net.*;
import soccer.common.RWLock;
import soccer.common.Player;
import soccer.common.TOS_Constants;

/**
 *  This represents a player object in the server world model 
 *  (player properties, its state, and some behaviors).
 *  This is NOT what is transmitted over communication channels;
 *  only some parameters of the state vector are transmitted.
 *  
 *  Synchronization in some methods is required because updates to 
 *  player parameters may arrive asynchronously, from different threads
 *  
 *  @author Yu Zhang; Vadim Kyrylov (since 2006)
 */

public class Splayer extends Player {

	//--------------------  private state variables  -----------
	// networking properties
	private InetAddress address;
	private int port;

	private int 		noBallCount = 0;	// step counter between kicks
	private int 		noDashCount = 0;	// step counter between the last kick and next dash
	private int 		noWordCount = 0;	// step counter between verbal messages
	private int 		withBallCount = 0;	// step counter while the ball is kickable by this player
	private boolean 	isCoach = false;	

	// these locks are necessary to sychronize updates;
	// some synchronized state variables belong to super class
	private RWLock 	directionLock = null;
	private RWLock 	forceLock = null;
	private RWLock 	forceDirLock = null;
	private RWLock 	kickBallLock = null;
	private RWLock 	withBallLock = null;
	private RWLock 	messageLock = null;
	private RWLock  lastTimeLock = null;

	private double 	force = 0;		// dash/kick force actually used in simulation
	private double 	forceDir = 0;	// dash/kick force direction actually used in simulation
	private boolean kickBall = false; // true if the player is trying to kick the ball
	private String 	message = null; // the message from player client
	private int 	lastTime; // last time (step ID) when player was active  

	
	// constructor creating a dummy instance
	public Splayer() {
		super();
	}
	
	// primary constructor
	public Splayer( InetAddress address,
					int port,
					char side,
					int id,
					boolean isGoalie,
					boolean isKicker,
					int lastTime,
					boolean coach ) 	{
		super();
		
		this.setSide(side);
		this.setId(id);
		this.setGoalie(isGoalie);
		this.setKicker(isKicker);

		this.address = address;
		this.port = port;

		this.lastTime = lastTime;
		this.isCoach = coach;

		this.lastTimeLock = new RWLock();
		this.directionLock = new RWLock();
		this.forceLock = new RWLock();
		this.forceDirLock = new RWLock();
		this.kickBallLock = new RWLock();
		this.withBallLock = new RWLock();
		this.messageLock = new RWLock();
		//System.out.println("Locks created. directionLock=" + directionLock);

		if (side == 'l') {
			getPosition().setXY(
				-TOS_Constants.LENGTH / 4,
				3 * (5-id) );
			setDirection(0);
		} else {
			getPosition().setXY(
					TOS_Constants.LENGTH / 4,
				3 * (id-5) );
			setDirection(180);
		}
	}


	// reset Splayer attributes using variables from the Player object 
	public void assign( Player player ) {
		this.getPosition().setXY( player.getPosition() );	
		super.setDirection(player.getDirection());	// ignore synchronization
		this.getVelocity().setXY( 0, 0 );
		this.getAcceleration().setXY( 0, 0 );
	}

	
	// reset Splayer attributes using variables from the Splayer object 
	public void copy( Splayer splayer ) {
		this.getPosition().setXY( splayer.getPosition() );	
		super.setDirection(splayer.getDirection());	// ignore synchronization
		this.getVelocity().setXY( 0, 0 );
		this.getAcceleration().setXY( 0, 0 );
		setSide(splayer.getSide());
		setId(splayer.getId());
	}
	

    /**
     * ----------------  synchronized getters and setters  ----------------------
     * methods below use RWLock object to prevent from the simultaneous access to 
     * the locked variable by other threads before the operation is completed
     */
	
	/**
	 * @return
	 */
	public int getLastTime() {
		try {
			lastTimeLock.lockRead();
			return lastTime;
		} finally {
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
		} finally {
			lastTimeLock.unlock();
		}
	}

	/**
	 * override super class method
	 * @return
	 */
	public double getDirection() {
		try {
			directionLock.lockRead();
			return super.getDirection();
		} finally {
			directionLock.unlock();
		}
	}

	/**
	 * @return
	 */
	public double getForce() {
		try {
			forceLock.lockRead();
			return force;
		} finally {
			forceLock.unlock();
		}
	}

	/**
	 * @return
	 */
	public double getForceDir() {
		try {
			forceDirLock.lockRead();
			return forceDir;
		} finally {
			forceDirLock.unlock();
		}
	}

	/**
	 * @return
	 */
	public boolean isKickBall() {
		try {
			kickBallLock.lockRead();
			return kickBall;
		} finally {
			kickBallLock.unlock();
		}
	}

	/**
	 * @return
	 */
	public boolean isWithBall() {
		try {
			withBallLock.lockRead();
			return super.isWithBall();
		} finally {
			withBallLock.unlock();
		}
	}

	/**
	 * @return
	 */
	public String getMessage() {
		try {
			messageLock.lockRead();
			return message;
		} finally {
			messageLock.unlock();
		}
	}

	/**
	 * override super class method
	 * @param d
	 */
	public void setDirection(double d) {
		try {
			directionLock.lockWrite();
			super.setDirection(d);
		} finally {
			directionLock.unlock();
		}
	}

	/**
	 * @param d
	 */
	public void setForce(double d) {
		try {
			forceLock.lockWrite();
			force = d;
		} finally {
			forceLock.unlock();
		}
	}

	/**
	 * @param d
	 */
	public void setForceDir(double d) {
		try {
			forceDirLock.lockWrite();
			forceDir = d;
		} finally {
			forceDirLock.unlock();
		}
	}

	/**
	 * @param b
	 */
	public void setKickBall(boolean b) {
		try {
			kickBallLock.lockWrite();
			kickBall = b;
			//System.out.println(id + "-" + side + " setKickBall: " + kickBall);
		} finally {
			kickBallLock.unlock();
		}
	}

	/**
	 * @param b
	 */
	public void setWithBall(boolean b) {
		try {
			withBallLock.lockWrite();
			super.setWithBall(b);
			//System.out.println(id + "-" + side + " setKickBall: " + kickBall);
		} finally {
			withBallLock.unlock();
		}
	}

	/**
	 * @param string
	 */
	public void setMessage(String string) {
		try {
			messageLock.lockWrite();
			message = string;
		} finally {
			messageLock.unlock();
		}
	}

    /**
     * -------------------  "plain" getters and setters  ----------------------
     * (presumably, these variables are accessed from just one thread)
     */
	
	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getNoBallCount() {
		return noBallCount;
	}

	public void setNoBallCount(int noBallCount) {
		this.noBallCount = noBallCount;
	}

	public int getNoDashCount() {
		return noDashCount;
	}

	public void setNoDashCount(int noDashCount) {
		this.noDashCount = noDashCount;
	}

	public int getNoWordCount() {
		return noWordCount;
	}

	public int getWithBallCount() {
		return withBallCount;
	}

	public void setWithBallCount(int withBallCount) {
		this.withBallCount = withBallCount;
	}

	public void setNoWordCount(int noWordCount) {
		this.noWordCount = noWordCount;
	}

	public boolean isCoach() {
		return isCoach;
	}

	// this method is used for debugging
	public String toString() {
		String s = "Splayer side=" + getSide() + " id=" + getId()
			+ " position=" + getPosition() + "\n   velocity=" + getVelocity() + "\n" 
			+ "   isWithBall=" + isWithBall() + " isKickBall=" + isKickBall() 
			+ " noBallCount=" + noBallCount
		;	
		return s;
	}

}
