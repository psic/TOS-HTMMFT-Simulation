/** Cviewer.java 
 * 
 * This class receives data packets (gets sensing info) from server
 * and displays it.
 * Also this class plays sounds on some commands received from the 
 * server and sends back empty packets to confirm that the viewer 
 * client is still running. 
 * 
 * Copyright (C) 2001  Yu Zhang
 * 
 * Modifications by Vadim Kyrylov  
 * 2006-2012
*/

package soccer.client;

import java.io.IOException;

import javax.swing.JOptionPane;

import soccer.client.action.PlayGameAction;
import soccer.client.view.j3d.FieldJ3D;
import soccer.common.ByeData;
import soccer.common.DriveData;
import soccer.common.EmptyData;
import soccer.common.HearData;
import soccer.common.InfoData;
import soccer.common.InitData;
import soccer.common.KickData;
import soccer.common.Packet;
import soccer.common.Player;
import soccer.common.RefereeData;
import soccer.common.TOS_Constants;
import soccer.common.Vector2d;
import soccer.common.ViewData;


public class Cviewer {
	
	protected ViewerClientMain soccerMaster;
	private ViewerWorld world; //world view
	private int 	leftGoalieID;
	private int 	rightGoalieID;
	private int 	packetCount;

	// this allows the user to move objects around the field 
	private Controller controller; 

	/**
	 * This constructor is good for both the regular and debug mode
	 * @param soccerMaster
	 */
	public Cviewer(ViewerClientMain soccerMaster) {
		this.soccerMaster = soccerMaster;
		world = new ViewerWorld();
		soccerMaster.setWorld(world);
		soccerMaster.arena2D.setWorld(world);
		soccerMaster.arena3D.setWorld(world); 
		((FieldJ3D)(soccerMaster.arena3D)).enableMouseNavigation(true); 
		controller = new Controller(world, soccerMaster);
		soccerMaster.arena2D.addMouseListener(controller);
		soccerMaster.arena2D.addMouseMotionListener(controller);
	}

	/**
	 * This method receives a data packet and processes it.
	 * It returns the processed packet for using elsewhere
	 */
	public Packet runOneStep() {
		
		Packet receivedPacket = new Packet();	// dummy packet
		
		if ( soccerMaster.getGState() != TOS_Constants.WAITING 
								&& !soccerMaster.isPlaying() ) {
				controller.clearGrabFlags();
		}
		
		try {
			receivedPacket = soccerMaster.getTransceiver().receive();
			
			if (receivedPacket != null)	// null may happen in the debug mode
				processPacket( receivedPacket );

			// do some household work
			packetCount++;
			
			if (packetCount % 300 == 0) {
				// keep the server aware of this viewer is alive
				soccerMaster.sendToServer(Packet.EMPTY, new EmptyData());
			}

		
		} catch (IOException e) { }
		// if this packet is dummy, it will not crash the system
		return receivedPacket;	
	}

	
	/**
	 * This method processes the received data packet.
	 * It is declared public for the sole purpose of 
	 * calling it from the debugger application.
	 * This method also executes persistent user commands, if any.
	 */
	public void processPacket(Packet receivedPacket) {

		// process the received data
		switch ( receivedPacket.packetType ) {
			
			case Packet.VIEW:
				//System.out.println(" Packet.VIEW packet received");	
				// visual info update (once per simulation step)
				
				// update viewer client world and display 
				processViewPacket(receivedPacket);
				
				if (soccerMaster.isPlaying()) {
					// allow using keyboard to control the player 
					soccerMaster.setSendingCommandAllowed(true);
					// execute persistent control commands, if any
					executeUserCommands();
				}
				break;
				
			case Packet.HEAR:
				// verbal message arrrived
				world.setHearData((HearData) receivedPacket.data);
				if (world.getHearData().side == 'l')
					world.setLeftMhearData(world.getHearData());
				else if (world.getHearData().side == 'r')
					world.setRightMhearData(world.getHearData());
				break;
				
			case Packet.INFO: 
				// message with miscellaneous info arrived
				processInfoPacket(receivedPacket);				
				break;
				
			case Packet.REFEREE: 
				// message about game mode and referee signal arrived
				//try {
				//System.out.println( "received Packet.REFEREE = " 
									//+ receivedPacket.writePacket() );
				//} catch (IOException e ) {}
				processRefereePacket(receivedPacket);
				
				break;
			
			case Packet.INIT:
				// this is only possible when the user takes the control of a player
				processInitPacket(receivedPacket);
				break;
				
			case Packet.BYE: 
				// termination message arrived
				System.out.println("--- Packet.BYE received ---");
				JOptionPane.showMessageDialog(null,
						"Termination of this Viewer Client confirmed by server", 
						"Confirmed",
						JOptionPane.PLAIN_MESSAGE);
				if (!soccerMaster.isCoordinator())
					System.exit(0);
				break;
				
			default:
				;
		}
	}

	
	/**
	 * Process confirmation message about taking control of a player
	 */
	private void processInitPacket(Packet receivedPacket) {
		if (receivedPacket.packetType == Packet.INIT) {
			InitData initData = (InitData) receivedPacket.data;
			try {
				System.out.println("Received Packet.INIT " + receivedPacket.writePacket() );
			} catch (IOException e) {
				e.printStackTrace();
			}			
			if (initData.clientType == InitData.USER) {
				// taking control confirmed
				soccerMaster.setPlaying(true);
				Player p = world.getPlayer(initData.role, initData.num);
				soccerMaster.setControlledPlayer(p);
				System.out.println("Controlled player = " + p );
			} else if (initData.clientType == InitData.USER_DENIED) {
				// taking control denied
				soccerMaster.setDenied(true);
				JOptionPane.showMessageDialog(soccerMaster,
						"Player already taken by user #" + initData.num, 
						"Denied",
						JOptionPane.ERROR_MESSAGE);
				// restore Play Game dialog
				soccerMaster.getAction((Class<?>) PlayGameAction.class)
								.actionPerformed(null);				
			}
		}
	}
	
