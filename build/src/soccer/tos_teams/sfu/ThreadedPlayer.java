package soccer.tos_teams.sfu;

import soccer.common.Transceiver;

/**
 * This class executed a soccer player thread.
 * To its super class it adds the real-time functionality only.
 * 
 * It is not used in the debug mode; facilitating the debug mode
 * is the only reason why this class was factored out. 

 * @author Vadim Kyrylov
 *
 */

public class ThreadedPlayer extends AIPlayer implements Runnable {
	
	public ThreadedPlayer( 
							Transceiver transceiver, 
					  		char side, 
					  		int playerid,						
					  		boolean isGoalie,				
					  		Formation aFormation 
	  					 ) {
		
		super("Player-" + playerid + "-" + side );	// set the thread name
		System.out.print("AI -- Starting " + getName() );
		super.setTransceiver(transceiver);
		if ( isGoalie )
			System.out.println("  ** goalie **");
		else 
			System.out.println();
		super.setWorldModel(new PlayerWorld( transceiver, side, playerid, isGoalie));
	}


	/**
	 * Run repeatedly the main player loop.
	 * All functionality is implemented in the super class. 
	 */
	public void run() {	
		while(true)	{
			// this infinite loop terminates with the application
			super.runOneStep();
		}
	} 
	
}
