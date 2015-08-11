/* Transmitter.java
   The engine of the world to keep it going

   Copyright (C) 2001  Yu Zhang

	Substantial modifications by Vadim Kyrylov 
						2006 - 2011
*/

package soccer.server;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;

import com.htmmft.JSONObserver.CreateTeamsThread;

import soccer.common.Ball;
import soccer.common.ByeData;
import soccer.common.HearData;
import soccer.common.InfoData;
import soccer.common.KickData;
import soccer.common.Packet;
import soccer.common.Player;
import soccer.common.RWLock;
import soccer.common.RefereeData;
import soccer.common.SeeData;
import soccer.common.TOS_Constants;
import soccer.common.Util;
import soccer.common.Vector2d;
import soccer.common.ViewData;
import soccer.tos_teams.sfu.SoccerTeamMain;

/**
 * This class executes the simulation step. It 
 * updates the list of clients as they join or leave, 
 * enforces the rules of the soccer game, executes the soccer 
 * physics, and transmits messages to all clients. 
 * These messages update clients about the 
 * state of the world and of the soccer game itself.
 * Some messages control the state of clients or even turn them off. 
 * 
 * This class is used both in the regular and the debug mode. 
 * 
 * @author vkyrylov
 *
 */
public class Transmitter {
	
    // record game log flag
    public boolean log = false;
    
    // saved situation replication parameters
    private int numOfSteps = 0;			// steps to run in each replication
    private int numOfReplicas = 0;		// number of replicas to run 
    private int tickerToStartOver = 0; 
    private int replicaCount = 1;		
    private int numOfStepsLeft = 0;		// steps to run left
    protected boolean isSituationSaved = false;
    
    // log file 
    protected RandomAccessFile saved = null;

    protected SoccerServerWorld soccerWorld = null;
    protected SoccerRules soccerRules = null;
    
    protected boolean isTerminated = false;	// if true, all TOS bundle is terminated
    
    // time parameters
    protected int cyclesInHalfTime;
    protected int cyclesInPause;

    // read/write lock to prevent conflicts while sending INIT packets
	private RWLock initLock = null;
	// a list of INIT packets, if any, to be sent to clients
	protected Vector<Packet> initPackets = new Vector<Packet>();

	private RWLock tickLock = null;
    private int ticker = 0; 		// simulation step number (discrete time)

	private RWLock stepLock = null;    
    private boolean isStepping = false;
    protected int timer = 0;
    
	private RWLock passLock = null;    
	private boolean passFlag = false;
	private Vector2d endPoint = new Vector2d();
	private int receiverID = -1;
	private char receiverSide = '?';
	
	protected boolean isSoundOnOld = false; 

    /** 
     * constructor
     * @param soccerWorld
     * @param soccerRules
     */
    public Transmitter(SoccerServerWorld soccerWorld, SoccerRules soccerRules) {
        
    	this.soccerWorld = soccerWorld;
        this.soccerRules = soccerRules;
        this.initLock = new RWLock();
        this.tickLock = new RWLock();
		this.stepLock = new RWLock();
		this.passLock = new RWLock();
				
		cyclesInHalfTime = (int)(TOS_Constants.HALF_TIME_MINUTES * 60 / TOS_Constants.SIM_STEP_SECONDS); 
		cyclesInPause 	 = (int)(TOS_Constants.PAUSE_DURATION / TOS_Constants.SIM_STEP_SECONDS); 
        System.out.println("===================================   @ Transmitter started");
		
		if(log) {
			// do something	
		}
    }


    //---------------------------------------------------------------------------
    
    /**
     * This method executed one step of the game and sends current 
     * state of the world to clients; (this state is updated by a 
     * stand alone thread which receives data from these clients).
     * The method is declared public to allow running it from the 
     * debugger application and using it by the sub class.  
     * 
     * @throws Exception
     */
    public synchronized void stepForward() {
    	
    	
    	//System.out.println("1");
    	if (isTerminated) {
    		// send termination messages to all clients
    		terminateClients();
    	}
    	
    	if ( soccerRules.getPeriod() == RefereeData.NO_GAME ) {
			// *** freeze everything while NO_GAME lasts ***
            soccerRules.total_score_L = 0;
            soccerRules.total_score_R = 0;
            soccerRules.score_L = 0;
            soccerRules.score_R = 0;
            soccerRules.sqr_score_diff = 0;
		
		} else {
        	// *** normal game mode ***
			// repeat pre-saved situation, if any
	        if ( soccerWorld.replicationIsOn ) {
	        	runReplica();
	        }
	        // move game timer one step forward
	        advanceTickers();
		}

		// add/remove players/viewers who joined or left the game
		soccerWorld.updateClientList();

		// make a working copy of both teams (to enforce the symmetry)
		soccerWorld.copyTeams( getTicker() );
		// enforce rules of the game and execute player actions
        soccerRules.enforce( getTicker() );
        // send referee signal, if applicable
        if (soccerRules.isRefereeSignal()) {
            sendRefereeData( soccerRules.getMode() );
            // forget change of game mode signal
            soccerRules.setRefereeSignal(false);
            // forget ball was grabbed
            soccerWorld.stepBallWasGrabbed = Integer.MAX_VALUE; 
            // forget corner kick	
            soccerWorld.stepCornerKickDecided = -Integer.MAX_VALUE; 	
        }
        // send updates to all clients
        sendInitPackets();
        sendVisualData();
        sendAudioData();
        // clean up the set of clients, if applicable
        clean();
        setPassFlag(false, null);        
    }
    
