/* WorldModel.java
   The soccer game world as percieved by the AI player. 

   Copyright (C) 2001  Yu Zhang
   (with modifications by Vadim Kyrylov; October 2004) 
 *
 * by Vadim Kyrylov
 * January 2006
 *
*/
// NOTE: all this application is better to view with the soccer agent' standapoint.
//       So you will find comments like "I player", "Should I do something", etc. 

package soccer.tos_teams.sfu;

import soccer.common.*;
import soccer.tos_teams.sfu.BallPassData;

import java.io.*;
import java.util.Vector;

/**
 * This class maintains what I player perceive about the world and
 * my own state.
 * All coordinates are transformed so that I believe that my team is
 * playing on the left side (even though this is may be not the case).
 * 
 * NOTE: this class best viewed from the soccer player agent standpoint.
 *       So you will find comments like "I player", "Should I do something", etc.
 *         
 * @author Vadim Kyrylov (since 2010)
 *
 */

public class PlayerWorld extends ClientWorld {
	
	//==========  private members  ====================
    // number of cycles allowed keeping the ball grabbed by the goalie 
	public static final int MAX_GRABBED_STEPS 
		= (int)(TOS_Constants.MAX_GRABBED_TIME / TOS_Constants.SIM_STEP_SECONDS);
	
	private WorldData   worldData; 		// the visual info about the world
	private Transceiver transceiver;
	private Formation 	myFormation; 
	
	// variables used for the perceived ball possession
	private int 		whoseBallIs = TOS_AgentConstants.NEUTRAL_BALL; 
	// ball possession indicator
	private double 		ballPossIndicator = 0;  
	// weight for smoothening ball possession indicator
	// (determines how fast player reacts on changes)
	private static final double 	BALLPOSWEIGHT = 0.25;
	// decision making threshold for ball possession
	private static final double 	BALLPOSTHRESHOLD = 0.7;   

	// some ball state attributes
	private MyPlayer 	nearestPlayerToBall;  
	@SuppressWarnings("unused")
	private double 		minPlayerDistance;	
	private MyPlayer 	nearestTmmToBall; 
	private double 		minTmmDistance;	
	private MyPlayer 	nearestOppToBall; 
	private double 		minOppDistance;	

	// variables used to store all player positions at previous step,
	// needed for calculating player velocities.
	private Vector<Vector2d> ourTeamPrevPositions = new Vector<Vector2d>(); 
	private Vector<Vector2d> ourTeamVelocities = new Vector<Vector2d>(); 
	private Vector<Vector2d> theirTeamPrevPositions = new Vector<Vector2d>(); 
	private Vector<Vector2d> theirTeamVelocities = new Vector<Vector2d>(); 
	
	// agent role information
	private int 		playerID; 		
	private char 		myside;			// my team's side ('l' or 'r') 
	private Vector2d 	offensePos;		// my position in defense
	private Vector2d 	defensePos;		// my position in defense
	private Vector2d 	homePos; 		// my home position 	
	
	// this is used for debug print only
	private String playerName = "Unknown ";
	
	// my action parameters 
	private double 		kickForce;
	private double 		kickDirection;
	
	// other player attributes
	private Vector2d	facingPos = new Vector2d();		// my intended position to face to
	private boolean 	iAmGoalie = false;
	private boolean 	iAmNearestToBall = false;
	private boolean 	iAmNearestTmmToBall = false;
	
	// offside state parameters
	private boolean 	isInOffsidePosition = false;
	private boolean 	myTeamIsOffside = false;
	
	// grabbed ball state parameters
	private boolean 	isGrabbedByMe = false;
	private int 		grabbedBallCount;

	private int			time;			// current server time (in steps) 
	private int			timeKickOff;	// time (in steps) kickoff was done
	private int 		lastTimeBallWasKicked;
	
	private SndSystem 	soundSystem;	// non-critical; is used to play audio clips
	
	/**
	 * Constructor
	 * 
	 * @param transceiver
	 * @param side
	 * @param id
	 * @param isGoalie
	 */
	public PlayerWorld( Transceiver transceiver, 
						char side, int id, boolean isGoalie) {
		this.transceiver = transceiver;
		this.myside = side;
		this.playerID = id;
		this.playerName = "PlayerWorld " + id + "-" + side + " ";
		this.iAmGoalie = isGoalie;
		// fill with zero 2d vectors
		for (int i=0; i<TOS_Constants.TEAM_FULL; i++) {
			ourTeamPrevPositions.add(new Vector2d());
			ourTeamVelocities.add(new Vector2d());
			theirTeamPrevPositions.add(new Vector2d());
			theirTeamVelocities.add(new Vector2d());
		}
		this.soundSystem = new SndSystem();
//		this.soundSystem.setSoundOn(true);
	}
    
  
 	/*******************************************
	 *
	 *  world model update methods
	 *
	 *******************************************/
 
