/* Formation.java
 
 by Vadim Kyrylov
 2006 - 2010
 
 This class implements hard coded team formation. 
 
 An alternative implementation is possible by manually placing players 
 on the field with the mouse in the Stepwise mode. Then this situation 
 can be saved to a file. This file could be loaded into this class. 
 
 Methods for saving game situation and loading it from file can be found
 in viewer client ( see classes SaveSnapshotAction, CoachLoadFileAction  
 in package soccer.client.action package and class SituationDialog in 
 package soccer.client.dialog)
 
*/


package soccer.tos_teams.sfu;

import soccer.common.*;
import soccer.tos_teams.sfu.Formation.FPlayer;

import java.util.*;

public class Formation
{		
	public Vector<FPlayer> fplayers = new Vector<FPlayer>();			
	private FPlayer player; 
	private int teamSize = 11; 
	
	// default constructor
	public Formation() 
	{
		super();
	}	 
	
	// this constructor creates a full team by default
	public Formation( String formationType )
	{
		super();
		// formation is one-based; the zero element is not used
		fplayers.addElement( player );	// adding a fiction element in place 0;
		
		if (formationType.equals("AUTO"))
			init("523");
		else
			init(formationType);
	}
	
	// this constructor creates a team of given size, with the goalie or without
	public Formation( String formationType, int teamSize, boolean useGoalie)
	{
		super();
		this.teamSize = teamSize;
		// formation is one-based; the zero element is not used
		fplayers.addElement( player );	// adding a fiction element in place 0;
		
		if (formationType.equals("AUTO"))
			initAuto(teamSize, useGoalie);
		else{
			if(formationType.equals("CUSTOM"))
				;
			else
				init(formationType);
		}
	}

	/**
	 * This method generates rather poor formation that  
	 * could be used for debugging small teams
	 * @param teamSize
	 * @param useGoalie
	 */
	private void initAuto(int teamSize, boolean useGoalie)
	{
		System.out.println("Formation AUTO  team size = " + teamSize);
		
		if (useGoalie) {
			System.out.println("  creating goalie");
			player = new FPlayer ( new Vector2d( -48.0, 0.0 ), false, false, false, false );
			player.goalie = true;
			fplayers.addElement( player );	
			teamSize--;
		}
		
		if (teamSize <= 2) {
			createAttackers(teamSize);
		} else if (teamSize < 5) {
			createDefenders(1);
			createAttackers(teamSize-1);
		} else if (teamSize < 7) {
			createDefenders(1);
			createMidfielders(2);
			createAttackers(teamSize-3);
		} else {
			createDefenders(teamSize-7);
			createMidfielders(3);
			createAttackers(4);
		}
		//printFormation();
	}
	
	private void createDefenders(int number)
	{
		System.out.println("  creating " + number + " defenders");
		createFieldPlayers(number, -33, 20, true, false, false);
	}
	
	private void createMidfielders(int number)
	{
		System.out.println("  creating " + number + " midfielders");
		createFieldPlayers(number, -15, 30, false, true, false);
	}
	
	private void createAttackers(int number)
	{
		System.out.println("  creating " + number + " attackers");
		createFieldPlayers(number, -2, 25, false, false, true);
	}
	
	/**
	 * This method evenly arranges players on given x-coordinate
	 * between -yMax and yMax.
	 * 
	 * @param number
	 * @param x
	 * @param yMax
	 * @param defender
	 * @param midfielder
	 * @param attacker
	 */
	private void createFieldPlayers(int number, double x, double yMax, 
			boolean defender, boolean midfielder, boolean attacker)
	{
		double yShift = yMax*(number-1.0)/number;
		double yStep;
		if (number > 1)
			yStep = 2*yShift/(number-1.0);
		else
			yStep = 1;	// with just one player, this results in one iteration
		for (double y=-yShift; y<=yShift; y=y+yStep) {
			if (fplayers.size() > this.teamSize)
				return;		
			player = new FPlayer ( new Vector2d( x, y ), defender, midfielder, attacker, false );
			fplayers.addElement( player );					
		}
	}