	/**
	 * Process information about the state of the world and display it on the field 
	 */
	private void processViewPacket(Packet receivedPacket) {
		
		world.setViewData((ViewData) receivedPacket.data);
		
		world.setMe(null);
		world.setBall(world.getViewData().ball);
		world.setLeftTeam(world.getViewData().leftTeam);
		setGoalieInLeftTeam();	// goalie id was received separately
		world.setRightTeam(world.getViewData().rightTeam);
		setGoalieInRightTeam();
		
		// some object could be grabbed with the mouse by the user;
		// clear all isGrabbed flags except the ball
		controller.updateGrabFlags( false );

		// get the time information
		int sec = (int)( world.getViewData().time * TOS_Constants.SIM_STEP_SECONDS + 0.5 );
		int min = sec / 60;
		sec = sec % 60;

		//System.out.println("Packet.VIEW  time = " + world.view.time 
		//			+ " min=" + min + " sec=" + sec);
			
		soccerMaster.timeJLabel.setText(min + ":" + sec);
		/*
		// find out if somebody has kicked the ball
		if (world.getBall().controllerType != world.getPreviousController()) {
			world.setPreviousController(world.getBall().controllerType);
			if (world.getBall().controllerType == 'f')
				soccerMaster.getSoundSystem().playClip("kick");
		}	
		*/
		// update the arena
		if(soccerMaster.isIn3D()) {
			soccerMaster.arena3D.repaint();
		}
		else {
			soccerMaster.arena2D.repaint();
		}
		
		// periodically reset all flags, as some might be outdated
		if (world.getViewData().time%5 == 0)
			world.setChasingBallOff();
	}

	
	// process referee commands
	private void processRefereePacket(Packet receivedPacket) {
		
		world.setRefereeData((RefereeData) receivedPacket.data);
		
		int sec = (int)(world.getRefereeData().time * TOS_Constants.SIM_STEP_SECONDS);
		int min = sec / 60;
		sec = sec % 60;
		
		//System.out.println("Packet.REFEREE  period = " 
			//+ RefereeData.periods[world.referee.period] 
			//+ " mode = " + RefereeData.modes[world.referee.mode] );

		soccerMaster.periodJLabel.setText(
			RefereeData.periods[world.getRefereeData().period] + ":");
		soccerMaster.modeJLabel.setText(
			RefereeData.modes[world.getRefereeData().mode] + ":");
		soccerMaster.timeJLabel.setText(min + ":" + sec);

		soccerMaster.leftName.setText(world.getRefereeData().leftName);
		String scoreL = world.getRefereeData().score_L 
								+ " ("+ world.getRefereeData().total_score_L + ")";
		soccerMaster.leftScore.setText(" " + scoreL );

		soccerMaster.rightName.setText(world.getRefereeData().rightName);
		String scoreR = world.getRefereeData().score_R 
								+ " ("+ world.getRefereeData().total_score_R + ")";
		soccerMaster.rightScore.setText(" " + scoreR );

		
		// do not display the last message, as no games would follow
		if ( world.getRefereeData().game <= world.getRefereeData().games )
			soccerMaster.gameJLabel.setText("Game " 
				+ world.getRefereeData().game + " of " + world.getRefereeData().games);

		playRefereeSound();
	}

	
	private void processInfoPacket(Packet receivedPacket) {
		
		world.setInfoData((InfoData) receivedPacket.data);
		
		switch ( world.getInfoData().info ) {
			
			case InfoData.WAIT_NEXT:
				// this is the server feedback from button pressed
				soccerMaster.setGState( TOS_Constants.WAITING );
				soccerMaster.getSoundSystem().playClip("click01");
				break;
			
			case InfoData.RESUME:
				// this is the server feedback from button pressed
				soccerMaster.setGState( TOS_Constants.RUNNING );	
				break;

			case InfoData.REPLICA:
				// this is a server feedback about the next replica
				if ( world.getInfoData().info2 > 0 )
					soccerMaster.replicaJTextField
						.setText( world.getInfoData().info1 
						+ " of " + world.getInfoData().info2 );
				else 
					soccerMaster.replicaJTextField.setText( "" );
				break;
			
			case InfoData.WHO_ARE_THE_GOALIES:
				// this info is about the goalies
				leftGoalieID = world.getInfoData().info1;
				rightGoalieID = world.getInfoData().info2;
	            //System.out.println("received goalie ids: " + leftGoalieID + " " + rightGoalieID);
				break;
	             
			case InfoData.WHO_IS_CHASING_BALL:
				// this optional info is about players chasing the ball
				// (player clients are not required to release this info)
				int teamSide = world.getInfoData().info1;
				int playerID = world.getInfoData().info2;
				world.setChasingBallOn(teamSide, playerID);
				break;

			case InfoData.BALL_PASS:
				// this optional info is about the ball pass end point;
				// (player clients are not required to release this info)
				double x = world.getInfoData().info1/100.0;
				double y = world.getInfoData().info2/100.0;
				world.setEndPoint(new Vector2d(x, y));
				world.setReceiverID(world.getInfoData().info3);
				world.setReceiverSide(world.getInfoData().extraInfo.charAt(0));
				//System.out.println("Received ball pass info. receiverID=" 
						//+ world.receiverID + " endPoint " + world.endPoint);
				break;

			case InfoData.BALL_KICK:
				// play sound if the ball kick was executed 
				soccerMaster.getSoundSystem().playClip("kick");
				break;
				
			case InfoData.COLLISION:
				// this info is about player collision
				int id = (int)(3.0 * Math.random() + 0.5);
				// play randomly selected audio clip 
				soccerMaster.getSoundSystem().playClip("oops" + id);
				break;

			default: ; 		// do nothing
		} 
	}
	
