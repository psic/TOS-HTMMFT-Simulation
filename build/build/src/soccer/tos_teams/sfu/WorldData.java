/* WorldData.java

   by Vadim Kyrylov
   2006 - 2010
*/

package soccer.tos_teams.sfu;

import soccer.common.*;
import java.util.*;
import java.io.*;

/**
 * This class provides the low-level services that can hardly be
 * further improved. However, errors in this class, if any, 
 * may spoil everything. Do not modify this code unless you are
 * absolutely sure. 
 * 
 * This class wraps around the SeeData class and provides the visual 
 * perception of the world by the player who assumes that his team 
 * is playing on the left side. If this is indeed not the case, 
 * all the appropriate transformation of coordinates is made. 
 * 
 * In addition to its super class, this class implements important
 * features calculating the time balance for reaching the ball by each
 * player. 
 */
 
public class WorldData extends SeeData {
	
	// the player who percieves this information
	private MyPlayer me;
	private boolean iAmGoalie;

	// players in my team, assuming that it is on the left side. 
	private Vector<MyPlayer> myTeam = new Vector<MyPlayer>(); 
	
	// players in the opponent team, assuming that it is on the right side. 
	private Vector<MyPlayer> theirTeam = new Vector<MyPlayer>(); 
	
	// the side on which my team is actually playing 
	private char myside;	
	private Transceiver transceiver;
	
	// the fastest player to ball in my team
	private MyPlayer fastestTeammate;
	
	// the second fastest player to ball in my team
	private MyPlayer fastestTeammate2;
	
	// the fastest player to ball in their team
	private MyPlayer fastestOpponent;
	
	// the absolute fastest player to ball 
	private MyPlayer fastestPlayer;
	
	/**
	 * Constructor
	 * 
	 * @param sd	- visual sensor data
	 * @param side
	 * @param iAmGoalie
	 * @param tr
	 */
	public WorldData( SeeData sd, char side, boolean iAmGoalie, Transceiver tr ) {
		super(sd.time, sd.player, sd.status, sd.ball, 
							sd.leftTeam, sd.rightTeam);
		myside = side;
		this.iAmGoalie = iAmGoalie;
		transceiver = tr;
		transformCoodinates(); 
		calcReachTimes();
	} 
	
