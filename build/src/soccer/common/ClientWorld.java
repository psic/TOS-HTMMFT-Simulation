package soccer.common;

import java.util.Vector;


/**
 * This class provides common features for all client 
 * applications in TOS bundle related to the world model
 * 
 * @author Vadim Kyrylov (2010)
 *
 */
public class ClientWorld extends World {
	
	// opponent goal center
	private Vector2d 	oppGoalPosition = new Vector2d(TOS_Constants.LENGTH / 2.0, 0);
	// own goal center
	private Vector2d 	ownGoalPosition = new Vector2d(-TOS_Constants.LENGTH / 2.0, 0);		
	
	// latest info from server
	private InitData 	initData = null;
	private InfoData 	infoData = null;
	private RefereeData refereeData = null;	
	private SeeData 	seeData = null;
	
	private HearData 	hearData = null;
	private HearData 	leftMhearData = null;
	private HearData 	rightMhearData = null;
	
	// my status
	private Player 		me;
	// my drive force and direction
	private double 		dashForce;
	private double 		dashDirection;
	// my offside status
	private int 		amIOffSide;
	// my position at previous step (for calculating my velocity)
	private Vector2d 	previousPosition = new Vector2d();
	private Vector2d 	myVelocity = new Vector2d();
	
	// ball status
	private Ball 		ball;
	
	// used to store ball position at previous step,
	// for calculating ball velocity.
	private Vector2d 	previousBallPosition =  new Vector2d(); 
	private Vector2d 	ballVelocity = new Vector2d();  
	
	// players
	private Vector<Player> 		leftTeam;
	private Vector<Player> 		rightTeam;
	
	// high-level knowledge
	private boolean 	isBallKickable = false;
	private boolean 	isBallControlledByMe = false;
	private double 		distance2Ball;
	private double 		direction2Ball;
	
	private int 		previousPeriod = -1;
	private int 		previousMode = RefereeData.BEFORE_KICK_OFF;
	private char 		previousController = 'f';
	private int 		leftGoalCount = 0;
	private int 		rightGoalCount = 0;
	
	// high level action code
	private int actionType;
	// my action time
	private int actionTime = 0;
		
	private Vector2d destination = new Vector2d();
	
	public ClientWorld() {}
	
	// create default world
	public void init() 	{
		
		ball = new Ball();
		leftTeam = new Vector<Player>();
		rightTeam = new Vector<Player>();	
		
		for ( int i=0; i<TOS_Constants.TEAM_FULL; i++ ) {
			Player plr1 = new Player('l', i+1, new Vector2d(), 0);
			leftTeam.addElement( plr1 );
			Player plr2 = new Player('r', i+1, new Vector2d(), 0);
			rightTeam.addElement( plr2 );
		}	
	}

	// for debug print only
	public void printBall() {
		System.out.println();
		System.out.println("<< Ball parameters: >> position: " + ball.getPosition() );	
		System.out.print("  controllerType = " + ball.controllerType );	
		System.out.print("  controllerId = " + ball.controllerId );	
		System.out.println("  isGrabbed = " + ball.isGrabbed );	
		System.out.println();
	}

	// for debug print only
	public void printTeams() {
		System.out.println();
		System.out.println("<< Left team parameters: >>");	
		for ( int i=0; i<leftTeam.size(); i++ ) {
			Player plr = (Player)leftTeam.elementAt(i);
			System.out.println(plr.getId() + " " + plr.getSide() + "  position: " + plr.getPosition() );	
			System.out.println("  direction = " + plr.getDirection() + "  isGrabbed = " + plr.isGrabbed() );
		}	
		System.out.println();
		System.out.println("<< Right team parameters: >>");	
		for ( int i=0; i<rightTeam.size(); i++ ) {
			Player plr = (Player)rightTeam.elementAt(i);
			System.out.println(plr.getId() + " " + plr.getSide() + "  position: " + plr.getPosition() );	
			System.out.println("  direction = " + plr.getDirection() + "  isGrabbed = " + plr.isGrabbed() );
		}	
		System.out.println();
	}
	
	public void setChasingBallOn(int teamSide, int playerID) {
		Vector<Player>team = new Vector<Player>();
		if (teamSide == -1)
			team = leftTeam;
		else if (teamSide == 1)
			team = rightTeam;
		for ( int i=0; i<team.size(); i++ ) {
			Player plr = team.elementAt(i);
			if (plr.getId() == playerID)
				plr.setChasingBall(true);
		}
	}
	
	public void setChasingBallOff() {
		for ( int i=0; i<leftTeam.size(); i++ ) {
			Player plr = leftTeam.elementAt(i);
			plr.setChasingBall(false);
		}
		for ( int i=0; i<rightTeam.size(); i++ ) {
			Player plr = rightTeam.elementAt(i);
			plr.setChasingBall(false);
		}
	}

	//-------------------  getter and setters --------------------
	