	/**
	 * This method uses information about the new state of the world
	 * in the received packet and updates my world model; 
	 * 
	 * @param 'receivedPacket' is the message from the server
	 */
	public void updateAll( Packet receivedPacket)  throws IOException {		
		
		if ( receivedPacket.packetType == Packet.REFEREE ) {
			
			//****  process game control data and determine the game state
			
			RefereeData aRefereeData = (RefereeData)receivedPacket.data;	
			
//			if (  worldData.getMe().getId() == 6  ) 
//				System.out.println(playerName + " received Packet.REFEREE "+ " period = " + RefereeData.periods[getGamePeriod()] 
//	        			+ " mode = " + RefereeData.modes[getGameMode()] );  
			
			// set the game state as decided by the referee:
			setGamePeriod(aRefereeData.period);
			setGameMode(aRefereeData.mode); 
			
			if (aRefereeData.period == RefereeData.GAME_OVER){
				System.out.println(playerName + " received Packet.REFEREE "+ " period = " + RefereeData.periods[getGamePeriod()] 
        			+ " mode = " + RefereeData.modes[getGameMode()] );  
				transceiver.disconnect();
				Thread.currentThread().interrupt();
			}
			lastTimeBallWasKicked = -1;	// forget previous ball kick/grab time
			if ((aRefereeData.mode == RefereeData.KICK_OFF_L && myside == 'l')
					|| (aRefereeData.mode == RefereeData.KICK_OFF_R && myside == 'r')) 
				timeKickOff = time;
			// team side who continues the game after interruption 
			setSideToContinue(aRefereeData.sideToContinue);			
			/*
			if ( worldData.getMe().getId() == 1 ) {
				System.out.println( playerName + " received Packet.REFEREE "
					//	+ " amIGoalie() = " + amIGoalie()
        			+ " period = " + RefereeData.periods[getGamePeriod()] 
        			+ " mode = " + RefereeData.modes[getGameMode()] );
        	} 
        	*/		
		} else if( receivedPacket.packetType == Packet.INFO ) {
			//****  process some non-critical info
			InfoData infoData = (InfoData)receivedPacket.data;
			if (infoData.info == InfoData.SOUND_ON_OFF) {
				/*
				try {
					System.out.println(playerName + " receiving Packet.INFO: " 
							+ receivedPacket.writePacket() );
				} catch (Exception e ) {
					System.out.println("Error while receiving Packet.INFO " + e );
				}
				*/
				// toggle sound on/off
				boolean setting = (infoData.info1 == 1);
//				soundSystem.setSoundOn(setting);
			}
		} else if( receivedPacket.packetType == Packet.SEE ) {
			//****  process visual data (most critical)  ****
			//if (  worldData.getMe().getId() == 6  ) 
				//System.out.println(playerName + " received Packet.SEE ");  
			processSeeData((SeeData)receivedPacket.data);
		}
		
		// default settings for robustness
		super.setBallKickable(false);
		super.setBallControlledByMe(false);
		
		try {
			// set these two shortcut variables (they are used in some methods)
			super.setDirection2Ball(worldData.getMe().getPosition()
							.direction(worldData.getBall().getPosition()));
			super.setDistance2Ball(worldData.getMe().getPosition()
							.distance(worldData.getBall().getPosition()));
			// reset isBallKickable if applicable
			super.setBallKickable(super.getDistance2Ball() < TOS_Constants.BALLCONTROLRANGE);
			// am I the last player who had kicked the ball?
			super.setBallControlledByMe( amIBallController() ); 
		} catch (NullPointerException e) {}
	
	} // updateAll
	
	/**
	 * This method processes the SEE data 
	 * (i.e. what this player 'sees')
	 * and updates player's world model
	 */
	private void processSeeData(SeeData aSeeData) {
		// memorize the time when this update arrived
		time = aSeeData.time;
		
		try {
			if (time > 2 && 
					aSeeData.player.isUserControlled() != worldData.getMe().isUserControlled()) {
				// player control changed
				if (aSeeData.player.isUserControlled())
					System.out.println(time + " " + playerName + " is NOW controlled by user " 
															+ aSeeData.player.getControllerID()); 
				if (worldData.getMe().isUserControlled())
					System.out.println(time + " " + playerName + " is NO LONGER controlled by user " 
															+ worldData.getMe().getControllerID()); 				
			}
		} catch (NullPointerException e) {
			System.out.println("processSeeData caught:" + e);
		}
		
		// convert aSeeData coordinates so that I perceive everything 
		// like my team is playing on the left-hand side
		worldData = new WorldData( aSeeData, myside, iAmGoalie, transceiver ); 
		
		//System.out.println(playerName
					//+ " worldData.getMyTeam().size()=" + worldData.getMyTeam().size()  
					//+ " worldData.getTheirTeam().size()=" + worldData.getTheirTeam().size() ); 
				
		// check for Offside (this info may be in the received SEE packet)
		checkOffsidePosition();	
		
		determineNearestPlayerToBall();
		
		// am I nearest to ball?
		iAmNearestToBall 
				= nearestPlayerToBall.equals( worldData.getMe() );
		// am I nearest in my team?
		iAmNearestTmmToBall  
				= nearestTmmToBall.equals( worldData.getMe() );
		
		/*
		if ( iAmNearestToBall )	
			System.out.println(playerName 
						+ " I'm nearest: " + nearestPlayerToBall.getId() 
						+ " " + nearestPlayerToBall.getSide() 
						+ " minTmmDistance=" + (float)minTmmDistance 
						+ " minOppDistance=" + (float)minOppDistance ); 
		*/
		// check for defense/attack situation
		determineBallPossession();
		
		// estimate velocities of all players in my team
		updatePlayerVelocities(worldData.getMyTeam(), 
							ourTeamPrevPositions, ourTeamVelocities);
		
		// estimate velocities of all players in the opponent team
		updatePlayerVelocities(worldData.getTheirTeam(), 
							theirTeamPrevPositions, theirTeamVelocities);
		
		// estimate ball velocity
		Vector2d.subtract(worldData.getBall().getPosition(), 
										getPreviousBallPosition(), getBallVelocity());
		worldData.getBall().setVelocity(getBallVelocity()); // ball speed is also updated 
		getPreviousBallPosition().setXY(worldData.getBall().getPosition());  
		
		if (worldData.getMe().isGoalie()) {
			/*
			System.out.println(playerName 
					+ "  controllerId=" + worldData.getBall().controllerId 
					+ "  myID=" + worldData.getMe().getId()
					+ "  controllerType=" + worldData.getBall().controllerType
					+ "  myType=" + worldData.getMe().getSide()
					+ "  isGrabbed=" + worldData.getBall().isGrabbed
					);
			*/
			// figure it out if the server has executed my Grab Ball command
			if(worldData.getBall().controllerId == worldData.getMe().getId() 
					&& worldData.getBall().controllerType == worldData.getMe().getSide()
					&& worldData.getBall().isGrabbed) {
				isGrabbedByMe = true;
				grabbedBallCount--;
				//System.out.println(playerName + "  ball grabbed by me");
			} else {
				isGrabbedByMe = false;
				// reset counter
				grabbedBallCount = MAX_GRABBED_STEPS;
				//System.out.println(playerName + "  ball NOT grabbed by me");
			}
		}		
	}
  
