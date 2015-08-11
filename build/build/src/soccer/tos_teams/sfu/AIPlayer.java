/* AIPlayer.java
   This class implements a soccer player who is gets 
   sensing info, plans its moves and executes the plan.

   Copyright (C) 2001  Yu Zhang
   modified by Vadim Kyrylov (2004 - 2010)

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
 *
 * by Vadim Kyrylov
 * (2006-2010)
 *
*/


package soccer.tos_teams.sfu;

import soccer.common.*;

import java.util.*;
import java.io.*;

import javax.swing.JOptionPane;

/**
 * This class implements a simulated soccer player.
 * It receives information from the server about the state of the world 
 * and generates  commands controlling this player. These commands
 * are communicated back to the server that executes them by updating the
 * state of the world.
 * 
 * In the debug mode the debugger applications stands between the 
 * player and the server; this feature is hidden in class Transceiver. 
 * 
 * The player transforms all coordinates in the received data so that he 
 * assumes that he is playing for the left-hand team. Therefore, the opponent   
 * is always perceived to be on the right. 
 * Before sending data back to the server, the player transforms all  
 * coordinates as appropriate. As these transformations are trivial, they    
 * do not create any noticeable overhead. The benefit is the significant 
 * simplification of all algorithms dealing with geometry.
 * 
 * NOTE: this class is best viewed from the soccer player agent standpoint.
 *       So you will find comments like "I player", "Should I do something", etc. 
 *    
 * @author Vadim Kyrylov
 */

/**
 * In this version, many player features are disabled. 
 * Students are encouraged to develop their own. 
 */

public class AIPlayer {
	
	// these objects are used for the communication with the server
	private Transceiver transceiver;
	protected DriveData 	aDriveData;
	protected	KickData 	aKickData;
	protected BallPassData aBallPassData;
	
	// the state of the world as perceived by the player 
	protected PlayerWorld	 playerWorld;	
	protected WorldData    worldData; 		// the visual info about the world
	private int 		 lastTimeSeeUpdated;
	private boolean 	 iAmAtOffsideRisk; 
	
	// These variables are used for collecting statistics about lost packets; 
	// A lost packet is a lost opportunity for the agent to act in curent cycle. 
	// (Packets could be lost if computations are too complex or 
	// the computer is too slow.)
	private static final int  MODULUS = 1000;
	private	int 		receivedPacketCount = 0;	
	private	int 		previousReceivedPacketID = -1;	
	private	int 		lostPacketCount = 0;
	private double 		lostPacketFactor = 0;
	private long 		timeBefore;
	// time interval in steps to print out the idling time statistics
	private static final int REPORT_STEPS_NUM = 10000;
	// a very small number
	private static final double EPS6 = 1e-9;	
	// a very, very small number
	protected static final double EPS9 = 1e-6;	
	// this variable is used for calculating the idle time of this thread
	private double		processingTime = 0;
	// this is used to prevent from playing sound too frequently
	protected long 		lastSoundPlayedMS;
	protected static final int  SOUNDDELAYMS = 1500;
	
	// household/cosmetic/debug items
	private String name;	// object name 
	private boolean isDebugMode = false;
	
	/**
	 * This constructor is only used in the regular (threaded) mode
	 */
	public AIPlayer(String name) {
		this.name = name;
	}

	/**
	 * This constructor is only used with the Debugger.
	 * It initializes all necessary class variables for 
	 * running the AIPlayer object in a non-threaded mode
	 */
	public AIPlayer(Transceiver transceiver, char side, int id, 
						boolean amIGoalie, Formation formation) {
		this.isDebugMode = true;
		this.name = "Player-" + id + "-" + side;
		System.out.print("Debug starting " + name);
		this.transceiver = transceiver;
		if ( amIGoalie )
			System.out.println("  ** goalie **");
		else 
			System.out.println();
		this.playerWorld = new PlayerWorld( transceiver, side, id, amIGoalie );
	}
	
	
	/**
	 * This method gets a data packet from server, processes it, 
	 * makes decision, and sends the action command back.
	 * 
	 * This method is declared protected to provide access by the 
	 * threaded sub class in the regular mode.
	 */
	protected void runOneStep() {
		Packet p = receiveInfoAndUpdate();
		if (p != null) {
			if (playerWorld.getTime() == lastTimeSeeUpdated 
						&& playerWorld.getGameMode() == RefereeData.PLAY_ON) {
				// do nothing;
				// prevent from sending action commands more than once per 
				// simulation step (they would be ignored by the server anyway)
				System.out.println("AI player sending nothing : " + playerWorld.getGameMode());
			} else if (worldData.getMe().isUserControlled()) {
				// do virtually nothing;
				// because this player is controlled by human, the server would 
				// not process any action commands sent by this thread;
				// only send EMPTY packet from time to time to inform server that 
				// this thread is still alive
				if (playerWorld.getTime()%50 == 0) {
					Packet emptyP = new Packet(Packet.EMPTY, new EmptyData());
					try {
						worldData.send(emptyP);
					} catch (IOException e) { }
				}
			} else {
				// save time new visual info received from server
				lastTimeSeeUpdated = playerWorld.getTime();
				planAndExecute(p);	
			}
		}
	}
	
	/**
	 * This method receives one data packet and updates the 
	 * perceived state of the world. 
	 * 
	 * This method is declared public to provide access by the 
	 * debugger application in the debug mode. 
	 * 
	 * @return - the received packet (could be null in debug mode)
	 */
	public Packet receiveInfoAndUpdate() {
		
		Packet receivedPacket = null;
		
		try {				
			// I sense the world state 
			receivedPacket = transceiver.receive();
	        
	        // memorize the time before computations
	        timeBefore = System.currentTimeMillis();
	        // idle time includes waiting for the packet to arrive 
	        // (most part of the time) and receiving it (a small fraction)
	
	        // (null may happen in the debug mode)
	       // System.out.println(receivedPacket.writePacket());
	        if (receivedPacket != null) {	        	
        		/** 
        		 * Checking if the server decided to quit simulation; if this is 
        		 * the case, the whole Player Client application must be terminated.
        		 */
	        	if (receivedPacket.packetType == Packet.BYE) {
	        		ByeData byeData = (ByeData)receivedPacket.data;
	        		if (byeData.actionType == ByeData.TERMINATE) {
	        			if (!SoccerTeamMain.isTerminated) {
	        				// this is the first thread that has ever received the termination BYE
	        				// prevent other threads from doing same by setting isTerminated to true
	        				SoccerTeamMain.isTerminated = true;	
							System.out.println("--- Server terminated this Player Client. See message box.");
							// make a pause to prevent from user confusion
		        			JOptionPane.showMessageDialog(null,
		        					"All threads in this Player Client terminated by server", 
		        					"Forced Termination. Thread " + getName(),
		        					JOptionPane.PLAIN_MESSAGE);
		        			// this terminates this player client including all its threads
		        			System.exit(0);
//<=========================
	        			}
	        		}
	        	}	        	
	        	/**
	        	 * Regula
	        	 * r game playing operation; 
	        	 * I update my perception of the state of the world  
	        	 */
	        	playerWorld.updateAll(receivedPacket);
	        	worldData = playerWorld.getWorldData();
	        }
	        
		} catch( IOException ioe ) { 
			System.out.println("Exception caught in receiveInfoAndUpdate: " + ioe );
		}
		
		return receivedPacket;
	}

