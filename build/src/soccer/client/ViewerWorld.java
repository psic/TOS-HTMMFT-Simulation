/** ViewerWorld.java
 * 
 * The soccer world as viewed by the viewer client.
 * 
 * Copyright (C) 2001  Yu Zhang
 * 
 * with modifications by Vadim Kyrylov 
 * January 2006.
*/

	
package soccer.client;

import java.util.Vector;
import soccer.common.Ball;
import soccer.common.ClientWorld;
import soccer.common.InitData;
import soccer.common.Player;
import soccer.common.TOS_Constants;
import soccer.common.Vector2d;
import soccer.common.ViewData;


public class ViewerWorld extends ClientWorld {
	
	// latest info from server
	private ViewData 	viewData = null;
	
	// ball pass info
	private Vector2d 	endPoint = new Vector2d();
	private int 		receiverID = -1;
	private char 		receiverSide = '?';
	
	// variables used to store ball and all player positions at previous step,
	// needed for calculating their velocities.
	private int previousTimeBallVelCalculated;
	private Vector2d previousBallPosition = new Vector2d();
	private int previousTimeLeftVelCalculated;
	private int previousTimeRightVelCalculated;
	private Vector<Vector2d> leftTeamPrevPositions = new Vector<Vector2d>(); 
	private Vector<Vector2d> leftTeamVelocities = new Vector<Vector2d>(); 
	private Vector<Vector2d> rightTeamPrevPositions = new Vector<Vector2d>(); 
	private Vector<Vector2d> rightTeamVelocities = new Vector<Vector2d>(); 
	
	
	public ViewerWorld(InitData init) {
		super();
		this.setInitData(init);  		
	}
	
	public ViewerWorld() {
		// fill with zero 2d vectors
		for (int i=0; i<TOS_Constants.TEAM_FULL; i++) {
			leftTeamPrevPositions.add(new Vector2d());
			leftTeamVelocities.add(new Vector2d());
			rightTeamPrevPositions.add(new Vector2d());
			rightTeamVelocities.add(new Vector2d());
		}
	}  
	

	//-------------------  getter and setters --------------------


	public ViewData getViewData() {
		return viewData;
	}

	public void setViewData(ViewData viewData) {
		this.viewData = viewData;
	}

	public Vector2d getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(Vector2d endPoint) {
		this.endPoint = endPoint;
	}

	public int getReceiverID() {
		return receiverID;
	}

	public void setReceiverID(int receiverID) {
		this.receiverID = receiverID;
	}

	public char getReceiverSide() {
		return receiverSide;
	}

	public void setReceiverSide(char receiverSide) {
		this.receiverSide = receiverSide;
	}
	//--------- setters with some data processing functionality  -----
	
	public void setBall(Ball ball) {
		super.setBall(ball);
		int deltaT = viewData.time - previousTimeBallVelCalculated;
		if (deltaT > 0) {
			// estimating ball velocity
			Vector2d ballVelocity  
					= Vector2d.subtract(getBall().getPosition(), previousBallPosition);
			getBall().setVelocity(ballVelocity); // ball speed is also updated 
			previousBallPosition.setXY(getBall().getPosition());
			previousTimeBallVelCalculated = viewData.time;
		}
	}
	
	public void setLeftTeam(Vector<Player> leftTeam) {
		super.setLeftTeam(leftTeam);
		// estimate velocities of all players in left team
		updatePlayerVelocities(leftTeam, previousTimeLeftVelCalculated,
					leftTeamPrevPositions, leftTeamVelocities);
		previousTimeLeftVelCalculated = viewData.time;
	}

	public void setRightTeam(Vector<Player> rightTeam) {
		super.setRightTeam(rightTeam);
		// estimate velocities of all players in right team
		updatePlayerVelocities(rightTeam, previousTimeRightVelCalculated,
					rightTeamPrevPositions, rightTeamVelocities);
		previousTimeRightVelCalculated = viewData.time;
	}

	/**
	 * this method updates player velocities in given team.
	 * 
	 * @return -- number of simulation steps between updates
	 */
	private int updatePlayerVelocities(Vector<Player> team, int previousTime,
					Vector<Vector2d> positions, Vector<Vector2d>velocities) {
		try {
			// estimate velocities of all players if only different simulation step
			int deltaT = viewData.time - previousTime;
			if (deltaT > 0) {
				for (int i=0; i<team.size(); i++) {
					Player plr = team.elementAt(i);		// current player state (except velocity)
					Vector2d pos = positions.elementAt(i);	// previous position
					Vector2d vel = velocities.elementAt(i);	// previous velocity
					
					Vector2d.subtract(plr.getPosition(), pos, vel);	// calculate velocity
					plr.setVelocity(vel);			// set current player velocity and speed
					pos.setXY(plr.getPosition());  // save current position
				}
			}
			return deltaT;
		} catch (NullPointerException npe) {
			// protect when no teams are not present yet
			return 0;
		}
	}
	
}