	/**
	 * inform the server that this Viewer Client has left the game
	 */
	public void end(char actionType) {
		try {
			soccerMaster.sendToServer(Packet.BYE, new ByeData(actionType));
			System.out.println("sending Packet.BYE  actionType = " + actionType);
		} catch (IOException e) {
			System.out.println("Cviewer end caught " + e);
		}
	}

	private void playRefereeSound() {
		
		if (world.getRefereeData().total_score_L > world.getLeftGoalCount()) {
			world.setLeftGoalCount(world.getRefereeData().total_score_L);
			soccerMaster.getSoundSystem().playClip("applause");
		} else if (world.getRefereeData().total_score_R > world.getRightGoalCount()) {
			world.setRightGoalCount(world.getRefereeData().total_score_R);
			soccerMaster.getSoundSystem().playClip("applause");
		} else if (world.getRefereeData().period != world.getPreviousPeriod()) {
			soccerMaster.getSoundSystem().playClip("referee2");
			world.setPreviousPeriod(world.getRefereeData().period);
		} else if (world.getRefereeData().mode != world.getPreviousMode()) {
			soccerMaster.getSoundSystem().playClip("referee1");
			if (world.getRefereeData().mode == RefereeData.KICK_OFF_L 
					|| world.getRefereeData().mode == RefereeData.GOAL_KICK_L ) {
				soccerMaster.getSoundSystem().playClip("left_side_kicks_in");
			}
			if (world.getRefereeData().mode == RefereeData.KICK_OFF_R 
					|| world.getRefereeData().mode == RefereeData.GOAL_KICK_R ) {
				soccerMaster.getSoundSystem().playClip("right_side_kicks_in");
			}
			
			if (world.getRefereeData().mode == RefereeData.CORNER_KICK_L ) {
				soccerMaster.getSoundSystem().playClip("corner_kick_on_the_right_side");
			}
			if (world.getRefereeData().mode == RefereeData.CORNER_KICK_R ) {
				soccerMaster.getSoundSystem().playClip("corner_kick_on_the_left_side");
			}
			
			if (world.getRefereeData().mode == RefereeData.THROW_IN_L ) {
				soccerMaster.getSoundSystem().playClip("left_side_throws_in");
			}
			if (world.getRefereeData().mode == RefereeData.THROW_IN_R ) {
				soccerMaster.getSoundSystem().playClip("right_side_throws_in");
			}

			if (world.getRefereeData().mode == RefereeData.OFFSIDE_L 
					|| world.getRefereeData().mode == RefereeData.OFFSIDE_R ) {
				String clipName = "" + world.getRefereeData().playerID;
				//System.out.println(" offside violatorID = " + clipName);
				soccerMaster.getSoundSystem().playClip(clipName);
			}
			
			world.setPreviousMode(world.getRefereeData().mode);
		}
		
	}
	
