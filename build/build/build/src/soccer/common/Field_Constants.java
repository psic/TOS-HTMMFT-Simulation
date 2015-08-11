package soccer.common;
/**
 * This class must be the only place for keeping all 
 * soccer field hard-coded constants; they all are final.
 * Using locally defined public static constants in other 
 * classes is still allowed, yet not recommended. 
 * 
 * @author vkyrylov (2010)
 */

public interface Field_Constants {

	//---  Soccer Field parameters (in meters) ---
	public static final float LENGTH = 100; 	// use even-number value for this parameter
	public static final float WIDTH = 66;		// use even-number value for this parameter
	
	//public static final float SIDEWALK = 5;
	public static final float SIDEWALK = 3;
	public static final float RADIUS = 11;
	public static final float GOAL_DEPTH = 2;
	public static final float GOAL_WIDTH = 8;
	public static final float GOAL_HEIGHT = 2.5f;
	public static final float GOALAREA_WIDTH = 18;
	public static final float GOALAREA_DEPTH = 6;
	//public static final float PENALTY_WIDTH = 40;
	//public static final float PENALTY_DEPTH = 16;
	public static final float PENALTY_WIDTH = 47;
	public static final float PENALTY_DEPTH = 19;
	public static final float PENALTY_CENTER = 14;
	public static final float CORNER = 1;

	public static final float METER = 9.0F;	// scaling factor for correct displaying

	public static final float BALLSIZE = 0.6f;
	public static final float PLAYERSIZE = 0.9f;

	// penalty area corners
	public static final Vector2d 	// left top
		PENALTY_CORNER_L_T = new Vector2d(	-LENGTH/2 + PENALTY_DEPTH,
											PENALTY_WIDTH/2 );
	public static final Vector2d 	// left botttom 
		PENALTY_CORNER_L_B = new Vector2d(	-LENGTH/2 + PENALTY_DEPTH,
											-PENALTY_WIDTH/2 );
	public static final Vector2d 	// right top 
		PENALTY_CORNER_R_T = new Vector2d(	LENGTH/2 - PENALTY_DEPTH,
											PENALTY_WIDTH/2 );
	public static final Vector2d 	// right botttom  
		PENALTY_CORNER_R_B = new Vector2d(	LENGTH/2 - PENALTY_DEPTH,
											-PENALTY_WIDTH/2 );	
	
	// viewer client window dimensions
	public static final int DISPLAYWIDTH = (int)((TOS_Constants.LENGTH 
						+ 2*TOS_Constants.SIDEWALK)*TOS_Constants.METER + 0.5);
	public static final int FIELDHEIGHT = (int)((TOS_Constants.WIDTH 
						+ 2*TOS_Constants.SIDEWALK)*TOS_Constants.METER + 0.5);
	public static final int MENUBARHEIGHT = 20;
	public static final int TOOLBARHEIGHT = 32;
	public static final int STATUSPANEHEIGHT = 32;
	public static final int DISPLAYHEIGHT = MENUBARHEIGHT + TOOLBARHEIGHT 
										+ STATUSPANEHEIGHT + FIELDHEIGHT;
	

}
