/* SoccerPhysics.java
   This class simulates ball and player movements in the Soccer Game
   
   Copyright (C) 2004  Yu Zhang
   Amendments by Vadim Kyrylov
   since 2006

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
*/
package soccer.server;

import java.util.Enumeration;
import java.util.Vector;

import soccer.common.RefereeData;
import soccer.common.TOS_Constants;
import soccer.common.Util;
import soccer.common.Vector2d;

/**
 * This class implements a simplified physics model of the soccer game.
 * Note that in current simulation step, player can either dash or 
 * kick the ball; but not both. This rule is enforced elsewhere by 
 * allowing just one action per step. 
 * Besides, same Splayer data members are used for the action 
 * force and its direction. 
 * TODO: different variables should be used to remove confusion.
 * 
 * @author Yu Zhang
 * amendments by Vadim Kyrylov
 * since 2006
 *
 */
public class SoccerPhysics {	 
	
	private SoccerServerWorld world;
	private Sball 		ball;
	private Vector<Splayer>	bothTeams;
	private int 		stepID;			// is used for debug print only
	private int 		gamemode;		// game mode
	private boolean 	keepLeftTeamOff;	// if true, keep left team off the ball
	private boolean 	keepRightTeamOff;	// if true, keep right team off the ball
	private double 		sumOfBallX = 0;		// sum of ball x-coordinate in current game
	
	private double xp, yp; 		// player coordinates (shared by many methods)
	
	/**
	 * Constructor
	 * @param world
	 */
	public SoccerPhysics(SoccerServerWorld world) {
		this.world = world;
	}

	//---------------------------------------------------------------------------
	/**
	 * Enforce rules of physics on current simulation step
	 */
	public void apply( 	int stepID, // so far is used for debugging only
						int mode, 
						boolean leftOff, 
						boolean rightOff, 
						int period ) {
		
		ball = world.getBall();
		bothTeams = world.bothTeams;
		
		this.stepID = stepID;
		this.gamemode = mode;
		this.keepLeftTeamOff = leftOff;
		this.keepRightTeamOff = rightOff;
		
		// this is a group of players trying to to take the control of 
		// the ball; they are all close enough to the ball to kick it;
		// who eventually gets the ball, is decided randomly.
		Vector<Splayer> fighters = new Vector<Splayer>();

		boolean isGoalieFound = false;;
		Enumeration<Splayer> players = bothTeams.elements();				
		while (players.hasMoreElements()) {
			Splayer player = (Splayer) players.nextElement();
			// update player state and populate fighters
			if (updatePlayerState( player, fighters ))
				isGoalieFound = true; // goalie found among fighters
		}

		if (!isGoalieFound)
			ball.isGrabbed = false;	// clear flag (because ball is not grabbed)
		
		// use one randomly selected fighter to update the ball state
		updateBallState( determineBallController( fighters ) );
		// gather statistics
		sumOfBallX = sumOfBallX + ball.getPosition().getX();

		if (period == RefereeData.PRE_GAME
				|| period == RefereeData.HALF_TIME) {
			ball.getPosition().setXY(0, 0);
		}	
		
		// clear dash/kick force for each player; it could be renewed 
		// in the next cycle by the players if the decide to do so
		players = bothTeams.elements();				
		while (players.hasMoreElements()) {
			Splayer player = (Splayer) players.nextElement();
			player.setForce(0);
			player.setForceDir(0);
		}
	}