	/**
	 * This method executes persistent user initiated player control actions
	 */
	private void executeUserCommands() {
		Player player = world.getPlayer(soccerMaster.getControlledPlayer().getSide(), 
										soccerMaster.getControlledPlayer().getId());
		if (Controller.isChasingToKickBall()) {
			/**
			 *  execute persistent chase and kick ball action
			 */
			if (player.getPosition().distance(
						world.getBall().getPosition()) <= TOS_Constants.BALLCONTROLRANGE) {
					// the ball is kickable 
				double direction = player.getPosition().direction(Controller.getKickEndPoint());
				double distance = player.getPosition().distance(Controller.getKickEndPoint());
				double force = 100;
				double distMax = 20;	// max distance for 100% force
				if (distance < distMax) 
					force = 5 + 95 * distance/distMax;
				try {
					soccerMaster.sendToServer(Packet.KICK, new KickData(direction, force));
				} catch (IOException e) {}
			} else {
				// chase the ball
				double direction = player.getPosition().direction(world.getBall().getPosition());
				double distance = player.getPosition().distance(world.getBall().getPosition());
				double force = 100;
				double distMax = 3.0;	// max distance for 100% force
				if (distance < distMax) 
					force = 10*(1.0 + distance/distMax);
				try {
					soccerMaster.sendToServer(Packet.DRIVE, new DriveData(direction, force));
				} catch (IOException e) {}
				// prepare to display
				world.setReceiverSide('u');	// user controlled player
				// make it blinking
				player.setChasingBall(world.getViewData().time%2 == 0);
				
				// limit number of executions
				Controller.setKickCommandCount(Controller.getKickCommandCount() + 1); 
				// limit repetition of this command to the present number of steps
				if (Controller.getKickCommandCount() > Controller.KICK_COMMAND_DURATION)
					Controller.resetChaseKickFlags();
			}
		} else if (Controller.isDrivenByUser()) {
			/**
			 *  execute persistent drive action to given point
			 */
			double direction = player.getPosition().direction(Controller.getDriveEndPoint());
			double distance = player.getPosition().distance(Controller.getDriveEndPoint());
			double force = 100;
			double distMax = 3.0;	// max distance for 100% force
			if (distance < distMax) 
				force = 10*(1.0 + distance/distMax);
			try {
				soccerMaster.sendToServer(Packet.DRIVE, new DriveData(direction, force));
			} catch (IOException e) {}
			// limit number of executions
			Controller.setDriveCommandCount(Controller.getDriveCommandCount() + 1); 
			// limit repetition of this command to the present number of steps
			if (Controller.getDriveCommandCount() > Controller.DRIVE_COMMAND_DURATION)
				Controller.resetDriveFlags();
		}
	}

	private void setGoalieInLeftTeam() {
		for (int i=0; i<world.getLeftTeam().size(); i++) {
			Player plr = world.getLeftTeam().elementAt(i);
			if (plr.getId() == leftGoalieID)
				plr.setGoalie(true);
			else
				plr.setGoalie(false);	
		}
	}
	
	private void setGoalieInRightTeam() {
		for (int i=0; i<world.getRightTeam().size(); i++) {
			Player plr = world.getRightTeam().elementAt(i);
			if (plr.getId() == rightGoalieID)
				plr.setGoalie(true);
			else
				plr.setGoalie(false);	
		}
	}

}
