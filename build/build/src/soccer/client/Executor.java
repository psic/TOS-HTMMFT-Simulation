/**
 *  Executor.java 
 *  
 *  This class sends to server the action commands 
 *  placed by user in the world model. 
 *  
 *  Copyright (C) 2001  Yu Zhang
 */



package soccer.client;

import java.io.IOException;
import java.net.InetAddress;

import soccer.common.DriveData;
import soccer.common.KickData;
import soccer.common.Packet;
import soccer.common.TOS_Constants;
import soccer.common.Transceiver;
import soccer.common.Vector2d;


public class Executor {

	private ViewerWorld world;
	private Transceiver transceiver;
	private InetAddress address;
	private int port;
	private DriveData driver = null;
	private KickData kicker = null;
	private Packet command = null;

	public Executor(ViewerWorld world, ViewerClientMain soccerMaster) {
		this.world = world;
		this.transceiver = soccerMaster.getTransceiver();
		this.address = soccerMaster.getAddress();
		this.port = soccerMaster.getPort();
	}

	// predict ball stop position
	private Vector2d calcBallStopPos() {
		Vector2d ballPos = new Vector2d(world.getBall().getPosition());
		Vector2d ballVel = new Vector2d(world.getBallVelocity());
		double ballSpeed = ballVel.norm();
		while (ballSpeed > 0.1) {
			ballPos.add(ballVel);
			ballVel.times(1 - TOS_Constants.FRICTIONFACTOR);
			ballSpeed = ballVel.norm();
		}
		return ballPos;
	}

	// predict the best ball interception position
	private Vector2d interceptPos() {
		double force;
		if (world.getDistance2Ball() >= 3)
			force = 100;
		else
			force = 50;

		Vector2d ballPos = new Vector2d(world.getBall().getPosition());
		Vector2d ballVel = new Vector2d(world.getBallVelocity());

		Vector2d myPos = new Vector2d(world.getMe().getPosition());
		Vector2d myVel = new Vector2d(world.getMyVelocity());
		Vector2d myAcc = new Vector2d();
		myAcc.setX(
			force * Math.cos(Math.toRadians(world.getMe().getDirection())) * TOS_Constants.K1
				- myVel.getX() * TOS_Constants.K2);

		myAcc.setY(
			force * Math.sin(Math.toRadians(world.getMe().getDirection())) * TOS_Constants.K1
				- myVel.getY() * TOS_Constants.K2);
		double dir2Ball;

		for (int i = 0; i < 100; i++) {
			ballPos.add(ballVel);
			ballVel.times(1 - TOS_Constants.FRICTIONFACTOR);

			dir2Ball = myPos.direction(ballPos);

			myPos.add(myVel);
			myVel.add(myAcc);
			myAcc.setX(
				force * Math.cos(Math.toRadians(dir2Ball)) * TOS_Constants.K1
					- myVel.getX() * TOS_Constants.K2);

			myAcc.setY(
				force * Math.sin(Math.toRadians(dir2Ball)) * TOS_Constants.K1
					- myVel.getY() * TOS_Constants.K2);

			if (myPos.distance(ballPos) < TOS_Constants.BALLCONTROLRANGE)
				return ballPos;
		}
		return calcBallStopPos();
	}
	

	private void chaseBall() throws IOException {
		double force;
		if (world.getDistance2Ball() >= 3)
			force = 100;
		else
			force = 50;
		double direction2Ball = world.getMe().getPosition().direction(interceptPos());
		world.setDashDirection(direction2Ball);
		world.setDashForce(force);
		driver = new DriveData(direction2Ball, force);
		command = new Packet(Packet.DRIVE, driver, address, port);
		transceiver.send(command);
	}

	private void moveTo() throws IOException {
		double force;
		double distance = world.getMe().getPosition().distance(world.getDestination());
		double direction = world.getMe().getPosition().direction(world.getDestination());
		if (distance >= 5)
			force = 100;
		else
			force = 10;
		world.setDashDirection(direction);
		world.setDashForce(force);
		driver = new DriveData(direction, force);
		command = new Packet(Packet.DRIVE, driver, address, port);
		transceiver.send(command);
        System.out.println("Drive: force->" + driver.force + " dir->" + driver.dir);
	}

	private void drive() throws IOException {

		driver = new DriveData(world.getDashDirection(), world.getDashForce());
		command = new Packet(Packet.DRIVE, driver, address, port);
		transceiver.send(command);
	}

	private void shootGoal() throws IOException {
		Vector2d goal = null;

		goal = world.getOppGoalPosition();
		if (world.getMe().getSide() != 'l')
			goal.setX(goal.getX() * -1);

		double dir = world.getMe().getPosition().direction(goal);

		kicker = new KickData(dir, 100);
		command = new Packet(Packet.KICK, kicker, address, port);
		transceiver.send(command);
	}

	private void passTo() throws IOException {
		double force;
		double distance = world.getMe().getPosition().distance(world.getDestination());
		double direction = world.getMe().getPosition().direction(world.getDestination());
		if (distance >= 40)
			force = 100;
		else
			force = 70;

		kicker = new KickData(direction, force);
		command = new Packet(Packet.KICK, kicker, address, port);
		transceiver.send(command);
	}

	private void kick() throws IOException {

		kicker = new KickData(world.getMe().getDirection(), 100);
		command = new Packet(Packet.KICK, kicker, address, port);
		transceiver.send(command);
	}
	

	public void executeCommand() throws IOException {
		
		switch (world.getActionType()) {
			case TOS_Constants.SHOOT :
				shootGoal();
			break;
		
			case TOS_Constants.MOVE :
				moveTo();
			break;
	
			case TOS_Constants.PASS :
				passTo();
			break;
	
			case TOS_Constants.CHASE :
				chaseBall();
			break;
	
			case TOS_Constants.DRIVE :
				drive();
			break;
	
			case TOS_Constants.KICK :
				kick();
			break;
		}
	}

}
