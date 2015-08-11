package soccer.tos_teams.sfu;

import soccer.common.Ball;
import soccer.common.Player;
import soccer.common.TOS_Constants;
import soccer.common.Vector2d;

/**
 * This class supplements basic Player class with some functionality
 * needed to implement player client
 * @author vkyrylov
 *
 */

public class MyPlayer extends Player {
	
	// time in cycles to reach the ball
	private int cyclesToReachBall;
	// true if this player id the fastest to the ball
	private boolean isFastestToBall = false;
	// ball interception point for this player
	private Vector2d interceptionPoint;
	// calculated ball interception point for some player
	@SuppressWarnings("unused")	// TODO
	private static Vector2d interceptPointCalc;
	
	 /**
	   * Construct a player.
	   *
	   * @param side the player's side.
	   * @param id   the player's number.
	   * @param position the location of the player.
	   * @param dir the  facing direction of the player.
	   */
	public MyPlayer(char side, int id, Vector2d position, double dir)
	{
		super(side, id, position, dir);
	}

	public int getCyclesToReachBall() {
		return cyclesToReachBall;
	}

	public void setCyclesToReachBall(int cyclesToReachBall) {
		this.cyclesToReachBall = cyclesToReachBall;
	}

	public boolean isFastestToBall() {
		return isFastestToBall;
	}

	public void setFastestToBall(boolean isFastestToBall) {
		this.isFastestToBall = isFastestToBall;
	}
	

	
	/**
	 * This method returns the soonest time (number of cycles) that it 
	 * takes for the given player to intercept the ball.
	 * It also assigns value to the static variable interceptPointCalc.
	 * 
	 * It is assumed that the player is initially facing the 
	 * interception point. 
	 * 
	 * Algorithm based on the publication:
	 * Stone, P.; McAllester, D.: An Architecture for Action Selection in 
	 * Robotic Soccer. In: Proceedings AGENTS01, 5th International 
	 * Conference on Autonomous Agents, May 28-June 1, 2001, 
	 * Montreal, Quebec, Canada. (2001) 316-323 (in Appendix)   
	 */
	public int calcMinimalInterceptTime( MyPlayer receiver, Ball ball) {
		double eps = 0.0001;
		// time (number of cycles) for ball speed decreasing exp(1) times
		double tau = -1/Math.log(1 - TOS_Constants.FRICTIONFACTOR);	
		// time **in cycles** since the ball started rolling
		double t = 0;	
		
		// initial ball position
		Vector2d b0 = new Vector2d( ball.getPosition() );
		Vector2d v0 = new Vector2d( ball.getVelocity() );
		Vector2d r0 = new Vector2d( receiver.getPosition() );
		Vector2d vr = new Vector2d( receiver.getVelocity() );
		
		// initial vector from the receiver to ball (pr0+r0=b0; pr0=b0-r0)
		Vector2d br0 = Vector2d.subtract(b0, r0);   
		// initial distance between ball and receiver 
		// if he were to run directly toward the ball
		double g = br0.norm();
		
		// initial estimated interception point 
		Vector2d p = b0;
		
		// we use a modified Newton's method to find the interception time
		int count = 0;
		while (Math.abs(g) > TOS_Constants.BALLCONTROLRANGE/2) {
			// estimated interception point (ball position after t cycles)
			p = Vector2d.add( b0, v0.timesV( tau * (1 - Math.exp(-t/tau)) ) );
			
			// TODO player velocity vr should be recalculated here 
			// depending on estimated interception point p
						
			// the vector from r0 to p ( p=r0+pr0; pr0=p-r0 )
			Vector2d pr0 = Vector2d.subtract(p, r0);
			double pr0norm = pr0.norm();
			// protect from division by zero
			pr0norm = (Math.abs(pr0norm) > eps) ? pr0norm : eps;
			// u is the unit vector for pr0
			Vector2d u = new Vector2d(pr0.getX()/pr0norm, pr0.getY()/pr0norm);
			
			double v0uDot = Vector2d.dot(v0, u);
			double vrnorm = vr.norm();
			
			// the derivative of g
			double g1 = Math.exp(-t/tau) * v0uDot - vrnorm;
			// the alternative estimated time
			double s = t - g/g1;
			
			//--- next estimate of t --- (critical)
			if (v0uDot < 0)
				t = s;
			else if (v0uDot > 0 && g1 < 0) {
				//--- recalculate the unit vector for time s
				// estimated interception point (ball position after t cycles)
				Vector2d ps = Vector2d.add( b0, v0.timesV( tau * (1 - Math.exp(-s/tau)) ) );
				// the vector from r0 to ps ( ps=r0+pr0; pr0=ps-r0 )
				Vector2d psr0 = Vector2d.subtract(ps, r0);
				double psr0norm = psr0.norm();
				// protect from division by zero
				psr0norm = (Math.abs(psr0norm) > eps) ? psr0norm : eps;
				// u is the unit vector for pr0
				Vector2d us = new Vector2d(psr0.getX()/psr0norm, psr0.getY()/psr0norm);
				double v0usDot = Vector2d.dot(v0, us);
				// the derivative of g
				g1 = Math.exp(-s/tau) * v0usDot - vrnorm;
				// the estimated time
				t = t - g/g1;
				pr0norm = psr0norm;		// ?????
			} else 
				t = t + g/vrnorm;
		
			// next mismatch g
			g = pr0norm - vr.norm() * t;
			
			// protect from looping infinitely
			count++;
			if (count > 20)
				break;
	//<-------
		}
		
		interceptPointCalc = p;
		return (int)(t + 0.5);
	}
	
/**
	 * this method returns time (number of cycles) that it 
	 * takes for the given player to reach given point.
	 */
	public static int calcTimeToReachPoint( MyPlayer player, Vector2d endPos) {
		int bigInt = 1000;
		double force;
		// these variables are separated from object player 		
		Vector2d myPos = new Vector2d( player.getPosition() );
		Vector2d myVel = new Vector2d( player.getVelocity() );
		Vector2d myAcc = new Vector2d();
			
		for(int i=0; i < bigInt; i++)
		{
			// increment player position and velocity
			myPos.add(myVel);
			myVel.add(myAcc);

			// determine player dash force and calculate acceleration
			double distance2endPos = myPos.distance ( endPos );
			if (distance2endPos >= 3) 
				force = TOS_Constants.MAXDASH;
			else 
				force = TOS_Constants.MAXDASH/2;

			double dir2endPos = player.getPosition().direction(endPos);
			
			myAcc.setX(force * Math.cos(Math.toRadians(dir2endPos)) * TOS_Constants.K1
								   - myVel.getX() * TOS_Constants.K2);
			myAcc.setY(force * Math.sin(Math.toRadians(dir2endPos)) * TOS_Constants.K1
								   - myVel.getY() * TOS_Constants.K2);
		
			if(distance2endPos < TOS_Constants.BALLCONTROLRANGE/2) 
				return i;				 
		}
		
		return bigInt;
	}