	private void init( String formationType )
	{
		// all settings should apply to the left-hand team only;
		// mirroring for the right-hand team is done elsewhere.
		System.out.println("Creating hard-coded formation " + formationType);
		
		if ( formationType.equals("433") )
		{
			// frequently used formation since 1980-s
			// the goalie 
			player = new FPlayer ( new Vector2d( -48.0, 0.3 ), false, false, false, false );
			player.goalie = true;
			fplayers.addElement( player );		
			
			// three defenders
			player = new FPlayer ( new Vector2d( -33.0, 20.0 ), true, false, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -35.0, -0.3 ), true, false, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -33.0, -20.0 ), true, false, false, false );
			fplayers.addElement( player );		
				
			// three midfielders
			player = new FPlayer ( new Vector2d( -17.0, 25.0 ), false, true, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -22.0, 0.0 ), false, true, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -17.0, -25.0 ), false, true, false, false );
			fplayers.addElement( player );		

			// four forwards
			player = new FPlayer ( new Vector2d( -1.5, 20.0 ), false, false, true, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -1.5, 8.0 ), false, false, true, true );	// kicker
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -1.5, -8.0 ), false, false, true, false ); 
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -1.5, -20.0 ), false, false, true, false );
			fplayers.addElement( player );		
		}
		else if ( formationType.equals("523") )
		{
			// the good old "W" formation (in use since 1950-s)
			// the goalie 
			player = new FPlayer ( new Vector2d( -48.0, 0.3 ), false, false, false, false );
			player.goalie = true;
			fplayers.addElement( player );		

			// three defenders
			player = new FPlayer ( new Vector2d( -33.0, 15.0 ), true, false, false, false );
			fplayers.addElement( player );	
			// the central defender and the goalie are slightly shifted apart by y-axis
			player = new FPlayer ( new Vector2d( -35.0, -0.3 ), true, false, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -33.0, -15.0 ), true, false, false, false );
			fplayers.addElement( player );		

			// two midfielders
			player = new FPlayer ( new Vector2d( -17.0, 21.0 ), false, true, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -17.0, -21.0 ), false, true, false, false );
			fplayers.addElement( player );		

			// five forwards
			player = new FPlayer ( new Vector2d( -6.5, 26.0 ), false, false, true, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -12.0, 10.0 ), false, false, true, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -3.0, 0.0 ), false, false, true, true );	// kicker	
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -12.0, -10.0 ), false, false, true, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -6.5, -26.0 ), false, false, true, false );
			fplayers.addElement( player );		
		}		
		else if ( formationType.equals("424") )
		{
			// the "Brasilian" formation (frequently used formation since 1966)
			// the goalie 
			player = new FPlayer ( new Vector2d( -48.0, 0.3 ), false, false, false, false );
			player.goalie = true;
			fplayers.addElement( player );		
			
			// three defenders
			player = new FPlayer ( new Vector2d( -27.0, 20.0 ), true, false, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -33.0, -7.0 ), true, false, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -33.0, 7.0 ), true, true, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -27.0, -20.0 ), true, false, false, false );
			fplayers.addElement( player );		
				
			// two midfielders
			player = new FPlayer ( new Vector2d( -15.0, 13.0 ), false, true, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -15.0, -13.0 ), false, true, false, false );
			fplayers.addElement( player );		

			// four forwards
			player = new FPlayer ( new Vector2d( -1.5, 22.0 ), false, false, true, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -1.5, 8.0 ), false, false, true, true );	// kicker
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -1.5, -8.0 ), false, false, true, false ); 
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -1.5, -22.0 ), false, false, true, false );
			fplayers.addElement( player );		
		}	
		else
		{
			// default formation (in fact, 523)
			// the goalie 
			player = new FPlayer ( new Vector2d( -48.0, 0.3 ), false, false, false, false );
			player.goalie = true;
			fplayers.addElement( player );		

			// three defenders
			player = new FPlayer ( new Vector2d( -33.0, 20.0 ), true, false, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -35.0, -0.3 ), true, false, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -33.0, -20.0 ), true, false, false, false );
			fplayers.addElement( player );		

			// two midfielders
			player = new FPlayer ( new Vector2d( -17.0, 18.0 ), false, true, false, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -17.0, -18.0 ), false, true, false, false );
			fplayers.addElement( player );		

			// five forwards
			player = new FPlayer ( new Vector2d( -1.5, 26.0 ), false, false, true, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -10.0, 10.0 ), false, false, true, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -3.0, 0.0 ), false, false, true, true );	// kicker	
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -10.0, -10.0 ), false, false, true, false );
			fplayers.addElement( player );		
			player = new FPlayer ( new Vector2d( -1.5, -26.0 ), false, false, true, false );
			fplayers.addElement( player );		
		}
			
		//System.out.println( "\n Set formation " + formation );	
	}
	

	public Vector2d getHome( int role )
	{
		FPlayer player = (FPlayer)fplayers.elementAt( role );
		return player.home;
	}
	
	public boolean isGoalie( int role ) 
	{
		FPlayer player = (FPlayer)fplayers.elementAt( role );
		return player.goalie;
	}

	public boolean isDefender( int role ) 
	{
		FPlayer player = (FPlayer)fplayers.elementAt( role );
		return player.defender;
	}
	
	public boolean isMidfielder( int role ) 
	{
		FPlayer player = (FPlayer)fplayers.elementAt( role );
		return player.midfielder;
	}

	public boolean isAttacker( int role ) 
	{
		FPlayer player = (FPlayer)fplayers.elementAt( role );
		return player.attacker;
	}

	public boolean isKicker( int role ) 
	{
		FPlayer player = (FPlayer)fplayers.elementAt( role );
		return player.kicker;
	}

	private void printFormation()
	{
		System.out.println("Created " + (fplayers.size()-1) + " players");
		for (int i=1; i<fplayers.size(); i++) {
			System.out.println( i + "\t" + fplayers.elementAt(i).home 
					+ "  goalie="+ fplayers.elementAt(i).goalie);
		}
		System.out.println();
	}
	
	/**
	 * This method is only used for unit testing.
	 * @param a
	 */
	public static void main(String a[])
	{
		Formation f;
		
		System.out.println("test 1: two players, no goalie ");
		f = new Formation("AUTO", 2, false);
		f.printFormation();
		
		System.out.println("test 2: five players with a goalie ");
		f = new Formation("AUTO", 5, true);
		f.printFormation();
	
		System.out.println("test 3: nine players with a goalie ");
		f = new Formation("AUTO", 9, true);
		f.printFormation();	
		
		System.out.println("test 4: just one goalie ");
		f = new Formation("AUTO", 1, true);
		f.printFormation();	
		
		System.out.println("test 5: just one field player ");
		f = new Formation("AUTO", 1, false);
		f.printFormation();	
		
		System.out.println("test 6: three players, no goalie ");
		f = new Formation("AUTO", 3, false);
		f.printFormation();	
	}
	
	// instances of this inner class are stored in the Formation 
	public class FPlayer
	{
		Vector2d 	home; 	// player default position on the field  
		
		public boolean 	goalie;	// special team member
		
		// these three categories could be treated differently in positioning
		boolean 	defender;
		boolean 	midfielder; 
		boolean 	attacker;
		
		boolean 	kicker;	// if true, the server would move this guy close to 
							// the ball before kickoff or corner kick.
							// if there is no kicker, server selects a default 
							// player itself, thus disturbing the formation

		public FPlayer( Vector2d 	home, 
				 boolean 	defender, 
				 boolean 	midfielder, 
				 boolean 	attacker,
				 boolean 	kicker )
		{
			this.home 		= home;
			this.defender 	= defender;
			this.midfielder = midfielder;	
			this.attacker 	= attacker;
			this.kicker 	= kicker;
			this.goalie 	= false;	// default value
		}
		
	}

	public void add(FPlayer player) {
		fplayers.addElement(player);
		
	}
	
}