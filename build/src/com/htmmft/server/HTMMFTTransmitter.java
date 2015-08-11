/* Transmitter.java
   The engine of the world to keep it going

   Copyright (C) 2001  Yu Zhang

	Substantial modifications by Vadim Kyrylov 
						2006 - 2011
*/

package com.htmmft.server;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;

import com.htmmft.Match;
import com.htmmft.MatchIterator;
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
import soccer.server.SoccerRules;
import soccer.server.SoccerServerMain;
import soccer.server.SoccerServerWorld;
import soccer.server.Splayer;
import soccer.server.Sviewer;
import soccer.server.Transmitter;
import soccer.server.TransmitterThread;
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
public class HTMMFTTransmitter extends Transmitter{
	protected MatchIterator matchs = null;
	protected Match Currentmatch;
	protected HTMMFTSoccerServerMain soccerServer;

    
    /** 
     * constructor
     * @param soccerWorld
     * @param soccerRules
     */
    public HTMMFTTransmitter(SoccerServerWorld soccerWorld, SoccerRules soccerRules) {
        super(soccerWorld,soccerRules);
    }


    //---------------------------------------------------------------------------
   

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
     * This method sends INIT packets, if any, and removes 
     * them from the waiting list
     */
    private void sendInitPackets() {
    	
	    if ( isSendInit() ) {
	    	
	   // 	System.out.println("**************** YO ************* " + initPackets.size() );
	    	while (initPackets.size() > 0) {
	    		Packet p = initPackets.elementAt(0);
	    		try {
	    			//System.out.println("sendInitPackets caught ");
	    			HTMMFTSoccerServerMain.transceiver.send(p);
	    			// remove packet from the waiting list
	    			setSendInit(p, false);	
	    		} catch (IOException ioe) {
	            	System.out.println("sendInitPackets caught " + ioe);	    			
	    		}
	    	}
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
            	HTMMFTSoccerServerMain.transceiver.send(seePacket);
            } catch (IOException ioe) {
            	//System.out.println("sendSeeDataToPlayers caught " + ioe);
            	Thread.currentThread().interrupt();
            }
            
            team.add(i, see.player);
            i++;
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
            pp.setGoalie(player.isGoalie());
            pp.setKicker(player.isKicker());
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
            pp.setGoalie(player.isGoalie());
            pp.setKicker(player.isKicker());
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
           // System.out.println("Send Info Packet to: "+ player.getId() +"-"+ player.getSide()+ " / " +p2.address + " : "+ p2.port  );
            HTMMFTSoccerServerMain.transceiver.send(p2);
        }

        gamers = soccerWorld.getRightPlayers();
        while (gamers.hasMoreElements()) {
            player = (Splayer) gamers.nextElement();
        	// create separate packet for each client
        	// (this concerns debug mode only)
        	Packet p2 = new Packet(p);
            p2.address = player.getAddress();
            p2.port = player.getPort();
            HTMMFTSoccerServerMain.transceiver.send(p2);
        }
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
   	     	
    //	System.out.println(teamNameL+ " - " + teamNameR);
    	
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
        	//System.out.println("sendRefereeData caught " + ioe);
        	Thread.currentThread().interrupt();
        }
        
         			
		if (soccerRules.getPeriod() == RefereeData.GAME_OVER){
//			transceiver.disconnect();
			Thread.currentThread().interrupt();
			HTMMFTSoccerServerMain.transceiver.disconnect();
//			soccerServer.arret();
		}
    }
	public void setSaved(RandomAccessFile file) {
		if(saved != null)
			try {
				saved.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		saved = file;
	}

	public void setServer(HTMMFTSoccerServerMain soccerServerMainHTMMFT) {
		soccerServer = soccerServerMainHTMMFT;
		
	}
	
}
