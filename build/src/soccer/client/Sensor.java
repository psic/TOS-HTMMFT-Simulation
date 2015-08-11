/**
 *  Sensor.java
 *  
 *  This class get info from the server and build the world from it
 *  
 *  Copyright (C) 2001  Yu Zhang
 */  


package soccer.client;

import java.io.IOException;

import soccer.common.HearData;
import soccer.common.Packet;
import soccer.common.RefereeData;
import soccer.common.SeeData;
import soccer.common.TOS_Constants;
import soccer.common.Vector2d;


public class Sensor {
	
    // End time (in min)
    public static int END = 30;
    private int round;
    
	private ViewerWorld world;
	private Executor executor;
	private ViewerClientMain soccerMaster;
	
	// constructor
	public Sensor(ViewerWorld world, Executor executor, ViewerClientMain soccerMaster) {
		this.world = world;
		round = (int) (END * (60 / TOS_Constants.SIM_STEP_SECONDS));
		this.executor = executor;
		this.soccerMaster = soccerMaster;
	}
	

	// get sensor info from server
	public void getSensorInfo(Packet info) throws IOException {

		int min;
		int sec;

		// process the info
		if (info.packetType == Packet.SEE) {
			world.setSeeData((SeeData) info.data);

			world.setMe(world.getSeeData().player);
			world.setAmIOffSide(world.getSeeData().status);
			world.setBall(world.getSeeData().ball);
			world.setLeftTeam(world.getSeeData().leftTeam);
			world.setRightTeam(world.getSeeData().rightTeam);

			// get simulation time 
			sec = world.getSeeData().time / (int)(1 / TOS_Constants.SIM_STEP_SECONDS);
			min = sec / 60;
			sec = sec % 60;

			soccerMaster.timeJLabel.setText(min + ":" + sec);

			// find out if somebody has kicked the ball
			if (world.getBall().controllerType != world.getPreviousController()) {
				world.setPreviousController(world.getBall().controllerType);
				if (world.getBall().controllerType == 'f')
					soccerMaster.getSoundSystem().playClip("kick");
			}

			// find out if I have the ball
			if (world.getBall().controllerType == world.getMe().getSide()
				&& world.getBall().controllerId == world.getMe().getId())
				world.setBallKickable(true);
			else
				world.setBallKickable(false);

			// find out ball's velocity
			Vector2d.subtract(
								world.getBall().getPosition(),
								world.getPreviousBallPosition(),
								world.getBallVelocity());
			world.getPreviousBallPosition().setXY(world.getBall().getPosition());

			// find out my own velocity
			Vector2d.subtract(
								world.getMe().getPosition(),
								world.getPreviousPosition(),
								world.getMyVelocity());
			world.getPreviousPosition().setXY(world.getMe().getPosition());

			// ball's relative position 
			world.setDistance2Ball(
				world.getSeeData().player.getPosition().distance(world.getSeeData().ball.getPosition()));
			world.setDirection2Ball(
				world.getSeeData().player.getPosition().direction(world.getSeeData().ball.getPosition()));

			// find out if action on the previous step was successful or not
			synchronized (world) {
				if (world.getActionType() == TOS_Constants.KICK
							|| world.getActionType() == TOS_Constants.SHOOT
							|| world.getActionType() == TOS_Constants.PASS) {
					if (!world.isBallKickable()) {
						world.setDashForce(0);
						world.setActionType( TOS_Constants.DRIVE);
					}
				} else if (world.getActionType() == TOS_Constants.CHASE) {
					if (world.isBallKickable()) {
						world.setDashForce(0);
						world.setActionType( TOS_Constants.DRIVE);
					}
				} else if (world.getActionType() == TOS_Constants.MOVE) {
					double dist = world.getDestination().distance(world.getMe().getPosition());
					if (dist < 5) {
						world.setDashForce(0);
						world.setActionType( TOS_Constants.DRIVE);
					}
				}
			}
			
			if(soccerMaster.isIn3D()) {
				soccerMaster.arena3D.repaint();
			} else {
				soccerMaster.arena2D.repaint();
			}

			// execute my commands
            int reactionTime = world.getSeeData().time - world.getActionTime();
            if(reactionTime < 0) 
            	reactionTime += round;
			if (reactionTime >= TOS_Constants.INERTIA
									|| world.isBallKickable()) {
				executor.executeCommand();
				world.setActionTime(world.getSeeData().time);
			}
		} else if (info.packetType == Packet.HEAR) {
			
			world.setHearData((HearData) info.data);
			if (world.getHearData().side == 'l')
				world.setLeftMhearData(world.getHearData());
			else if (world.getHearData().side == 'r')
				world.setRightMhearData(world.getHearData());
			
		} else if (info.packetType == Packet.REFEREE) {
			
			world.setRefereeData((RefereeData) info.data);

			// get simulation time from the referee message
			sec = world.getRefereeData().time / (int)(1 / TOS_Constants.SIM_STEP_SECONDS);
			min = sec / 60;
			sec = sec % 60;

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

			// this is a duplication; see Cviewer for the latest version of this feature
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
				world.setPreviousMode(world.getRefereeData().mode);
			}
		}

	}
}
