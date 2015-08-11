package soccer.server;

import java.net.InetAddress;
import java.util.*;
import soccer.common.*;
/** 
 * This class maintains the true state of the simulated
 * soccer game and its environment. Similar 'world' classes 
 * in other applications are just partial, perceived views 
 * of this true state.
 * 
 * @author Vadim Kyrylov (since 2010)
*/
public class SoccerServerWorld extends World {
	
	// write to log file if true
	public static boolean log = false;
	
    /**
     * Available Id numbers for the incoming players and viewers
     */
	public Stack<Integer> viewerAvailable;
	public Stack<Integer> leftAvailable;
	public Stack<Integer> rightAvailable;
	/** 
	 * These vectors are used to keep new player/viewer clients which 
	 * should be added to the simulation
	 */
	private Vector<Splayer> addedPlayers = new Vector<Splayer>();
	private Vector<Sviewer> addedViewers = new Vector<Sviewer>();
	/** 
	 * These vectors are used to keep player/viewer clients which 
	 * should be removed from the simulation
	 */
	private Vector<Splayer> removedPlayers = new Vector<Splayer>();
	private Vector<Sviewer> removedViewers = new Vector<Sviewer>();

	// registered 'viewer' clients
	private Vector<Sviewer> viewers;
	// registered 'player' clients (two teams)
	public Vector<Splayer> leftTeam;
	public Vector<Splayer> rightTeam;
	
	// this vector has been created only in order to  
    // ensure complete symmetry of the execution turns
	public Vector<Splayer> bothTeams;
	
	// the ball  
	private  Sball 	ball;
	
	// game state variables
	public boolean throwInModeL = false; 	// keeps track of throw-ins
	public boolean throwInModeR = false; 	// keeps track of throw-ins
	public int 		stepCornerKickDecided; 
    public int 		stepBallWasGrabbed; 
    public char 	sideGrabbedBall;
	
	// Ball and team replication data  
	public Sball 	ballSaved;
	public Vector<Splayer> leftTeamSaved;
	public Vector<Splayer> rightTeamSaved;
	public boolean  replicationIsOn = false; // replication mode flag
    public int 		stepBallWasGrabbedSaved; 
    public int 		stepCornerKickDecidedSaved; 
    public char 	sideGrabbedBallSaved;
    
    private boolean isSoundOn = true;	// sound on/off toggle for the clients
	private boolean isCollision = false;
	private boolean isBallKicked = false;

	//---------------------------------------------------------------------------
	/**
	 * Constructor
	 */
	public SoccerServerWorld() 	{
		// fill the stacks with available ID numbers 
		leftAvailable = new Stack<Integer>();
		rightAvailable = new Stack<Integer>();
		viewerAvailable = new Stack<Integer>();

		for (int i = TOS_Constants.TEAM_FULL; i >= 1; i--) {
			Integer num = new Integer(i);
			leftAvailable.push(num);
			num = new Integer(i);
			rightAvailable.push(num);
		}

		for (int i = TOS_Constants.VIEWER_FULL; i >= 1; i--) {
			Integer num = new Integer(i);
			viewerAvailable.push(num);
		}

		// initialize team player and viewers vector
		leftTeam = new Vector<Splayer>();
		rightTeam = new Vector<Splayer>();
		leftTeamSaved = new Vector<Splayer>();
		rightTeamSaved = new Vector<Splayer>();
		viewers = new Vector<Sviewer>();

		// initialize the ball
		ball = new Sball();
		ballSaved = new Sball();
		stepBallWasGrabbed = Integer.MAX_VALUE;
		stepCornerKickDecided = -Integer.MAX_VALUE;
		
		// assign default positions to players on both teams.
		// (this can me overriden by the player clients, once 
		// they are connected to server)
		setInitPlayerPos();
	}


	//---------------------------------------------------------------------------    
 