	/**
	 * this method updates estimated player velocities in given team.
	 * (TODO: could be better moved in class WorldData)
	 */
	private void updatePlayerVelocities(Vector<MyPlayer> team, 
					Vector<Vector2d> positions, Vector<Vector2d>velocities) {
		for (int i=0; i<team.size(); i++) {
			MyPlayer plr = team.elementAt(i);		// current player state (except velocity)
			Vector2d pos = positions.elementAt(i);	// previous position
			Vector2d vel = velocities.elementAt(i);	// previous velocity
			
			Vector2d.subtract(plr.getPosition(), pos, vel);	// calculate velocity
			plr.setVelocity(vel);			// set current player velocity
			pos.setXY(plr.getPosition());  // save current position
			/*
			if (time%500 == 0 && playerID == 1)
				System.out.println(plr.getId() + "-" + plr.getSide() 
						+ " velocity: " + plr.getVelocity());
			*/
		}
	}

	/**
	 *  This method determines whether I am controlling the ball as 
	 *  perceived by the server.
	 *  
	 * @return true if I can kick
	 */
	private boolean amIBallController() {				
		if( worldData.getBall().controllerType == worldData.getMe().getSide() &&  // my side
		     worldData.getBall().controllerId == worldData.getMe().getId() ) 	 // my id
		     // server confirms that I am the ball controller
			return true;
		else 
			return false;
		
	} 
		
  
	/** 
	 *  This method memorizes the offside state warning received 
	 *  from the soccer server and resets this state after 
	 *  MAX_OFFSIDE_STEPS simulation cycles. 
	 *  It assigns two boolean variables, isOffsidePosition and 
	 *  myTeamIsOffside based on whether the server has 
	 *  detected the offside situation or not. It also updates counter 
	 *  offsideStepCount which is used for memorizing.
	 *  
	 *  It is meant that the server is warning this player
	 *  that the game would be interrupted if his team gains 
	 *  the control of the ball. This player must move back 
	 *  to prevent this from happening (implemented elsewhere). 
	 *  This information is sent to all players simultaneously. 
	 *  
	 *  This is an example of reactive behavior by this player. 
	 *  Proactive behavior would be better. This means predicting 
	 *  the situation and avoidng getting offside.
	 *  
	 *  For more about the offside rule, see comments in class
	 *  soccer.server.SoccerRules
	 * Also view the videos: 
	 * http://www.youtube.com/watch?v=jKxy45xqgSo&NR=1
	 * http://www.youtube.com/watch?v=JiwmR6CC0Bk 
	 */
	private void checkOffsidePosition() {
		// default state
		myTeamIsOffside = false;
		isInOffsidePosition = false;
	}
	
	
	/**
	 * This method decides which team possesses the ball based on the
	 * raw indicator that is calculated in class WorldData in each cycle; 
	 * This method assigns class variable 'whoseBallIs'.
	 * Decisions are made by smoothening raw data over several cycles; for
	 * this purpose, class variable 'ballPossIndicator' is used.
	 */
	private void determineBallPossession() {
		
		if ( getGameMode() == RefereeData.PLAY_ON ) {			
			// make ball possession decision in current cycle
			double delta = worldData.getBallPossession();
			
			if (delta == TOS_AgentConstants.NEUTRAL_BALL)
				ballPossIndicator = 0;	// reset filter
					
			// do exponential filtering of -1, 0, +1 'delta' sequence over time.
			// if ballPossWeight=1, no filtering is done;
			// if ballPossWeight=0, value from the previous step is just reused
			
			ballPossIndicator = BALLPOSWEIGHT*delta 
						+ ( 1 - BALLPOSWEIGHT )*ballPossIndicator; 		
			
			whoseBallIs = TOS_AgentConstants.NEUTRAL_BALL;	// default value
	
			// make the decision about ball possession
			if( ballPossIndicator < -BALLPOSTHRESHOLD ) 
				whoseBallIs = TOS_AgentConstants.OUR_BALL;		
			else if( ballPossIndicator > BALLPOSTHRESHOLD )
				whoseBallIs =  TOS_AgentConstants.THEIR_BALL;		
			else 
				whoseBallIs = TOS_AgentConstants.NEUTRAL_BALL; 	
	
			// limit the indicator growth 
			if( ballPossIndicator < -2.0 ) 
				ballPossIndicator = -2.0;
				
			if( ballPossIndicator > 2.0 ) 
				ballPossIndicator = 2.0;
				
			// if ball is grabbed, override all above
			if ( worldData.getBall().isGrabbed ) {
				if ( worldData.getBall().controllerType == myside ) {
					whoseBallIs = TOS_AgentConstants.OUR_BALL;		// grabbed by own goalie (including myself)
					ballPossIndicator = -2.0;
				} else {
					whoseBallIs = TOS_AgentConstants.THEIR_BALL;		// grabbed by opponent goalie
					ballPossIndicator = 2.0;
				}
			}
			
			// override in the kick off situation
			if (time - timeKickOff < 3/TOS_Constants.SIM_STEP_SECONDS) {
				whoseBallIs = TOS_AgentConstants.OUR_BALL;
				ballPossIndicator = -2.0;				
			}
				
		} else {
			// ball possession when game is interrupted by referee
			if ( getSideToContinue() == worldData.getMe().getSide() ) { 
				ballPossIndicator = -2.0;
				whoseBallIs = TOS_AgentConstants.OUR_BALL;
			} else {
				ballPossIndicator = 2.0;
				whoseBallIs = TOS_AgentConstants.THEIR_BALL;
			}
		}
	} // determineBallPossession
	

