/* Receiver.java

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

	Modifications by Vadim Kyrylov 	since 2006

*/

package com.htmmft.server;

import java.net.InetAddress;
import java.io.*;

import javax.swing.JOptionPane;

import soccer.common.*;
import soccer.server.Sball;
import soccer.server.SoccerRules;
import soccer.server.SoccerServerWorld;
import soccer.server.Splayer;
import soccer.server.Sviewer;
import soccer.server.Transmitter;


/**
  * The Receiver implements receiving data packets arriving from 
  * the clients (players and viewers) and processes this information.
  * 
  * Different types of packets arriving from different types of 
  * client are processed in different way. A dedicated data object 
  * is maintained for each client. The processing logic depends on 
  * the the state of these objects and the order these packets arrive.
  * First, Receiver establishes communication with the client by 
  * executing a handshaking procedure. Then it uses the received data 
  * to update the state of the client object stored in the server 
  * world model. 
  * Whenever Receiver decides to send a data packet back to client, it 
  * informs the Transmitter by passing data to it. 
  */

public class HTMMFTReceiver {
	
	
	protected HTMMFTSoccerServerMain soccerServer;
	private SoccerServerWorld 	soccerWorld = null;
	private Transmitter 	transmitter = null;
    private SoccerRules 	soccerRules = null;
    private static final double ALMOST_ZERO = 	1E-6;

	/**
	 * Constructor 
	 * 
	 * @param soccerWorld - the server world model
	 * @param transmitter - the transmitter object 
	 * @param soccerRules - need to access public constants
	 */
	public HTMMFTReceiver(SoccerServerWorld soccerWorld, 
								Transmitter transmitter, 
								SoccerRules soccerRules) {
		this.soccerWorld = soccerWorld;
		this.transmitter = transmitter;
        this.soccerRules = soccerRules;
        System.out.println("== Receiver started");
	}

	/**
	 * This method waits to receive one data packet from 
	 * clients and updates the state of the world. 
	 * It returns the received packet after processing its contents.
     * Used both in the regular and the debug mode. 
	 * The method is declared public for the sole purpose of using 
	 * by the sub class and the debugger application.
	 */
	public synchronized Packet runOneStep() {
		Packet packet = null;
		try {
			if (!HTMMFTSoccerServerMain.transceiver.isClosed()){
				packet = HTMMFTSoccerServerMain.transceiver.receive();
				processPacket(packet);
			}
			else
				return null;

		} catch (Exception e) {
//			System.out.println("Fatal error in Receiver. Server program terminated. " + SoccerServerMainHTMMFT.transceiver.isClosed());
//			e.printStackTrace();
//			System.exit(1);
			Thread.currentThread().interrupt();
		}
		return packet;
	}
	
	/**
	 * This method processes the received data packet.
	 * 
	 * @param packet
	 * @throws IOException
	 */
	private void processPacket(Packet packet) throws Exception {
		if (packet != null) {
			if (packet.packetType == Packet.CONNECT) {
				//System.out.println("Packet.CONNECT received. " + packet.writePacket() );
				shakeHands(packet);	// establish new connection
			} else
				setAction(packet);	// process the existing client's message
		} else {
			// do nothing
		}
	}