	/**
	 * This method makes a working copy of both teams with the purpose
	 * to ensure the symmetry on the average.  
	 * In even-numbered cycles, the first player is copied from the left team; 
	 * in the odd-numbered cycles, the first player is copied from the right team.
	 * (players are processed in the order as they had been copied.) 
	 */
	public void copyTeams( int time ) 	{
		int turn = time%2;
		try {
			bothTeams = new Vector<Splayer>(); 
//			int size = Math.max( leftTeam.size(), rightTeam.size() );
//			for (int i = 0; i < size; i++ ) {
//				if ( turn == 0 ) {
//					if ( i < leftTeam.size() ) {
//						Splayer playerL = (Splayer)leftTeam.elementAt( i );
//						bothTeams.addElement( playerL );
//					}	
//				}
//				if ( i < rightTeam.size() ) {
//					Splayer playerR = (Splayer)rightTeam.elementAt( i );
//					bothTeams.addElement( playerR );
//				}
//				if ( turn != 0 ) {
//					if ( i < leftTeam.size() ) {
//						Splayer playerL = (Splayer)leftTeam.elementAt( i );
//						bothTeams.addElement( playerL );
//					}
//				}
			int size = Math.max( leftTeam.size(), rightTeam.size() );
			for (int i = 0; i < size; i++ ) {
				if ( turn == 0 ) {
					if ( i < leftTeam.size() ) {
						Splayer playerL = (Splayer)leftTeam.elementAt( i );
						bothTeams.addElement( playerL );
					}	
				}
				if ( i < rightTeam.size() ) {
					Splayer playerR = (Splayer)rightTeam.elementAt( i );
					bothTeams.addElement( playerR );
				}
				if ( turn != 0 ) {
					if ( i < leftTeam.size() ) {
						Splayer playerL = (Splayer)leftTeam.elementAt( i );
						bothTeams.addElement( playerL );
					}
				}
			
				//System.out.println("copied velocity: " 
					//+ bothTeams.elementAt(0).getVelocity());
			}
		} catch (Exception e) {
			System.out.println("Exception caught in copyTeams() \n" + e );	
		}
	}
	
	//---------------------------------------------------------------------------    	
	// this method saves current situation to ballSaved,
	// leftTeamSaved, and rightTeamSaved
	public void initSavedSituation() {
		// save ball data
		ballSaved.copy( ball );
		
		// init left team cordinate data
		for ( int i = 0; i < leftTeam.size(); i++ ) {
			Splayer splr = (Splayer)leftTeam.elementAt(i);
			Splayer splrSaved = new Splayer();
			splrSaved.copy( splr );
			leftTeamSaved.addElement(splrSaved);
		}

		// init right team cordinate data
		for ( int i = 0; i < rightTeam.size(); i++ ) {
			Splayer splr = (Splayer)rightTeam.elementAt(i);
			Splayer splrSaved = new Splayer();
			splrSaved.copy( splr );
			rightTeamSaved.addElement(splrSaved);
		}

		saveStepIDs();

		System.out.println("Situation saved. leftTeam.size()=" 
				+ leftTeam.size() + " rightTeam.size()=" + rightTeam.size() );
	}

	//---------------------------------------------------------------------------    	
	private void saveStepIDs() {
    	stepBallWasGrabbedSaved = stepBallWasGrabbed; 
    	stepCornerKickDecidedSaved = stepCornerKickDecided; 
    	sideGrabbedBallSaved = sideGrabbedBall;
	}

	//---------------------------------------------------------------------------    	
	// this method restores the situation from the saved snapshot
	public void restoreSituation() {
		// restore ball data
		ball.copy( ballSaved );
		
		// restore left team cordinate data 
		restoreTeam( leftTeamSaved, leftTeam );

		// restore right team cordinate data
		restoreTeam( rightTeamSaved, rightTeam );
		
    	stepBallWasGrabbed = stepBallWasGrabbedSaved; 
    	stepCornerKickDecided = stepCornerKickDecidedSaved; 
    	sideGrabbedBall = sideGrabbedBallSaved;

		//System.out.println("Situation restored. leftTeam.size()=" 
				//+ leftTeam.size() + " rightTeam.size()=" + rightTeam.size() );
	}


	//---------------------------------------------------------------------------    	
	/** 
	 * this method restores the team coordinate data from a saved copy
	 */
	private void restoreTeam( Vector<Splayer> savedTeam, 
									Vector<Splayer> team ) {
		for ( int i = 0; i < savedTeam.size(); i++ ) {
			try {
				Splayer splr = (Splayer)team.elementAt(i);
				Splayer splrSaved = (Splayer)savedTeam.elementAt(i);
				splr.copy( splrSaved );
				
				//System.out.println("team.elementAt(" + i + ") restored."
								//+ splr.position ); 
			} catch (Exception e ) {}
		}
	}


	//---------------------------------------------------------------------------    	
	/** 
	 * returns the available player Id or zero if unavailable    
	 */
	public int getNewPlayerId(char side) {
		int id;
		try {
			if (side=='l')
				id = ((Integer) leftAvailable.pop()).intValue();
			else
				id = ((Integer) rightAvailable.pop()).intValue();
		} catch (EmptyStackException e) {
			id = 0;
		}
		return id;
	}