	/*******************************************
	 *
	 * miscellaneous computations 
	 *
	 *******************************************/	 

		
	/**
	 *  This method determines the nearest player to the ball.
	 *  Class instance variables 'minPlayerDistance', 'minTmmDistance', and
	 *  'minOppDistance' assigned here could be reused in other methods
	 */
	private void determineNearestPlayerToBall() {
		
		nearestOppToBall = null;
		minOppDistance = Float.MAX_VALUE;	// a very big number
		
		for( int i = 0; i < worldData.getTheirTeam().size(); i++ ) {
			MyPlayer player = (MyPlayer) worldData.getTheirTeam().elementAt( i );
			double dis2ball = player.getPosition().distance( worldData.getBall().getPosition() );
			if( dis2ball < minOppDistance ) {
				minOppDistance = dis2ball;
				nearestOppToBall = player;
			}
		}
		
		nearestTmmToBall = null;
		minTmmDistance = Float.MAX_VALUE;	

		for( int i = 0; i < worldData.getMyTeam().size(); i++ ) {
			MyPlayer player = (MyPlayer) worldData.getMyTeam().elementAt( i );
			double dis2ball = player.getPosition().distance( worldData.getBall().getPosition() );
			if( dis2ball < minTmmDistance ) {
				minTmmDistance = dis2ball;
				nearestTmmToBall = player;
			}
		}

		if ( minTmmDistance < minOppDistance ) {
			nearestPlayerToBall = nearestTmmToBall;
			minPlayerDistance = minTmmDistance; 	
		} else {
			nearestPlayerToBall = nearestOppToBall; 
			minPlayerDistance = minOppDistance; 	
		}				
	}

	/**
	 * This method returns one of the three heuristic levels of risk 
	 * (zero, moderate, and high) of me getting in the offside 
	 * position. The method also sets the new destination point to 
	 * avoid offside if this risk is high.
	 * Because both the player and the offside line itself are 
	 * continuously moving, their predicted positions are considered.
	 * 
	 * This is an example of proactive behavior.
	 * 
	 * For more about the offside rule, see comments in class
	 * soccer.server.SoccerRules
	 * Also view the videos: 
	 * http://www.youtube.com/watch?v=JiwmR6CC0Bk
	 * http://www.youtube.com/watch?v=jKxy45xqgSo&NR=1  
	 */
	public int calcOffSideRisk() {
		return 0;
	}
	

	/**
	 *  this method determines where I am planning to move to
	 *  if I have got into the offside position 
	 */
	public void determineWhereToMoveOffSide() {
		// pulling back from the opponent side by 15 meters 
		getDestination().setXY( worldData.getMe().getPosition().getX() - 15, 
							worldData.getMe().getPosition().getY() );
	}

	
	/**
	 *  This method determines where I am planning 
	 *  to move to without the ball; 
	 *  it updates the class instance variable 'destination' 
	 */ 
	public void determineWhereToMove() {
		
		if ( getGamePeriod() != RefereeData.FIRST_HALF &&
				getGamePeriod() != RefereeData.SECOND_HALF ) { 	// not actual game play
			// ** pre-game or between halves **
			
			// I am just going to my home position 			
			setMoveToHomePosAction();
		
		} else {
			// ** first or second half of the game **
			
			if ( getGameMode() == RefereeData.BEFORE_KICK_OFF ||	
					getGameMode() == RefereeData.KICK_OFF_L ||
					getGameMode() == RefereeData.KICK_OFF_R ) {
				
				// I am just going to my home position 
				setMoveToHomePosAction();
				
			} else if ( getGameMode() == RefereeData.THROW_IN_L ||	
					getGameMode() == RefereeData.THROW_IN_R ||
					getGameMode() == RefereeData.CORNER_KICK_L ||
					getGameMode() == RefereeData.CORNER_KICK_R || 
					getGameMode() == RefereeData.GOAL_KICK_L ||
					getGameMode() == RefereeData.GOAL_KICK_R ||
					getGameMode() == RefereeData.OFFSIDE_L ||
					getGameMode() == RefereeData.OFFSIDE_R ) {
		
				// ** game was interrupted by the referee **
				
				if ( iAmNearestTmmToBall && !amIGoalie()
							&& worldData.getMe().getSide() == getSideToContinue() ) {
					setActionType(TOS_Constants.CHASE);		// trying to repossess the ball
				} else {
					// I decide to move somewhere else without the ball.
					determinePlayerPosSpecial(); 
				}
			
			} else {
				/** 
				 * ** regular game play; I am not possessing or chasing the ball **
				 * 
				 * this case is of critical importance, as 90 per cent of the time 
				 * it determines my actions on figuring it out where to move to. 
				 * possible improvements:
				 * (1) if the ball is controlled by us, I should open myself for 
				 * 		receiving a pass
				 * (2) if the ball is controlled by them, I should be blocking 
				 * the closest opponent from receiving the pass by him
				 * (3) attackers and defenders should be using different tactics
				 */
				determinePlayerPos(); 
			}
		}
	}
	

	/**
	 *  this method sets class instance variables to ensure that 
	 *  I go to my home position
	 */
	private void setMoveToHomePosAction() {
		double distance2Home = worldData.getMe().getPosition()
													.distance( homePos );		
		if ( distance2Home > 1.5 ) {
			// keep moving to home position
			getDestination().setXY( homePos );	
		} else {
			// just turn body to ball
			double direction2Ball 	= worldData.getMe().getPosition()
								.direction( worldData.getBall().getPosition() );
			if ( Math.abs( worldData.getMe()
									.getDirection() - direction2Ball ) > 5.0 ) {
				setActionType(TOS_Constants.TURN);
				facingPos.setXY( worldData.getBall().getPosition() );	
			} else 
				setActionType(TOS_Constants.NOACTION);	
		}
	}
	
	
	/**
	 *  This method determines the position where I player should
	 *  move to and updates my 'destination' which is a weighed sum
	 *  of my fixed 'reference' position and current ball position 
	 */
	private void determinePlayerPos() {
		
		if ( amIGoalie() ) {
			// all goalie movements are calculated separately
			determineGoaliePos();
			return;
	//<=====
		}
		
		// I am a field player 
		double weight = 0.30; 			// magic number 	
		double xball = worldData.getBall().getPosition().getX();
		double yball = worldData.getBall().getPosition().getY();
		double xavg;
		double yavg;		
		Vector2d refPsn;		// my 'reference' position
		
		if( whoseBallIs == TOS_AgentConstants.OUR_BALL ) 	
			refPsn = offensePos;	
		else if( whoseBallIs == TOS_AgentConstants.THEIR_BALL ) 
			refPsn = defensePos;	
		else 
			refPsn = homePos;
		
		double xref = refPsn.getX(); 
		double yref = refPsn.getY();
		
		// weighted average coordinates
		xavg = xball*weight + xref*(1-weight);
		yavg = yball*weight + yref*(1-weight);	
		// modify my xavg in the attacking situation if applicable
		xavg = calcAdvancedXinAttack(xavg);		
		
		// wrap them in vector (could be modified below)
		Vector2d dest = new Vector2d(xavg, yavg);
		
		// prevent me from crossing own goal line
		dest.setX(Math.max(-Field_Constants.LENGTH/2.0 + 2.0, dest.getX()));
		// prevent me from crossing opponent goal line
		dest.setX(Math.min(Field_Constants.LENGTH/2.0 - 2.0, dest.getX()));
		
		setDestination( dest ); 
		//if ( worldData.getMe().getId() < 3 && worldData.getMe().getSide() == 'l') 
			//System.out.println(playerName  
			//+ " destination = " + dest + " whoseBallIs = " + whoseBallIs);
	}

