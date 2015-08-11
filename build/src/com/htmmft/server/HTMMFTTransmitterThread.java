package com.htmmft.server;

import soccer.common.RefereeData;
import soccer.common.TOS_Constants;
import soccer.server.SoccerRules;
import soccer.server.SoccerServerWorld;

import com.htmmft.Match;
import com.htmmft.team.HTMMFTCreateTeamsThread;

public class HTMMFTTransmitterThread extends HTMMFTTransmitter implements Runnable {
	
	private HTMMFTCreateTeamsThread equipes = null;
	//private SoccerServerMainHTMMFT soccerServer = null;
	
	// server performance statistics
	private int 	idleCount = 0;
	private int 	playOnCount = 0;
	private int 	deficitCount = 0;
	private double 	idleTimeSum = 0; 
	private double 	idleTimeSumOfSquares = 0; 
	private boolean end = false;
	
	public HTMMFTTransmitterThread(SoccerServerWorld soccerWorld,
			SoccerRules soccerRules, Match currentMatch) {
		super(soccerWorld, soccerRules);
		Currentmatch = currentMatch;
		equipes  = new HTMMFTCreateTeamsThread(Currentmatch);
		equipes.start();
	}
	
	/**
	 * This method implements the thread activity, which is 
	 * repeatedly running the simulation cycle.
	 * 
	 * It is not used in the debug mode. 
	 */
	public void run() {

		long startTimeMsec = System.currentTimeMillis();
	//	JSONObserver HTMMFTObs = null;
		//CreateTeamsThread equipes = null;
		while ( getSoccerRules().gameCount <= TOS_Constants.GAMES_TO_PLAY ) {
		
			if (Thread.currentThread().isInterrupted()){
				System.out.println("Transmitter INNNNNNNNNNNNNNTTTTTTTTTTEEEEEEEEEEEEERRRRRRRRRRRUUUUUPPPPPPPTTTTTTTIIIIIIOOOOOONNNNN");
				break;
			}
			
     		// get the time once this step began
			long timeBefore = System.currentTimeMillis();

			//if ( ticker%100 == 0 && ticker > 0 )
			//System.out.println("@ Transmitter ticker " + ticker 
			//+ " Heart is still beating. Game period:  " 
			//+  getSoccerRules().getPeriod());

			if(!isStepping()) {
				//--- execute the simulation step ---
				super.stepForward();
			}
			// determine time remaining until the end of cycle
			int idleTime = calcIdleTimeMs( timeBefore );

			if ( idleTime > 0 ) {
				//System.out.println("idling " + idleTime + " milliseconds");
				try {
					Thread.sleep( idleTime );
				} catch (InterruptedException e) {}
			}                   

			// allow the server running games on its own, without the monitor
			if ( getSoccerRules().getPeriod() == RefereeData.NO_GAME ) {
				if ( System.currentTimeMillis() 
						- startTimeMsec > (long)(TOS_Constants.NO_GAME_SECONDS*1000.0) ) {

					if ( !getSoccerWorld().getViewers().hasMoreElements() ) {
						// no viewer client has registered yet
	//					Match match = matchs.next();
		//				CreateTeamsThreadHTMMFT equipes  = new CreateTeamsThreadHTMMFT(Currentmatch);
			//			equipes.start();
						getSoccerRules().setPeriod( RefereeData.PRE_GAME );
						
						
						if ( !isSituationSaved() ) {
							// initialize for the replication
							getSoccerWorld().initSavedSituation();
							setSituationSaved(true);
						}
					}
				}
			}
		} //while (matchs.hasNext());	// end while
	//	equipes.writeFile();
		System.out.println("======= Simulation done. " 
				+ (getSoccerRules().gameCount-1) + " games played =======." );
		equipes.disconnect();
//		ArrayList<Future<?>> playerfuts = equipes.getFuturePlayersThread();
//		for (Future<?> fut : playerfuts){
//			boolean futb = fut.cancel(true);
//			System.out.println(futb + " " + fut.getClass().toString());
//		}
		
		//soccerServer.end();
		//SoccerServerMainHTMMFT.transceiver.disconnect();
		//Thread.currentThread().interrupt();
	}

	/**
	 * This method determines how long (in milliseconds) the server 
	 * can be waiting until the end of current simulation cycle.
	 * As a byproduct, this method measures the server performance
	 * by collecting useful statistics and printing it out.
	 * 
	 * @param timeBefore
	 * @return
	 */
	protected int calcIdleTimeMs( long timeBefore ) {

		// get the time after last step
		long timeAfter = System.currentTimeMillis();

		// figure out how long it takes to process
		long timeSpent = timeAfter - timeBefore;

		int idleTime = (int)(TOS_Constants.SIM_STEP_SECONDS * 1000
				- timeSpent);

		// collect performance statistics.
		// this info allows to figure out whether the platform
		// performance is sufficient indeed
		if ( getSoccerRules().getMode() == RefereeData.PLAY_ON ) {

			if (idleTime > 0) {
				idleCount++;
				idleTimeSum = idleTimeSum + idleTime; 
				idleTimeSumOfSquares = idleTimeSumOfSquares 
						+ idleTime * idleTime; 				
			} else
				deficitCount++;		// server is too slow

				playOnCount++;

				if ( playOnCount % 10000 == 0 ) {
					double avgTime = idleTimeSum/idleCount;
					double stdDev = idleTimeSumOfSquares/idleCount 
							- avgTime*avgTime;
					// convert them into per cent scale
					avgTime = 100 * avgTime/(TOS_Constants.SIM_STEP_SECONDS * 1000);
					stdDev = 100 * Math.sqrt( stdDev )/(TOS_Constants.SIM_STEP_SECONDS * 1000);
					System.out.println("\n   == Idling time statistics == \n" 
							+ " average = " + (int)avgTime + "%,"
							+ " std dev = " + (int)stdDev + "%,"
							+ " deficit = " 
							+ (int)(100.0*deficitCount/playOnCount) + "% \n" );   	
				}
		}

		// recalculate idle time 
		timeAfter = System.currentTimeMillis();
		timeSpent = timeAfter - timeBefore;
		idleTime = (int)(TOS_Constants.SIM_STEP_SECONDS * 1000 - timeSpent);

		return idleTime; 
	}

	public void end() {
		end=true;
		Thread.currentThread().interrupt();
		
	}

}