	/**
	 * This method copies the coordinates of the ball and teams into 
	 * new data objects as perceived by me if I were playing on the left side.
	 * Thus all coordinates in the right-hand team are inverted.
	 */
	private void transformCoodinates() {
		// create a separate copy of the ball data
		boolean isGrabbed 	= ball.isGrabbed;
		boolean isFree 		= ball.isFree;
		
		ball =  new Ball(
						getRealPosOrVel( myside, ball.getPosition() ), 
						ball.controllerType, 
						ball.controllerId ); 
		ball.isGrabbed 	= isGrabbed;
		ball.isFree		= isFree;
		
		// create a copy of own team
		Vector<Player> team1;
		if ( myside == 'l' )
			team1 = super.leftTeam;		// this comes from SeeData
		else 
			team1 = super.rightTeam;	// this comes from SeeData
			
		//System.out.println(me.id + "-" + myside 
				//+ " leftTeam.size() = " + leftTeam.size() 
				//+ " rightTeam.size() = " + rightTeam.size() );

		myTeam = new Vector<MyPlayer>();
		for ( int i = 0; i < team1.size(); i++ ) {
			Player plr = team1.elementAt( i );
			if (super.player.getId() != plr.getId()) {
				// skip myself
				Vector2d pos = new Vector2d( getRealPosOrVel( myside, plr.getPosition() ) ); 
				double dir = getRealDir( myside, plr.getDirection() );
				MyPlayer teammate = new MyPlayer(plr.getSide(), plr.getId(), pos, dir );
				Vector2d vel = new Vector2d( getRealPosOrVel( myside, plr.getVelocity() ) );
				teammate.setVelocity(vel);
				teammate.setUserControlled(plr.isUserControlled()); 

				myTeam.addElement( teammate );
			}
		}

		// create a copy of the opponent team (cast Player -> MyPlayer) 
		Vector<Player> team2;
		if ( myside == 'r' )
			team2 = leftTeam;
		else 
			team2 = rightTeam;
		
		theirTeam = new Vector<MyPlayer>();
		for ( int i = 0; i < team2.size(); i++ ) {
			Player plr = team2.elementAt( i );
			Vector2d pos = new Vector2d( getRealPosOrVel( myside, plr.getPosition() ) ); 
			double dir = getRealDir( myside, plr.getDirection() );
			MyPlayer opponent = new MyPlayer(plr.getSide(), plr.getId(), pos, dir ); 
			Vector2d vel = new Vector2d( getRealPosOrVel( myside, plr.getVelocity() ) );
			opponent.setVelocity(vel);
			opponent.setUserControlled(plr.isUserControlled());
			
			theirTeam.addElement( opponent );	
		}
		
		
		Vector2d mypos = new Vector2d( getRealPosOrVel( myside, 
											super.player.getPosition() ) ); 
		double mydir = getRealDir( myside, super.player.getDirection() );
		me = new MyPlayer(super.player.getSide(), 
							super.player.getId(), mypos, mydir ); 
		me.setGoalie(iAmGoalie);
		me.setUserControlled(super.player.isUserControlled()); 
		myTeam.addElement( me );	// I add myself, as I was skipped above
		
		//System.out.println(me.id + "-" + myside + " myTeam.size() = " + myTeam.size() );
		//System.out.println(me.id + "-" + myside + " theirTeam.size() = " + theirTeam.size() );
	}
	
	/**
	 * This method calculates time to reach the ball by each player;
	 * this time is measured in cycles;
	 * also it sets the two fastest players in each team
	 */
	public void calcReachTimes() {
		try {
			for ( int k = 0; k < 2; k++ ) {	
				Vector<MyPlayer> team;
				if ( k== 0 )
					team = myTeam;
				else
					team = theirTeam;
				int min_time = 1000;
				int fastestIdx = -1;
				for ( int i = 0; i < team.size(); i++ ) {
					MyPlayer plr = team.elementAt( i );
					plr.setFastestToBall(false);
					plr.solveInterceptionProblem(ball);
					int cycles = plr.getCyclesToReachBall();
					if (cycles < min_time) {
						min_time = cycles;
						fastestIdx = i;
					}
					/*
					int cyclesSmart = plr.calcSmartInterceptTime(plr, ball);
					System.out.println("Player " + plr.getId() + "-" + plr.getSide() 
						+ "\t cycles = " + cycles + "\t" + cyclesSmart 
						+ "\t" + (cycles-cyclesSmart) + "\t point: " + plr.getInterceptionPoint());
					*/
				}
					if ( k== 0 ) {
						if (team.size() > 0)
							fastestTeammate = team.elementAt( fastestIdx );
					} else {
						if (team.size() > 0)
							fastestOpponent = team.elementAt( fastestIdx );
					}
			}
			// finalize calculations
			if (fastestTeammate != null && fastestOpponent == null) 
				fastestPlayer = fastestTeammate;
			else if (fastestTeammate == null && fastestOpponent != null) 
				fastestPlayer = fastestOpponent;
			else if (fastestTeammate.getCyclesToReachBall() < fastestOpponent.getCyclesToReachBall())
				fastestPlayer = fastestTeammate;
			else
				fastestPlayer = fastestOpponent;
			if (fastestPlayer != null)
				fastestPlayer.setFastestToBall(true);
			/*
			System.out.println("fastestTeammate " + fastestTeammate.getId() + "-" 
					+ fastestTeammate.getSide() + " cycles=" + fastestTeammate.getCyclesToReachBall());
			System.out.println("fastestOpponent " + fastestOpponent.getId() + "-" 
					+ fastestOpponent.getSide() + " cycles=" + fastestOpponent.getCyclesToReachBall());
			*/
		} catch (Exception e) { // protect from possibly empty team
			//System.out.println("calcReachTimes: caught " + e);
		}
		find2ndFastestTmm();
	}
	