	/**
	 * This method modifies my destination point if I can block the
	 * opponent player from the ball (magic numbers used).
	 * I am doing so if I am the closest player in my team.
	 * 
	 * @param myDest - my intended destination
	 * @param oppPlayer
	 * @return
	 */
	@SuppressWarnings("unused")
	private Vector2d calcBlockingPosition(MyPlayer oppPlayer, Vector2d myDest) {		
		return null;
	}
	
	/**
	 * This method advances the destination x-coordinate for 
	 * some players in attack
	 * 
	 * @param xdest - the original destination
	 * @return modified xdest
	 */
	private double calcAdvancedXinAttack(double xdest) {
		return xdest;
	}
	
	/**
	 * This method determines the recommended position that allows me to
	 * stay away from given player
	 * 
	 * @param otherPlayer - player I should stay away from
	 * @param myDest - my intended destination (input/output)
	 * @param minDist - minimal distance at that I should care
	 * @return - position where I should move to
	 */
	public Vector2d calcStayAwayPosFromPlayer(MyPlayer otherPlayer, 
									Vector2d myDest, double minDist) {
		return myDest;
	}
	
	
	/** 
	 * This method determines the position where I goalie should 
	 * move and updates my 'destination' which is a weighed sum 
	 * of my fixed 'reference' position and current ball position
	 */
	private void determineGoaliePos() {
		
		double xball = worldData.getBall().getPosition().getX();
		double yball = worldData.getBall().getPosition().getY();
		
		double weightX = 0.10; 			// magic number
		double weightY = 0.25; 			// magic number
		// my 'reference' position
		double xref = homePos.getX(); 
		double yref = homePos.getY();
				
		// default weighted average coordinates
		double xavg = xball*weightX + xref*(1-weightX);
		double yavg = yball*weightY + yref*(1-weightY);	
		/*
		if ( worldData.getMe().getSide() == 'l' && time%50 == 0 )
			//System.out.println(playerName + " mode = " + RefereeData.modes[getGameMode()] );
			System.out.println(playerName + " xref=" + (float)xref 
					+ " xball=" + (float)xball 
					+ " xavg=" + (float)xavg );
		*/
		// prevent from moving too far away from the goal in the corner kick mode
		if ( (getGameMode() == RefereeData.CORNER_KICK_R 
							&& worldData.getMe().getSide() == 'l')
				|| (getGameMode() == RefereeData.CORNER_KICK_L 
							&& worldData.getMe().getSide() == 'r') ) {
			// corner kick near my goal; stay tight by overriding defaults
			xavg = -TOS_Constants.LENGTH/2.0 + 1.5;
			yavg = Util.sign( yball ) * TOS_Constants.GOAL_WIDTH/2;
			/*
			if ( worldData.getMe().getSide() == 'l' && time%20 == 0 ) 
				System.out.println(playerName + " dest= " 
						+ (float)xavg + ", " + (float)yavg 
						+ " mypos " + worldData.getMe().getPosition());
			*/
		} else {
			//-- regular game play with the ball controlled by someone else 
			// I should not leave the goal area
			double xmax = -TOS_Constants.LENGTH/2.0 + TOS_Constants.PENALTY_DEPTH;
			if ( xavg > xmax )
				xavg = xmax;
			if ( Math.abs( yavg ) > TOS_Constants.PENALTY_WIDTH/2 )
				yavg = Util.sign( yavg ) * TOS_Constants.PENALTY_WIDTH/2;
		}
		
		// prevent from crossing own goal line
		xavg = Math.max(-Field_Constants.LENGTH/2.0 + 1.0, xavg);		
		Vector2d dest = new Vector2d( xavg, yavg );		
		// find better position near own goal if possible
		dest = calcAdjsutsedGoaliePos(dest);	
		// stay a little away from  own goal line
		dest.setX(Math.max(-Field_Constants.LENGTH/2.0 + 1.0, dest.getX()));

		setDestination( dest ); 
		
		//if ( worldData.getMe().getSide() == 'l' && time%50 == 0 ) 
			//System.out.println(playerName + " destination = " + getDestination());
	}
			
	/**
	 * This method determines the optimized goalie position given the 
	 * location of the ball. Best position for me goalie has the minimal 
	 * risk of ball interception, as perceived by the opponent. This is 
	 * the worst case for my team. So I want to fill this gap.
	 * 
	 * @param dest - destination point (input/output)
	 */
	private Vector2d calcAdjsutsedGoaliePos(Vector2d dest) {
		return dest;
	}
	