    /** 
     * this method runs the game from a pre-saved situation;
     * after numOfReplicas repetitions, it restores the regular game mode.
     */
    protected void runReplica() {
    	
    	if ( numOfStepsLeft == 0 ) {	    		
    		System.out.println("-- starting replica " + replicaCount + " --" );
	    	sendReplicaInfo();    	
			if ( replicaCount > 1 ) {	
				// not the first replica; restore data
    			//System.out.println("... restoring data for replica " + replicaCount );
				setTicker( tickerToStartOver );
				soccerWorld.restoreSituation(); 
				soccerWorld.setPlayerViewerActive( ticker ); 
 				notifyAll();
    		} 	
			if ( replicaCount  < numOfReplicas ) {
        		// initialize next replica
        		numOfStepsLeft = numOfSteps; 
        		replicaCount++; 
        	} else {
        		// finish replication
        		numOfReplicas = 0;
        		replicaCount = 1;
    			System.out.println("=== replication done === " );
    			sendReplicaInfo();
    			soccerWorld.replicationIsOn = false;
        	}
    	}
    	numOfStepsLeft--;    	 	
    }
    
    
	/** 
	 * this method advances the game period and resets 
	 * the ticker accordingly
	 */
	public void periodForward() {
		
        switch ( soccerRules.getPeriod() ) { 

        	case RefereeData.NO_GAME: 
				soccerRules.setPeriod( RefereeData.PRE_GAME );
        		soccerWorld.setPlayerViewerActive(getTicker());
			break; 

        	case RefereeData.PRE_GAME: 
				soccerRules.setPeriod( RefereeData.FIRST_HALF );
			break; 

        	case RefereeData.FIRST_HALF: 
				soccerRules.setPeriod( RefereeData.HALF_TIME );
			break; 
			
        	case RefereeData.HALF_TIME: 
				soccerRules.setPeriod( RefereeData.SECOND_HALF );
			break; 

        	case RefereeData.SECOND_HALF: 
				soccerRules.setPeriod( RefereeData.GAME_OVER );
			break; 
        	
        	case RefereeData.GAME_OVER: 
				soccerRules.setPeriod( RefereeData.PRE_GAME );
			break; 
			
			default:
		}
		
        try {	
            sendRefereeData( soccerRules.getMode() );
        }
        catch (Exception e)	{
        	System.out.println( "Exception in periodForward: " + e );
        }
	}

    /** 
     * this method advances the timer and decides which game period is in 
     * and set its game mode according to the period change, if any
     */
    protected void advanceTickers()  {
        
    	switch ( soccerRules.getPeriod() ) { 
        	
        	case RefereeData.PRE_GAME: 
	        	timer++;
	        	if ( timer > cyclesInPause ) {
	        		// before kickoff pause ended
	        		//TODO
	        		//SoccerTeamMain.StartWithPropertiesFile("sfu_team.ini");
	        	//	CreateTeamsThread equipes  = new CreateTeamsThread();
	        		//equipes.start();
	        		soccerRules.setPeriod( RefereeData.FIRST_HALF );
	        		timer = 0;
	        	} else {
	        		// game started
	        		if ( TOS_Constants.RESET_SCORE ) {
		                soccerRules.total_score_L = 0;
		                soccerRules.total_score_R = 0;
		            }
			        soccerWorld.getBall().set(0, 0);
			        // keep players off the ball
			        soccerRules.leftOff = true;
			        soccerRules.rightOff = true;
			        if ( !isSituationSaved ) {
			        	// initialize for the replication
			        	soccerWorld.initSavedSituation();
			        	isSituationSaved = true;
			        }
        		}	 
			break; 
		
        	case RefereeData.FIRST_HALF: 
		        // advance the ticker only during actually playing the game
		        setTicker(getTicker() + 1);
		        if ( getTicker() == cyclesInHalfTime ) {
	        		// set a break between two halves
	        		soccerRules.setPeriod( RefereeData.HALF_TIME );
	        	}
        	break;
        	
        	case RefereeData.HALF_TIME: 
	        	timer++;
	        	if ( timer > cyclesInPause ) {
	        		// half-time pause ended
	        		soccerRules.setPeriod( RefereeData.SECOND_HALF );
        			timer = 0;
	        	}	 
			break; 

        	case RefereeData.SECOND_HALF:		        
		        // advance the ticker only during actually playing the game
		        setTicker(getTicker() + 1);
		        if ( getTicker() == 2 * cyclesInHalfTime ) {	        		
	        		// finish this game
	        		soccerRules.setPeriod( RefereeData.GAME_OVER );
	        	}
			break; 

        	case RefereeData.GAME_OVER: 
        		// start over the next game
        		soccerRules.setPeriod( RefereeData.PRE_GAME );
        		setTicker( 0 );
        		updateStatistics();
    		break; 
			
			default: ; // do nothing
		}        
        
    	/*
        System.out.println("period = " 
    				+ RefereeData.periods[soccerRules.getPeriod()] 
    				+ " mode = " + RefereeData.modes[soccerRules.getMode()] );
       	*/
    }
    