	/**
	 * This method updates player state vector. Also it updates the list 
	 * of players, if any, who are fighting for the ball and figures it 
	 * out if the goalie is among these players
	 * 
	 * @param player - given player
	 * @param fighters - the list of players who are fighting for the ball
	 * @return - true if given player is a goalie and is among the fighters
	 */
	private boolean updatePlayerState( Splayer player, Vector<Splayer> fighters ) {
		
		//System.out.println(stepID + " updatePlayer " + player.id + "-" + player.side
						//+ " size = " + bothTeams.size() );
		
		boolean goalieFound = false;
		
		// find out if player can have ball again after he kick it out
		int count1 = player.getNoBallCount();
		if (count1 > 0)
			player.setNoBallCount(count1-1);

		// find out if player can communicate again after sending a verbal message
		int count2 = player.getNoWordCount();
		if (count2 > 0)
			player.setNoWordCount(count2-1);

		// find out if player can dash again after he kick it out
		int count3 = player.getNoDashCount();
		if (count3 > 0)
			player.setNoDashCount(count3-1);

		// the player's next position if nothing else happens
		// (used in several methods)
		xp = player.getPosition().getX() + player.getVelocity().getX();
		yp = player.getPosition().getY() + player.getVelocity().getY();

		// select players who can potentially take the control of the ball
		if (ball.getPosition().distance(xp, yp) < TOS_Constants.BALLCONTROLRANGE) {
			if ( player.getNoBallCount() == 0 	// player can kick the ball now
					|| (player.isGoalie() && ball.isGrabbed) ) {
				if (player.isGoalie()) 
					goalieFound = true; 
				fighters.addElement(player);
			}
			player.setWithBallCount(player.getWithBallCount() + 1);
		} else {
			player.setWithBallCount(0); 
		}
		// the flag to indicate if there's collision between players;
		// is null if no collision
		Splayer playerCollidedWith = updatePlayerPosition( player );
		
		// accelerate or decelerate player
		updatePlayerVelAcc( player, playerCollidedWith );	
		
		return goalieFound;
	}


	/**
	 * This method updates player position using global variables xp, yp;
	 * it returns another player, if any, whom this player has collided with.
	 * 
	 * @param player
	 * @return
	 */
	private Splayer updatePlayerPosition( Splayer player ) {
		// adjust xp,yp if necessary
		adjustXYOffBall( player.getSide() );
		adjustXYOutsideField();
		
		// set the new player position     
		player.getPosition().setXY(xp, yp);
		
		return getPlayerCollidedWith( player, bothTeams );
	}
	
	
	/**
	 * This method adjusts player position (xp, yp) if it must be kept off the ball
	 * 
	 * @param side
	 */
	private void adjustXYOffBall( char side ) {
		// if the player is not allowed to be close to ball
		double dist = world.getBall().getPosition().distance(xp, yp);
		
		boolean enforceOffBall = ( side == 'r' && keepRightTeamOff 
									|| side == 'l' && keepLeftTeamOff );
		
		if (dist < TOS_Constants.RADIUS && enforceOffBall) {
			double x1 =
				(xp - world.getBall().getPosition().getX()) * TOS_Constants.RADIUS / dist;
			double y1 =
				(yp - world.getBall().getPosition().getY()) * TOS_Constants.RADIUS / dist;
			xp = world.getBall().getPosition().getX() + x1;
			yp = world.getBall().getPosition().getY() + y1;
		}
	}
	
	/**
	 *  this method adjusts player position (xp, yp) if it attempts to 
	 *  leave the field; player is not allowed to run too far away 
	 *  (which might be caused by bugs in the player software application);
	 *  this method just adds some robustness 
	 */
	private void adjustXYOutsideField() {
		// if the player is moving outside of the soccer field
		if (Math.abs(xp) > TOS_Constants.LENGTH / 2 + TOS_Constants.SIDEWALK - 1) {
			if (xp > 0)
				xp = TOS_Constants.LENGTH / 2 + TOS_Constants.SIDEWALK - 1;
			else
				xp = - (TOS_Constants.LENGTH / 2 + TOS_Constants.SIDEWALK - 1);
		}
	
		if (Math.abs(yp) > TOS_Constants.WIDTH / 2 + TOS_Constants.SIDEWALK - 1) {
			if (yp > 0)
				yp = TOS_Constants.WIDTH / 2 + TOS_Constants.SIDEWALK - 1;
			else
				yp = - (TOS_Constants.WIDTH / 2 + TOS_Constants.SIDEWALK - 1);
		}		
	} 
	