	//---------------------------------------------------------------------------    
	/** 
	 * places the left player Id back to stack  
	 */
	public void putBackLeftPlayerId(int id) {
		Integer num = new Integer(id);
		leftAvailable.push(num);
	}

	//---------------------------------------------------------------------------
	/** 
	 * places the right player Id back to stack   
	 */
	public void putBackRightPlayerId(int id) {
		Integer num = new Integer(id);
		rightAvailable.push(num);
	}

	//---------------------------------------------------------------------------
	/** 
	 * returns a viewer Id for the new connected viewer or zero if unavailable      
	 */
	public int getNewViewerId() {
		int Id;
		try {
			Id = ((Integer) viewerAvailable.pop()).intValue();
		} catch (EmptyStackException e) {
			Id = 0;
		}

		return Id;
	}

	//---------------------------------------------------------------------------    
	/** 
	 * places the viewer Id back to stack  
	 */
	public void putBackViewerId(int id) 
	{
		Integer num = new Integer(id);
		viewerAvailable.push(num);
	}

	//---------------------------------------------------------------------------
	/** 
	 * returns player by team side and Id or null if no such player
	 */
	public synchronized Splayer getPlayer(char type, int id) {
		Enumeration<Splayer> players = null;
		Splayer player = null;

		if (type == 'l') {
			players = leftTeam.elements();
			while (players.hasMoreElements()) {
				player = (Splayer) players.nextElement();
				if (player.getId() == id)
					return player;
			}
			return null;
		} else if (type == 'r') {
			players = rightTeam.elements();
			while (players.hasMoreElements()) {
				player = (Splayer) players.nextElement();
				if (player.getId() == id)
					return player;
			}
			return null;
		} else
			return null;
	}

	//---------------------------------------------------------------------------    	
	/** 
	 * returns the goalie by team side  or null if no such player
	 */
	public synchronized Splayer getGoalie(char type) {
		
		Enumeration<Splayer> players = null;
		Splayer player = null;

		if (type == 'l') {
			players = leftTeam.elements();
			while (players.hasMoreElements()) {
				player = (Splayer) players.nextElement();
				if (player.isGoalie()) {
					return player;
				}
			}
			return null;
		} else if (type == 'r') {
			players = rightTeam.elements();
			while (players.hasMoreElements()) {
				player = (Splayer) players.nextElement();
				if (player.isGoalie()) {
					//System.out.println("Right goalie found: id=" + player.id );
					return player;
				}
			}
			return null;
		} else
			return null;

	}

	//---------------------------------------------------------------------------
	/** 
	 * returns player copy by team side and Id or null if no such player
	 */
	public synchronized Splayer getPlayerSaved(char type, int id) {
		
		//System.out.println( "leftTeamSaved.size()=" + leftTeamSaved.size() 
							//+ "rightTeamSaved.size()=" + rightTeamSaved.size() );
			
		Enumeration<Splayer> players = null;
		Splayer player = null;

		if (type == 'l') {
			players = leftTeamSaved.elements();
			while (players.hasMoreElements()) {
				player = (Splayer) players.nextElement();
				if (player.getId() == id)
					return player;
			}
			return null;
		} else if (type == 'r') {
			players = rightTeamSaved.elements();
			while (players.hasMoreElements()) {
				player = (Splayer) players.nextElement();
				if (player.getId() == id)
					return player;
			}
			return null;
		} else
			return null;

	}

	//---------------------------------------------------------------------------
	/** 
	 * returns player by its network address or null if no such player
	 */
	public synchronized Splayer getPlayer(InetAddress address, int port) {
		
		Enumeration<Splayer> players = null;
		Splayer player = null;

		players = leftTeam.elements();
		while (players.hasMoreElements()) {
			player = (Splayer) players.nextElement();
			if (player.getPort() == port && player.getAddress().equals(address))
				return player;
		}

		players = rightTeam.elements();
		while (players.hasMoreElements()) {
			player = (Splayer) players.nextElement();
			if (player.getPort() == port && player.getAddress().equals(address))
				return player;
		}

		return null;

	}

	//---------------------------------------------------------------------------    
	public synchronized Sviewer getViewer(InetAddress address, int port) {
		Enumeration<Sviewer> observers = viewers.elements();
		Sviewer viewer = null;
		while (observers.hasMoreElements()) {
			viewer = (Sviewer) observers.nextElement();
			if (viewer.port == port && viewer.address.equals(address))
				return viewer;
		}
		return null;
	}


	//---------------------------------------------------------------------------
	