	/**
	 *  this method updates and prints game statistics; in particular, this 
	 *  allows to reveal asymmetry, if any, when same team is playing on 
	 *  both sides; if the score or offside difference is greater than two 
	 *  sigmas, the asymmetry is likely at 95% confidence level
	 */
	private void updateStatistics() {
		
        // print last game and overall games result 
        System.out.print("\n--------------  Game " + soccerRules.gameCount + " over. "); 
        System.out.println("Score " + soccerRules.score_L 
        						+ ":" + soccerRules.score_R + "  --------------"); 
        System.out.println("Total score " + soccerRules.total_score_L 
        						+ ":" + soccerRules.total_score_R ); 
		
        // accumulate score difference statistics
		int scoreDiff = soccerRules.score_L - soccerRules.score_R;
		soccerRules.sqr_score_diff = soccerRules.sqr_score_diff 
											+ scoreDiff * scoreDiff;
        
		// calculate 95% confidence interval half-width for the score difference
        double twoSigmas = 0;
    	// average score difference 
    	double avgScoreDiff = (double)( soccerRules.total_score_L 
    							- soccerRules.total_score_R )
    											/(double)soccerRules.gameCount;
        if ( soccerRules.gameCount > 1 ) {
        	// score difference variance
        	double varScoreDiff = soccerRules.sqr_score_diff
        						/( soccerRules.gameCount - 1 );
        	varScoreDiff = varScoreDiff - avgScoreDiff * avgScoreDiff;
			// confidence interval half-width
			twoSigmas = 2.0 * Math.sqrt( varScoreDiff );
        }
        
        twoSigmas = twoSigmas/Math.sqrt(soccerRules.gameCount);
        
        System.out.println("Average score difference:\t" + Util.round(avgScoreDiff, 3) 
        					+ ",\ttwo sigmas: " + Util.round(twoSigmas, 3) ); 
	    
        // check if there is statistically significant difference in the score
	    if ( Math.abs( avgScoreDiff ) > twoSigmas && soccerRules.gameCount > 5 ) {					
	        if ( avgScoreDiff > 0 ) 					
				System.out.print("** left ** ");
			else  
				System.out.print("** right ** ");
			System.out.println("team is playing better with 95% confidence");	
		}
	    
	    // process ball X-coordinate and offside rule violation statistics
	    double avgBallX = soccerRules.sum_avgBallX/soccerRules.gameCount;
	    double varBallX = soccerRules.sum_sqr_BallX/(soccerRules.gameCount - 1) - avgBallX*avgBallX;
	    System.out.println("Ball X-coordinate:\taverage = " + Util.round(avgBallX, 3) 
	    		+ "\ttwo sigmas = " + Util.round(2.0 * Math.sqrt( varBallX ), 3) );
	    
	    System.out.print("Offside count\tleft: " + soccerRules.offsideCountL
	    		+ "  right: "  + soccerRules.offsideCountR);
	    try{
	    	System.out.println("\tper game: " 
	    		+ Util.round((double)soccerRules.offsideCountL/soccerRules.gameCount, 3) 
	    		+ " : " + Util.round((double)soccerRules.offsideCountR/soccerRules.gameCount, 3) );	    
		    double avgOffsideDiff = (double)(soccerRules.offsideCountL - soccerRules.offsideCountR)
		    							/(double)(soccerRules.gameCount);
		    double varOffsideDiff = (double)(soccerRules.sqr_offside_diff)/(soccerRules.gameCount - 1) 
		    							- avgOffsideDiff*avgOffsideDiff;
		    System.out.println("Offside average difference = " + Util.round(avgOffsideDiff, 3) 
		    		+ "\ttwo sigmas = " + Util.round(2.0 * Math.sqrt( varOffsideDiff ), 3) );
	    } catch (Exception e) {
	    	System.out.println();
	    }
	    		
		System.out.println("------------------------------------------------------");
			
		soccerRules.score_L = 0;
		soccerRules.score_R = 0;
		soccerRules.gameCount++;		
	}
	 

    /** 
     * send packet to all clients
     * @param p
     * @throws IOException
     */
    private void sendPacketToAllClents(Packet p) throws IOException {
        p.senderIDdebug = 0;
    	sendPacketToPlayers(p);
    	sendPacketToViewers(p);
    }
    
