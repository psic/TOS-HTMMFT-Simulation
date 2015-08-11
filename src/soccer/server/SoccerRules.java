/* SoccerRules.java
   This class simulates rules used in the Soccer Game
   
   Copyright (C) 2004  Yu Zhang

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
package soccer.server;

import java.util.Enumeration;
import java.util.Vector;

import soccer.common.Game_Constants;
import soccer.common.RefereeData;
import soccer.common.TOS_Constants;
import soccer.common.Util;
import soccer.common.Vector2d;
import soccer.common.World;


/**
 * This class maintains the soccer game control logic.
 * 
 * @author Yu Zhang
 * 	modifications by Vadim Kyrylov (since 2006)
 */
public class SoccerRules {
	
    // number of cycles to ignore offside rule after the corner kick
	private static final int OFFSIDE_DELAY = 100;
	   // number of cycles allowed keeping the ball grabbed by the goalie 
	private static int MAX_GRABBED_STEPS 
		= (int)(TOS_Constants.MAX_GRABBED_TIME / TOS_Constants.SIM_STEP_SECONDS);
	private static int stepsInGame 
		= (int)(120.0*Game_Constants.HALF_TIME_MINUTES/TOS_Constants.SIM_STEP_SECONDS);	
	
	//***  Game state variables ***
	private int 		period 	= RefereeData.NO_GAME;
	private int 		mode 	= RefereeData.BEFORE_KICK_OFF;
	private int 		timer = 0; // before kick off timer

	// A flag to indicate that referee is going to signal
	// all players and viewers a change in the game mode
	private boolean 	refereeSignal = false;

	// TODO: these public variables should be declared as private and 
	// 		public getters and setters added
	
	// A flag to indicate that left team players must be
	// 10 meters away from the ball
	public boolean 		leftOff = true;
	// A flag to indicate that right team players must be
	// 10 meters away from the ball
	public boolean 		rightOff = true;
	public char 	sideToContinue; 	// the side who continues the game after the pause
	
	//--- game statistics (TODO: could be wrapped into a single data object)
	// offside parameters and statistics
	public int 		offsideCountL = 0;
	private int		offCntGameL = 0;	// count of offsides in current game (L)
	public int 		offsideCountR = 0;	
	private int		offCntGameR = 0;	// count of offsides in current game (R)
	public int 		sqr_offside_diff = 0;    // sum of squares of the offside difference
	public boolean 	offsideL = false; 	// one or more left players are at Offside position.
	public boolean 	offsideR = false; 	// one or more right players are at Offside position.	
	// score statistics (TODO: could be wrapped into a single data object)
	public int 		gameCount = 1;
	public int 		total_score_L = 0;    	// total left team's score
	public int 		total_score_R = 0;  	// total right team's score
	public int 		score_L = 0;    		// left team's score
	public int 		score_R = 0;  			// right team's score
	public int 		sqr_score_diff = 0;    	// sum of squares of the score difference
	// ball statistics
	public double 	sum_avgBallX = 0;	// overall sum of average ball x-coordinate
	public double 	sum_sqr_BallX = 0;	// sum of squares of avgBallX
	
	// key objects
	private SoccerServerWorld 	world;
	private SoccerPhysics 	soccerPhysics;
	private Sball 			ball;
	private Vector<Splayer> bothTeams;

	private int 	violatorID;				// ID of the player who violated the rules
	private int 	kickOffStepsToPause;	// wait this number of steps before continue
	@SuppressWarnings("unused")
	private int 	otherStepsToPause;	// wait this number of steps before continue
	
	/**
	 * Constructor
	 * 
	 * @param world
	 */
	public SoccerRules(SoccerServerWorld world) {
		//System.out.println("entered SoccerWorld");
		this.world = world;
		soccerPhysics = new SoccerPhysics(world);
		//sideToContinue = '?';
		sideToContinue = 'l';
		kickOffStepsToPause = (int)(TOS_Constants.KICK_OFF_TIME 
									/ TOS_Constants.SIM_STEP_SECONDS);
		otherStepsToPause = (int)(TOS_Constants.TWROW_IN_TIME 
									/ TOS_Constants.SIM_STEP_SECONDS);
	}

		
	//---------------------------------------------------------------------------
	
