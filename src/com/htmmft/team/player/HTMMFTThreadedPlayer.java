package com.htmmft.team.player;

import com.htmmft.server.HTMMFTSoccerServerMain;

import soccer.common.Transceiver;
import soccer.tos_teams.sfu.Formation;
import soccer.tos_teams.sfu.PlayerWorld;

/**
 * This class executed a soccer player thread.
 * To its super class it adds the real-time functionality only.
 * 
 * It is not used in the debug mode; facilitating the debug mode
 * is the only reason why this class was factored out. 

 * @author Vadim Kyrylov
 *
 */

public class HTMMFTThreadedPlayer extends HTMMFTAI implements Runnable {
	
	public HTMMFTThreadedPlayer( 
							Transceiver transceiver, 
					  		char side, 
					  		int playerid,						
					  		boolean isGoalie,				
					  		Formation aFormation 
	  					 ) {
		
		super("Player-" + playerid + "-" + side );	// set the thread name
		System.out.print("HTMMFT AI -- Starting " + getName() );
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
		while(!Thread.currentThread().isInterrupted())	{
			// this infinite loop terminates with the application
			if (!HTMMFTSoccerServerMain.transceiver.isClosed())
				super.runOneStep();
			if (Thread.currentThread().isInterrupted()){
				transceiver.disconnect();
				break;
			}
		}
		System.out.println("*********************************************HTMMFT AI stop");
	} 
	
}