	/**
	 * This method determines the least risky direction for shooting at 
	 * the goal by the agent playing on given 'side';
	 * some magic numbers are used
	 */
	public BallPassData calcBestShootingDirection(Vector2d startPos, char side) {
		/**
		 * THis is a stub; player always shoots at the goal center
		 */
		BallPassData bestKickData = new BallPassData();
		
		if (side == 'l')
			bestKickData.setEndPoint(new Vector2d(getOppGoalPosition()));
		else
			bestKickData.setEndPoint(new Vector2d(getOwnGoalPosition()));
		
		bestKickData.setDirection(startPos.direction(bestKickData.getEndPoint()));
		bestKickData.setForce(TOS_Constants.MAXKICK);
		bestKickData.setRisk(0);		
		
		return bestKickData;  
	}

	
	/**
	 * This method returns the evaluated ball kick from given start to end;
	 * the risk of getting the ball out of field may or may not be calculated.
	 * The calculations are done as perceived by given side ('l' own player,
	 * 'r' opponent). 
	 */
	public BallPassData evaluateBallKick(Vector2d startPos, 
							Vector2d endPos, boolean outOfFieldRisk, char side) {
		
		BallPassData passData = null;	// this is calculated as a return value
		char their_side = (side == 'l') ? 'r' : 'l';
		
		double pass_dir = startPos.direction(endPos);
		double risk = 0, risk1 = 0, risk2 = 0, risk3 = 0, gain = 0;
		
		if (outOfFieldRisk) {
			// risk1: the possibility of kicking the ball out of the soccer field
			risk1 = World.calcRiskOfKickingOutOfField(pass_dir, endPos);
		}
		// risk2: risk imposed by the opponents 			
		risk2 = calcRiskOfInterception(their_side, startPos, endPos); 
		
		// risk3: kicking towards own goal
		double distToOwnGoal;
		double ownGoalDir;
		if (side == 'l') {
			distToOwnGoal = startPos.distance(getOwnGoalPosition());
			ownGoalDir = startPos.direction(getOwnGoalPosition());
		} else {
			distToOwnGoal = startPos.distance(getOppGoalPosition());
			ownGoalDir = startPos.direction(getOppGoalPosition());
		}
		if (distToOwnGoal < 30) {
			double delta = Math.abs(Util.normal_dir(pass_dir - ownGoalDir));
			double threshold = 20;	// magic number
			if (delta < threshold) {
				risk3 = 2*(threshold-delta);
			}
		}
		
		risk = risk1 + risk2 + risk3;
		risk = Math.min(risk, 100);			// apply upper limit
		gain = calcBallPassGain(endPos);	// side 'r' is looking to maximize this
		// kicking force will be calculated elsewhere
		passData = new BallPassData(pass_dir, 0, risk, gain, endPos);
		/*
		System.out.println(playerName + " ball kick risk: " + (int)risk + " = " + 
				(int)risk1 + " + " + (int)risk2 + " + " + (int)risk3
				+ "  total risk = " + (int)passData.risk);
		*/		
		return passData;
	}
	
	/**
	 * This method returns the tactical gain of passing the ball;
	 * only the location of the end point matters; the less gain, the better  
	 */
	public double calcBallPassGain(Vector2d endPos) {
		double xMax = Field_Constants.LENGTH/2.0;
		Vector2d goal = new Vector2d(xMax, 0);
		if (endPos.getX() > xMax)	// this may happen for clearing the ball
			endPos = new Vector2d(xMax, endPos.getY());
		double dist = endPos.distance(goal);
		double gain = dist*dist/Field_Constants.LENGTH;
		// increase gain near the goal line
		if (xMax - endPos.getX() < 2)
			gain = gain + 50*( 2 - (xMax - endPos.getX()) );
		return gain;
	}
			
		
	/**
	 *  this is a stub.
	 *  this method determines the position where I player should move in
	 *  the special situations other than regular game play
	 */
	private void determinePlayerPosSpecial() {
		determinePlayerPos();
	}
	
	/**
	 *  returns count of opponent players in the circle with given center 
	 *  and radius; if 'fuzzy' is true, this count is weighed by the 
	 *  deviation from the center; the fuzzy 'count' is 2.0 in the center 
	 *  and decreases to zero at the circle boundary.
	 */
	public double countOpponentsInCircle( Vector2d center, double radius, boolean fuzzy ) {
		double count = 0;		
		for( int i = 0; i < worldData.getTheirTeam().size(); i++ ) {
			MyPlayer player = (MyPlayer) worldData.getTheirTeam().elementAt( i );
			double disToPlayer = player.getPosition().distance( center );
			if( disToPlayer < radius ) 
				if (fuzzy)
					count = count + 2.0*(1.0 - disToPlayer/radius);
				else
					count++;
		}
		return count;
	}
		
	/**
	 *  returns count of teammates in the circle with given center 
	 *  and radius; if 'fuzzy' is true, this count is weighed by the 
	 *  deviation from the center; the fuzzy 'count' is 2.0 in the center 
	 *  and decreases to zero at the circle boundary.
	 */
	public double countTeammatesInCircle( Vector2d center, double radius, boolean fuzzy ) {
		double count = 0;		
		for( int i = 0; i < worldData.getMyTeam().size(); i++ ) {
			MyPlayer player = (MyPlayer) worldData.getMyTeam().elementAt( i );
			double disToPlayer = player.getPosition().distance( center );
			if( disToPlayer < radius ) 
				if (fuzzy)
					count = count + 2.0*(1.0 - disToPlayer/radius);
				else
					count++;
		}
		return count;
	}
		
	/**
	 *  returns count of players in the circle with given center and 
	 *  radius; (my side is always 'l')
	 */
	public double countPlayersInCircle(char side, Vector2d center, 
										double radius, boolean fuzzy ) {
		double count = 0;	
		if( side == 'l') {
			count = countTeammatesInCircle(center, radius, fuzzy);
		} else {
			count = countOpponentsInCircle(center, radius, fuzzy);
		}
		return count;
	}
		
	/**
	 *  returns count of opponent players in the sector with given center, central
	 *  line direction, half-width (in degrees), and maximal distance;
	 *  if 'fuzzy' is true, this count is weighed by the deviation from the 
	 *  central direction; 
	 *  the fuzzy 'count' is 2.0 on this line and decreases with the angular 
	 *  distance from it to zero on the sector boundary.
	 */
	public double countOpponentsInSector( Vector2d center, double biSectorDirection, 
									double halfWidth, double maxDistance, boolean fuzzy ) {
		double count = 0;
		for( int i = 0; i < worldData.getTheirTeam().size(); i++ ) {
			MyPlayer player = (MyPlayer) worldData.getTheirTeam().elementAt( i );
			double dis2opponent = player.getPosition().distance( center );
			if( dis2opponent < maxDistance ) {
				double dirToPlayer = center.direction(player.getPosition());
				double delta = Math.abs(Util.normal_dir(biSectorDirection - dirToPlayer));
				if (delta < halfWidth) {
					if (fuzzy)
						count = count + 2.0*(1.0 - delta/halfWidth);
					else
						count++;
				}
			}
		}
		return count;
	}
		