    /** 
     * send packet to all viewer clients
     * @param p
     * @throws IOException
     */
    protected void sendPacketToViewers(Packet p) throws IOException {
        Enumeration<Sviewer> viewers = soccerWorld.getViewers();
        Sviewer viewer = null;
        
        while (viewers.hasMoreElements()) {
            viewer = (Sviewer) viewers.nextElement();
        	// create separate packet for each client
        	// (this concerns debug mode only)
        	Packet p2 = new Packet(p);
            p2.address = viewer.address;
            p2.port = viewer.port;
            SoccerServerMain.transceiver.send(p2);
        }
    }
    
    /** 
     * send packet to all player clients
     * @param p
     * @throws IOException
     */
    private void sendPacketToPlayers(Packet p) throws IOException {
        Enumeration<Splayer> gamers = null;
        Splayer player = null;

        gamers = soccerWorld.getLeftPlayers();
        while (gamers.hasMoreElements()) {
            player = (Splayer) gamers.nextElement();
        	// create separate packet for each client
            // (this concerns debug mode only)
        	Packet p2 = new Packet(p);
            p2.address = player.getAddress();
            p2.port = player.getPort();
            SoccerServerMain.transceiver.send(p2);
        }

        gamers = soccerWorld.getRightPlayers();
        while (gamers.hasMoreElements()) {
            player = (Splayer) gamers.nextElement();
        	// create separate packet for each client
        	// (this concerns debug mode only)
        	Packet p2 = new Packet(p);
            p2.address = player.getAddress();
            p2.port = player.getPort();
            SoccerServerMain.transceiver.send(p2);
        }
    }


