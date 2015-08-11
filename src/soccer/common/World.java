package soccer.common;

import java.util.Vector;

/**
 * This class defines common functionality shared by all
 * applications in TOS bundle related to the world model
 * 
 * @author vkyrylov
 */

public abstract class World {

	// player positions (real coordinates) that could be used as 
	// the initial team formation
	public static Vector<Vector2d> leftInitPos;
	public static Vector<Vector2d> rightInitPos;
	
	// game state info  
	private int 		gameMode = 0;	  
	private int 		gamePeriod = 0;  
	private char		sideToContinue = '?'; // who should continue the game 
	
	
	// this method assigns default initial player positions
	public static void setInitPlayerPos() 	{
		
		
		
		leftInitPos = new Vector<Vector2d>();
		leftInitPos.setSize(30);
		leftInitPos.addElement( new Vector2d() );	 		// dummy element; is never used		
//		leftInitPos.addElement( new Vector2d(-45, 0) );	 	// #1		
//		leftInitPos.addElement( new Vector2d(-35, -10) );	// #2	
//		leftInitPos.addElement( new Vector2d(-35, 10) );	// #3	
//		leftInitPos.addElement( new Vector2d(-25, -20) ); 	// #4
//		leftInitPos.addElement( new Vector2d(-25, 20) );	// #5	
//		leftInitPos.addElement( new Vector2d(-10, -15) );	// #6		
//		leftInitPos.addElement( new Vector2d(-10, 15) );	// #7		
//		leftInitPos.addElement( new Vector2d(-2, -28) );	// #8	
//		leftInitPos.addElement( new Vector2d(-2, -5) );	 	// #9	
//		leftInitPos.addElement( new Vector2d(-5, 3) );	 	// #10 		
//		leftInitPos.addElement( new Vector2d(-2, 28) );		// #11
//	
		
		rightInitPos = new Vector<Vector2d>();
		rightInitPos.setSize(30);
		rightInitPos.addElement( new Vector2d() );	 		// dummy element; is never used		
//		rightInitPos.addElement( new Vector2d(45, 0) );	 	// #1	
//		rightInitPos.addElement( new Vector2d(35, 10) );	// #2	
//		rightInitPos.addElement( new Vector2d(35, -10) );	// #3	
//		rightInitPos.addElement( new Vector2d(25, 20) ); 	// #4	
//		rightInitPos.addElement( new Vector2d(25, -20) );	// #5
//		rightInitPos.addElement( new Vector2d(10, 15) );	// #6			
//		rightInitPos.addElement( new Vector2d(10, -15) );	// #7
//		rightInitPos.addElement( new Vector2d(2, 28) );		// #8	
//		rightInitPos.addElement( new Vector2d(2, 5) );	 	// #9		
//		rightInitPos.addElement( new Vector2d(5, -3) );	 	// #10 		
//		rightInitPos.addElement( new Vector2d(2, -28) );	// #11			
	}	
	
	/**
	 *  This method returns true if the position is inside given 
	 *  penalty area within some margin.
	 *  
	 *  For the player client who uses mirror coordinates for the right-side team,
	 *  this method must be only using side0=='l'
	 *  
	 * @param side0 - team side
	 * @param pos	- position to be tested
	 * @param margin - (negative - decreased size, positive - increased)
	 * 
	 * @return true if pos in this area
	 */
	// 
	public static boolean inPenaltyArea( char side0, Vector2d pos, double margin ) 	{
		
		boolean horizontalOK = false;
		boolean verticalOK = Math.abs( pos.getY() ) 
								< TOS_Constants.PENALTY_WIDTH/2.0 + margin; 
		
		if ( side0 == 'l' ) 
			if ( pos.getX() < 0 ) {
				horizontalOK =  ( TOS_Constants.LENGTH/2 + pos.getX() ) 
									< ( TOS_Constants.PENALTY_DEPTH + margin );
			} else
				return false; 
		else if ( side0 == 'r' )
			if ( pos.getX() >= 0 )			
				horizontalOK = ( TOS_Constants.LENGTH/2 - pos.getX() ) 
									< ( TOS_Constants.PENALTY_DEPTH + margin ); 
			else
				return false; 
		else
			return false;
			
		return 
			( horizontalOK && verticalOK && Math.abs( pos.getX() ) 
										<= TOS_Constants.LENGTH/2 );
	}

	/**
	 * This method returns the risk (a heuristic value) of getting 
	 * out of the soccer field of the ball kicked from 'startPos' 
	 * in 'direction' with 'force'
	 */
	public static double calcRiskOfKickingOutOfField( Vector2d startPos, 
											double direction, double force) {
		// ball rolling distance (roughly); magic number is used
		double d = 3.0*Math.sqrt(force); 
		// extrapolated coordinates
		double x = startPos.getX() + d * Math.cos( Math.toRadians(direction) );
		double y = startPos.getY() + d * Math.sin( Math.toRadians(direction) );
		
		Vector2d endPos = new Vector2d(x, y);
		
		return calcRiskOfKickingOutOfField(direction, endPos);
	}