	/**
	 *  returns count of teammates in the sector with given center, central
	 *  line direction, half-width (in degrees), and maximal distance;
	 *  if 'fuzzy' is true, this count is weighed by the deviation from the 
	 *  central direction; 
	 *  the fuzzy 'count' is 2.0 on this line and decreases with the angular 
	 *  distance from it to zero on the sector boundary.
	 */
	public double countTeammatesInSector( Vector2d center, double biSectorDirection, 
									double halfWidth, double maxDistance, boolean fuzzy ) {
		double count = 0;
		for( int i = 0; i < worldData.getMyTeam().size(); i++ ) {
			MyPlayer player = (MyPlayer) worldData.getMyTeam().elementAt( i );
			double dis2teammate = player.getPosition().distance( center );
			if( dis2teammate < maxDistance ) {
				double dirToPlayer = center.direction(player.getPosition());
				double delta = Math.abs(Util.normal_dir(biSectorDirection - dirToPlayer));
				if (delta < halfWidth) {
					if (fuzzy)
						count = count + 2.0*(1.0 - delta/halfWidth);
					else
						count++;
				}
			}
		}
		return count;
	}
		
	/**
	 * This method returns a heuristic value of the risk imposed to ball by the 
	 * players on given side near to endPos and on the way of the rolling 
	 * ball from startPos to endPos; risk is measured between 0 and 100;
	 * some magic numbers are used
	 */
	public double calcRiskOfInterception(char side, Vector2d startPos, Vector2d endPos) {
		
		//--- move the end point by coeff times further away to take into 
		//    account the remote opponents, if any
		double COEFF = 1.5;
		Vector2d endPosPlus = new Vector2d(
				startPos.getX() + (endPos.getX() - startPos.getX())*COEFF,
				startPos.getY() + (endPos.getY() - startPos.getY())*COEFF
			);
		double distance = startPos.distance(endPosPlus);	// increased distance
		double direction = startPos.direction(endPos);
		
		//--- estimate risks 1..5 (magic numbers everywhere)
		double risk1 = 0, risk2 = 0, risk3 = 0, risk4 = 0, risk5 = 0;
		double HALFWIDTH = 5;	// narrow sector half-width in degrees
		
		// risk1: opponents in the narrow sector
		double oppCount1 = countPlayersInSector(side, startPos, direction, 
												HALFWIDTH, distance, true);
		if (oppCount1 > 1) 
			return 100;
		else 
			risk1 = 25*oppCount1;
		
		// risk2: opponents in the wider sector
		double oppCount2 = countPlayersInSector(side, startPos, direction, 
												2*HALFWIDTH, distance, true);
		risk2 = 10*oppCount2;		
		
		// risk3: opponents in more wider sector
		double oppCount2a = countPlayersInSector(side, startPos, direction, 
												3*HALFWIDTH, distance, true);
		risk3 = 5*oppCount2a;	
		
		// risk4: opponents in the wide sector close to me
		double oppCount3 = countPlayersInSector(side, startPos, direction, 
												4*HALFWIDTH, Math.min(distance, 12), true);
		MyPlayer opp = null;
		if (oppCount3 > 0) {
			opp = getNearestOpponentInSector(startPos, direction, 4*HALFWIDTH);
			double dist = startPos.distance(opp.getPosition());
			risk4 = 5*oppCount3 + 60/(dist + 2);	// risk increases as distance decreases
		}
		
		// risk5: opponents in a circle near the end point
		double circleRadius = Math.pow(distance, 1.2)/10;  
		double oppCount4 = countPlayersInCircle(side, endPosPlus, circleRadius, true); 
		if (oppCount4 > 1) 
			return 100;		
		else 
			risk5 = 10*oppCount4;
		
		double risk = risk1 + risk2 + risk3 + risk4 + risk5;
		
		//System.out.println(playerName + " interception risk: " + (int)risk + " = " + (int)risk1 + " + " + (int)risk2 
				//+ " + " + (int)risk3 + " + " + (int)risk4 + " + " + (int)risk5 
				//+ "  dir = " + (int)direction);
		
		return Math.min(risk, 100);		// apply upper limit
	}
	
	
	/**
	 *  This method returns count of players in the sector with given center and 
	 *  radius; (my side is always 'l'). This count can be either integer or fuzzy. 
	 */
	public double countPlayersInSector(char side, Vector2d center, double biSectorDirection, 
				double halfWidth, double maxDistance, boolean fuzzy ) {
		double count = 0;	
		if( side == 'l') {
			count = countTeammatesInSector(center, 
					biSectorDirection, halfWidth, maxDistance, fuzzy);
		} else {
			count = countOpponentsInSector(center, 
					biSectorDirection, halfWidth, maxDistance, fuzzy);
		}
		return count;
	}
		
	/**
	 * this method returns the nearest opponent in
	 * given sector whose center is 'this' player;
	 * returns null if there are no opponents
	 */
	public MyPlayer getNearestOpponentInSector(MyPlayer thisPlayer, 
							double biSectorDirection, double halfWidth) {
		Vector2d point = thisPlayer.getPosition();
		MyPlayer nearestOppPlayer = getNearestOpponentInSector(point, 
											biSectorDirection, halfWidth);
		return nearestOppPlayer;
	}
		
	/**
	 * this method returns the nearest opponent player in
	 * given sector;
	 * returns null if there are no opponents
	 */
	public MyPlayer getNearestOpponentInSector(Vector2d center, 
							double biSectorDirection, double halfWidth) {
		MyPlayer nearestOppPlayer = null;
		double minDistance = Double.MAX_VALUE;
		for( int i = 0; i < worldData.getTheirTeam().size(); i++ ) {
			MyPlayer player = (MyPlayer) worldData.getTheirTeam().elementAt( i );
			double dirToPlayer = center.direction(player.getPosition());
			double delta = Math.abs(Util.normal_dir(biSectorDirection - dirToPlayer));
			if (delta < halfWidth) {
				// player is inside sector
				double dis2opponent = player.getPosition().distance(center);
				if( dis2opponent < minDistance ) { 
					minDistance = dis2opponent;
					nearestOppPlayer = player;
				}
			}
		}
		return nearestOppPlayer;
	}
						