    /** 
     * this method sends a message to the ball pass receiver; 
     * the receiver id+side is inside the packet
     * @param p
     */
    public void sendPassData( Packet p ) {
    	// <<< this is a TODO stub >>>
    	// see sendInfoData() below as an example of how this method
    	// could be implemented. In this case, the packet would be 
    	// sent to all clients, but only the intended receiver must be 
    	// reacting on it. 
    	// An alternative and more elegant solution involves using receiver 
    	// address+port info and sending the packet to the pass receiver 
    	// only. See sendPacket() method above to figure out where 
    	// address+port info is stored for each player.  
    }
    
    
    /** 
     * send referee message to all clients
     * @param mode
     */
    public void sendRefereeData(int mode) {
    	
    	String teamNameL, teamNameR;
    	if (TOS_Constants.LEFTNAME != "") 
    		teamNameL = TOS_Constants.LEFTNAME;
    	else
       		teamNameL = "Unknown_L";
    	
    	if (TOS_Constants.RIGHTNAME != "") 
    		teamNameR = TOS_Constants.RIGHTNAME;
    	else
       		teamNameR = "Unknown_R";
   	     	
    	// fill the referee data      
        RefereeData referee =
            new RefereeData(getTicker(),
			                soccerRules.getPeriod(),
			                mode,
			                soccerRules.getViolatorID(), 
			                soccerRules.sideToContinue, 
			                teamNameL,
			                soccerRules.score_L,
			                soccerRules.total_score_L,
			                teamNameR,
			                soccerRules.score_R,
			                soccerRules.total_score_R,
			                soccerRules.gameCount,
			                TOS_Constants.GAMES_TO_PLAY);
        
        // create a data packet and send it
        Packet refereePacket = new Packet(Packet.REFEREE, referee);
        try {
	        sendPacketToAllClents(refereePacket);			
			//System.out.println( "sent Packet.REFEREE = " + refereePacket.writePacket() );
	        
	        if (isSoundOnOld != soccerWorld.isSoundOn()) {
	        	// send update about the sound on/off state if changed
	        	int onOff = (soccerWorld.isSoundOn()) ? 1 : -1;
	            InfoData infoData = new InfoData(InfoData.SOUND_ON_OFF, onOff, 0, "");           
	            Packet infoPacket = new Packet(Packet.INFO, infoData);        	
	        	sendPacketToPlayers(infoPacket);
	        	isSoundOnOld = soccerWorld.isSoundOn();
				try {
					System.out.println("Sending Packet.INFO: " + infoPacket.writePacket() );
				} catch (Exception e ) {
					System.out.println("Error while sending Packet.INFO " + e );
				}
	        }
	
	        if (log) {
	            saved.writeBytes(refereePacket.writePacket());
	            saved.writeByte('\n');
	        }
        } catch (IOException ioe) {
        	System.out.println("sendRefereeData caught " + ioe);
        }
    }

    
    /** 
     * send information data packet to clients about the server state
     * @param state
     * @throws IOException
     */
    private void sendInfoData(int state) throws IOException {
        InfoData infoData = new InfoData(state);        
        Packet infoPacket = new Packet(Packet.INFO, infoData);
        sendPacketToAllClents(infoPacket);		
		//System.out.println( "sent Packet.INFO = " + infoPacket.writePacket() );
    }
    
    
	/** 
	 * this method sends replica data to the viewer clients
	 */
	private void sendReplicaInfo() {
		try {
			InfoData aInfoData = 
				new InfoData( 
						InfoData.REPLICA, 	// info
						replicaCount,  		// info1
						numOfReplicas); 	// info2
					
        	
			Enumeration<Sviewer> viewers = soccerWorld.getViewers();
        	while ( viewers.hasMoreElements() ) {
	            Sviewer viewer = (Sviewer) viewers.nextElement();
				// create separate packet for each viewer
	            Packet packet =
					new Packet(Packet.INFO, aInfoData, null, 0);
	            packet.address = viewer.address;
	            packet.port = viewer.port;
	            SoccerServerMain.transceiver.send( packet );
	        }			
			//System.out.println("sending Packet.INFO - replica");
			
		} catch (IOException e) {

		}		
	}
	
	
	/** 
     * send audio information to clients
     */
    protected void sendAudioData() {
        
    	Enumeration<Splayer> gamers = null;
        Splayer player = null;

        // fill the hear data and send it out
        gamers = soccerWorld.getLeftPlayers();
        while (gamers.hasMoreElements()) {
            player = (Splayer) gamers.nextElement();
            if (player.getMessage() != null && player.getNoWordCount() == 0) {
                HearData hear =
                    new HearData(
                        getTicker(),
                        player.getSide(),
                        player.getId(),
                        player.getMessage());
                Packet hearPacket = new Packet(Packet.HEAR, hear);
                // player can not speak in NOWORD sec
                player.setNoWordCount((int) (TOS_Constants.NOWORD / TOS_Constants.SIM_STEP_SECONDS));
                player.setMessage(null);
                try {
	                sendPacketToAllClents(hearPacket);
	                if (log) {
	                    saved.writeBytes(hearPacket.writePacket());
	                    saved.writeByte('\n');
	                }
        		} catch (IOException ioe) {
        	    	System.out.println("sendAudioData-1 caught " + ioe);	    			
        		}
                return;
            }
        }
        gamers = soccerWorld.getRightPlayers();
        while (gamers.hasMoreElements()) {
            player = (Splayer) gamers.nextElement();
            if (player.getMessage() != null && player.getNoWordCount() == 0) {
                HearData hear =
                    new HearData(
                        getTicker(),
                        player.getSide(),
                        player.getId(),
                        player.getMessage());
                Packet hearPacket = new Packet(Packet.HEAR, hear);
                // player can not speak in NOWORD sec
                player.setNoWordCount((int) (TOS_Constants.NOWORD / TOS_Constants.SIM_STEP_SECONDS));
                player.setMessage(null);
                try {
	                sendPacketToAllClents(hearPacket);
	                if (log) {
	                    saved.writeBytes(hearPacket.writePacket());
	                    saved.writeByte('\n');
	                }
	    		} catch (IOException ioe) {
	    	    	System.out.println("sendAudioData-2 caught " + ioe);	    			
	    		}
                return;
            }
        }
    }

    
    /**
     * this method sends visual information to all clients;
     * also it sends additional information with reduced rate, as needed 
     */
    public void sendVisualData() {
    	
        // loop parameters
        Enumeration<Splayer> gamers = null;
        Splayer player = null;
        Sviewer viewer = null;

        // set up ball data
        Ball b =
            new Ball(
                soccerWorld.getBall().getPosition(),
                soccerWorld.getBall().controllerType,
                soccerWorld.getBall().controllerId);
        b.isGrabbed = soccerWorld.getBall().isGrabbed;
        if (soccerWorld.getBall().isFree) {
            b.controllerType = 'f';
            b.controllerId = 0;
        }
        b.setDirection(soccerWorld.getBall().getDirection());
        int leftGoalieID = -1;
        int rightGoalieID = -1;
        Vector<Integer> chaserIDs = new Vector<Integer>();
        
        // create the left team vector
        Vector<Player> leftTeam = new Vector<Player>();
        gamers = soccerWorld.getLeftPlayers();
        
        while (gamers.hasMoreElements()) {
            player = (Splayer) gamers.nextElement();
            Player pp =
                new Player(
                    player.getSide(),
                    player.getId(),
                    player.getPosition(),
                    player.getDirection());
            pp.setUserControlled( player.isUserControlled() );
            leftTeam.add(pp);
            
            if (player.isGoalie())
            	leftGoalieID = player.getId();
           
            if (player.isChasingBall() && getTicker()%2 == 0) {
            	// this must be synchronized with other methods sending updates
            	chaserIDs.addElement(player.getId());
            }
        }

        // create the right team vector
        Vector<Player> rightTeam = new Vector<Player>();
        gamers = soccerWorld.getRightPlayers();
        while (gamers.hasMoreElements()) {
            player = (Splayer) gamers.nextElement();
            Player pp =
                new Player(
                    player.getSide(),
                    player.getId(),
                    player.getPosition(),
                    player.getDirection());
            pp.setUserControlled( player.isUserControlled() );
            rightTeam.add(pp);
            
            if (player.isGoalie())
            	rightGoalieID = player.getId();

            if (player.isChasingBall() && getTicker()%2 != 0) {
            	// this must be synchronized with other methods sending updates
            	chaserIDs.addElement(player.getId());
            }
        }

		// to ensure the symmetry, send visual info to teams in turns 
		if ( getTicker()%2 == 0 )
			sendSeeDataToPlayers(soccerWorld.getLeftPlayers(), b, leftTeam, rightTeam);
		sendSeeDataToPlayers(soccerWorld.getRightPlayers(), b, leftTeam, rightTeam);
		if ( getTicker()%2 != 0 )
			sendSeeDataToPlayers(soccerWorld.getLeftPlayers(), b, leftTeam, rightTeam);

        ViewData view = new ViewData();
        view.time = getTicker();
        view.ball = b;
        view.leftTeam = leftTeam;
        view.rightTeam = rightTeam;
    	
    	Packet viewPacket = new Packet();
        viewPacket.packetType = Packet.VIEW;
        viewPacket.data = view;

        if (log) {
        	try {
                saved.writeBytes(viewPacket.writePacket());
                saved.writeByte('\n');
    		} catch (IOException ioe) {
    	    	System.out.println("sendVisualData-2 caught " + ioe);	    			
    		}
       }

        Enumeration<Sviewer> viewers = soccerWorld.getViewers();
        
        while (viewers.hasMoreElements()) {
            
            viewer = (Sviewer) viewers.nextElement();
        	// create separate packet for each client
        	// (this concern debug mode only)
        	Packet p2 = new Packet(viewPacket);
        	p2.address = viewer.address;
            p2.port = viewer.port;
            
        	try {
        		SoccerServerMain.transceiver.send(p2);    	
    		} catch (IOException ioe) {
    	    	System.out.println("sendVisualData-1 caught " + ioe);	    			
    		}
        }
        sendInfoToViewers(leftGoalieID, rightGoalieID, chaserIDs);
        sendInfoToPlayers(leftGoalieID, rightGoalieID);
    }
    