	/**
	 * This method returns the other player if given player collides with him.
	 * If there is no collision, null is returned.
	 **/
	private Splayer getPlayerCollidedWith( Splayer thisPlayer, Vector<Splayer> team) {
		Splayer otherPlayer = null;	
		double minDistance = Float.MAX_VALUE; // still very big number 
		Enumeration<Splayer> players2 = null;	
		players2 = team.elements();
		
		// find the nearest player in given team
		while (players2.hasMoreElements()) {
			Splayer player2 = players2.nextElement();
			boolean myself = (thisPlayer.getId() == player2.getId()) 
				  			&& (thisPlayer.getSide() == player2.getSide());
			if ( !myself ) {
				double d = thisPlayer.getPosition().distance(player2.getPosition());
				if ( minDistance > d ) {
					minDistance = d; 
					otherPlayer = player2;
				}
			}
		}

		if ( minDistance < TOS_Constants.COLLIDERANGE) 
			return otherPlayer;
		else
			return null;
	}

	/**
	 * This method increments player velocity given its acceleration.
	 * In doing so, it applies limit to the player maximal angular
	 * speed.
	 * Because we use simplified physics that neglects player 
	 * rotation momentum, we address this by setting this upper limit.
	 * 
	 * @param player
	 */
	private void calcPlayerVelocity(Splayer player) {
		
		// calculate unrestricted new velocity on the next step
		Vector2d newVel = new Vector2d(player.getVelocity());
		newVel.add(player.getAcceleration());
		double newSpeed = newVel.norm();
		// calculate player direction angle change
		double angleChange = Vector2d.polar_dir(newVel) - player.getDirection(); 
		angleChange = Util.normal_dir(angleChange);
		double sign = Math.signum(angleChange);
		angleChange = Math.abs(angleChange);
		
		// apply upper limit to the direction change rate
		if (angleChange > TOS_Constants.maxDirChangePerCycle) {
			// keep same speed but restrict turn
			double newDir = Util.normal_dir(player.getDirection() 
							+ sign*TOS_Constants.maxDirChangePerCycle);
			newVel.setX(newSpeed*Math.cos(Math.toRadians(newDir)));
			newVel.setY(newSpeed*Math.sin(Math.toRadians(newDir)));
		}
		player.setVelocity(newVel);
	}

	/**
	 *  this method updates player velocity and acceleration with
	 *  respect to possible collision with another player
	 * @param player
	 * @param playerCollidedWith
	 */
	private void updatePlayerVelAcc( Splayer player, Splayer playerCollidedWith ) {
		
		if (playerCollidedWith == null) 
			world.setCollision(false);
		else
			updatePlayerVelAccInCollision(player, playerCollidedWith);
			
		/**
		 * Increment player velocity and apply upper limit to player angular 
		 * speed; this limit only kicks in when the player is moving 
		 * slowly. At higher speed, player inertia naturally limits 
		 * the angular speed.
		 */ 
		calcPlayerVelocity(player);
		
		// if the ball has been grabbed by the goalie, keep this player away 
		// from the ball (except the goalie himself)
		if ( world.getBall().isGrabbed && !player.isGoalie() ) {
			double myRadius = TOS_Constants.RADIUS/1.5;
			double dist = player.getPosition().distance( world.getBall().getPosition() );
			if (  dist < myRadius ) {
				double x1 =
					(xp - world.getBall().getPosition().getX()) * myRadius / dist;
				double y1 =
					(yp - world.getBall().getPosition().getY()) * myRadius / dist;
				double xnew = world.getBall().getPosition().getX() + x1;
				double ynew = world.getBall().getPosition().getY() + y1;
				player.getPosition().setXY( xnew, ynew );
			}
		} 	

		if (playerCollidedWith == null) {
			applyDashingToPlayerAcc(player);
			// set the upper limit of 100% the maximal speed
			limitPlayerSpeed(player, 1.0);	
		} else {
			// set the upper limit of 25% the maximal speed
			limitPlayerSpeed(player, 0.25);		
		}
	}