	/**
	 * This method determines the second fastest teammate to ball
	 * given that the first one is known
	 */
	private void find2ndFastestTmm() {
		fastestTeammate2 = null;
		double time = Float.MAX_VALUE;
		if (fastestTeammate != null && myTeam.size() > 1) {
			for ( int i = 0; i < myTeam.size(); i++ ) {
				MyPlayer plr = myTeam.elementAt( i );
				if (!plr.equals(fastestTeammate)) {
					if (plr.getCyclesToReachBall() < time) {
						time = plr.getCyclesToReachBall();
						fastestTeammate2 = plr;
					}
				}
			}
		}
	}
	
	/**
	 * this method returns the ball possession mode based on
	 * ball reaching times
	 */
	public int getBallPossession() {
		int ourTime, theirTime; 
		// protect from crashes 
		if (fastestTeammate != null)
			ourTime = fastestTeammate.getCyclesToReachBall();
		else
			ourTime = 1000;
		if (fastestOpponent != null)
			theirTime = fastestOpponent.getCyclesToReachBall();
		else
			theirTime = 1000;
			
		if (Math.min(ourTime, theirTime) > 100)
			return TOS_AgentConstants.NEUTRAL_BALL;
		else if (ourTime == theirTime)
			return TOS_AgentConstants.NEUTRAL_BALL;
		else if (ourTime < theirTime)
			return TOS_AgentConstants.OUR_BALL;
		else 
			return TOS_AgentConstants.THEIR_BALL;
	}
	
	
	
	// returns the inverted position 
	public static Vector2d getRealPosOrVel( char side, Vector2d pos ) {
		if ( side == 'l' )
			return new Vector2d( pos );
		else 
			// invert  coordiantes
			return new Vector2d( -pos.getX(),
								 -pos.getY() );
	}
	
	
	// returns the inverted direction
	public static double getRealDir(  char side, double dir ) {
		if ( side == 'l' )
			return dir;
		else 
			// invert direction
			return Util.normal_dir( dir + 180.0 );
	}

		
	/**
	 * This is a wrap-up method for sending data in the correct coordinates
	 */
	public void send( Packet p ) throws IOException {
		// attach sender ID for debugging
		int debugID = me.getId();
		if (me.getSide() == 'l')
			debugID = -debugID;
		p.senderIDdebug = debugID;
		
		// transform coordinates back to the true side
		switch ( p.packetType ) {
			case Packet.KICK:
				KickData aKickData = (KickData)p.data;
				aKickData.dir = getRealDir( myside, aKickData.dir );
			break;
			
			case Packet.DRIVE:
				DriveData aDriveData = (DriveData)p.data;
				aDriveData.dir = getRealDir( myside, aDriveData.dir );
			break;

			case Packet.TELEPORT:
				TeleportData aTeleportData = (TeleportData)p.data;
				if (me.getSide() == 'r') {
					aTeleportData.newX = -aTeleportData.newX;
					aTeleportData.newY = -aTeleportData.newY;
				}
			break;
		} 
		
		transceiver.send( p );	
	}
	
	public Ball getBall() {
		return ball;	
	}

	public Vector<MyPlayer> getMyTeam() {
		return myTeam;	
	}

	public Vector<MyPlayer> getTheirTeam() {
		return theirTeam;	
	}

	public MyPlayer getMe() {
		return me;	
	}


	public MyPlayer getFastestTeammate() {
		return fastestTeammate;
	}


	public MyPlayer getFastestOpponent() {
		return fastestOpponent;
	}


	public MyPlayer getFastestPlayer() {
		return fastestPlayer;
	}

	public MyPlayer getFastestTeammate2() {
		return fastestTeammate2;
	}

	
}