    /**
     * This method sends INFO packets to all player clients
     */
    protected void sendInfoToPlayers(int leftGoalieID, int rightGoalieID) {
    	
        boolean needToUpdateGoalies = (getTicker()%500 == 0);
        
        if (needToUpdateGoalies) {
        	// create an INFO packet telling who the goalies are
        	InfoData info = new InfoData(InfoData.WHO_ARE_THE_GOALIES, 
    				leftGoalieID, rightGoalieID);
        	
        	Packet infoPacket = new Packet();
        	infoPacket.packetType = Packet.INFO;
        	infoPacket.data = info;
        	infoPacket.address = null;
        	infoPacket.port = -1;
        	infoPacket.senderIDdebug = 0;
        	try {
        		sendPacketToPlayers(infoPacket);
        	} catch (IOException ioe) {}
        }    	
    }
    
    
    /**
     * This method sends INFO packets to all viewer clients
     */
    protected void sendInfoToViewers(int leftGoalieID, 
    			int rightGoalieID, Vector<Integer> chaserIDs) {
    	
        boolean needToUpdateGoalies = (getTicker()%1000 == 0);
        // note different encoding for team side (an integer)
        int teamSide = (getTicker()%2 == 0) ? -1 : 1;
        
        Enumeration<Sviewer> viewers = soccerWorld.getViewers();
        
        while (viewers.hasMoreElements()) {
            
        	Sviewer viewer = (Sviewer) viewers.nextElement();
          
            if (needToUpdateGoalies) {
                // update viewer clients on who the goalies are
            	InfoData info = new InfoData(InfoData.WHO_ARE_THE_GOALIES, 
            				leftGoalieID, rightGoalieID);
            	sendInfoPacket(viewer.address, viewer.port, info);
                 //System.out.println("Sending goalie ids: " + leftGoalieID + " " + rightGoalieID);
            }
            
            if (chaserIDs.size() > 0) {
            	// send info about who is chasing the ball
            	for (int i=0; i<chaserIDs.size(); i++) {
	            	InfoData info = new InfoData(InfoData.WHO_IS_CHASING_BALL, 
	            			teamSide, chaserIDs.elementAt(i));
	            	sendInfoPacket(viewer.address, viewer.port, info);
            	}
            }
            
            if (passFlag) {
            	// send info about the ball pass
              	InfoData info = new InfoData(InfoData.BALL_PASS, 
	            			(int)(endPoint.getX()*100), (int)(endPoint.getY()*100) );
              	info.info3 = receiverID;
              	info.extraInfo = receiverSide + "";
	            sendInfoPacket(viewer.address, viewer.port, info);
	            //System.out.println("Sending ball pass info. receiverID=" 
	            			//+ receiverID + " endPoint " + endPoint);
            }
            
            if (soccerWorld.isCollision()) {
            	// send info about player collision
              	InfoData info = new InfoData(InfoData.COLLISION);
	            sendInfoPacket(viewer.address, viewer.port, info);
           }
            
            if (soccerWorld.isBallKicked()) {
            	// send info about ball kicked
              	InfoData info = new InfoData(InfoData.BALL_KICK);
	            sendInfoPacket(viewer.address, viewer.port, info);
           }
        }    	
    }
    
    
    /**
     * This method sends an INFO packet 
     */
    private void sendInfoPacket(InetAddress address, int port, InfoData infoData) {
    	Packet infoPacket = new Packet();
    	infoPacket.packetType = Packet.INFO;
    	infoPacket.data = infoData;
    	infoPacket.address = address;
    	infoPacket.port = port;
    	infoPacket.senderIDdebug = 0;
    	try {
    		SoccerServerMain.transceiver.send(infoPacket);    	
		} catch (IOException ioe) {
	    	System.out.println("sendInfoPacket caught " + ioe);	    			
		}
    }
    
    
    /**
     * This method sends BYE packets to all clients thus terminating them
     */
    protected void terminateClients() {
    	Packet byePacket = new Packet();
    	byePacket.packetType = Packet.BYE;
    	byePacket.data = new ByeData(ByeData.TERMINATE);
    	try {
			sendPacketToAllClents(byePacket);
		} catch (IOException e) {
			System.out.println("terminateClients caught " + e);
		}
    }
	