	/** 
	 * returns player by team side and Id or null if no such player
	 */
	public Player getPlayer(char type, int id) {
		if (type == 'l') {
			for (int i=0; i<leftTeam.size(); i++) {
				Player player = leftTeam.get(i);
				if (player.getId() == id)
					return player;
			}
			return null;
		} else if (type == 'r') {
			for (int i=0; i<rightTeam.size(); i++) {
				Player player = rightTeam.get(i);
				if (player.getId() == id)
					return player;
			}
			return null;
		} else
			return null;
	}

	public int getActionTime() {
		return actionTime;
	}

	public void setActionTime(int actionTime) {
		this.actionTime = actionTime;
	}

	public InitData getInitData() {
		return initData;
	}

	public void setInitData(InitData initData) {
		this.initData = initData;
	}

	public InfoData getInfoData() {
		return infoData;
	}

	public void setInfoData(InfoData infoData) {
		this.infoData = infoData;
	}

	public RefereeData getRefereeData() {
		return refereeData;
	}

	public void setRefereeData(RefereeData refereeData) {
		this.refereeData = refereeData;
	}

	public SeeData getSeeData() {
		return seeData;
	}

	public void setSeeData(SeeData seeData) {
		this.seeData = seeData;
	}

	public HearData getHearData() {
		return hearData;
	}

	public void setHearData(HearData hearData) {
		this.hearData = hearData;
	}

	public HearData getLeftMhearData() {
		return leftMhearData;
	}

	public void setLeftMhearData(HearData leftMhearData) {
		this.leftMhearData = leftMhearData;
	}

	public HearData getRightMhearData() {
		return rightMhearData;
	}

	public void setRightMhearData(HearData rightMhearData) {
		this.rightMhearData = rightMhearData;
	}

	public Player getMe() {
		return me;
	}
	
	public void setMe(Player me)
	{
		this.me = me;
	}

	public double getDashForce() {
		return dashForce;
	}

	public void setDashForce(double force) {
		this.dashForce = force;
	}

	public double getDashDirection() {
		return dashDirection;
	}

	public void setDashDirection(double direction) {
		this.dashDirection = direction;
	}

	public int getAmIOffSide() {
		return amIOffSide;
	}

	public void setAmIOffSide(int amIOffSide) {
		this.amIOffSide = amIOffSide;
	}

	public Vector2d getPreviousPosition() {
		return previousPosition;
	}

	public Vector2d getMyVelocity() {
		return myVelocity;
	}

	public Ball getBall() {
		return ball;
	}

	public void setBall(Ball ball) {
		this.ball = ball;
	}

	public Vector2d getPreviousBallPosition() {
		return previousBallPosition;
	}

	public Vector2d getBallVelocity() {
		return ballVelocity;
	}

	public Vector<Player> getLeftTeam() {
		return leftTeam;
	}

	public void setLeftTeam(Vector<Player> leftTeam) {
		this.leftTeam = leftTeam;
	}

	public Vector<Player> getRightTeam() {
		return rightTeam;
	}

	public void setRightTeam(Vector<Player> rightTeam) {
		this.rightTeam = rightTeam;
	}

	public boolean isBallKickable() {
		return isBallKickable;
	}

	public void setBallKickable(boolean isBallKickable) {
		this.isBallKickable = isBallKickable;
	}

	public boolean isBallControlledByMe() {
		return isBallControlledByMe;
	}

	public void setBallControlledByMe(boolean isBallControlledByMe) {
		this.isBallControlledByMe = isBallControlledByMe;
	}

	public double getDistance2Ball() {
		return distance2Ball;
	}

	public void setDistance2Ball(double distance2Ball) {
		this.distance2Ball = distance2Ball;
	}

	public double getDirection2Ball() {
		return direction2Ball;
	}

	public void setDirection2Ball(double direction2Ball) {
		this.direction2Ball = direction2Ball;
	}

	public int getPreviousPeriod() {
		return previousPeriod;
	}

	public void setPreviousPeriod(int previousPeriod) {
		this.previousPeriod = previousPeriod;
	}

	public int getPreviousMode() {
		return previousMode;
	}

	public void setPreviousMode(int previousMode) {
		this.previousMode = previousMode;
	}

	public char getPreviousController() {
		return previousController;
	}

	public void setPreviousController(char previousController) {
		this.previousController = previousController;
	}

	public int getLeftGoalCount() {
		return leftGoalCount;
	}

	public void setLeftGoalCount(int leftGoalCount) {
		this.leftGoalCount = leftGoalCount;
	}

	public int getRightGoalCount() {
		return rightGoalCount;
	}

	public void setRightGoalCount(int rightGoalCount) {
		this.rightGoalCount = rightGoalCount;
	}

	public int getActionType() {
		return actionType;
	}

	public void setActionType(int actionType) {
		this.actionType = actionType;
	}

	public Vector2d getDestination() {
		return destination;
	}

	public void setDestination(Vector2d d) {
		this.destination = d;
	}
	
	public Vector2d getOppGoalPosition() {
		return oppGoalPosition; 
	}  
		
	public Vector2d getOwnGoalPosition() {
		return ownGoalPosition; 
	}  

}