	/**
	 * this method returns the nearest opponent player to given position;
	 * returns null if there are no opponents
	 */
	public MyPlayer getNearestOpponent(Vector2d position) {
		MyPlayer nearestOppPlayer = null;
		double minDistance = Double.MAX_VALUE;
		for( int i = 0; i < worldData.getTheirTeam().size(); i++ ) {
			MyPlayer player = (MyPlayer) worldData.getTheirTeam().elementAt( i );
			double dis2opponent = player.getPosition().distance(position);
			if( dis2opponent < minDistance ) { 
				minDistance = dis2opponent;
				nearestOppPlayer = player;
			}
		}
		return nearestOppPlayer;
	}
			
			
	/**
	 * this method returns the nearest teammate to given position;
	 * returns null if there are no such
	 */
	public MyPlayer getNearestTeammate(Vector2d position) {
		MyPlayer nearestTmmPlayer = null;
		double minDistance = Double.MAX_VALUE;
		for( int i = 0; i < worldData.getMyTeam().size(); i++ ) {
			MyPlayer player = (MyPlayer) worldData.getMyTeam().elementAt( i );
			if (!worldData.getMe().equals(player)) {
				double dis2teammate = player.getPosition().distance(position);
				if( dis2teammate < minDistance ) { 
					minDistance = dis2teammate;
					nearestTmmPlayer = player;
				}
			}
		}
		return nearestTmmPlayer;
	}
			
	/**
	 * this method returns the nearest player to given position;
	 * returns null if there are no such
	 */
	public MyPlayer getNearestPlayer(Vector2d position) {
		MyPlayer tmm = getNearestTeammate(position);
		MyPlayer opp = getNearestOpponent(position);
		if (position.distance(tmm.getPosition()) 
				< position.distance(opp.getPosition()))
			return tmm;
		else
			return opp;
	}
		

	/*******************************************
	 *
	 * public get/set access methods for class variables
	 *
	 *******************************************/
	 
	public WorldData getWorldData() {
		return worldData;
	}

	public boolean amIGoalie() {
		return ( worldData.getMe().isGoalie() );
	}		

	public boolean amInearestToBall() {
		return iAmNearestToBall;
	}		

	public boolean amInearestTmmToBall() {
		return iAmNearestTmmToBall;
	}		

	public boolean amItheFastestToBall() {
		return worldData.getMe().equals(worldData.getFastestTeammate());
	}
	
	public boolean amIthe2ndFastestToBall() {
		return worldData.getMe().equals(worldData.getFastestTeammate2());
	}
	
	public boolean isInOffsidePosition() {
		return isInOffsidePosition;
	}
	
	public void setIAmOffside(boolean s) {
		isInOffsidePosition = s;
	}

	public boolean isMyTeamOffside() {
		return myTeamIsOffside;
	}		

	public Vector2d getHomePos() {
		return homePos;
	}

	public Ball getBall() {
		return worldData.getBall();	
	}
	
	public void setKickForce( double kf ) {
		kickForce = kf;
	}

	public double getKickForce() {
		return kickForce;	
	}
	
	public void setKickDirection( double kd ) {
		kickDirection = kd;
	}

	public double getKickDirection() {
		return kickDirection;	
	}

	public int getBallPossession() {
		return whoseBallIs; 
	}
	
	public void setBallPossession( int possession ) {
		whoseBallIs = possession; 
	}	

	public boolean isGrabbedByMe() {
		return isGrabbedByMe;
	}

	public int getGrabbedBallCount() {
		return grabbedBallCount;
	}

	public char getMySide() {
		return myside; 
	}	

	public Vector2d getFacingPos() {
		return facingPos;
	}

	public void setFacingPos( Vector2d pos ) {
		facingPos = pos;
	}

	public SndSystem getSoundSystem() {
		return soundSystem;
	}

	public int getTime() {
		return time;
	}

 	public int getTimeKickOff() {
		return timeKickOff;
	}


	public int getLastTimeBallWasKicked() {
		return lastTimeBallWasKicked;
	}


	public void setLastTimeBallWasKicked(int lastTimeBallWasKicked) {
		this.lastTimeBallWasKicked = lastTimeBallWasKicked;
	}


	/*******************************************
	 *
	 *  Formation methods
	 *
	 *******************************************/
    
    /** 
     * this method sets my position in my team's formation
     */
	public void setFormation( Formation formation ) {
		this.myFormation = formation;
		// figure out where my place in the team formation is
		setDefaultFormationParams(); 
	}

	
    /** 
     * This method sets my default home, defensive, and offensive positions 
     * with respect to my role. All the magic numbers are 
     * worthy to experiment with. 
     */
	private void setDefaultFormationParams() {
		
		double deltaXdef = 0;	// x-coordinate shift in defensive situation
		double deltaXoff = 0;	// x-coordinate shift in offensive situation
		
		homePos = new Vector2d( myFormation.getHome( playerID ) );
		offensePos = new Vector2d( myFormation.getHome( playerID ) );
		defensePos = new Vector2d( myFormation.getHome( playerID ) );
		
		if ( myFormation.isDefender( playerID ) ) {
			deltaXdef = -20;	
			deltaXoff = 20;	
		} else if ( myFormation.isMidfielder( playerID ) ) {
			deltaXdef = -15;	
			deltaXoff = 25;	
		} else if ( myFormation.isAttacker( playerID ) ) {
			deltaXdef = -15;	
			deltaXoff = 30;	
		} 
		
		if ( myFormation.isGoalie( playerID ) ) {
			deltaXdef = 0;	
			deltaXoff = 0;	
		}
		
		offensePos.setX( offensePos.getX() + deltaXoff );
		defensePos.setX( defensePos.getX() + deltaXdef );
				
		//if ( role == 10 )
			//System.out.println( "side = " + side + " homePos = " + homePos 
				 //+ "\n defensePos = " + defensePos + " offensePos = " + offensePos); 
	}

}
