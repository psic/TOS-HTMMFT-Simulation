package soccer.common;
/**
 * This interface must be the only place for keeping all 
 * TOS project-wide constants. (Also see the super 
 * class and implemented interfaces.)
 * Using locally defined public static constants in other 
 * classes is still allowed, but not recommended. 
 * 
 * @author vkyrylov (2010)
 */

public class TOS_Constants extends Game_Constants
						implements Field_Constants, TOS_AgentConstants {

	// TOS bundle version ID
	private static final int MAJOR_VERSION = 2;
	private static final int MINOR_VERSION = 2;
	private static final int PATCH_VERSION = 3;		// this version uses player with disabled capabilities
	private static final String VERSION_LABEL = "";
	public static final String APP_VERSION =
		MAJOR_VERSION
			+ "."
			+ MINOR_VERSION
			+ "."
			+ PATCH_VERSION
			+ VERSION_LABEL;

	// Player/Viewer Number allowed
	// the maximum number of clients for each team
	public static int TEAM_FULL = 11;
	// the maximum number of viewer clients
	public static int VIEWER_FULL = 22;
	
	// write to log file if true
	public static boolean log = false;

	// Team names
	public static String LEFTNAME = "";
	public static String RIGHTNAME = "";

	// server network address info
	public static String SERVER_ADDRESS = "localhost";
	public static int SERVER_PORT = 7777;
		
	//===============  player client actions  ================
	// High Level Actions Codes
	public static final int NOACTION   = 0;		// do nothing
	public static final int SHOOT      = 1;		// shoot the goal 
	public static final int MOVE       = 2;		// move to wherever is good for the team 
	public static final int PASS       = 3;		// pass the ball to teammate or myself
	public static final int CHASE      = 4;		// chase the ball
	public static final int UNSTUCK	   = 5;		// resolve a 'got stuck' situation
	public static final int OFFSIDE    = 6;		// move to wherever rules permit
	public static final int GRAB	   = 7;		// grab the ball (for goalie) 
	public static final int MOVEWBALL  = 8;		// move with the grabbed the ball (for goalie)
	public static final int TELEPORT   = 9;		// teleport myself (also is used by the viewer)
	public static final int TURN	   = 10;	// turn to 'facingPos'
	// Low Level Actions 
	public static final int DRIVE      = 50;	// dash and turn 
	public static final int KICK       = 60;	// kick ball
	
	//=========  viewer client states ============= 
	public static final int INIT 		=  0; 
	public static final int CONNECTED 	= 20; 
	public static final int READY	 	= 30; 
	public static final int WAITING 	= 40; 
	public static final int RUNNING 	= 50; 
	public static final int END		 	= 60; 
	
	//=================  ball and player dynamics  ===================
	// Once a player makes up his mind to do an action, the action will
	// be executed for next INERTIA steps. So this means, the client
	// does not need to send a command every step, he only needs to send
	// a command every INERTIA steps.
	// This will also give a client more time to think before to actually 
	// do something.
	public static int INERTIA   =  1;
	// Simulation step in seconds (in some places referred to as 'heart rate')
	public static double SIM_STEP_SECONDS    = 0.05;	
	//public static double SIM_STEP_SECONDS    = 0.1;
	// if is false, this sets almost all random factors to zero
	public static boolean RANDOMNESS_ON 	= true;
	
	//--- player parameters ---
	// player will have chance to get the ball when 
	// the ball-player distance is under this control range
	public static double BALLCONTROLRANGE   = 1.5; 
    // player maximal angular speed when it is not moving, degrees/sec
    public static double MAX_ANGULAR_SPEED = 360.0;
    // maximal player angular increment per simulation cycle
    public static final double maxDirChangePerCycle 
				= 	TOS_Constants.MAX_ANGULAR_SPEED 
						* TOS_Constants.SIM_STEP_SECONDS;
	// player maximum speed (in m/s)
	public static double PLAYERMAXSPEED   = 7.0; 
	// time player can accelerate to (1-exp(-1)), i.e. 63% of full 
	// speed (in sec) with maximal force and without friction
	public static double TIMETOMAX  = 1;
	// player's maximum dash force 
	public static double MAXDASH  = 100;
	// player minimum dash force 
	public static double MINDASH = -30;
	// max random factor for player movement
	public static double DASHRANDOM = 0.01;
	// player's maximum kick force 
	public static double MAXKICK  = 100;
	// player minimum kick force (in the opposite direction)
	public static double MINKICK = -30;
	// kick direction random factor. When you decide to kick the ball to X direction,
	// the actual ball moving direction will be X +/- KICKRANDOM degrees. So, the closer to the
	// goal, the better chance to score.
	public static double KICKRANDOM = 3.0;
	// max dribble force factor, when player is dribbling, 
	//the max force he can use to dash is MAXDASH * DRIBBLEFACTOR 
	public static  double DRIBBLEFACTOR  = 0.4;
	// players collides when their distance is smaller than COLLIDERANGE
	public static double COLLIDERANGE = 1.0;
	
	// K1 is the player force factor, MAXSPEED speed divided by TIMETOMAX
	// MAXDASH * K1 * TIMETOMAX * (1 / SIM_STEP_SECONDS) = MAXSPEED * SIM_STEP_SECONDS
	public static double K1 = PLAYERMAXSPEED 	* SIM_STEP_SECONDS 
										* SIM_STEP_SECONDS 
										/ TIMETOMAX 
										/ MAXDASH;
	// K2 is the player friction factor,
	// 0 = MAXDASH * K1 - MAXSPEED * SIM_STEP_SECONDS * K2;
	public static double K2 = K1 * MAXDASH / (PLAYERMAXSPEED * SIM_STEP_SECONDS);
	
    // determines the bouncing force when players collide
	public static double COLLISION_ACCELERATION = 1.0 * SIM_STEP_SECONDS;		
	// Once the player has sent the message, he has to wait at least NOWORD sec to   
	// communicate again
	public static double NOWORD = 3.0;
	// Once the player has kicked the ball, he has to wait at least NOBALL sec to have  
	// the ball under his control again; this parameter is critical for dribbling 
	public static double NOBALL = 0.50;
	// Once the player has kicked the ball, he has to wait at least NODASH sec to be  
	// able to continue dashing; this parameter is critical for dribbling 
	public static double NODASH = 0.200001;
	
	//---  ball  ---
	// maximum ball speed in m/s
	public static double BALLMAXSPEED   = 30;
	// BK1 is the kick force factor for ball movement 
	public static double BK1     = BALLMAXSPEED * SIM_STEP_SECONDS / MAXKICK;
	// ball friction factor, such as a1 = -FRICTIONFACTOR * v0;
	public static double FRICTIONFACTOR     = 0.045;
	// max random factor for ball movement
	public static double BALLRANDOM     = 0.02; 

    

}