	/**
	 * This method sets upper limit on player speed; it just adds some
	 * robustness by mitigating possible calculation errors elsewhere.
	 * 
	 * @param player
	 * @param factor - must be between 0 and 1;
	 * 			standard limit applies if equals 1
	 */
	private void limitPlayerSpeed(Splayer player, double factor) {
		double speed = player.getSpeed()/TOS_Constants.SIM_STEP_SECONDS;
		double limit = factor*TOS_Constants.PLAYERMAXSPEED;
		if (player.getSpeed() > limit) {
			player.getVelocity().timesV(limit/speed);
			// player speed is measured in distance per cycle.
			speed = TOS_Constants.PLAYERMAXSPEED*TOS_Constants.SIM_STEP_SECONDS;
			player.setSpeed(speed);
		}		
	}
	

	/**
	 * This method updates player acceleration if he is dashing
	 * @param player
	 */
	private void applyDashingToPlayerAcc(Splayer player) {
		
		double force;	// force to move the dashing player 
		
		// decrease force if player is close to ball (dribbling slows him down)
		if (player.isWithBall() ) {
			if (player.isGoalie() && ball.isGrabbed)
				// the goalie who has grabbed the ball
				force = player.getForce() * 1.00;	// do not slow him down
			else
				force = player.getForce() * TOS_Constants.DRIBBLEFACTOR;
		} else {
			force = player.getForce();
		}
		// decrease force if player has kicked ball recently (still can make turns)	
		// noDashCount slows down dribbling so that the chasing 
		// opponent player without ball could reach
		if (player.getNoDashCount() > 0)
			force = Math.min(force, TOS_Constants.MV_FORCE_SMALL);

		if ( !player.isKickBall()) {
			// set player acceleration, a1 = (force * k1 - v0 * k2) * (1 +/- 0.05);
			// add between -DASHRANDOM and +DASHRANDOM percent to the dash force
			player.getAcceleration().setX(
				(force
					* Math.cos(Math.toRadians(player.getForceDir()))
					* TOS_Constants.K1
					- player.getVelocity().getX() * TOS_Constants.K2)
					* (1 + 2 * (Math.random() - 0.5) * TOS_Constants.DASHRANDOM));

			player.getAcceleration().setY(
				(force
					* Math.sin(Math.toRadians(player.getForceDir()))
					* TOS_Constants.K1
					- player.getVelocity().getY() * TOS_Constants.K2)
					* (1 + 2 * (Math.random() - 0.5) * TOS_Constants.DASHRANDOM));
		} else {
			// set player acceleration when force is 0, a1 = ( (- v0) * k2) * (1 +/- 0.05);
			// (this is deceleration)
			player.getAcceleration().setX(
				(-player.getVelocity().getX() * TOS_Constants.K2)
					* (1 + 2 * (Math.random() - 0.5) * TOS_Constants.DASHRANDOM));

			player.getAcceleration().setY(
				(-player.getVelocity().getY() * TOS_Constants.K2)
					* (1 + 2 * (Math.random() - 0.5) * TOS_Constants.DASHRANDOM));

		}		
	}
	
	
	/**
	 * This method updates player velocity and acceleration if 
	 * he has collided with anybody else; bouncing is randomized
	 * 
	 * @param player
	 * @param playerCollidedWith
	 */
	private void updatePlayerVelAccInCollision( Splayer player, 
								Splayer playerCollidedWith) {
		//System.out.println("collision: player " + player.id + "-" + player.side );
		
		// bounce the player back if it collides; set velocity to zero and
		// apply some acceleration to the player in the opposite direction
		
		double oppositeDir = playerCollidedWith.getPosition().direction(player.getPosition());
		player.getVelocity().setXY(0, 0);
		
		// randomize direction and force to prevent deadlocks
		oppositeDir = Util.normal_dir(oppositeDir + 60*(Math.random() - 0.5));
		double force = TOS_Constants.COLLISION_ACCELERATION * Math.random();
		double accX = force * Math.cos(Math.toRadians(oppositeDir));
		double accY = force * Math.sin(Math.toRadians(oppositeDir));
		player.getAcceleration().setXY( accX, accY ); 
		
		// inform world if collision is near the ball
		if (player.getPosition().distance(ball.getPosition()) 
										< 2.5*TOS_Constants.BALLCONTROLRANGE ) {
			world.setCollision(true);
		} else {
			world.setCollision(false);
		}
		/*
		System.out.println(player.getId() + "-" + player.getSide() 
				+ " preventing collision with " + playerCollidedWith.getId() 
				+ "-" + playerCollidedWith.getSide());
		*/
	}