    /**
     * This method sends INIT packets, if any, and removes 
     * them from the waiting list
     */
    private void sendInitPackets() {
    	
	    if ( isSendInit() ) {
	    	while (initPackets.size() > 0) {
	    		Packet p = initPackets.elementAt(0);
	    		try {
	    			System.out.println("sendInitPackets caught ");
	    			SoccerServerMain.transceiver.send(p);
	    			// remove packet from the waiting list
	    			setSendInit(p, false);	
	    		} catch (IOException ioe) {
	            	System.out.println("sendInitPackets caught " + ioe);	    			
	    		}
	    	}
	    }
    }

    
	/** 
	 * this method sends the SEE packets to given set of clients 
	 * @param clients	- the set of recepients
	 * @param b			- the ball data
	 * @param left		- the team data
	 * @param right		- the team data
	 */
	private void sendSeeDataToPlayers( 	Enumeration<Splayer> clients, Ball b, 
								  		Vector<Player> left, Vector<Player> right ) {  
        int i = 0;
        
        while ( clients.hasMoreElements() ) {
            
            Splayer player = (Splayer) clients.nextElement();
            
            Vector<Player> team = null;
            if ( player.getSide() == 'l' )
            	team = left;
            else if ( player.getSide() == 'r' )
            	team = right;
            
            // create separate data set for each player
            SeeData see = new SeeData();
            see.time = getTicker();
            see.ball = b;
            see.leftTeam = left;
            see.rightTeam = right;
            see.player = (Player) team.remove(i); // the addressee of this info
            see.status = 0;
            if (player.isOffside())
                see.status = 1;
            else if ( player.getSide() == 'l' && soccerRules.offsideL ||
            		  player.getSide() == 'r' && soccerRules.offsideR )
                see.status = 2;
            
            // create separate packet for each player
            // (this is critical in debug mode)
            Packet seePacket = new Packet();
            seePacket.packetType = Packet.SEE;
            seePacket.data = see;
            seePacket.address = player.getAddress();
            seePacket.port = player.getPort();
            
            try {
            	SoccerServerMain.transceiver.send(seePacket);
            } catch (IOException ioe) {
            	System.out.println("sendSeeDataToPlayers caught " + ioe);
            }
            
            team.add(i, see.player);
            i++;
        }		
	}

    
    /**
     * This method removes inactive clients, if any, and 
     * updates the active client ID lists
     */
	protected void clean() {
        
        Enumeration<Splayer>  gamers = null;
        Splayer player = null;
        Sviewer viewer = null;

        int stepsInGame = (int) ( 2*TOS_Constants.HALF_TIME_MINUTES 
        				* (60 / TOS_Constants.SIM_STEP_SECONDS));
        int maxIdleSteps = (int) (TOS_Constants.IDLE_MINUTES 
        				* (60 / TOS_Constants.SIM_STEP_SECONDS));

        gamers = soccerWorld.getLeftPlayers();
        while (gamers.hasMoreElements()) {
            player = (Splayer) gamers.nextElement();
            int diff = getTicker() - player.getLastTime();
            if (diff < 0)
                diff = diff + stepsInGame;
            if (diff > maxIdleSteps) {
            	System.out.println("Removing player: " 
            			+ player.getId() + "-" + player.getSide()
            			+ " diff=" + diff + " idle=" + maxIdleSteps);
				soccerWorld.removePlayer(player);
				soccerWorld.putBackLeftPlayerId(player.getId());
            }
        }

        gamers = soccerWorld.getRightPlayers();
        while (gamers.hasMoreElements()) {
            player = (Splayer) gamers.nextElement();
            int diff = getTicker() - player.getLastTime();
            if (diff < 0)
                diff = diff + stepsInGame;
            if (diff > maxIdleSteps) {
            	System.out.println("Removing player: " 
            			+ player.getId() + "-" + player.getSide()
            			+ " diff=" + diff + " idle=" + maxIdleSteps);
				soccerWorld.removePlayer(player);
				soccerWorld.putBackRightPlayerId(player.getId());
            }
        }

        Enumeration<Sviewer> viewers= soccerWorld.getViewers();
        while (viewers.hasMoreElements()) {
            viewer = (Sviewer) viewers.nextElement();
            int diff = getTicker() - viewer.getLastTime();
            if (diff < 0)
                diff = diff + stepsInGame;
            if (diff > maxIdleSteps) {
            	System.out.println("Removing viewer: diff=" + diff + " idle=" + maxIdleSteps);
            	soccerWorld.removeViewer(viewer);
				soccerWorld.putBackViewerId(viewer.viewerId);
            }
        }
    }

	
    /**
     * ----------------  synchronized getters and setters  ----------------------
     * methods below use RWLock object to prevent from the simultaneous access to 
     * the locked variable by other threads before the operation is completed
     */
	