	/** 
	 * establish connection on the client's request by 
	 * sending him back an acknowledgment
	 * @param packet
	 * @throws IOException
	 */
	private void shakeHands(Packet packet) throws IOException {
		
		// figure it out who is the sender
		InetAddress address = packet.address;
		int port = packet.port;
		Sviewer viewer = null;
		Splayer player = soccerWorld.getPlayer(address, port);
		if (player == null)
			viewer = soccerWorld.getViewer(address, port);
		
		// extract data from the received package
		ConnectData aConnectData = (ConnectData) packet.data;
		
		switch ( aConnectData.clientType ) {
		
			case ConnectData.VIEWER: 
				connectToViewerClient( viewer, address, port, aConnectData );
			break; 	
		
			case  ConnectData.PLAYER: 
				
				if (player != null) {
					// if this player is already connected, reply to the client, 
					// send the player team, number, and role back.
					char role = ConnectData.FIELD_PLAYER;
					if (player.isGoalie())
						role = ConnectData.GOALIE;
					else if (player.isKicker())
						role = ConnectData.FIELD_PLAYER_KICKER;
					InitData initdata 
							= new InitData(	player.getSide(), 
											player.getId(),
											role,	
											TOS_Constants.SIM_STEP_SECONDS, 
											0 	// not used
											);
					Packet initPacket =
						new Packet(	Packet.INIT, 
									initdata, 
									address, 
									port );
					transmitter.setSendInit(initPacket, true);
					//System.out.println("Init packet repeatedly sent, player.id = " + player.id );
					return;
	//<=============
				}
				
				// we get here if only the player is new
				switch ( aConnectData.sideType ) {
					
					case ConnectData.LEFT: 
					case ConnectData.LEFT_COACH: 
					case ConnectData.RIGHT: 
					case ConnectData.RIGHT_COACH: 
					
						connectToPlayerClient( aConnectData, address, port );
						
					break; 
					
					case ConnectData.ANYSIDE: 
					case ConnectData.ANYSIDE_COACH: 
					
						if (soccerWorld.rightAvailable.size()
										>= soccerWorld.leftAvailable.size()) {
							if (soccerWorld.rightAvailable.size() > 0) {
								connectToPlayerClient( aConnectData, address, port );
							} else
								notAvailable(address, port); // no ID available
						} else {
							connectToPlayerClient( aConnectData, address, port );
						}
					break; 
					
					default:
						;
				}	// end switch sideType
			
			break; 
			
			default:
				;
		}	// end switch clientType
	}


	/** 
	 * This method can make two connections:
	 * 	(1) register a new viewer client and 
	 * 	(2) establish user control of a player via a registered viewer client. 
	 * In both cases, this method sends an acknowledgment. 
	 */
	private void connectToViewerClient( Sviewer viewer, 
						InetAddress address, 
						int port, ConnectData aConnectData ) throws IOException {
		
		if (viewer != null) {
			/**
			 *  this viewer has been already connected
			 */
			
			char clientMode = InitData.USER;
			int id = viewer.playerID;
			char role = viewer.playerSide;
			
			if (aConnectData.playerRole == ConnectData.USER) {
				// this viewer is taking control of a player
				System.out.println("user controlled player = " 
							+ aConnectData.playerID + "-" + aConnectData.sideType
							+ ", viewerId = " + viewer.viewerId );
				
				// release the player, if any previously controlled by this viewer
				Splayer myPlayer = soccerWorld.getPlayer(viewer.playerSide, viewer.playerID);
				if (myPlayer != null) {
					myPlayer.setUserControlled(false);
					myPlayer.setControllerID(-1);
				} 
				
				// get the player requested by this viewer
				Splayer userPlayer = soccerWorld.getPlayer(aConnectData.sideType, aConnectData.playerID);
				
				if (userPlayer != null) {
					if (userPlayer.isUserControlled()) {
						// player already taken; deny the request
						clientMode = InitData.USER_DENIED;
						id = userPlayer.getControllerID(); 
						role = viewer.playerSide;
					} else {
						// assign this player to viewer
						userPlayer.setControllerID(viewer.viewerId);
						userPlayer.setUserControlled(true);
						// set cross references
						viewer.playerID = aConnectData.playerID;
						id = viewer.playerID;
						viewer.playerSide = aConnectData.sideType;
						role = viewer.playerSide;	
					}
				}
			} else {
				// just confirm connection
				clientMode = InitData.VIEWER;
				id = viewer.viewerId;
				role = ConnectData.ROLE_UNKNOWN;
			}
			// reply to the registered client, send the viewer or player number back.
			InitData initData = new InitData(	clientMode, 
												id,
												role,
												TOS_Constants.SIM_STEP_SECONDS, 
												0 	// not used here
											);
			Packet initPacket =
				new Packet(	Packet.INIT, 
								initData, 
								address, 
								port 
							);
			transmitter.setSendInit(initPacket, true);
			System.out.println("Packet.INIT sent. " + initPacket.writePacket() );			
			return;
//<=========
		}
		
		/**
		 *  new viewer client registration
		 */
		int viewerId = soccerWorld.getNewViewerId();
		
		if (viewerId > 0) {
			// ID is still available
			boolean coach = false;
			if (aConnectData.sideType == ConnectData.ANYSIDE_COACH) {
				coach = true;
			}
			viewer =
				new Sviewer(
						address,
						port,
						viewerId,
						transmitter.getTicker(),
						coach
					);
					
			soccerWorld.addViewer(viewer);
			// reply to the client, send the viewer number back.
			InitData initdata = new InitData(	InitData.VIEWER, 
											viewerId, 
											ConnectData.ROLE_UNKNOWN,
											TOS_Constants.SIM_STEP_SECONDS, 
											0 	// not used
											);
			Packet initPacket =
				new Packet(Packet.INIT, initdata, address, port);
			transmitter.setSendInit(initPacket, true);
			System.out.println("-- registered viewerId = " + viewerId 
								+ " address = " + address + " port = " + port );
			
			// this makes the HeartOfWorld advancing time
			//soccerRules.setPeriod( RefereeData.PRE_GAME );

		} else
			notAvailable(address, port); // no ID available		
	}
	