	/**
	 * This method enforces the soccer rules in current simulation cycle
	 */
	public void enforce( int stepID ) {
		
		ball = world.getBall();
		bothTeams = world.bothTeams;
		
		if ( enforceGameModes() ) { 
			// game interrupted; do nothing
		} else {
			// enforce the rules of the regular game play
			//sideToContinue = '?';
			sideToContinue = 'l';
			
			double x = ball.getPosition().getX();
			double y = ball.getPosition().getY();
			
			// enforce the situations when the ball gets outside the field
			if (x < -TOS_Constants.LENGTH / 2) {	
				enforceLeftGoalLine(stepID );
				refereeSignal = true;
			} else if (x > TOS_Constants.LENGTH / 2) {			
				enforceRightGoalLine(stepID );
				refereeSignal = true;				
			} else if (Math.abs(y) > TOS_Constants.WIDTH / 2) {				
				enforceThrowIns();
				refereeSignal = true;				
			} 
			
			if ( !refereeSignal ) {
				if ( ballGrabbedTooLong( stepID ) ) {
					
					System.out.println(stepID + " *** penalty: goalie '" 
						+ ball.controllerType + "' holding the ball for too long." );				
					// set the corner kick mode if the ball is grabbed for too long
					// (in the real soccer this rule is different)
					setCornerKick( y, world.sideGrabbedBall ); 
				
				} else if ( ballGrabbedOutsidePenaltyArea() ) {
				
					System.out.println(stepID + " *** penalty: goalie '"
						+ ball.controllerType + "' forced to release the grabbed ball." );				
					releaseGrabbedBall(); 	
					
				} else if ( TOS_Constants.OFFSIDERULE_ON ) {
					
					// ignore offside, if any, after the corner kick for awhile
					if ( (long)stepID - (long)world.stepCornerKickDecided 
										> (long)OFFSIDE_DELAY ) { 
						if ( isOffSideRuleViolated() ) {
							refereeSignal = true;
							if (offsideL) {
								offsideCountL++;
								offCntGameL++;
							} else if (offsideR) {
								offsideCountR++;
								offCntGameR++;
							}
						}
						// forget the corner kick, if any
						world.stepCornerKickDecided = -Integer.MAX_VALUE; 	
					}
				}
			}				
		}
		
		if ( refereeSignal )
			printRefereeInfo( mode, stepID );	// display debug info
			
		// apply the laws of physics
		soccerPhysics.apply(stepID, mode, leftOff, rightOff, period);
	}

	
	/**
	 * This method enforces standard game modes by deciding 
	 * if the game must be interrupted.
	 * It assigns values to several global variables determining
	 * the game state and gathers some statistics. 
	 * 
	 * @return - true if must be interrupted
	 */
	private boolean enforceGameModes() {
		
		boolean result = true;
		
		switch ( mode ) {
		
			case RefereeData.BEFORE_KICK_OFF: 
				
				try {
					// "before_kick_off" lasts for KICK_OFF_TIME seconds
					timer++;
					if ( timer > kickOffStepsToPause // "before_kick_off" ends
					 	|| world.replicationIsOn ) 	 // no delay if replication
					 {	
						timer = 0;
						if ( sideToContinue == 'l' ) { 
							mode = RefereeData.KICK_OFF_L;
							rightOff = true;
							leftOff = false;
						} else  if ( sideToContinue == 'r' ) {
							mode = RefereeData.KICK_OFF_R;
							rightOff = false;
							leftOff = true;
						}
						refereeSignal = true;
						teleportKicker( sideToContinue );
					} else {
						// keep all players off the ball on own side 
						ball.set(0, 0);
						rightOff = true;
						leftOff = true;
						teleportOffendingPlayers();
					}
				} catch (Exception e ) {
					// we may get here when not all clients have been comnnected
					System.out.println("Exception caught in enforceGameModes: " + e   );
					e.printStackTrace();
				}
			break;
		
			case RefereeData.KICK_OFF_L:
				//--- new game begins; gather some statistics
				// average ball x-coordinate in current game
				double 	avgBallX = soccerPhysics.getSumOfBallX()/stepsInGame;
				sum_avgBallX = sum_avgBallX + avgBallX;
				sum_sqr_BallX = sum_sqr_BallX + avgBallX*avgBallX;
				soccerPhysics.setSumOfBallX(0);
				// offside counts difference
				int offsideDelta = offCntGameL - offCntGameR;
				sqr_offside_diff = sqr_offside_diff + offsideDelta*offsideDelta;
				offCntGameL = 0;
				offCntGameR = 0;
				// no break here; next case continues
				
			case RefereeData.KICK_OFF_R:
				// set state for both left and right kick-off
				refereeSignal = true;
				mode = RefereeData.PLAY_ON;
				// allow all players approach the ball
				rightOff = false;
				leftOff = false;

			break;
			
			case RefereeData.CORNER_KICK_L:
			case RefereeData.THROW_IN_L:
			case RefereeData.GOAL_KICK_L:
			case RefereeData.OFFSIDE_R: 
			
				if (ball.controllerType == 'l') {
					mode = RefereeData.PLAY_ON;
					rightOff = false;
					leftOff = false;
					refereeSignal = true;
				} else {
					// wait until left player gets to the ball
					rightOff = true;
					leftOff = false;
				}
			break;
		
			case RefereeData.CORNER_KICK_R:
			case RefereeData.THROW_IN_R:
			case RefereeData.GOAL_KICK_R:
			case RefereeData.OFFSIDE_L: 
			
				if (ball.controllerType == 'r') {
					mode = RefereeData.PLAY_ON;
					rightOff = false;
					leftOff = false;
					refereeSignal = true;
				} else {
					// wait until right player gets to the ball
					rightOff = false;
					leftOff = true;
				}
			break;
			
			case RefereeData.PLAY_ON: 
				// this is the main mode; no enforcement is needed
				result = false;
			break; 
		}
		return result;
	}
	