	public void setPlayerViewerActive(int ticker) {
		
		// variable used to loop
		Enumeration<Splayer> players = null;
		Enumeration<Sviewer> viewers = null;
		Sviewer viewer = null;
		Splayer player = null;

		// for each player in left team, set up their last active time
		players = leftTeam.elements();
		while (players.hasMoreElements()) {
			player = (Splayer) players.nextElement();
			player.setLastTime(ticker);

		}

		// for each player in right team, set up their last active time
		players = rightTeam.elements();
		while (players.hasMoreElements()) {
			player = (Splayer) players.nextElement();
			player.setLastTime(ticker);
		}

		// for each viewer, set up their last active time
		viewers = getViewers();
		while (viewers.hasMoreElements()) {
			viewer = (Sviewer) viewers.nextElement();
			viewer.setLastTime(ticker);
		}
	}
	
	//---------------------------------------------------------------------------
	/**
	 * @return enumeration with left team players
	 */
	public Enumeration<Splayer> getLeftPlayers() {
		return leftTeam.elements();
	}

	//---------------------------------------------------------------------------
	/**
	 * @return enumeration with right team players
	 */
	public Enumeration<Splayer> getRightPlayers() {
		return rightTeam.elements();
	}

	public Enumeration<Sviewer> getViewers() {
		return viewers.elements();
	}

	//---------------------------------------------------------------------------
	/**
	 * Add new player to simulation.
	 * Because we have two active thread we can't add player now.
	 * We have to wait until the second thread will do it.
	 */
	public void addPlayer(Splayer player) {
		synchronized (addedPlayers) {
			addedPlayers.add(player);
		}
	}

	//---------------------------------------------------------------------------
	/**
	 * Remove player from simulation.
	 * Because we have two active thread we can't remove player now.
	 * We have to wait until the second thread will do it.
	 */
	public void removePlayer(Splayer player) {
		synchronized (removedPlayers) {
			removedPlayers.add(player);
			System.out.println("### Player " + player.getId() + " side " + player.getSide() + " removed");
		}
	}

	//---------------------------------------------------------------------------    	
	public void addViewer(Sviewer viewer) {
		synchronized (addedViewers) {
			addedViewers.add(viewer);
		}
	}

	//---------------------------------------------------------------------------    	
	public void removeViewer(Sviewer viewer) {
		synchronized (removedViewers) {
			removedViewers.add(viewer);
			System.out.println("@@@ Viewer " + viewer.viewerId + " removed");
		}
	}
	//---------------------------------------------------------------------------
	/**
	 * This method updates player/viewer list
	 */
	public synchronized void updateClientList() {

		Enumeration<Splayer> ep;
		Splayer p;
		Enumeration<Sviewer> ev;
		Sviewer v;

		synchronized (addedPlayers) {

			ep = addedPlayers.elements();
			while (ep.hasMoreElements()) {

				p = (Splayer) ep.nextElement();
				if (p.getSide() == 'l') {
					leftTeam.add(p);
				} else {
					rightTeam.add(p);
				}
			}

			addedPlayers.clear();
		}

		synchronized (removedPlayers) {

			ep = removedPlayers.elements();
			while (ep.hasMoreElements()) {

				p = (Splayer) ep.nextElement();
				if (p.getSide() == 'l') {
					leftTeam.remove(p);
				} else {
					rightTeam.remove(p);
				}
			}

			removedPlayers.clear();
		}

		synchronized (addedViewers) {

			ev = addedViewers.elements();
			while (ev.hasMoreElements()) {

				v = (Sviewer) ev.nextElement();
				viewers.add(v);
			}

			addedViewers.clear();
		}

		synchronized (removedViewers) {

			ev = removedViewers.elements();
			while (ev.hasMoreElements()) {

				v = (Sviewer) ev.nextElement();
				viewers.remove(v);
			}

			removedViewers.clear();
		}
	}

	//----------------  simple getters and setters  ------------
	
	public boolean isSoundOn() {
		return isSoundOn;
	}
	
	public void setSoundOn(boolean isOnOff) {
		this.isSoundOn = isOnOff;
	}

	public boolean isCollision() {
		return isCollision;
	}

	public void setCollision(boolean isCollision) {
		this.isCollision = isCollision;
	}

	public boolean isBallKicked() {
		return isBallKicked;
	}

	public void setBallKicked(boolean isBallKicked) {
		this.isBallKicked = isBallKicked;
	}


	public Sball getBall() {
		return ball;
	}

	public void setBall(Sball ball) {
		this.ball = ball;
	}
	
	
}
