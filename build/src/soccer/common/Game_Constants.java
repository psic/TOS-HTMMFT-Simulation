package soccer.common;
/**
 * This interface defines constants that are used in soccer 
 * rules and govern the simulation process.
 * 
 * @author vkyrylov
 */
public class Game_Constants {

	// time in seconds after that the server starts the game without the viewer client
    public static double NO_GAME_SECONDS = 60; 	
    // Half time duration in minutes
   public static double HALF_TIME_MINUTES = 1.0; 	
    //public static double HALF_TIME_MINUTES = 0.25; 	// ######### used for debugging
    // pause between periods and games in seconds
    public static double PAUSE_DURATION = 5.0;
   // Idle time in minutes needed for deleting a inactive client
    public static double IDLE_MINUTES = 0.2;	//2.0;
	// the duration of before_kick_off mode  in seconds
	public static int KICK_OFF_TIME = 5; 
	// the duration of other paused modes  in seconds
	public static int TWROW_IN_TIME = 3; 
	// number of games to play in this simulation run
    public static int GAMES_TO_PLAY = 1; 	
	// if true, reset the score with the next game
	public static boolean RESET_SCORE = false;
    // time in seconds allowed to keep the ball grabbed by the goalie 
    public static double MAX_GRABBED_TIME = 4.0;

	// A flag to indicate that referee is going to signal
	// all players and viewers the offside game situation, if any
	public static boolean OFFSIDERULE_ON = true;
    // time (s) allowed to ignore offside rule after the corner kick assigned
    public static double OFFSIDE_DELAY = 3.5;

}