	/**
	 * This method watches for the ball crossing the left goal 
	 * line and sets some global variables such as game mode and 
	 * the score
	 * 
	 * @param stepID
	 */
	private void enforceLeftGoalLine(int stepID ) {
		
		double y = ball.getPosition().getY();

		if (Math.abs(y) < TOS_Constants.GOAL_WIDTH / 2) {
			// right team scores
			if (period == RefereeData.FIRST_HALF
				|| period == RefereeData.SECOND_HALF) {
				total_score_R++;
				score_R++;
				System.out.println( "Right team scores the goal. " 
										+ total_score_L +":" + total_score_R );
			}
			mode = RefereeData.BEFORE_KICK_OFF;
			timer = 0;
			ball.set(0, 0);
			rightOff = false;
			leftOff = true;
			sideToContinue = 'r';
		} else if (ball.controllerType == 'l') {
			// corner kick by the right team
			mode = RefereeData.CORNER_KICK_R;
			world.stepCornerKickDecided = stepID; 
			ball.set(-TOS_Constants.LENGTH / 2 + 0.5, 
					Math.signum(y)*(TOS_Constants.WIDTH / 2 - 0.5));
			rightOff = false;
			leftOff = true;
			sideToContinue = 'r';
		} else if (ball.controllerType == 'r') {
			// goal kick by the left team 
			mode = RefereeData.GOAL_KICK_L;
			ball.set(-TOS_Constants.LENGTH / 2 + TOS_Constants.PENALTY_DEPTH, 
					Math.signum(y)*TOS_Constants.PENALTY_WIDTH / 2);
			rightOff = true;
			leftOff = false;
			sideToContinue = 'l';
		}
	}	
	
	
	/**
	 * This method watches for the ball crossing the right goal 
	 * line and sets some global variables such as game mode and 
	 * the score
	 * 
	 * @param stepID
	 */
	private void enforceRightGoalLine(int stepID) {
		
		double y = ball.getPosition().getY();

		if (Math.abs(y) < TOS_Constants.GOAL_WIDTH / 2) { 
			// left team scores
			if (period == RefereeData.FIRST_HALF
				|| period == RefereeData.SECOND_HALF) {
				total_score_L++;
				score_L++;
				System.out.println( "Left team scores the goal. " 
										+ total_score_L +":" + total_score_R );
			}
			mode = RefereeData.BEFORE_KICK_OFF;
			timer = 0;
			ball.set(0, 0);
			rightOff = true;
			leftOff = false;
			sideToContinue = 'l';
		} else if (ball.controllerType == 'l') {
			// goal kick by the right team 
			mode = RefereeData.GOAL_KICK_R;
			ball.set(TOS_Constants.LENGTH / 2 - TOS_Constants.PENALTY_DEPTH, 
					Math.signum(y)*TOS_Constants.PENALTY_WIDTH / 2);
			rightOff = false;
			leftOff = true;
			sideToContinue = 'r';
		} else if (ball.controllerType == 'r') {
			// corner kick by the left team 
			mode = RefereeData.CORNER_KICK_L;
			world.stepCornerKickDecided = stepID;
			ball.set(TOS_Constants.LENGTH / 2 - 0.5, 
					Math.signum(y)*(TOS_Constants.WIDTH / 2 - 0.5));
			rightOff = true;
			leftOff = false;
			sideToContinue = 'l';
		}
	}
	