	/**
	 * this method returns time (number of cycles) that it 
	 * takes for the given player to intercept the ball.
	 * this is a simplified method when player is literately 
	 * chasing the ball (not smart) 
	 */
	public static int calcInterceptTime( MyPlayer player, Ball ball) {
		int bigInt = 1000;
		double force;
		// these variables are separated from objects player and ball		
		Vector2d ballPos = new Vector2d( ball.getPosition() );
		Vector2d ballVel = new Vector2d( ball.getVelocity() );
		Vector2d myPos = new Vector2d( player.getPosition() );
		Vector2d myVel = new Vector2d( player.getVelocity() );
		Vector2d myAcc = new Vector2d();
		
		double dir2Ball;
		
		for(int i=0; i < bigInt; i++)
		{
			// increment ball position and decrement its velocity
			ballPos.add( ballVel );
			ballVel.times(1 - TOS_Constants.FRICTIONFACTOR);
			
			// increment player position and velocity
			myPos.add(myVel);
			myVel.add(myAcc);

			// determine player dash force and calculate acceleration
			double distance2Ball = myPos.distance ( ballPos );
			if (distance2Ball >= 3) 
				force = TOS_Constants.MAXDASH;
			else 
				force = TOS_Constants.MAXDASH/2;

			dir2Ball = myPos.direction( ballPos );

			myAcc.setX(force * Math.cos(Math.toRadians(dir2Ball)) * TOS_Constants.K1
								   - myVel.getX() * TOS_Constants.K2);
			myAcc.setY(force * Math.sin(Math.toRadians(dir2Ball)) * TOS_Constants.K1
								   - myVel.getY() * TOS_Constants.K2);
		
			if(myPos.distance(ballPos) < TOS_Constants.BALLCONTROLRANGE) 
				return i;				 
		}
		
		return bigInt;
	}
	
	
	/**
	 * this method determines time (number of cycles) that it 
	 * takes for the this player to intercept the ball. also it
	 * calculates the interception point. 
	 * this algorithm assumes that the player is moving straight to 
	 * current ball position chasing as it moves.
	 * this is not the smartest way, but the calculation is rather fast 
	 */
	public void solveInterceptionProblem(Ball ball) {
		int bigInt = 1000;
		int cycles = bigInt;
		
		// these variables are separated from objects player and ball		
		Vector2d ballPos = new Vector2d( ball.getPosition() );
		Vector2d ballVel = new Vector2d( ball.getVelocity() );
		Vector2d myPos = new Vector2d( this.getPosition() );
		Vector2d myVel = new Vector2d( this.getVelocity() );
		Vector2d myAcc = new Vector2d();
		
		double dir2Ball;
		
		for(int i=0; i < bigInt; i++) {
			// increment ball position and decrement its velocity
			ballPos.add( ballVel );
			ballVel.times(1 - TOS_Constants.FRICTIONFACTOR);
			
			// increment player position and velocity
			myPos.add(myVel);
			myVel.add(myAcc);

			// determine player dash force and calculate acceleration
			double distance2Ball = myPos.distance ( ballPos );
			double force;
			if (distance2Ball >= 3) 
				force = TOS_Constants.MAXDASH;
			else 
				force = TOS_Constants.MAXDASH/2;

			dir2Ball = myPos.direction( ballPos );
			double cos = Math.cos(Math.toRadians(dir2Ball));
			double sin = Math.sin(Math.toRadians(dir2Ball));

			myAcc.setX(force * cos * TOS_Constants.K1
								   - myVel.getX() * TOS_Constants.K2);
			myAcc.setY(force * sin * TOS_Constants.K1
								   - myVel.getY() * TOS_Constants.K2);
		
			if(myPos.distance(ballPos) < TOS_Constants.BALLCONTROLRANGE) {
				cycles = i;
				break;
		//<-----
			}
		}
		this.interceptionPoint = ballPos;
		this.cyclesToReachBall = cycles;
	}


	
	/**
	 * returns number of cycles it takes for the player to
	 * reach given end point
	 * 
	 * @param distToRun
	 * @param plrSpeed
	 * @return
	 */
	public int getTimePlayerToRun( MyPlayer player, Vector2d endPoint ) {
		int bigInt = 500;
		// these variables are separated from object player		
		Vector2d myPos = new Vector2d( player.getPosition() );
		Vector2d myVel = new Vector2d( player.getVelocity() );
		Vector2d myAcc = new Vector2d();
		
		for(int i=0; i < bigInt; i++) {		
			// increment player position and velocity
			myPos.add(myVel);
			myVel.add(myAcc);

			double dir2Ball = myPos.direction( endPoint );
			double cos = Math.cos(Math.toRadians(dir2Ball));
			double sin = Math.sin(Math.toRadians(dir2Ball));
			
			// determine player dash force and calculate acceleration
			double distance2Ball = myPos.distance ( endPoint );
			double force;
			if (distance2Ball >= 3) 
				force = TOS_Constants.MAXDASH;
			else 
				force = TOS_Constants.MAXDASH/2;

			myAcc.setX(force * cos * TOS_Constants.K1 
							- myVel.getX() * TOS_Constants.K2);
			myAcc.setY(force * sin * TOS_Constants.K1 
							- myVel.getY() * TOS_Constants.K2);
		
			if(myPos.distance(endPoint) < TOS_Constants.BALLCONTROLRANGE) 
				return i;				 
		}
		return bigInt;		
	}

	public Vector2d getInterceptionPoint() {
		return interceptionPoint;
	}	
	
	
}