	/**
	 * this method registers new player client and 
	 * sends back an acknowledgement 
	 * @param aConnectData
	 * @param address
	 * @param port
	 * @throws IOException
	 */
	private void connectToPlayerClient( ConnectData aConnectData, 
										InetAddress address, 
										int port ) throws IOException {
		// To make asynchronious registration of player clients possible, 
		// this method analyzes whether: 
		// (1) the  player attempting to register is the goalie or a field player;
		// (2) if it is the goalie, makes sure that only the first registered goalie
		//     is recognized as such, 
		// (3) resolve conflicts, if any, with player IDs, and
		// (4) send the the player ID and the goalie role as determined by server  
		
		// determine player ID
		int playerId;
		char side = aConnectData.sideType;
		char role = aConnectData.playerRole;
		playerId = soccerWorld.getNewPlayerId(side);
		
		//System.out.println("Server is connecting to player " + playerId + "-" + side);

		boolean isKicker = false;
		// server does not care how many players are assigned as kickers; 
		// class SoccerRules will recognize only one kicker; if no kicker 
		// is designated, it will pick one, anyway

		
		boolean isGoalie = false;
		if ( aConnectData.playerRole == ConnectData.GOALIE ) {
			// make sure that only one goalie, if any, can be registered;
			// (it is left up to the client to decide if the goalie is needed)
			if (soccerWorld.getGoalie(side) == null){
				isGoalie = true;
				role = ConnectData.GOALIE;
			}
			else {
				if ( aConnectData.playerRole == ConnectData.FIELD_PLAYER_KICKER ) {
					isKicker = true;
					role = ConnectData.FIELD_PLAYER_KICKER;
				}else{
				// no more than one goalie per team allowed
				role = ConnectData.FIELD_PLAYER;	// override GOALIE role
				}
			}
		}
		
		if ( playerId > 0 ) {
			// this object represents player client on the server side
			Splayer player =
				new Splayer(
						address,
						port,
						side,
						playerId,
						isGoalie, 	
						isKicker, 
						transmitter.getTicker(),
						false
					);
			
			soccerWorld.addPlayer(player);
			
			// set player initial position; also set the team name;
			// only the first received team name is accepted
			if ( side == 'l' ) {
				World.leftInitPos.setElementAt( aConnectData.pos, playerId );
				//if (TOS_Constants.LEFTNAME == "")
				TOS_Constants.LEFTNAME = aConnectData.teamName; 
			} else { 
				Vector2d pos = new Vector2d( -aConnectData.pos.getX(), -aConnectData.pos.getY() ); 
				World.rightInitPos.setElementAt( pos, playerId ); 
				//if (TOS_Constants.RIGHTNAME == "")
				TOS_Constants.RIGHTNAME = aConnectData.teamName; 
			}
		//	System.out.println("Team  : " +aConnectData.teamName );
			// reply to the client, send the player team, number, and some other data back.
			InitData initdata; 
			if ( side == 'l')
				initdata = new InitData(InitData.LEFT, 
										playerId,
										role,
										TOS_Constants.SIM_STEP_SECONDS,  
										0 	// not used here
									);
			else 
				initdata = new InitData(InitData.RIGHT, 
										playerId,
										role, 
										TOS_Constants.SIM_STEP_SECONDS, 
										0 	// not used here
									);
			Packet initPacket =
				new Packet(Packet.INIT, initdata, address, port);
			transmitter.setSendInit(initPacket, true);
			System.out.print("--HTMMFT registered playerId = " + playerId + " side = " + side 
				+ " addr = " + address + " port = " + port );
			if ( isKicker )
				System.out.println(" ** kicker **" );
			
			if ( isGoalie )
				System.out.println(" ** goalie **" );
			else
				System.out.println();
			
		} else
			notAvailable(address, port); // no ID available
	}

	
	// reply to client there's no spot available
	private void notAvailable(InetAddress address, int port)
											throws IOException {
		InitData initdata = new InitData(	InitData.FULL, 
											0,
											ConnectData.ROLE_UNKNOWN, 
											TOS_Constants.SIM_STEP_SECONDS,
											0 	// not used
										);
		Packet initPacket = new Packet(Packet.INIT, initdata, address, port);
		transmitter.setSendInit(initPacket, true);
	}