	/**
	 * This method decides which team must throw the ball in and 
	 * sets some global variables such as game mode 
	 */
	private void enforceThrowIns() {
		
		System.out.println("enforceThrowIns: controllerType = " + ball.controllerType );
		 
		double x = ball.getPosition().getX();
		double y = ball.getPosition().getY();

		if (ball.controllerType == 'r') {
			// left team must throw in
			mode = RefereeData.THROW_IN_L;
			ball.set(x, Math.signum(y)*(TOS_Constants.WIDTH/2-0.5));
			world.throwInModeL = true;
			rightOff = true;
			leftOff = false;
			sideToContinue = 'l';
		} else if (ball.controllerType == 'l') {
			// right team must throw in
			mode = RefereeData.THROW_IN_R;
			ball.set(x, Math.signum(y)*(TOS_Constants.WIDTH/2-0.5));
			world.throwInModeR = true;
			rightOff = false;
			leftOff = true;
			sideToContinue = 'r';
		}
		System.out.println("   ball at " + ball.getPosition());
	} 
				
	/**
	 * This method returns true if the offside rule is violated. The rule 
	 * itself is enforced elsewhere. The text below just explains this rule.
	 * Note that the rule as described has some omissions that still should 
	 * be corrected in this program.  
	 * For the explanation of the official rules, view these videos:
	 * http://www.youtube.com/watch?v=JiwmR6CC0Bk 
	 * http://www.youtube.com/watch?v=jKxy45xqgSo&NR=1 
	 * 
	 * The offside rule concerns the attacking player without the ball  
	 * whose x-coordinate is maximal and only is enforced when his team 
	 * controls the ball. 
	 * His x-coordinate must meet one of the two conditions:
	 * (1) the ball x-coordinate must be between the player and 
	 * the opponent goal; 
	 * OR
	 * (2) if the ball is behind this player, at least two opponent  
	 * players must be between him and their goal; the opponent 
	 * goalie, if any, does count. 
	 * 
	 *  This rule is waived or relaxed in the following situations:
	 *  -- do not enforce the offside rule if the attacking player controls 
	 *  the ball even though his x-coordinate is greater than the ball 
	 *  x-coordinate (the ball is still staying within the control radius);
	 *  -- do not enforce this rule for OFFSIDE_DELAY cycles after 
	 *  the corner kick;
	 *  -- relax this rule accordingly if there are only two, or one, or 
	 *  zero players in the opponent team.  
	 *  
	 * It is meant that if this rule is violated, the server may send the 
	 * generated signal to all players about that some player occurred in
	 * an offside position; this still may not be an offense yet.
	 *  
	 * This reflects that in real life soccer, the assistant referee at 
	 * the side line raises the red flag when such situation occurs. All 
	 * players can see this flag. The game is interrupted by the referee 
	 * if only the team who violated the offiside rule gains control of 
	 * the ball (implemented elsewhere). 
	 */
	private boolean isOffSideRuleViolated()	{
		
		//System.out.println("entered enforceOffSideRule" );
		boolean isViolated = false; 
		// variable used to loop
		Enumeration<Splayer> players = null;
		Splayer player = null;
		double lastL, secondL, tmpL, posX;
		double lastR, secondR, tmpR;

		// determine the two rear defenders in each team
		lastL = TOS_Constants.LENGTH/2;
		secondL = TOS_Constants.LENGTH/2;
		lastR = -lastL;
		secondR = -secondL;

		players = bothTeams.elements();
		
		while (players.hasMoreElements()) {
			
			player = (Splayer) players.nextElement();
			
			if ( player.getSide() == 'l' ) {
				if ( player.getPosition().getX() < secondL ) {
					secondL = player.getPosition().getX();
					if (secondL < lastL) {
						tmpL = lastL;
						lastL = secondL;
						secondL = tmpL;
					}
				}
			} else if ( player.getSide() == 'r' ) {
				if ( player.getPosition().getX() > secondR ) {
					secondR = player.getPosition().getX();
					if (secondR > lastR) {
						tmpR = lastR;
						lastR = secondR;
						secondR = tmpR;
					}
				}
			}
		}
		
		// for each player in each team, decide if any is in offside position
		players = bothTeams.elements();
		offsideR = false;
		offsideL = false;
		violatorID = -1;
		
		while (players.hasMoreElements()) {
			player = (Splayer) players.nextElement();
			player.setOffside(false);
			posX = player.getPosition().getX();
			if ( player.getSide() == 'r' ) {
				if (posX < 0
					&& posX < secondL
					&& ball.getPosition().getX() < 0
					&& !ball.isFree
					&& posX < ball.getPosition().getX()) {
					player.setOffside(true);
					offsideR = true;
					if (ball.controllerType == 'r'
						&& ball.controllerId != player.getId()) {
						mode = RefereeData.OFFSIDE_R;
						isViolated = true;
						sideToContinue = 'l';
						violatorID = player.getId();
					}
				}
			} else if ( player.getSide() == 'l' ) {
				if (posX > 0
					&& posX > secondR
					&& ball.getPosition().getX() > 0
					&& !ball.isFree
					&& posX > ball.getPosition().getX()) {
					player.setOffside(true);
					offsideL = true;
					if (ball.controllerType == 'l'
						&& ball.controllerId != player.getId()) {
						mode = RefereeData.OFFSIDE_L;
						isViolated = true;
						sideToContinue = 'r';
						violatorID = player.getId();
					}
				}
			}
		}
		return isViolated;
	}
				