	/**
	 * This method uses the perceived state of the world to plan
	 * actions and execute them. 
	 * 
	 * This method is declared public to provide access by the 
	 * debugger application in the debug mode. 
	 * 
	 * @param - the received packet (used for counting lost packets only)
	 */
	public void planAndExecute(Packet receivedPacket) {
		try {
			// first I plan my actions and save them to the World Model
			playerWorld = planForAll();		
			// next I execute actions by sending commands to server
			execute();
			playerWorld.setActionTime( worldData.time );					
		} catch( Exception e ) { 
			// this just protects from crashing before full
			// connection with the server is established
			System.out.println(getName() + " planAndExecute - Exception caught: " + e );
			e.printStackTrace();
		}	
		
		if (!isDebugMode) {
			// here I do some housekeeping in the regular mode only
			receivedPacketCount++;
			checkLostPackets( receivedPacket );
			/*
			if ( receivedPacketCount%100 == 0 )
				System.out.println("* packet " + receivedPacketCount 
						+ " " + getName() + "  is still alive");				
			try {
				sleep( 70 );		// this just slows down the agent to 
									// aSeeData what happens with packets
			} catch ( InterruptedException ie ) {}
			*/
	        
	        // get the time after the computations
	        long timeAfter = System.currentTimeMillis();
	        calcIdlingPercent( timeBefore, timeAfter, receivedPacketCount );
		}
	}
	
	/**
	 * This method executes my action by generating a commandPacket 
	 * for the server. The action must be set in the World Model beforehand.
	 *  
	 * @throws IOException - normally no exception is expected
	 */
	private void execute() throws IOException {   
		switch( playerWorld.getActionType() ) {
			case TOS_Constants.NOACTION:	; 					break; // do nothing
			case TOS_Constants.SHOOT: 	 	shootGoal(); 		break;
			case TOS_Constants.MOVE:  	 	moveTo(); 			break;
			case TOS_Constants.TURN:  	 	turn(); 			break;
			case TOS_Constants.PASS:  	 	passTo(); 			break;
			case TOS_Constants.CHASE: 	 	chaseBall(); 		break;
			case TOS_Constants.GRAB:  	 	grabBall();			break;
			case TOS_Constants.MOVEWBALL:	moveWithBall();		break;
			default: ;  
		}
	}
 

	/*******************************************
	 *
	 * player high-level decision making methods 
	 *
	 *******************************************/
	
	/**
	 *  This method determines my (soccer player) action and 
	 *  sets action type in the World Model, no matter what 
	 *  my role and what the game situation are.
	 *  Action parameters are calculated elsewhere.
	 */
	private PlayerWorld planForAll() {
		
		playerWorld.setActionType( TOS_Constants.NOACTION );	// do nothing by default
		
		if ( playerWorld.getGamePeriod() == RefereeData.NO_GAME ) {
			// do nothing until the Soccer Server is ready to run the game
			return playerWorld;
	//<-----
		}
		
		if( worldData.getMe().isGoalie() ) {
			// plan goalie actions
			planForGoalie();
		} else {
			// I am a field player		
			iAmAtOffsideRisk = false;	// may be changed as the situation is analyzed			
			if( playerWorld.isBallKickable() ) {				
				// plan actions if I am with the ball
				if (canIkickBall()) {
					kickBall();
					return playerWorld;
	//<-------------
				} else if ( worldData.getBall().getSpeed() < EPS6 ) {
					// ball is standing still near me
					if ( shouldIHoldBall() )
						return playerWorld;
	//<-----------------
				}
			}
				/*
				else if (!shouldIHoldBall()) {
					// try to stay away from the nearest opponent
					MyPlayer opp = worldData.getFastestOpponent();
					if (opp != null) {
						Vector2d dest = new Vector2d();
						dest = playerWorld.calcStayAwayPosFromPlayer(opp, dest, 4.0);
						playerWorld.setDestination(dest);
						playerWorld.setActionType( TOS_Constants.MOVE );
						System.out.println( playerWorld.getTime() + " " + getName() 
								+ " avoiding to bump into " + opp.getId() + "-" + opp.getSide());					}
				}
				*/
			
			// I am not close to the ball or I am close but should not hold the ball
			if ( shouldIinterceptBall() ) { 
				// plan actions if I do not control the ball yet, but
				// can repossess the control soon
				/*
				System.out.println(getName() + " I'm  fastest getBallPossession() = " 
							+ playerWorld.getBallPossession() );
				*/
				// avoid the offside situation 
				if( playerWorld.isMyTeamOffside() 
						|| worldData.getBall().isGrabbed ) {
					playerWorld.setActionType( TOS_Constants.MOVE );
					//System.out.println( getName() + " moving (my team is offside or ball is grabbed)");	      
				} else { 
					playerWorld.setActionType( TOS_Constants.CHASE );
				}
			} else {
		    	// as I am rather far away from the ball, I decide to move
		    	// and thus determine my destination
		    	// ( this is critical, as this is exactly what I am doing 
				// 90 per cent of the time ) 
				playerWorld.determineWhereToMove();
				playerWorld.setActionType( TOS_Constants.MOVE );
				//System.out.println(getName() + " moving because I am far away");	      
			} 
				
			if ( playerWorld.getActionType() == TOS_Constants.MOVE ) {
				// I decided to move; let's see what adjustments could be done
				if(playerWorld.getGameMode() == RefereeData.PLAY_ON ) {
					// avoid any offside situation by modifying the planned 
					// destination point in playerWorld 
					if( playerWorld.isInOffsidePosition() ) {
						// this what server has told me ... moving to different point
						playerWorld.determineWhereToMoveOffSide();
						//System.out.println( getName() + " moving from offside");	
					} else if (playerWorld.calcOffSideRisk() == 1) {
						// moderate risk of getting offside - just slow down;
						// (all the rest is done inside calcOffSideRisk(),
						// e.g. moving to different point if risk is >1
						iAmAtOffsideRisk = true;
						//System.out.println( getName() + " avoiding offside");	
					}
					
					//if ( playerTeamID*playerNumber == 6 ) {
						/*
						Vector2d mypos = worldData.getMe().getPosition();
						float disToMove = (float)mypos.distance( playerWorld.getDestination() );  
						System.out.println(getName() + " Moving disToMove = " + disToMove );
						*/
				}
			}
		}		
		
	    return playerWorld; 	// this returned value contains updated action variables
	    
  	} // planAll
  	
	
	/** 
	 * this (very basic, though) method plans actions for the goalie;
	 * updates world.actionType
	 * 
	 * @return updated PlayerWorld
	 */
	private PlayerWorld planForGoalie() {
		
		if( playerWorld.isBallKickable() ) {			
			// catch, or pass the ball, or move with it
			//System.out.println(playerWorld.getTime() + " " + getName() 
					//+ " == Ball is kickable by golie; can kick = " + canIkickBall());
			if ( shouldICatchBall() ) {
				System.out.println(playerWorld.getTime() + " " + getName() 
											+ " == Goalie caught the ball"); 
				playerWorld.setActionType( TOS_Constants.GRAB );
				playerWorld.setBallControlledByMe(true);
				return playerWorld;
	//<=========
			} else if ( playerWorld.isGrabbedByMe() ) {		
				//System.out.println(playerWorld.getTime() + " " + getName() 
						//+ " == Goalie is moving with grabbed ball"
						//+ " count=" + playerWorld.getGrabbedBallCount()); 
				if (shouldIKickOrMoveWithBall())
					return playerWorld;
	//<=============
			} else if ( canIkickBall() ) {
				// I kick the ball if I can and if I do not catch it and it is not grabbed
				kickBall();
				return playerWorld;
	//<=============
			}
		}
		
		// we get here if the ball is not kickable or catchable, or the goalie 
		// cannot kick it; so figure it out where the ball could be intercepted
		Vector2d interceptionPoint = worldData.getMe().getInterceptionPoint();
		if ( playerWorld.amItheFastestToBall() )  {
			if (World.inPenaltyArea('l', interceptionPoint, 0.001) ) {
				// I intercept the ball in the whole penalty area; 
				playerWorld.setActionType( TOS_Constants.CHASE );
			} else {
				// I also intercept ball outside the penalty area if the 
				// opponents are far away (except corner kick)
				boolean isCornerKick = 
						(worldData.getMe().getSide() == 'l' 
							&& playerWorld.getGameMode() == RefereeData.CORNER_KICK_R)
						|| (worldData.getMe().getSide() == 'r' 
							&& playerWorld.getGameMode() == RefereeData.CORNER_KICK_L);
				if (isCornerKick) {
					//  move to the default position
					playerWorld.determineWhereToMove();
					playerWorld.setActionType( TOS_Constants.MOVE );
				} else if (worldData.getFastestOpponent() != null) {
					int oppTime = worldData.getFastestOpponent().getCyclesToReachBall();
					int myTime = worldData.getMe().getCyclesToReachBall();
					if (oppTime - myTime > 7) {
						// I am significantly faster to the ball and hence intercept it
						playerWorld.setActionType( TOS_Constants.CHASE );
						return playerWorld;
	//<=================
					}
				}
			}
		} else { 
			// even not the fastest should attempt intercepting in some situations
			double distToMyGoal = worldData.getBall().getPosition()
							.distance(playerWorld.getOwnGoalPosition());
			if (distToMyGoal < 25) {	
				// if distance is not too big...
				double dirFromBallToMyGoal = worldData.getBall().getPosition()
						.direction(playerWorld.getOwnGoalPosition());
				double angle = dirFromBallToMyGoal - worldData.getBall().getDirection();
				//System.out.println(playerWorld.getTime() + " " + getName() 
						//+ " ball direction = " + (int)worldData.getBall().getDirection()
						//+ "  angle = " + (int)angle);
				// I chase the ball if it is rolling towards my goal
				if (Math.abs(Util.normal_dir(angle)) < 30*25/distToMyGoal)
					playerWorld.setActionType( TOS_Constants.CHASE );
				return playerWorld;
	//<=========
			}
			
			// just move to the default position
			playerWorld.determineWhereToMove();
			playerWorld.setActionType( TOS_Constants.MOVE );
			/*
			if (playerWorld.getTime()%50 == 0 && worldData.getMe().getSide() == 'l')
				System.out.println(getName() + " Goalie moving from " 
						+ worldData.getMe().getPosition() 
						+ "\n to " + playerWorld.getDestination()
						+ " dist=" + (int)worldData.getMe().getPosition()
								.distance(playerWorld.getDestination())
						+ " home " + playerWorld.getHomePos());
			*/
		}
		
		return playerWorld; 		
	}
	