	/**
	 * This method returns the player, if any, who is to control the ball;
	 * if there are several potential controllers, just one is randomly selected;
	 * even if the player does not kick the ball (but can do so), this 
	 * player still could be selected
	 *  
	 * @param fighters
	 * @return
	 */
	private Splayer determineBallController(Vector<Splayer> fighters) {		
		Splayer player = null;
		if (fighters.size() > 0) {
			int controller = (int) Math.floor(Math.random() * fighters.size());
			player = (Splayer) fighters.elementAt(controller);
			ball.controllerType = player.getSide();
			ball.controllerId = player.getId();
			ball.isFree = false;
			player.setWithBall(true);
			/*
			if ( fighters.size() > 1 )
				System.out.println( stepID + " selected controller = " + controller 
							+ " of total " + fighters.size() 
							+ ", player.id = " + player.getId() + "-" + player.getSide() );
			*/
		} else {
			ball.isFree = true;
		}
		return player;
	}

	
	/**
	 * This method updates the state variables of the ball and
	 * of the player who kicked it.
	 *  
	 * @param ballPlayer - player who kicked the ball
	 */
	private void updateBallState( Splayer ballPlayer ) {		
		
		world.setBallKicked(false);
		
		if ( gamemode == RefereeData.OFFSIDE_L 
				|| gamemode == RefereeData.OFFSIDE_R ) {
			ball.getVelocity().setXY( 0, 0 );
			ball.getAcceleration().setXY( 0, 0 );
			ball.isGrabbed = false;
			return;
	//<=====			
		}
		
		//System.out.println(stepID + " ballPlayer = " + ballPlayer );
		
		if (ballPlayer != null) {
			
			//System.out.println( stepID + " ballPlayer.id = " + ballPlayer.id 
							//+ ", kicking: " + ballPlayer.isKickBall() + " noBall=" + ballPlayer.noBall );
			
			if ( ballPlayer.isKickBall() ) {
				// the player is kicking the ball; update ball state
							
				// (prevent this player from kicking the ball in another NOBALL seconds)
				ballPlayer.setNoBallCount((int) (TOS_Constants.NOBALL / TOS_Constants.SIM_STEP_SECONDS));

				// (prevent this player from dashing with full force in another NODASH seconds)
				ballPlayer.setNoDashCount((int) (TOS_Constants.NODASH / TOS_Constants.SIM_STEP_SECONDS));
				
				//--- on each kick, the ball stops
				ball.getVelocity().setXY(0, 0);	
				
				//--- calculate the new ball acceleration
				Vector2d ballAcceleration = null;				
				
				if (ballPlayer.getWithBallCount() < 200) {
					// add up to KICKRANDOM degrees to ball kick direction (plus or minus)
					double kickDir =
							2 * (Math.random() - 0.5) * TOS_Constants.KICKRANDOM
								+ ballPlayer.getForceDir();
					ballAcceleration = new Vector2d(
								ballPlayer.getForce()
										* Math.cos(Math.toRadians(kickDir))
										* TOS_Constants.BK1,
								ballPlayer.getForce()
										* Math.sin(Math.toRadians(kickDir))
										* TOS_Constants.BK1 );
				} else {
					// for the whole system robustness only: 
					// prevent this player from staying with the ball too long by placing 
					// the ball out of reach and setting randomized ball position 
					ballAcceleration = new Vector2d(0, 0);
					Vector2d ballDelta = new Vector2d(
							2*(Math.random() - 0.5)*TOS_Constants.BALLCONTROLRANGE, 
							2*(Math.random() - 0.5)*TOS_Constants.BALLCONTROLRANGE);
					ball.setPosition( Vector2d.add(ball.getPosition(), ballDelta) );
					ballPlayer.setWithBallCount(0);	
					System.out.println( stepID + " " + ballPlayer.getId() + "-" + ballPlayer.getSide() 
							+ " *** forced to release ball to prevent from looping ***");
				}
				ball.setAcceleration(ballAcceleration);
				
				if (!ball.isGrabbed)
					world.setBallKicked(true);
				//System.out.println( stepID + " Ball kicked:  v = " + ball.velocity );

			} else {	
				// player can but is not kicking the ball
				if ( ballPlayer.isGoalie() && ball.isGrabbed ) {
					// create a visual effect how the goalie carries the grabbed ball;
					// this is a cosmetic feature
					if ( Math.abs(ballPlayer.getPosition().getX()) 
							< TOS_Constants.LENGTH / 2 - TOS_Constants.BALLCONTROLRANGE &&
						Math.abs(ballPlayer.getPosition().getY()) 
							< TOS_Constants.WIDTH / 2 - TOS_Constants.BALLCONTROLRANGE ) {
						double angle = Math.random() * 2 * Math.PI;
						double xb = ballPlayer.getPosition().getX()
								+ (0.95*TOS_Constants.BALLCONTROLRANGE - 0.5) * Math.cos(angle);
						// protect from apparently forcing the ball outside the goal 
						// line without the goalie's fault 
						if (Math.abs(xb) >= TOS_Constants.LENGTH/2)
							xb = ballPlayer.getPosition().getX();
						ball.getPosition().setXY(xb, 							
							ballPlayer.getPosition().getY()
								+ (0.95*TOS_Constants.BALLCONTROLRANGE - 0.5) * Math.sin(angle));
						ball.getVelocity().setXY(0, 0);
						ball.getAcceleration().setXY(0, 0);
					}
				}
			}

			ballPlayer.setWithBall(false);	// reset before next cycle
			
		} else  {	
			// decelerate the ball that is under nobody's control; add some 
			// randomness to its motion
			
			ball.getPosition().add(ball.getVelocity());
			ball.getVelocity().add(ball.getAcceleration());
			ball.getAcceleration().setXY(
				(-TOS_Constants.FRICTIONFACTOR)
					* ball.getVelocity().getX()
					* (1 + 2 * (Math.random() - 0.5) * TOS_Constants.BALLRANDOM),
				(-TOS_Constants.FRICTIONFACTOR)
					* ball.getVelocity().getY()
					* (1 + 2 * (Math.random() - 0.5) * TOS_Constants.BALLRANDOM));
			
			if(ball.isGrabbed)
				System.out.println(stepID + " Now ball is free:  v = " + ball.getVelocity() );
			
			ball.isGrabbed = false;
		}
		//if ( fighters.size() > 0 )	
			//System.out.println("Leaving updateBallState. fighters.size() = " + fighters.size() );
	}

	public double getSumOfBallX() {
		return sumOfBallX;
	}

	public void setSumOfBallX(double sumOfBallX) {
		this.sumOfBallX = sumOfBallX;
	}

}