	/**
	 * returns true if the goalie had grabbed the ball too long time ago 
	 */
	private boolean ballGrabbedTooLong( int stepID ) {
		// this can be out of range for 'int'
		long stepsHoldingBall = (long)stepID - (long)world.stepBallWasGrabbed;
		
		//if ( stepsHoldingBall > 0 )
			//System.out.println(stepID + " ball grabbed at: " 
				//+ world.stepBallWasGrabbed + ", kept for " + stepsHoldingBall );
		return ( stepsHoldingBall	> (long)MAX_GRABBED_STEPS );	
	}

	
	/**
	 *  set the corner kick mode 
	 * @param y
	 * @param offenderSide
	 */
	private void setCornerKick( double y, char offenderSide ) {
		if ( offenderSide == 'r' ) {
			mode = RefereeData.CORNER_KICK_L;
			if (y > 0)
				ball.set(TOS_Constants.LENGTH / 2 - 1, TOS_Constants.WIDTH / 2 - 1);
			else
				ball.set(TOS_Constants.LENGTH / 2 - 1, -TOS_Constants.WIDTH / 2 + 1);
			rightOff = true;
			leftOff = false;
			sideToContinue = 'l';
		} else {
			mode = RefereeData.CORNER_KICK_R;
			if (y > 0)
				ball.set(-TOS_Constants.LENGTH / 2 + 1, TOS_Constants.WIDTH / 2 - 1);
			else
				ball.set(-TOS_Constants.LENGTH / 2 + 1, -TOS_Constants.WIDTH / 2 + 1);
			rightOff = false;
			leftOff = true;
			sideToContinue = 'r';
		}
		
		refereeSignal = true;
	}
	
	
	/** 
	 * this method returns true if the ball is grabbed 
	 * outside the penalty area (1 cm margin is still allowed)
	 * @return
	 */
	private boolean ballGrabbedOutsidePenaltyArea()
	{
		return ( ball.isGrabbed 
				&& !World.inPenaltyArea( 'l', ball.getPosition(), 0.01 )
				&& !World.inPenaltyArea( 'r', ball.getPosition(), 0.01 ) );
	}