	/**
	 * This method returns the risk (a heuristic value) of getting 
	 * out of the soccer field of the ball kicked in 'direction' to 'endPos'
	 * assuming that it would not be intercepted and would keep rolling
	 * some distance after endPos.
	 */
	public static double calcRiskOfKickingOutOfField(
								double direction, Vector2d endPos) {
		double d = 5.0;	// rolling distance after the end point
		// extrapolated coordinates
		double x = endPos.getX() + d * Math.cos( Math.toRadians(direction) );
		double y = endPos.getY() + d * Math.sin( Math.toRadians(direction) );
		
		double riskXplus = x - Field_Constants.LENGTH/2;
		double riskXminus = -x - Field_Constants.LENGTH/2;
		double riskYplus = y - Field_Constants.WIDTH/2;
		double riskYminus = -y - Field_Constants.WIDTH/2;
		
		// risk is measured in meters from the field outer border
		double risk = 0;
		// find the worst case
		risk = Math.max(risk, riskXplus);	// risk cannot be negative
		risk = Math.max(risk, riskXminus);
		risk = Math.max(risk, riskYplus);
		risk = Math.max(risk, riskYminus);
		// set the upper limit
		risk = Math.min(risk, 100);
		
		return risk;
	}

	/**
	 * this method returns time (number of cycles) that it 
	 * takes for the ball having initial speed to roll given distance.
	 * (speed is measured in distance per simulation cycle)
	 */
	public static int calcCyclesBallToRoll(double distance, double speed) {
		int bigInt = 1000;
		for(int i=0; i < bigInt; i++) {
			// decrement ball distance and decrement its speed
			distance = distance - speed;
			speed = speed * (1 - TOS_Constants.FRICTIONFACTOR);
			if(distance < TOS_Constants.BALLCONTROLRANGE/3) 
				return i;				 
		}	
		return bigInt;
	}
	
	//-------------  getter and setters  ----------------------------
	
	public int getGameMode() {
		return gameMode;
	}

	public void setGameMode(int gameMode) {
		this.gameMode = gameMode;
	}

	public int getGamePeriod() {
		return gamePeriod;
	}

	public void setGamePeriod(int gamePeriod) {
		this.gamePeriod = gamePeriod;
	}

	public char getSideToContinue() {
		return sideToContinue;
	}

	public void setSideToContinue(char sideToContinue) {
		this.sideToContinue = sideToContinue;
	}
	
	/**
	 * This method should be used for testing purposes only
	 */
	public static void main(String[] a) 	{
		System.out.println("Testing inPenaltyArea\n");
		// inPenaltyArea( char side0, Vector2d pos, double margin )
		boolean result;
		
		System.out.println("=== Test case 1: side0='l', pos=(-40, 5), margin=1.0");
		System.out.println("must return true");
		result = inPenaltyArea('l', new Vector2d(-40, 5), 1.0);
		System.out.println("result = " + result);

		System.out.println("=== Test case 2: side0='l', pos=(-40, -25), margin=1.0");
		System.out.println("must return false");
		result = inPenaltyArea('l', new Vector2d(-40, -25), 1.0);
		System.out.println("result = " + result);

		System.out.println("=== Test case 3: side0='l', pos=(-50, -0), margin=0.0");
		System.out.println("must return true");
		result = inPenaltyArea('l', new Vector2d(-50, -0), 0.0);
		System.out.println("result = " + result);

		System.out.println("=== Test case 3a: side0='l', pos=(-51, -0), margin=0.0");
		System.out.println("must return false");
		result = inPenaltyArea('l', new Vector2d(-51, -0), 0.0);
		System.out.println("result = " + result);

		System.out.println("=== Test case 4: side0='l', pos=(-25, -10), margin=0.0");
		System.out.println("must return false");
		result = inPenaltyArea('l', new Vector2d(-25, -0), 0.0);
		System.out.println("result = " + result);

		System.out.println("-------------------------------------");
		
		System.out.println("=== Test case 5: side0='r', pos=(40, 5), margin=1.0");
		System.out.println("must return true");
		result = inPenaltyArea('r', new Vector2d(40, 5), 1.0);
		System.out.println("result = " + result);

		System.out.println("=== Test case 6: side0='r', pos=(40, 25), margin=1.0");
		System.out.println("must return false");
		result = inPenaltyArea('r', new Vector2d(40, -25), 1.0);
		System.out.println("result = " + result);

		System.out.println("=== Test case 7: side0='r', pos=(50, 25), margin=0.0");
		System.out.println("must return true");
		result = inPenaltyArea('r', new Vector2d(50, 0), 0.0);
		System.out.println("result = " + result);

		System.out.println("=== Test case 7a: side0='r', pos=(51, 25), margin=0.0");
		System.out.println("must return false");
		result = inPenaltyArea('r', new Vector2d(51, 0), 0.0);
		System.out.println("result = " + result);

		System.out.println("=== Test case 8: side0='r', pos=(25, 10), margin=0.0");
		System.out.println("must return false");
		result = inPenaltyArea('r', new Vector2d(25, 10), 0.0);
		System.out.println("result = " + result);
	}
	
}
