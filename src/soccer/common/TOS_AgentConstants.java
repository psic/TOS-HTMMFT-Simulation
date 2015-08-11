package soccer.common;
/**
 * This class must be the only place for keeping all 
 * TOS player agent final constants.
 * Using locally defined public static constants in other 
 * classes is still allowed, yet not recommended.
 * Rather, it is recommended creating inherited interfaces 
 * for this interface if new constants are introduced or existing 
 * ones overridden. 
 * 
 * @author vkyrylov (2010)
 */

public interface TOS_AgentConstants  {

	//========  player parameters (recommended but not required)  =====
	// ball kicking force constants
	// (used for naming convenience only)
	public static final int K_FORCE_SMALL 		= 15;
	public static final int K_FORCE_MODERATE 	= 30;
	public static final int K_FORCE_MEDIUM	 	= 50;
	public static final int K_FORCE_MAXIMAL 	= 100;
	
	// player dash force constants
	// (used for naming convenience only)
	public static final int MV_FORCE_NOTHING 	=  0;  
	public static final int MV_FORCE_SMALL 		= 10;  
	public static final int MV_FORCE_MODERATE 	= 30;
	public static final int MV_FORCE_MEDIUM 	= 50;
	public static final int MV_FORCE_MAXIMAL 	= 100;
	
	// ball state constants 
	public static final int 	OUR_BALL = -1;
	public static final int 	NEUTRAL_BALL = 0;
	public static final int 	THEIR_BALL = 1;
	
}