	/** 
	 * this method forces the goalie to kick the ball; 
	 * the direction of the kick is random and the force is just moderate so 
	 * that this kick were kind of penalty. No referee signal is generated.
	 */
	private void releaseGrabbedBall() {
		
		ball.isGrabbed = false;
		world.stepBallWasGrabbed 
						= Integer.MAX_VALUE;	// forget ball was grabbed
		Splayer goalie 
			= world.getPlayer( ball.controllerType, ball.controllerId );
			
		if ( goalie != null ) {
			goalie.setForce(TOS_Constants.MAXKICK/3.0 );
			double kickDir = (Math.random() - 0.5) * 135.0; 
			if ( goalie.getSide() == 'r' )
				kickDir = Util.normal_dir( kickDir + 180 );
			goalie.setDirection( kickDir );
			goalie.setKickBall( true );	
			//System.out.println("releaseGrabbedBall: goalie side=" 
					//+ ball.controllerType + " id=" + ball.controllerId 
					//+ " kickDir = " + (float)kickDir );
		} else {
			System.out.println("Error in releaseGrabbedBall: goalie cannot be found");
		}
	}

	
	/** 
	 * this method moves the dedicated kicker closer to the ball in 
	 * the kickoff situation, thus creating conditions for kickoff 
	 * (assuming that the rest players are kept away in the meantime)
	 * 
	 * @param side
	 */
	private void teleportKicker( char side ) {
		Splayer player = getKicker( side );
		
		double x = 0;
		double d = 0; 

		if ( side == 'l' ) {
			x = -(TOS_Constants.BALLCONTROLRANGE + 0.1);
		} else if ( side == 'r' ) {
			x =  TOS_Constants.BALLCONTROLRANGE + 0.1;
			d =  180;
		}
		
		if ( side == 'l' || side == 'r' ) {
			player.getPosition().setXY( new Vector2d( x, 0 ) );
			player.setVelocity(new Vector2d());	// zero velocity
			player.setDirection(d); 
		}
		//System.out.println("Kicker teleported: side = " 
							//+ player.side + "  id = " + player.id);		
	}

	/** 
	 * TODO: not used yet
	 * this method moves the kicker closer to the ball for the 11m penalty kick 
	 * (assuming that the rest players are kept away in the meantime)
	 * 
	 * @param side
	 */
	@SuppressWarnings("unused")
	private void teleport11mKicker( char side ) {
		Splayer player = getKicker( side );
		
		double x;

		if ( side == 'l' ) {
			x = TOS_Constants.LENGTH/2 - 11 - 2.5;
		} else {
			x =  -(TOS_Constants.LENGTH/2 - 11 - 2.5);
		}
		
		player.getPosition().setXY( new Vector2d( x, 0 ) );
	}
	
	
	/** 
	 * this method returns the dedicated kicker from given side/team
	 * @param side
	 * @return
	 */
	private Splayer getKicker( char side ) {
		Splayer player = null;
		Splayer player9 = null;
		boolean kickerFound = false;
		
		int i = 0;
		for (i = 0; i < bothTeams.size(); i++ ) {
			player = (Splayer) bothTeams.elementAt( i );
			// pick the kicker having the smallest number	
			//System.out.println(i + " : " + player.isKicker());
			if ( player.isKicker() && player.getSide() == side ) {
				kickerFound = true;
				break;
			}
			// if there is no designated kicker, pick anybody with the greatest id<=9
			if ( player.getId() <= 9 && player.getSide() == side )
				player9 = player;
		}
		
		if ( kickerFound )
			return player;
		else 
			return player9;
	}
	
		
	/** 
	 * this method teleports players, if any, who are 
	 * violating the kicikoff rule
	 */
	private void teleportOffendingPlayers() {
		
		Enumeration<Splayer> players = null;
		Splayer player = null;
		double xOffset = 5.0; 
		
		players = bothTeams.elements();
		while (players.hasMoreElements()) {
			player = (Splayer) players.nextElement();
			if ( player.getSide() == 'l' ) {
				if ( player.getPosition().getX() > -0.1 )
					player.getPosition().setX( -xOffset );
			} else 	if ( player.getSide() == 'r' ) {
				if ( player.getPosition().getX() < 0.1 )
					player.getPosition().setX( xOffset );
			}
		}		
	}
	