    public int getTicker() {
    	try{
			tickLock.lockRead();
			return ticker;
		}
		finally {
			tickLock.unlock();
		}
    }


    public void setTicker(int i) {
		try {
			tickLock.lockWrite();
			ticker = i;
		}    	
        finally {
			tickLock.unlock();
        }
    }
    
    
	public boolean isSendInit() {
		try{
			initLock.lockRead();
		//	System.out.println(initPackets.size());
			return (initPackets.size() > 0);
		}
		finally {
			initLock.unlock();
		}
	}

    /**
     * add packet to be sent on the list or remove it
     * @param p
     */
	public void setSendInit(Packet p, boolean add) {
		try {
			initLock.lockWrite();
			if (add){ 
				
				this.initPackets.add(p);
		//		System.out.println("==================== AAADDDDDDDDD================= " + initPackets.size());
			}
			else
				this.initPackets.remove(p);
		}    	
        finally {
        	initLock.unlock();
        }
	}

    public void setPassFlag(boolean flag, KickData kickData) {
		try {
			passLock.lockWrite();
			passFlag = flag;
			if (flag) {
				endPoint = kickData.endPoint;
				receiverID = kickData.receiverID;
				receiverSide = kickData.side;
			} else {
				endPoint = new Vector2d();
				receiverID = -1;				
				receiverSide = '?';
			}
		}    	
        finally {
			passLock.unlock();
        }
    }
    
    public boolean isStepping() {
		try{
			stepLock.lockRead();
			return isStepping;
		}
		finally {
			stepLock.unlock();
		}
    }
   
    
    /**
     * -------------------  "plain" getters and setters  ----------------------
     * (presumably, these variables are accessed from just one thread)
     */

    public void resetTicker() {
    	setTicker( tickerToStartOver );	
    }

    
    public boolean isSituationSaved() {
		return isSituationSaved;
	}


	public void setSituationSaved(boolean isSituationSaved) {
		this.isSituationSaved = isSituationSaved;
	}


	public SoccerServerWorld getSoccerWorld() {
		return soccerWorld;
	}


	public SoccerRules getSoccerRules() {
		return soccerRules;
	}

	public RandomAccessFile getSaved() {
		return saved;
	}


	public void setSaved(RandomAccessFile file) {
		saved = file;
	}

	public void setTerminated(boolean isTerminated) {
		this.isTerminated = isTerminated;
	}

	
    //-------------  more sophisticated setters  ----------------	

	/**
     * This method sets the Server state variable, |isStepping|, and sends  
     * messages to the clients about the changes of the Server state
     * @param dosteps
     */
    public void setStepping(boolean dosteps ) {
		try {
			stepLock.lockWrite();
			isStepping = dosteps;
			if ( isStepping ) {
				try {
					sendInfoData(InfoData.WAIT_NEXT);	// send message
					//System.out.println(" RefereeData.WAIT_NEXT packet sent");	
				} catch (Exception e ) {}
			} else {
				try {
					sendInfoData(InfoData.RESUME);		// send message	
					//System.out.println(" RefereeData.RESUME packet sent");	
				} catch (Exception e ) {}
			}
		}    	
		finally {
			stepLock.unlock();
		}
    }


	public void setReplication( int numOfSteps, int numOfReplicas, int stepID ) {
		this.numOfSteps = numOfSteps;
		this.numOfReplicas = numOfReplicas;
		tickerToStartOver = 0;	//stepID; 
		soccerWorld.replicationIsOn = true;
		setTicker( tickerToStartOver );
		numOfStepsLeft = 0;
		replicaCount = 1;
		soccerRules.setReplicationMode();
	}

}