	/*******************************************
	 *
	 * player low-level decision making methods 
	 *
	 *******************************************/

	/**
	 * This method plans my actions with the ball.
	 * It implements the high-level logic only. 
	 * All dirty work is done inside the condition checks.
	 * The order of checking these conditions is critical; 
	 * note the 'pass forward' action considered two times.
	 */
	public void kickBall() {
		// the order of the cases below changes the player 
		// behavior substantially
		aBallPassData = null;
		
		// do not dribble in special situations on both sides 
		boolean skipDribbling = 
			(playerWorld.getGameMode()== RefereeData.CORNER_KICK_L 
				|| playerWorld.getGameMode()== RefereeData.CORNER_KICK_R
				|| playerWorld.getGameMode()== RefereeData.GOAL_KICK_L
				|| playerWorld.getGameMode()== RefereeData.GOAL_KICK_R);
		
		// kick off situation
		boolean isKickOff = (playerWorld.getTime() - playerWorld.getTimeKickOff() 
								< 3/TOS_Constants.SIM_STEP_SECONDS);
		
		if( !worldData.getMe().isGoalie() && shouldIScore() ) {
			// try to score the opponent goal; all the rest is done inside shouldIScore
			playerWorld.setActionType( TOS_Constants.SHOOT );    	
			if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
				playerWorld.getSoundSystem().playClip("shooting_at_the_goal");
				lastSoundPlayedMS = System.currentTimeMillis(); 
			}
		} else if ( shouldIPassForwardLowRisk() ) {
			// do nothing; all is done inside shouldIPassForwardLowRisk
			if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
				playerWorld.getSoundSystem().playClip("passing_low_risk");
				lastSoundPlayedMS = System.currentTimeMillis(); 
			}
		} else if ( !skipDribbling && shouldIdribbleFast() ) {
			// do nothing; all is done inside shouldIdribbleFast
			if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
				playerWorld.getSoundSystem().playClip("dribble_fast");
				lastSoundPlayedMS = System.currentTimeMillis(); 
			}
		} else if ( !isKickOff && !skipDribbling && shouldIdribbleModerate() ) {
			// do nothing; all is done inside shouldIdribble
			if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
				int id = (int)(Math.random() + 0.5);
				playerWorld.getSoundSystem().playClip("dribbling" + id);
				lastSoundPlayedMS = System.currentTimeMillis(); 
			}
		} else if ( shouldIPassForward() ) {
			// do nothing; all is done inside shouldIPassForward
			if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
				playerWorld.getSoundSystem().playClip("passing_forward");
				lastSoundPlayedMS = System.currentTimeMillis(); 
			}
		} else if ( !isKickOff && !skipDribbling && shouldIdribbleSlow() 
							&& !worldData.getMe().isGoalie()) {
			// do nothing; all is done inside shouldIdribbleSlow
			if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
				playerWorld.getSoundSystem().playClip("dribbling_slow");
				lastSoundPlayedMS = System.currentTimeMillis(); 
			}
		} else if ( !isKickOff && !worldData.getMe().isGoalie() 
											&& shouldIHoldBall() ) {
			 // do nothing; all is done inside shouldIholdBall
			if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
				playerWorld.getSoundSystem().playClip("holding_ball");
				lastSoundPlayedMS = System.currentTimeMillis(); 
			}
		} else if ( shouldIPassSideways() ) {
			// do nothing; all is done inside shouldIPassSideways
		} else if ( !worldData.getMe().isGoalie() && shouldIPassBack() ) {
			 // do nothing; all is done inside shouldIPassBack
			if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
				playerWorld.getSoundSystem().playClip("passing_back");
				lastSoundPlayedMS = System.currentTimeMillis(); 
			}
		} else { 
			// execute the last resort action like kicking the ball far away
			// ( could be completely removed if the above methods
			// are good enough )
			clearBall();
			if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
				playerWorld.getSoundSystem().playClip("clearing_ball");
				lastSoundPlayedMS = System.currentTimeMillis(); 
			}
		}
		
	} // kickBall

	/**
	 * this method returns true if I must intercept the ball
	 */
	private boolean shouldIinterceptBall() {
		if (playerWorld.amItheFastestToBall()){
			return true;
		} else if (playerWorld.amIthe2ndFastestToBall() 
				&& World.inPenaltyArea('l', worldData.getMe().getInterceptionPoint(), 2.0)) {
			// inside the penalty area, I am still trying to intercept
			return true;			
		} else {
			// even not the fastest should attempt intercepting in some defensive situations
			if (worldData.getBall().getPosition().distance(playerWorld.getOwnGoalPosition()) < 35) {
				// ball is close to own goal
				if (worldData.getFastestPlayer().getSide() != worldData.getMe().getSide()) {
					// ball is controlled by opponent
					if (worldData.getMe().getPosition().distance(playerWorld.getOwnGoalPosition())< 20 ) {
						// I am close to own goal; therefore,  
						// I potentially can defend own goal
						return true;
					}
				}
			} else {
				// even not the fastest should attempt intercepting ball in
				// standard situations like free or corner kick TODO
				
				/*
				// even not the fastest should attempt intercepting the approaching ball 
				double dirFromBallToMe = worldData.getBall().getPosition()
									.direction(worldData.getMe().getPosition());
				double delta = Util.normal_dir(worldData.getBall().getDirection() - dirFromBallToMe);
				if (Math.abs(delta) < 5) {		// magic number
					// the ball is approaching me
					double distToBall = worldData.getBall().getPosition()
										.distance(worldData.getMe().getPosition());
					double cycles = World.calcCyclesBallToRoll(distToBall, 
										worldData.getBall().getSpeed());
					if (cycles > 20) {
						// time permits to react
						return true;
					}
				}
				*/
				// TODO I am already on the interception path
				// (to prevent from unnecessary turns)
				//return true;
			}
		}
		return false;
	}
	
	/**
	 *  This method plans and executes my actions as the goalie
	 *  while moving with the grabbed ball. In particular, I decide 
	 *  whether to kick it.
	 *  
	 *  return true if KICK or MOVEWBALL action is performed;
	 *  otherwise, return false 
	 */
	private boolean shouldIKickOrMoveWithBall() {
		boolean actionSelected = false;
		// I am always using left-hand side coordinates only; hence 'l'
		if (World.inPenaltyArea('l', worldData.getMe().getPosition(), -1 )  
								&& playerWorld.getGrabbedBallCount() > 2) { 	
			// I am with the ball still inside the penalty area and 
			// time permits; so I keep moving  
			playerWorld.setActionType( TOS_Constants.MOVEWBALL );
			actionSelected = true;
		} else if (canIkickBall()) {			
			if ( shouldIPassForward() ) {
				// do nothing; all is done inside shouldIPassForward 
				if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
					playerWorld.getSoundSystem().playClip("passing_forward");
					lastSoundPlayedMS = System.currentTimeMillis(); 
				}
			} else if ( shouldIPassSideways() ) {
				// do nothing; all is done inside shouldIPassSideways
			} else {
				clearBall();
				if (System.currentTimeMillis() - lastSoundPlayedMS > SOUNDDELAYMS) {
					playerWorld.getSoundSystem().playClip("clearing_ball");
					lastSoundPlayedMS = System.currentTimeMillis(); 
				}
			}
			actionSelected = true;
		}
		return actionSelected;
	}

	
	/**
	 * This method applies to the goalie only.
	 * It executes my actions as I am moving with the grabbed ball.
	 * I chose to move in the direction of the penalty area corner
	 * where fewer opponents are hanging around.
	 * 
	 * @throws IOException
	 */
	private void moveWithBall() throws IOException {
		Vector2d mydestination;
		Vector2d centerTop, centerBot;
		
		centerTop = TOS_Constants.PENALTY_CORNER_L_T;
		centerBot = TOS_Constants.PENALTY_CORNER_L_B;
		
		// once the goalie caught the ball, he must stop 
		// before leaving own penalty area
		if (PlayerWorld.MAX_GRABBED_STEPS - playerWorld.getGrabbedBallCount() < 20
				&& worldData.getMe().getPosition().getX() > centerTop.getX() - 6) {
			// just pull back inside the penalty area
			mydestination = new Vector2d(-TOS_Constants.LENGTH/2 + 8, 0);
		} else {
			// decide to which of the two corners of the penalty were to move
			double radius = 2*Field_Constants.WIDTH/3;
			double numOfOpponentsTop 
					= playerWorld.countOpponentsInCircle( centerTop, radius, true ); 																	
			double numOfOpponentsBot 
					= playerWorld.countOpponentsInCircle( centerBot, radius, true );
			// create separate Vector2d instance
			if ( numOfOpponentsTop < numOfOpponentsBot )
				mydestination = new Vector2d(centerTop);
			else
				mydestination = new Vector2d(centerBot);
			
		}
		moveToPos(mydestination);
		/*		
		System.out.println(playerWorld.getTime() + " " + getName() 
				+ " moving with grabbed ball to " + mydestination
				+ " dist = " + (int)worldData.getMe().getPosition().distance(mydestination)
				+ " dir = " + (int)worldData.getMe().getPosition().direction(mydestination));
		*/
	}
	
	/**
	 * this method returns true if I decide to shoot the goal.
	 * this magic number used to calculate the risk threshold depends 
	 * on other magic numbers in method calcBestShootingDirection
	 */
	private boolean shouldIScore() {
		boolean should = false;	
		double dist = worldData.getMe().getPosition()
							.distance(playerWorld.getOppGoalPosition());
		double threshold1 = 32; // magic number (maximal distance)
		double threshold2 = 20; // magic number (distance where kicking is less efficient)
		double threshold3 = 15; // magic number (risk threshold)
		if( dist < threshold1 ) {
			Vector2d startPos = playerWorld.getBall().getPosition();
			BallPassData kickData = playerWorld.calcBestShootingDirection(startPos, 'l');
			if (kickData != null) {
				if( dist > threshold2 )	// increase risk with the distance to goal
					kickData.setRisk(kickData.getRisk() + (dist - threshold2));
				if (kickData.getRisk() < threshold3) { 	
					should = true;
					playerWorld.setKickDirection(kickData.getDirection());
					kickData.setReceiverID(-1);	// nobody is supposed to receive pass
					aBallPassData = kickData;	// keep for reuse e.g. in debugging
					System.out.println(playerWorld.getTime() + " " + getName() 
							+ " shooting at the goal, dist=" + (int)dist 
							+ "   dir=" + (int)kickData.getDirection() 
							+ "   risk=" + (int)kickData.getRisk());
				}
			}
		}
		return should; 
	}
  
	/**
	 * This method returns true if I decide to stand with the ball and turn
	 * about it so that the closest opponent could not reach it and I could wait
	 * until good passing situation develops or I could dribble the ball.
	 */
	private boolean shouldIHoldBall() {
		
		if (!SoccerTeamMain.isBallHoldingAllowed)
			return false;	// ball holding turned off
	//<=====

		aBallPassData = null;	// clean up the artifacts, if any
		boolean should = false;
		
		int oppCount = (int)playerWorld.countOpponentsInCircle(
						playerWorld.getBall().getPosition(), 5.0, false);
		
		if (oppCount > 1) {	
			// do nothing - holding ball is too risky 
		} else {
			// zero or one opponent player is near me
			if ( canIkickBall() && Math.abs(playerWorld.getBall().getSpeed()) > 0.05 ) {
				// stop the rolling ball 
				playerWorld.setActionType( TOS_Constants.PASS );
				playerWorld.setKickForce( EPS9 ); 	// apply a non-zero but very tiny force	
				playerWorld.setKickDirection( 0 );
				System.out.println( playerWorld.getTime() + " " + getName() + " BALL STOPPED ");
				should = true;
			} else {
				// block the ball from the nearest opponent with own body
				// angle between directions to the opponent and the ball
			}
		}
		
		return should;		
	}
		

	/**
	 *  This method returns ball pass data object if it makes sense to 
	 *  pass the ball to a teammate; otherwise it returns null. 
	 *  Passing is considered in the sector determined by its central 
	 *  direction and half-width; all angles measured in degrees; 
	 *  magic numbers everywhere, much still could be improved. 
	 *  The pass direction is selected with best (minimal) "gain" 
	 *  among reasonably low-"risk" options.
	 *   
	 * @param myplayer - player for whom the calculation is done
	 * @param bisectorDirection
	 * @param sectorHalfWidth
	 * @param riskThreshold
	 * @return best ball pass data (or null if pass is infeasible)
	 */
	private BallPassData calcBestPassInSector(MyPlayer myplayer, 
							double bisectorDirection, 
							double sectorHalfWidth, double riskThreshold) {
		
		if (!SoccerTeamMain.isBallPassingAllowed)
			return null;	// ball passing turned off
	//<=====
		
		// find the minimal risk among all teammates receiving pass
		BallPassData bestPass = null;	// this will be calculated
		Vector<MyPlayer> teammates = getTeammates(); 
		double min_gain = Float.MAX_VALUE;			
		double pass_dist = 0;
		boolean found = false;
		// we assume that the ball is very close to this player;
		// this is important if calculating future opportunities
		Vector2d ballPosition = myplayer.getPosition();
		
		// I consider each teammate in sector and 
		// select the least risky one, if possible
		for( int i=0; i<teammates.size(); i++ ) {
			MyPlayer tmm = teammates.elementAt(i);
			if (tmm.equals(myplayer))
				continue;
		//<-----	// I do not pass to myself
			// distance to this teammate
			double distToTmm = ballPosition.distance(tmm.getPosition());
			if (distToTmm > 32)
				continue;
		//<-----		teammate is too far away
			// direction from me to the teammate 
			double dirToTmm = ballPosition.direction(tmm.getPosition());
			double delta = Util.normal_dir(bisectorDirection - dirToTmm);
			if (Math.abs(delta) > sectorHalfWidth)
				continue;
		//<-----		teammate outside the sector
			
			BallPassData passData = evaluateBallPass(ballPosition, tmm);
			if (tmm.isGoalie())
				passData.setRisk(passData.getRisk() + 5);	// discourage from passing to own goalie
			
			/*
			if (passData != null)
				System.out.println( getName() + " passing dir="
					+ (int)passData.direction + " risk=" + (int)passData.risk 
					+ " gain=" + (int)passData.gain); 
			*/
			if( passData.getGain() < min_gain && passData.getRisk() < riskThreshold) {
				// find the minimum among the low-risk options, if any
				min_gain = passData.getGain();
				pass_dist = ballPosition.distance(passData.getEndPoint());
				bestPass = passData;
				found = true;
			}
		} // end i	
				
		if ( found ) {
				double force = TOS_AgentConstants.K_FORCE_MAXIMAL;
				if (pass_dist < 20) 
					force = force * (0.25 + 0.75*pass_dist/20); // reduce force for close teammates
				bestPass.setForce(force);
				aBallPassData = bestPass;	// keep for reuse
				return bestPass;
		} else {
			// no good pass found
			return null;
		}	
	}

	
	private BallPassData calcBestPassInSector(MyPlayer myplayer, 
			double bisectorDirection, double sectorHalfWidth ) {
		double RISKTHRESHOLD = 9;
		return calcBestPassInSector(myplayer, 
							bisectorDirection, sectorHalfWidth, RISKTHRESHOLD);
	}

	/**
	 * This method returns the best direction for clearling the ball with
	 * reasonably low risk;
	 * it returns null is no such direction found 
	 */
	private BallPassData calcBestClearingDirection(double preferredDir) {
		BallPassData passData = new BallPassData();
		passData.setForce(TOS_Constants.MAXKICK);
		return passData;
	}
	

	/**
	 * This method returns the evaluated ball pass from given start to some teammate 
	 */
	private BallPassData evaluateBallPass(Vector2d startPos, MyPlayer tmm) {
		
		double distToTmm = startPos.distance(tmm.getPosition());
		// estimate the passing direction taking into account player velocity
		// (magic numbers are used)
		if (playerWorld.getGameMode() == RefereeData.THROW_IN_R 
								&& worldData.getMe().getSide() == 'r'
					|| playerWorld.getGameMode() == RefereeData.THROW_IN_L 
											&& worldData.getMe().getSide() == 'l') {
			// my team throws in
			if (distToTmm > 15)
				return null;	// throw-in has limited kick force (enforced by server)
	//<=========
		}
		// calculate the interception point in front of the receiveing teammate
		double cycles = World.calcCyclesBallToRoll(distToTmm, 
					TOS_Constants.BALLMAXSPEED*TOS_Constants.SIM_STEP_SECONDS);
		Vector2d deltaDist = tmm.getVelocity().timesV(cycles * 1.5);	// magic number
		Vector2d forecastTmmPos = Vector2d.add(tmm.getPosition(), deltaDist);
		BallPassData passData = evaluateBallKick(startPos, forecastTmmPos, true);
		passData.setReceiverID(tmm.getId());
		
		return passData;
	}
	
	
	/**
	 * This method returns the evaluated ball kick from given start to end;
	 * the risk of getting the ball out of field may or may not be calculated. 
	 */
	private BallPassData evaluateBallKick(Vector2d startPos, 
							Vector2d endPos, boolean outOfFieldRisk) {		
		return playerWorld.evaluateBallKick(startPos, endPos, outOfFieldRisk, 'l');
	}
	
	
	/**
	 * This method returns true if I decide to dribble the ball so that
	 * I am passing it to myself and advance to wherever the tactical 
	 * gain could be increased.
	 * 
	 * @return
	 */
	private boolean shouldIdribbleFast() {
		return shouldIdribble(1.7, 60, 90, 10);
	}
	
	/**
	 * This method returns true if I decide to dribble the ball so that
	 * I am passing it to myself and advance to wherever the tactical 
	 * gain could be increased.
	 * 
	 * @return
	 */
	private boolean shouldIdribbleModerate() {
		return shouldIdribble(1.0, 90, 90, 10);
	}
	
	/**
	 * This method returns true if I decide to dribble the ball so that 
	 * I could move with the ball while keeping it close to the control radius.
	 * 
	 * @return
	 */
	private boolean shouldIdribbleSlow() {
		if (!SoccerTeamMain.isSlowBallDribblingAllowed)
			return false;	// slow ball dribbling turned off
	//<=====
		// dribble carefully in any direction
		return shouldIdribble(0.5, 180, 150, 4);
	}

	/**
	 * This method returns true if I decide to dribble the ball so that
	 * I am passing it to myself in given sector and I advance towards 
	 * wherever the tactical gain could be increased without taking high risk.
	 * 
	 * @param scaleCoeff - scaling coefficient for the force and distance
	 * @param halfSector - half sector width for choosing dribbling direction
	 * @param halfSector2 - half sector width about the chosen dribbling direction 
	 * 						for calculating risk
	 * @param maxRisk - risk threshold
	 * @return
	 */
	private boolean shouldIdribble(double scaleCoeff, double halfSector, 
									double halfSector2, double maxRisk) {
		
		/**
		 * This is just a stub. Player always decides to dribble in 
		 * random direction between -30 and 30 degrees from the goal
		 */
		
		MyPlayer me = worldData.getMe();
		// it is bad idea for the goalie to dribble 
		if ( me.isGoalie() )
			return false;
	//<=====
		
		/**
		 *  find good dribbling direction to the right or to the left of 
		 *  the preferred direction by trying to avoid the opponent players
		 *  (code removed) 
		 */
		
		// dribble blindly towards the other side's goal (this maintains formation)
		double dir = 60 * (Math.random() - 0.5) 
				+ playerWorld.getBall().getPosition().direction(playerWorld.getOppGoalPosition());
		dir = Util.normal_dir(dir);
		
		playerWorld.setActionType( TOS_Constants.PASS );	// passing to myself
		playerWorld.setKickForce( TOS_AgentConstants.K_FORCE_MODERATE * scaleCoeff );
		playerWorld.setKickDirection( dir );
		
		System.out.println(playerWorld.getTime() + " " + getName() 
					+ " Dribble in dir=" + (int)(playerWorld.getKickDirection()+0.5) 
					+ " force=" + (int)playerWorld.getKickForce()  
					+ " risk = " + 0 );

		return true; 
	}

	
	/**
	 *  this method returns true if I decide to pass the ball to a teammate
	 *  (roughly) in the forward direction;
	 */	 
	private boolean shouldIPassForward() {
		return false; 
	}
	
	/**
	 *  this method returns true if I decide to pass the ball to a teammate
	 *  in the forward (roughly) direction with very low risk;
	 */	 
	private boolean shouldIPassForwardLowRisk() {
		// the direction to the opponent goal is the preferred one
		double preferredDir = worldData.getMe().getPosition()
									.direction(playerWorld.getOppGoalPosition());
		double RISK_THRESHOLD = 15;	// magic number
		BallPassData bestPass = calcBestPassInSector(worldData.getMe(), 
												preferredDir, 100, RISK_THRESHOLD);
		if (bestPass != null){
			// good ball passing opportunity exists
			double myGain = playerWorld.calcBallPassGain(worldData.getMe().getPosition());
			if (myGain - bestPass.getGain() > 0.1*myGain) {				
				// only pass the ball to places with higher tactical gain than my position
				playerWorld.setActionType( TOS_Constants.PASS );
				playerWorld.setKickForce( bestPass.getForce() ); 	
				playerWorld.setKickDirection( bestPass.getDirection() );
				System.out.println( playerWorld.getTime() + " " + getName() + " PASS LOWRISK " 
						+ " to=" + (int)bestPass.getReceiverID()  
						+ " dir=" + (int)bestPass.getDirection()  
						+ " force=" + (int)bestPass.getForce()  
						+ " risk = " + (int)bestPass.getRisk());
				return true;
			} else {
				// no low risk ball pass exist
				return false;
			}
		}
		return false; 
	}
	
		/**
	 *  this method returns true if I decide to pass the ball to a teammate
	 *  (roughly) in the backward direction;
	 */	 
	private boolean shouldIPassBack() {
		// the preferred direction is roughly the own side
		BallPassData bestPass = calcBestPassInSector(worldData.getMe(), 180, 60);
		if (bestPass != null){
			playerWorld.setActionType( TOS_Constants.PASS );
			playerWorld.setKickForce( bestPass.getForce() ); 	
			playerWorld.setKickDirection( bestPass.getDirection() );
			System.out.println(playerWorld.getTime() + " " + getName() + " PASS BACK " 
					+ " to=" + (int)bestPass.getReceiverID()  
					+ " dir=" + (int)bestPass.getDirection()  
					+ " force=" + (int)bestPass.getForce()  
					+ " risk = " + (int)bestPass.getRisk());
			return true;
		}
		return false; 
	}

	/**
	 *  this method returns true if I decide to pass the ball to a teammate
	 *  (roughly) in the right- or left-hand direction;
	 */	 
	private boolean shouldIPassSideways() {
		return false;
	}


	/**
	 * This method implements my decision as a goalie
	 * on whether to catch and grab the ball now.
	 * 
	 * @return true if decided to catch
	 */
	private boolean shouldICatchBall() {
		
		if (!canIkickBall())
			return false; 
		
		if (!World.inPenaltyArea('l', worldData.getBall().getPosition(), -0.1)) {
			// I should not catch the ball that is outside the penalty area
			// (I am always using left-hand side coordinates only; hence 'l')
			//System.out.println("Goalie: ball outside the penalty area");  
			return false;
		} else {
			// I decide to catch the ball only when some opponents are hanging around 
			if ( !playerWorld.isGrabbedByMe() ) {				
				Vector2d myPosition = worldData.getMe().getPosition();
				double numOfOpponentsClose 
						= playerWorld.countOpponentsInCircle( myPosition, 20.0, true ); 
				double numOfOpponentsTooClose 
						= playerWorld.countOpponentsInCircle( myPosition, 10.0, true );
				
				//System.out.println("Goalie: opponents Close = " + (float)numOfOpponentsClose 
						//+ " TooClose = " + (float)numOfOpponentsTooClose );
		
				return ( numOfOpponentsClose > 0.75 
							|| numOfOpponentsTooClose > 0 ); 
			} else {
				return false;	// I do not need to catch the ball, as I have grabbed it !
			}
		}
	}
	
	/**
	 * This method returns true if I can kick the ball now given 
	 * the time constraints that disallow kicking ball too frequently. 
	 */
	protected boolean canIkickBall() {
		
		if (playerWorld.isGrabbedByMe() 
				&& playerWorld.getGrabbedBallCount() > 9*PlayerWorld.MAX_GRABBED_STEPS/10) {	
			// applies to goalie only
			// do not kick grabbed ball for awile
			return false;
		} else if (playerWorld.getTime() - playerWorld.getLastTimeBallWasKicked() 
					> (int)(TOS_Constants.NOBALL/TOS_Constants.SIM_STEP_SECONDS)) {
			// time limit expired
			return true;
		} else {
			// must wait some time; kicks would not be executed by 
			// the server and I just waste this step if try kicking
			//System.out.println(playerWorld.getTime() + " " + getName() + " cannot kick ball");
			return false;
		}
		
	}
	
	/*******************************************
	 *
	 * player action execution methods 
	 *
	 *******************************************/
  
  
	/**
	 *  This method executes kicking the ball in the 
	 *  direction that has been calculated elsewhere. 
	 */
	private void shootGoal() throws IOException {
		double dir = playerWorld.getKickDirection();
		sendKickPacket( dir, TOS_Constants.K_FORCE_MAXIMAL ); 
	}
	
	/**
	 *  this method executes kicking the ball to a teammate; 
	 *  the kick direction and force have been calculated elsewhere. 
	 */
	// (hardly anything could be improved here)
	private void passTo() throws IOException {
		sendKickPacket( playerWorld.getKickDirection(), 
							playerWorld.getKickForce() );
	}

	/**
	 *  this method sends the KICK packet to the server
	 *  it is recommended to use just one method for all kicks, 
	 *  as it also re-sets some class instance variables
	 *  
	 * @param direction
	 * @param force
	 * @throws IOException
	 */
	private void sendKickPacket(double direction, double force) throws IOException {
		
		aKickData = new KickData( direction, force );
		// if this is a ball pass, inform the server for re-transmitting this to viewers
		if (aBallPassData != null) {
			if (playerWorld.getMySide() == 'r' ) {
				// invert xy-coordinates for the correct display
				aKickData.endPoint = new Vector2d(-aBallPassData.getEndPoint().getX(), 
													-aBallPassData.getEndPoint().getY());
			} else {
				aKickData.endPoint = aBallPassData.getEndPoint();
			}
			aKickData.receiverID = aBallPassData.getReceiverID();
			//System.out.println("Sending ball pass info. receiverID=" 
					//+ aBallPassData.receiverID + " endPos " + aBallPassData.endPoint);
		} else {
			// shooting at the goal or clearing ball
			aKickData.receiverID = -1;
		}
		aKickData.side = playerWorld.getMySide();
		
		Packet commandPacket = new Packet( 
									Packet.KICK, 
									aKickData, 
									SoccerTeamMain.address, 
									SoccerTeamMain.port );
		
		worldData.send( commandPacket );
		// memorize time ball was kicked/grabbed
		playerWorld.setLastTimeBallWasKicked( playerWorld.getTime() );
	}


	/**
	 * This method executes grabbing the ball by the goalie. 
	 * Player coodinates are sent to the server so that it
	 * assigned them to the ball; as the player is moving, the ball
	 * is dragged with the player.
	 * 
	 * @throws IOException
	 */
	private void grabBall() throws IOException 	{
		Vector2d position = worldData.getMe().getPosition();
		TeleportData aTeleportData = new TeleportData( TeleportData.GRAB, 
				worldData.getMe().getId(), worldData.getMe().getSide(), position );
		// TELEPORT action is only allowed for the goalie and 
		// for any player in the 'before kick off' state;
		// (rules are enforced by the server)
		Packet commandPacket = new Packet( 
									Packet.TELEPORT, 	
									aTeleportData, 
									SoccerTeamMain.address, 
									SoccerTeamMain.port );
		worldData.send( commandPacket );
		// memorize time ball was kicked/grabbed
		playerWorld.setLastTimeBallWasKicked( playerWorld.getTime() );
	}
  	

	/**
	 *  this method forces the soccer server to move me into
	 *  my home position without delay
	 */
	@SuppressWarnings("unused")
	private void teleportMyself() {
		TeleportData aTeleportData = 
			new TeleportData( 	worldData.getMe().getSide(), 
								worldData.getMe().getId(),		
								playerWorld.getHomePos().getX(), 
								playerWorld.getHomePos().getY() );
		
		Packet commandPacket = 
			new Packet( Packet.TELEPORT, 	// TELEPORT action is only allowed for the goalie and
											// for any player in the 'before kick off' state;
											// (rules are enforced by the server)
						aTeleportData, 
						SoccerTeamMain.address, 
						SoccerTeamMain.port );
		try {
			worldData.send( commandPacket );
		} catch ( IOException e ) {
				System.out.println(playerWorld.getTime() + " " + getName() 
						+ " teleportMyself() error " + e ); 
		}		
	}

  	/** 
  	 *  This method determines the direction where 
  	 *  I should kick the ball as the last resort (clearing ball).
  	 *  This could be further improved;
  	 *  one possibity is completely removing this action.
  	 */
	protected void clearBall() {	
		aBallPassData = null;	// clean up the artifacts

		// player kicks the ball towards the opponent goal
		// trying to avoid closeby opponent players
		
		double dirToOppGoal = worldData.getMe()
				.getPosition().direction(playerWorld.getOppGoalPosition() );
		
		double dir;			// kick direction (the output)
		BallPassData passData = new BallPassData();	// a wrapper

		boolean cornerKick = // applies to both teams
			(playerWorld.getGameMode()== RefereeData.CORNER_KICK_L 
				|| playerWorld.getGameMode()== RefereeData.CORNER_KICK_R);

		if (cornerKick) {
			// send ball in front of the opponent goal
			dir = dirToOppGoal - Math.signum(worldData.getMe()
										.getPosition().getY()) * 7.0;
		} else {
			passData = calcBestClearingDirection(dirToOppGoal);
			dir = passData.getDirection();	
		}
		
		playerWorld.setActionType( TOS_Constants.SHOOT );
		playerWorld.setKickDirection( dir );
		
		System.out.println(playerWorld.getTime() + " " + getName() 
					+ " Clear ball in dir = " + (float)playerWorld.getKickDirection()
					+ " risk = " + passData.getRisk());
	}


	/**
	 * This method plans and executes my actions to chase the ball;
	 * my resulting trajectory is not necessarily optimal, though
	 * 
	 * @throws IOException
	 */
	private void chaseBall() throws IOException {
		/*
		double directionToEndPoint = worldData.getMe()
					.getPosition().direction( worldData.getMe().getInterceptionPoint() );
		double directionToBall = worldData.getMe()
				.getPosition().direction( worldData.getBall().getPosition());
		double distToEndPoint = worldData.getMe()
					.getPosition().distance( worldData.getMe().getInterceptionPoint() );
		*/
		// TODO - this needs fine tuning
		// determine direction to chase the ball
		double directionToMove;
		if (worldData.getMe().isGoalie()) {
			// goalie moves straight to the calculated interception point
			directionToMove = worldData.getMe().getPosition()
					.direction(worldData.getMe().getInterceptionPoint());
		} else {
			// field player moves to some extrapolated point; this point
			// converges with the interception point over time
			int cyclesLeft = worldData.getMe().getCyclesToReachBall();
			int limit = 10;
			cyclesLeft = (cyclesLeft < limit) ? 0 : Math.min(cyclesLeft-limit, 2*limit);
			// extrapolate ball interception point
			Vector2d extrapoloatedPoint = new Vector2d(
					worldData.getMe().getInterceptionPoint().getX() +
							cyclesLeft * worldData.getBall().getVelocity().getX(),
					worldData.getMe().getInterceptionPoint().getY() +
							cyclesLeft * worldData.getBall().getVelocity().getY() );			
			directionToMove = worldData.getMe()
									.getPosition().direction(extrapoloatedPoint);
		}																
		aDriveData = new DriveData( directionToMove, getForce(), true );
		Packet commandPacket = new Packet( 
									Packet.DRIVE, 
									aDriveData, 
									SoccerTeamMain.address, 
									SoccerTeamMain.port );
		worldData.send( commandPacket );
	}

	
	/**
	 * This method plans and executes my actions to move on the field
	 * without the ball by sending a data packet to the server; 
	 * the planned destination point is set in playerWorld beforehand
	 * 
	 * @throws IOException
	 */
	private void moveTo() throws IOException {
		
		double distance = worldData.getMe().getPosition()
								.distance( playerWorld.getDestination() );
		double direction = worldData.getMe().getPosition()
								.direction( playerWorld.getDestination() );
		
		//if ( playerTeamID*playerNumber == 6 ) 
		//System.out.println(getName() + " distance = " + (float)distance 
									//+ " direction = " + (float)direction);		

		double force =  TOS_Constants.MV_FORCE_MAXIMAL;
		
		if( distance < 5.0) 
			// slow down near the destination
			force = TOS_Constants.MV_FORCE_SMALL;
		
		if( iAmAtOffsideRisk && Math.abs(direction) < 90)
			// slow down if moving towards the opponent side
			force = TOS_Constants.MV_FORCE_SMALL * (1 + Math.sin(Math.toRadians(direction)));
		
		if ( worldData.getBall().isGrabbed ) {
			double ballDist = worldData.getMe().getPosition()
						.distance( worldData.getBall().getPosition() );
			// do not bump into the goalie who has grabbed the ball
			// ( actually, the server also enforces this rule )
			if (ballDist < 4.0)
				force = TOS_Constants.MV_FORCE_SMALL/3.0;
		}
		
		// create a data packet and send it to server
		aDriveData = new DriveData( direction, force );
		Packet commandPacket = new Packet( 
									Packet.DRIVE, 
									aDriveData, 
									SoccerTeamMain.address, 
									SoccerTeamMain.port );
		worldData.send( commandPacket );
	}

	
	/**
	 *  this method executes my actions to move to given position
	 *  by sending a data packet to the server
	 *  
	 * @param position
	 * @throws IOException
	 */
	protected void moveToPos( Vector2d position ) throws IOException {
		double direction = worldData.getMe().getPosition()
								.direction( position );
				
		aDriveData = new DriveData( direction, TOS_Constants.MV_FORCE_MAXIMAL );
		Packet commandPacket = new Packet( 
									Packet.DRIVE, 
									aDriveData, 
									SoccerTeamMain.address, 
									SoccerTeamMain.port );
		worldData.send( commandPacket );
	}

	
	/**
	 *  this method executes my actions to turn in the direction
	 *  to the 'facing position' which is set in the world model
	 */
	private void turn() throws IOException {
		double direction = worldData.getMe().getPosition()
								.direction( playerWorld.getFacingPos() );
				
		// in the server, a drive with zero force just results in a turn
		aDriveData = new DriveData( direction, TOS_Constants.MV_FORCE_NOTHING );
		Packet commandPacket = new Packet( 
									Packet.DRIVE, 
									aDriveData, 
									SoccerTeamMain.address, 
									SoccerTeamMain.port );
		worldData.send( commandPacket );
		
		//System.out.println( getName()+ " Turning in direction = " + (float)direction );	      
	}
	

	/*******************************************
	 *
	 * low-level computational methods 
	 *
	 *******************************************/
	
  
  	/**
  	 *  determine the force necessary for for chasing the ball
  	 */
	private double getForce() {
		double force;
		if( playerWorld.getDistance2Ball() >= 3.0 ) 
			force = TOS_AgentConstants.MV_FORCE_MAXIMAL;
		else 
			force = TOS_AgentConstants.MV_FORCE_MEDIUM;
			
		return force;
	}


	/*******************************************
	 *
	 * 			householding methods
	 *
	 *******************************************/

	/**
	 *  This method collects lost packet statistics that could be 
	 *  useful for determining whether the client is running too slow.
	 *  (* A tradeoff exisits between the sophistication of algorithms and the
	 *  time required to execute them; with too long time, player performance 
	 *  tends to deteriorate. This is a platform-dependent effect, though. *) 
	 *  For example, on a slow computer, a substantial increase in the number 
	 *  of options the player evaluates when making decisions about passing 
	 *  or dribbling the ball may lead to a noticeable packet loss rate.
	 */
	private void checkLostPackets( Packet aPacket ) {
		if (aPacket == null) 
			return;		// this could happen in the debug mode
	//<=====
		
		if( aPacket.packetType == Packet.SEE ) {
			// as the the SEE packets are sent on each simulation step 
			// and the server inserts in them the step ID, this parameter
			// can be used to detect packets losses 

			SeeData aSeeData = (SeeData)aPacket.data;
			
			if ( previousReceivedPacketID < 0 ) {
				// skip the first packet
			} else {
				
				
				int delta = aSeeData.time  
						- ( previousReceivedPacketID + 1 );
				if ( delta <0 )
					delta = MODULUS - delta; 
	
				if ( delta > MODULUS/2 )
					delta = 0; 	// just ignore too big losses
				
				lostPacketCount = lostPacketCount + delta;
				
				// this is the exponential smoothening method
				double weight = 0.5;			// a magic number
				lostPacketFactor = weight * delta + 
									(1 - weight) * lostPacketFactor;
				
				if ( lostPacketFactor > 2.0 ) {
					// print a warning that packets are being lost
					System.out.println("** " + getName() + " lost " + delta 
						+ " packets" + "  lostPacketFactor = " 
						+ ((int)(1000.0*lostPacketFactor))/1000.0 + "  **" );	
				}
			}
			previousReceivedPacketID = aSeeData.time;
		}	
	}


	/**
	 *  This method estimates the percentage of idling time and prints it out
	 *  with regular intervals; this estimate gives the idea of how close 
	 *  the process time to its limit is; it is based on some assumptions.
	 *  Idling below 20% should be regarded insufficient. 
	 *  Its negative value means severe time deficit. 
	 *  
	 *  TODO needs further investigation. 
	 *  
	 * @param before
	 * @param after
	 * @param count
	 */
	private void calcIdlingPercent( long before, long after, long count ) {
		// assumptions: 
		// (1) there are 22 player threads running on this computer;
		// (2) 30% of the total time is consumed by everything else, 
		//     including the OS, server, monitor, and other applications;
		//     (just a guess)
		
		double compTimeMilliseconds = (double)(after - before);
		processingTime = processingTime + compTimeMilliseconds;
		
		if ( count%REPORT_STEPS_NUM == 0 ) {
			
			processingTime = processingTime/REPORT_STEPS_NUM; 
			double idleTime 
					= (1 - 0.3) * (TOS_Constants.SIM_STEP_SECONDS*1000.0) 
								- processingTime * 22;
			
			double idlingPercent 
						= 100.0*idleTime/(idleTime + processingTime * 22); 
			double lostPercent 
						= 100.0*lostPacketCount/REPORT_STEPS_NUM; 
			
			System.out.println("\n@@@  " + getName() + 
					":\n proc time " + (float)processingTime 
					+ " ms, idling " 
					+ ((int)(1000.0*idlingPercent))/1000.0 +  "%"
					+ ", lost packets "  
					+ ((int)(1000.0*lostPercent))/1000.0 +  "%");
			processingTime = 0;
			lostPacketCount = 0;
		}
	}

	/****************************************************
	 *
	 * public get/set access methods for class variables
	 *
	 ****************************************************/
  
	public PlayerWorld getWorldModel() {
		return playerWorld;
	}

	public void setWorldModel(PlayerWorld world) {
		playerWorld = world;
	}
	
	protected Vector<MyPlayer> getTeammates() {
		return worldData.getMyTeam();
	}

	public String getName() {
		return name;
	}


	public Transceiver getTransceiver() {
		return transceiver;
	}


	public void setTransceiver(Transceiver transceiver) {
		this.transceiver = transceiver;
	}

}