	/** 
	 * TODO: not in use yet;
	 * this methods moves all players outside the penalty 
	 * area on the opposite side; goalies are not moved 
	 * 
	 * @param side
	 */
	@SuppressWarnings("unused")
	private void teleportFor11mPenaltyKick( char side ) {
		
		System.out.println("teleportFor11mPenaltyKick: side = " + side );		
		
		// teleport ball
		double x;
		char side2;	// the penalty kick will be on this side of the field

		if ( side == 'l' ) {
			x = TOS_Constants.LENGTH/2 - 11.0;
			side2 = 'r';
		} else {
			x =  -(TOS_Constants.LENGTH/2 - 11.0);
			side2 = 'l';
		}		
		ball.set(x, 0);
		
		// teleport players 
		Enumeration<Splayer> players = bothTeams.elements();
		Splayer player = null;
		int i = 0;
		
		while (players.hasMoreElements()) {
			player = (Splayer) players.nextElement();
			if ( !player.isGoalie() ) {
				if ( World.inPenaltyArea( side2, player.getPosition(), -0.1 ) ) {
					
					System.out.println("teleporting: id=" + player.getId() 
						+ " side=" + player.getSide() + " pos=" + player.getPosition() );
					
					if ( i%2 == 0 ) {
						if ( side2 == 'l' )
							x = TOS_Constants.LENGTH/2 - TOS_Constants.PENALTY_DEPTH;
						else
							x = -(TOS_Constants.LENGTH/2 - TOS_Constants.PENALTY_DEPTH);						
						player.getPosition().setX( x );
					} else { 
						double y;
						if ( player.getPosition().getY() > 0 )
							y =  TOS_Constants.PENALTY_WIDTH/2;
						else
							y = -TOS_Constants.PENALTY_WIDTH/2;						
						player.getPosition().setY( y );
					}
					
					System.out.println("new pos=" + player.getPosition() );
				}
			}
			i++; 
		}
	}



	//=========  public get/set methods  =========
	 
	public synchronized int getPeriod() {
		return period;
	}

	public synchronized void setPeriod(int i) {
		if ( period != i ) {
			refereeSignal = true; // as period has changed
			period = i;
			mode = RefereeData.BEFORE_KICK_OFF;
			timer = 0;
			if ( period == RefereeData.FIRST_HALF ) 
				sideToContinue = 'l';
			else if ( period == RefereeData.SECOND_HALF ) 
				sideToContinue = 'r';
			else
				sideToContinue = '?';
		}
	}

	 
	public synchronized int getMode() {
		return mode;
	}

	public synchronized void setMode(int i) {
		if ( mode != i ) {
			refereeSignal = true; // as mode has changed
			mode = i;
			timer = 0;
		}
	}

	public int getViolatorID() {
		return violatorID;
	}

	/** 
	 * this method sets the standard game state for the replication
	 */
	public void setReplicationMode() {
		period = RefereeData.FIRST_HALF;
		setMode( RefereeData.PLAY_ON );	
		rightOff = false;
		leftOff = false;
		sideToContinue = '?';
		timer = 0;
	}


	/** 
	 * print out the referee info
	 * @param mode
	 * @param stepID
	 */
	private void printRefereeInfo( int mode, int stepID ) {
		
		switch ( mode ) {
			case RefereeData.KICK_OFF_R:
				System.out.println( stepID + " RIGHT team kicks off. " );
			break;
			case RefereeData.KICK_OFF_L:
				System.out.println( stepID + " LEFT team kicks off." );
			break;
			
			case RefereeData.GOAL_KICK_R:
				System.out.println( stepID + " RIGHT team kicks from own goal." );
			break;
			case RefereeData.GOAL_KICK_L:
				System.out.println( stepID + " LEFT team kicks from own goal." );
			break;
			
			case RefereeData.PLAY_ON:
				//System.out.println( stepID + " Resume playing game." );
			break;
							
			case RefereeData.CORNER_KICK_R:
				System.out.println( stepID + " <<== corner kick on the LEFT side." );
			break;
			case RefereeData.CORNER_KICK_L:
				System.out.println( stepID + " ==>> corner kick on the RIGHT side." );
			break;
			
			case RefereeData.THROW_IN_R:
				System.out.println( stepID + " -- throw in by the RIGHT team." );
			break;
			case RefereeData.THROW_IN_L:
				System.out.println( stepID + " -- throw in by the LEFT team." );
			break;
			
			case RefereeData.OFFSIDE_R:
				System.out.println( stepID + " >>> Right team offside, player " + violatorID);
			break;
			case RefereeData.OFFSIDE_L: 
				System.out.println( stepID + " <<< Left team offside,player " + violatorID);
			break;
		}
	}


	public boolean isRefereeSignal() {
		return refereeSignal;
	}


	public void setRefereeSignal(boolean refereeSignal) {
		this.refereeSignal = refereeSignal;
	}
	
}
