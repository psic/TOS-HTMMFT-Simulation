/* Player.java
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

	Modifications by Vadim Kyrylov since 2006

*/

package soccer.common;

/**
 *  This class (together with super class) represents player state vector; 
 *  these data are transmitted from the server to clients and back.
 *  Also could be used for building the world model in clients.
 * 
 * @author Yu Zhang
 * Modifications by Vadim Kyrylov (2004-2011)
 */

public class Player extends MovingObj {

	// for each simulation step; p1 = p0 + V0; v1 = v0 + a0; 
	// a1 = (force * k1 - v0 * k2) * (1 +/- 0.05); to define the 
	// maximum speed, we have to satisfy
	// 0 = force * k1 - maxSpeed * k2;

	/**
   	* The player's side.
   	*/
	private char side;
  	/**
  	 * The player's number.
  	 */
  	private int id;
  	/**
  	 * true if controlled by user
   	*/
  	private boolean isUserControlled = false;
  	/**
  	 * viewer ID who controls this player
  	 */
  	private int controllerID;
  	/**
  	 * true if is grabbed with the mouse
   	*/
  	private boolean isGrabbed = false;
  	/**
  	 * true if this player is a goalie
  	 */
	private boolean isGoalie = false;
	/**
	 * true if this is the designated kick-off player
	 */
	private boolean isKicker = false;
 
	// true if the player is at Offside position
	private boolean isOffside = false; 
	
	//true  if the player has the ball under his control 
	private boolean isWithBall = false;
	
	//true  if the player is chasing the ball 
	private boolean isChasingBall = false;
	

  /**
   * Construct an empty player. 
   */
  public Player() {
    super();
	side = 'n';
    id = 0;
  }
  
  /**
   * Construct a player.
   *
   * @param side the player's side.
   * @param id   the player's number.
   * @param position the location of the player.
   * @param dir the  facing direction of the player.
   */
  public Player(char side, int id, Vector2d position, double dir)  {
    this.side = side;
    this.id = id;
    setPosition(new Vector2d(position));
    setDirection(dir);
  }

	public char getSide() {
		return side;
	}
	
	public void setSide(char side) {
		this.side = side;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public boolean isUserControlled() {
		return isUserControlled;
	}

	public void setUserControlled(boolean isUserControlled) {
		this.isUserControlled = isUserControlled;
	}

	public boolean isGrabbed() {
		return isGrabbed;
	}
	
	public void setGrabbed(boolean isGrabbed) {
		this.isGrabbed = isGrabbed;
	}

	public boolean isGoalie() {
		return isGoalie;
	}

	public void setGoalie(boolean isGoalie) {
		this.isGoalie = isGoalie;
	}

	public boolean isKicker() {
		return isKicker;
	}

	public void setKicker(boolean isKicker) {
		this.isKicker = isKicker;
	}

	public boolean isOffside() {
		return isOffside;
	}

	public void setOffside(boolean isOffside) {
		this.isOffside = isOffside;
	}

	public boolean isWithBall() {
		return isWithBall;
	}

	public void setWithBall(boolean isWithBall) {
		this.isWithBall = isWithBall;
	}

	public boolean isChasingBall() {
		return isChasingBall;
	}

	public void setChasingBall(boolean isChasingBall) {
		this.isChasingBall = isChasingBall;
	}

	public int getControllerID() {
		return controllerID;
	}

	public void setControllerID(int controllerID) {
		this.controllerID = controllerID;
	}  
	
	public String toString() {
		return "Player " + id + "-" + side + " pos=" + position 
				+ " dir=" + Util.round(getDirection(), 1);
	}

}