	/**
	 * This method executes an action command received from client
	 * @param packet
	 */
	private void setAction(Packet packet) throws IOException {
		
		// figure it out who is the sender
		InetAddress address = packet.address;
		int port = packet.port;
		Sviewer viewer = null;
		Splayer player = soccerWorld.getPlayer(address, port);
		
		if (player == null)
			// this packet arrived from viewer client
			viewer = soccerWorld.getViewer(address, port);
		else {
			// this packet arrived from player client;
			// check if this player has already executed some action 
			if (player.getLastTime() == transmitter.getTicker()) {
				// no more than one action in one simulation cycle is allowed
				// in the regular game mode
				if ( soccerRules.getMode() == RefereeData.PLAY_ON )
					return;
//<=================
			}
		}
		
		if (packet.packetType == Packet.EMPTY) {		
			// just keep client in the SoccerWorld alive
			if (viewer != null)
				viewer.setLastTime(transmitter.getTicker());
			else {
				player.setLastTime(transmitter.getTicker());
				// no action command is processed from AI player if controlled by user
				if (player.isUserControlled()) 
					return;
//<=================
			}
		}
			
		switch ( packet.packetType ) {
			
			//---------- player/viewer client action commands -------------
	
			case Packet.DRIVE:
				
				if (player != null) {
					// packet arrived from AI player
		            
//					System.out.println(transmitter.getTicker() + " receiving Packet.DRIVE" 
//		            		+ " player: " + player.getId() + "-" + player.getSide() 
//		            		+ " data = " + packet.writePacket());
		            
					synchronized (player) {						
						setPlayerMotion( packet, player );
					}
				} else if (viewer != null) {
					// packet arrived from viewer (could be user controlled player)
					Splayer userPlayer = soccerWorld.getPlayer(viewer.playerSide, viewer.playerID); 
					if (userPlayer != null){
						// this packet is a command for the user controlled player
						/*
						System.out.println(transmitter.getTicker() + " receiving Packet.DRIVE from USER" 
			            		+ " player: " + userPlayer.getId() + "-" + userPlayer.getSide() 
			            		+ " data = " + packet.writePacket());
						*/
						setPlayerMotion( packet, userPlayer );
					}					
				}
			break; 
			
			case Packet.KICK:
				
				if (player != null) {
		            /*
					System.out.println(transmitter.getTicker() + " receiving Packet.KICK" 
		            		//+ " player.id=" + player.getId() + "-" + player.getSide() 
            				+ " data = " + packet.writePacket());
					*/
					synchronized (player) {						
						setBallMotion( packet, player );
					}
					//System.out.println(soccerHeart.getTicker() + " kicking: " + player.isKickBall() );
					KickData kickData = (KickData)packet.data;
					if (kickData.receiverID > 0) {
						// set ball pass info for retransmitting it to viewers
						transmitter.setPassFlag(true, kickData);
						//System.out.println("Received ball pass info. receiverID=" 
								//+ kickData.receiverID + " endPoint " + kickData.endPoint);
					}
				} else if (viewer != null) {
					Splayer userPlayer = soccerWorld.getPlayer(viewer.playerSide, viewer.playerID); 
					if (userPlayer != null){
						// this packet is a command for the user controlled player
						/*
						System.out.println(transmitter.getTicker() + " receiving Packet.KICK from USER" 
			            		+ " player: " + userPlayer.getId() + "-" + userPlayer.getSide() 
			            		+ " data = " + packet.writePacket());
						*/
						setBallMotion( packet, userPlayer );
					}
				} 
			break; 
			
			//------------- player and viewer client action commands ------------------ 
			case Packet.TELEPORT: 
				// the TELEPORT packet is used for:
				// (1) teleporting objects with the mouse; and
				// (2) dragging the grabbed ball by the goalie   
			
				//System.out.println("Receiving Packet.TELEPORT: " + packet.writePacket() );
				
				TeleportData aTeleportData = (TeleportData) packet.data;
				// now update the world model...
				if ( teleport( aTeleportData, viewer, player ) ) {
					// ... and force the feedback (in the stepwise mode only)
					try {
						if ( transmitter.isStepping() )
							transmitter.sendVisualData();
					} catch (Exception e ) {
						// error can be generated here if during the registration 
						// not all agents have been connected yet
						System.out.println("Error while replying to Packet.TELEPORT " + e );
					}
				}
			break; 
			
			//------------- viewer client action commands ------------------ 
			case Packet.TALK: 
				TalkData talk = (TalkData) packet.data;
				if (talk.message.length() > 30) {
					//System.out.println(talk.message.length() + talk.message);
					player.setMessage(talk.message.substring(0, 29));
					//System.out.println(player.message);
				} else
					player.setMessage(talk.message);
			break; 

			case Packet.VIEW: 
				//System.out.println("Receiving Packet.VIEW");			
				//System.out.println( packet.writePacket() ); 
				processViewPacket( packet );
			break; 

			case Packet.SITUATION: 
				System.out.println("Receiving Packet.SITUATION");
				processSituationPacket( packet );
				// let the TransmitterThread do its job now
				Thread.yield();	
					
			break; 
		
			case Packet.PERIOD:
				// change game state in the response to the human user command
				//try {
					//System.out.println("Receiving Packet.PERIOD: " + packet.writePacket() );
				//} catch (Exception e ) {
					//System.out.println("Error while receiving Packet.PERIOD " + e );
				//}
				if (player != null) {
					setPlayerPeriodAction( packet, player );
				} else {
					if (viewer != null) {
						setViewerPeriodAction( packet, viewer );
					} else
						return;
				}
			break; 
			
			//------------- special action commands --------------
			case Packet.INFO:
				
				try {
					System.out.println("Receiving Packet.INFO: " + packet.writePacket() );
				} catch (Exception e ) {
					System.out.println("Error while receiving Packet.INFO " + e );
				}
				
				// some non-critical info messages
				InfoData infoData = (InfoData) packet.data;
				
				switch ( infoData.info ) {
					
					case InfoData.SOUND_ON_OFF:
						// toggle sound on/off and re-transmit to player clients
						boolean setting = (infoData.info1 == 1);
						soccerWorld.setSoundOn(setting);
					break;
					
					default:
				}
			break; 
		
			case Packet.BYE:
				// remove client from the SoccerWorld 
				System.out.println("Receiving Packet.BYE: " + packet.writePacket() );
				if (player != null) {
					// from player client
					if (player.getSide() == 'l') {
						soccerWorld.removePlayer(player);
						soccerWorld.putBackLeftPlayerId(player.getId());
					} else {
						soccerWorld.removePlayer(player);
						soccerWorld.putBackRightPlayerId(player.getId());
					}
				} else {
					if (viewer != null) {
						// from viewer client 
						ByeData  data = (ByeData)packet.data;
						if (data.actionType == ByeData.TERMINATE) {
							// tell Transmitter to send termination messages to all clients 
							transmitter.setTerminated(true);
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) { }
							System.out.println("--- Server terminated by Game Coordinator. See message box.");
							JOptionPane.showMessageDialog(null,
									"Server terminated by Game Coordinator", 
									"Terminated",
									JOptionPane.PLAIN_MESSAGE);
							System.exit(0);
//<=========================
						} else if (data.actionType == ByeData.DISCONNECT) {
							// release user controlled player, if any
							releasePlayer(viewer);
							soccerWorld.removeViewer(viewer);
							soccerWorld.putBackViewerId(viewer.viewerId);
							// tell Transmitter to send disconnect confirmation to this client
							// TODO (similar to sending INIT packets)
						}  else if (data.actionType == ByeData.RELEASE) {
							// just release user controlled player, if any
							releasePlayer(viewer);
						}
					} 
				}
			break; 
		
			default: ;	// do nothing
		}
	}
	
	/**
	 * This method releases player, if any, controlled by this viewer client
	 */
	private void releasePlayer(Sviewer viewer) {
		Splayer myPlayer = soccerWorld.getPlayer(viewer.playerSide, viewer.playerID);
		if (myPlayer != null) {
			myPlayer.setUserControlled(false);
			myPlayer.setControllerID(-1);
			System.out.println("Releasing player " + viewer.playerID + "-" + viewer.playerSide);
			viewer.playerID = -1;
			viewer.playerSide = '?';
		}		
	}

	
	private void setViewerPeriodAction( Packet packet, Sviewer viewer ) {
		viewer.setLastTime(transmitter.getTicker());
		if (viewer.coach) {
			PeriodData serverControl = (PeriodData) packet.data;
			
			switch(serverControl.actionType) {
				
				case PeriodData.STEP:
					transmitter.setStepping(true);
					transmitter.stepForward();
				break;
				
				case PeriodData.PLAY:
					transmitter.setStepping(false);
					//System.out.println("Receiving Packet.PERIOD PeriodData.PLAY " 
								//+ " getTicker() = " + transmitter.getTicker() );
				break;
				
				case PeriodData.FORWARD:
					transmitter.setStepping(true);
					transmitter.periodForward();
                break;														
				
				default:
			}
		}
	}	
	
	
	private void setPlayerPeriodAction( Packet packet, Splayer player ) {
		
		player.setLastTime(transmitter.getTicker());
		if (player.isCoach()) {
			PeriodData serverControl = (PeriodData) packet.data;
			
			switch(serverControl.actionType) {
				
				case PeriodData.STEP:
					transmitter.setStepping(true);
					try{
						transmitter.stepForward();
					}
					catch(Exception e){
					}
				break;
				
				case PeriodData.PLAY:
					transmitter.setStepping(false);
				break;
				
				case PeriodData.FORWARD:
                	transmitter.setStepping(true);
                	transmitter.periodForward();
                break;														
				
				default:
			}
		} 		
	}

	/**
	 * This method changes the player state by executing his
	 * DRIVE command; in doing so, it imposes constraints
	 * on player dashing force 
	 * 
	 * @param packet
	 * @param player
	 */
	private void setPlayerMotion( Packet packet, Splayer player ) {
		
		player.setLastTime(transmitter.getTicker());
		
		DriveData aDriveData = (DriveData) packet.data;

		// player cannot kick the ball if it is driving itself	
		player.setKickBall(false);
		
		// player is regarded having the ball if it is close
		if (player.getPosition().distance(soccerWorld.getBall().getPosition()) 
											< TOS_Constants.BALLCONTROLRANGE)
			player.setWithBall(true);
		else
			player.setWithBall(false);

		// ** set player force/acceleration direction **
		player.setForceDir(aDriveData.dir); 

		// ** set player driving force magnitude **
		// (zero force means TURN)
		if (aDriveData.force > TOS_Constants.MAXDASH)
			aDriveData.force = TOS_Constants.MAXDASH;
		else if (aDriveData.force < TOS_Constants.MINDASH)
			aDriveData.force = TOS_Constants.MINDASH;
		else if (Math.abs(aDriveData.force) < ALMOST_ZERO)
			aDriveData.force = ALMOST_ZERO;			
		
		player.setForce(aDriveData.force);
		
		// this info is used for re-sending to viewers only
		player.setChasingBall(aDriveData.chasingBall);
	} 

	
	/** 
	 * this method applies some constraints on the kick ball force and 
	 * direction contained in the received packet;
	 * @param packet
	 * @param player
	 */
	private void setBallMotion( Packet packet, Splayer player ) {
		/*
		try {
			System.out.println(transmitter.getTicker() 
					+ " setBallMotion: packet = " + packet.writePacket() );
		} catch ( IOException e ) {}
		*/
		
		if (player.getPosition().distance(soccerWorld.getBall().getPosition()) 
							> TOS_Constants.BALLCONTROLRANGE) {
			player.setKickBall(false);
			player.setWithBall(false);
			return;
//<=========
	}
		
		player.setLastTime(transmitter.getTicker());

		soccerWorld.stepBallWasGrabbed 
						= Integer.MAX_VALUE;	// forget ball was grabbed
		soccerWorld.getBall().isGrabbed = false;
		
		KickData kick = (KickData) packet.data;

		player.setKickBall(true);
		player.setWithBall(true);
		
		// restrict kick direction
		if (kick.dir > 180)
			kick.dir = 180;
		else if (kick.dir < -180)
			kick.dir = -180;

		// restrict kick force
		if (kick.force > TOS_Constants.MAXKICK)
			kick.force = TOS_Constants.MAXKICK;
		else if (kick.force < TOS_Constants.MINKICK)
			kick.force = TOS_Constants.MINKICK;
			
		// limit the force of the first kicks in the throw-in mode
		// (this is somewhat enforcing the throw-in rules)
		if ( soccerWorld.throwInModeL ) {
			if ( player.getSide() == 'l' ) {
				soccerWorld.throwInModeL = false;	// forget THROW_IN_L
				kick.force = Math.min(kick.force, TOS_Constants.MAXKICK/2.0);
			} else {
				kick.force = 0; 	// ignore this kick
			}
			//System.out.println("kicking force reduced to " + kick.force 
				//+ " for player id = " + player.id + " side = " + player.side );
		}
		if ( soccerWorld.throwInModeR ) {
			if ( player.getSide() == 'r' ) {
				soccerWorld.throwInModeR = false;	// forget THROW_IN_R
				kick.force = Math.min(kick.force, TOS_Constants.MAXKICK/2.0);
			} else {
				kick.force = 0; 	// ignore this kick
			}
			//System.out.println("kicking force reduced to " + kick.force 
				//+ " for player id = " + player.id + " side = " + player.side );
		}
		
		player.setForce(kick.force);
		player.setForceDir(kick.dir);
		
		//System.out.println(transmitter.getTicker() + " setBallMotion: player kickDir= " 
				//+ (int)player.getDirection() + " kickForce=" + (int)player.getForce());
	}
	
	
	/** 
	 * this method updates the **saved** world state 
	 * with the data in the received packet 
	 * (the packet may contain not a whole team, though)
	 * @param packet
	 * @throws IOException
	 */
	private synchronized void processViewPacket( Packet packet ) throws IOException {	
		try {
			// update ball
			ViewData receivedData = (ViewData)packet.data; 	
			soccerWorld.ballSaved.assign( receivedData.ball );		
			
			// update left team players
			for ( int i=0; i<receivedData.leftTeam.size(); i++ ) {
				Player player = (Player)receivedData.leftTeam.elementAt( i );
				Splayer splayer = soccerWorld.getPlayerSaved( player.getSide(), player.getId() );
				splayer.assign( player );
			}
	
			// update right team players
			for ( int i=0; i<receivedData.rightTeam.size(); i++ ) {
				Player player = (Player)receivedData.rightTeam.elementAt( i );
				Splayer splayer = soccerWorld.getPlayerSaved( player.getSide(), player.getId() );
				splayer.assign( player );
			}
		} catch (Exception e) {
			System.out.println( "Exception caught in processViewPacket: " + e );
			System.out.println( "packet = " + packet.writePacket() );
		}
	}


	/** 
	 * this method updates the world with the data in the received packet
	 * @param packet
	 */
	private void processSituationPacket( Packet packet ) {
		SituationData receivedData 	= (SituationData)packet.data; 
		
		transmitter.setReplication( receivedData.numOfSteps,
									receivedData.numOfReplicas,
									receivedData.stepID );
					
		// reset the world to the saved situation
		soccerWorld.restoreSituation();	
		
		// update all clients about change
		try {
			transmitter.sendVisualData();
		} catch (Exception e ) {
			System.out.println("Error while replying to Packet.SITUATION " + e );
		}		
		System.out.println("server reset to new situation. numOfReplicas = " 
			+ receivedData.numOfReplicas + " numOfSteps = " + receivedData.numOfSteps );
	}
	
	
	/**
	 * This method teleports the object referred to in the input data
	 * returns true if only the object can be teleported.
	 * 
	 * @param aTeleportData
	 * @param sendingViewer
	 * @param sendingPlayer
	 * @return
	 */
	public boolean teleport( 	TeleportData aTeleportData, 
								Sviewer sendingViewer, 	
								Splayer sendingPlayer )	{
		
		char objType 	= aTeleportData.objType;
		char side 		= aTeleportData.side;
		int playerID 	= aTeleportData.playerID; 
		double newX 	= aTeleportData.newX;
		double newY 	= aTeleportData.newY;
		Vector2d newpos = new Vector2d(newX, newY);
		
		// determine whether the teleportation is allowed
		boolean	teleportationAllowed = false;
		
		if ( sendingViewer != null ) 
		
			teleportationAllowed = true;	// teleporting an object with the mouse
		
		else if ( sendingPlayer != null ) {
			if ( soccerRules.getMode() == RefereeData.PLAY_ON && 
				 sendingPlayer.isGoalie()	)	
				 // the goalie must be in the penalty area on own side
				if ( World.inPenaltyArea( side, newpos, 0 ) ) {
					teleportationAllowed = true;	
				}
		}
			
		if ( teleportationAllowed ) {
			
			//System.out.println("  objType = " + objType +" playerID = " + playerID 
						//+ "  side = " + side );
			
			switch ( objType ) {
				
				case TeleportData.GRAB:  
					
					if ( sendingPlayer != null ) {
						soccerWorld.setBall( new Sball() );
						// set the ball grabbed by this player
						soccerWorld.getBall().setAtPlayerPos( sendingPlayer, true  );
						if ( soccerWorld.stepBallWasGrabbed ==Integer.MAX_VALUE ) {
							// first GRAB command received
							soccerWorld.stepBallWasGrabbed 
											= transmitter.getTicker(); 
							soccerWorld.sideGrabbedBall = side;
							//System.out.println("Ball grabbed. step = " + transmitter.getTicker()
									//+ " isGrabbed=" + soccerWorld.ball.isGrabbed );
						}
					}
				break;

				case TeleportData.BALL:  
					
					if ( ( sendingViewer != null ) 
							|| ( soccerWorld.getBall().isGrabbed	
								&& soccerWorld.sideGrabbedBall == side ) ) {	
						
						// the viewer is dragging ball with the mouse 
						// OR the goalie teleports grabbed ball
						
						soccerWorld.setBall( new Sball() );
						soccerWorld.getBall().set( newX, newY );
					}
				break;
				
				
				case TeleportData.LEFT_PLAYER:  
				case TeleportData.RIGHT_PLAYER:    
					
					Splayer player = soccerWorld.getPlayer( side, playerID );
					player.getPosition().setXY( newpos ); 
					player.getVelocity().setXY( 0, 0 );
					player.getAcceleration().setXY( 0, 0 );

					
				default:
			}	
		}
		return teleportationAllowed;
	}
	
	public void setServer(HTMMFTSoccerServerMain soccerServerMainHTMMFT) {
		soccerServer = soccerServerMainHTMMFT;
	}

}
