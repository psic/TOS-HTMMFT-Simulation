package soccer.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.htmmft.JSONObserver.CreateTeamsThread;
import com.htmmft.JSONObserver.HTMMFTThreadPool;
import com.htmmft.JSONObserver.JSONObserver;

import soccer.client.Executor;
import soccer.common.RefereeData;
import soccer.common.TOS_Constants;
import soccer.tos_teams.sfu.SoccerTeamMain;

/**
 * This class adds a synchronous thread functionality to its super class. 
 * The thread runs in cycles TOS_Constants.SIM_STEP_SECONDS long.
 * It also measures the server performance - to determine if any 
 * deficiency of computing power exists. 
 * 
 * It is NOT used in the debug mode; facilitating the debug mode
 * is the only reason why this class was factored out.
 * 
 *  @author V KYRYLOV since 2009
 */

public class TransmitterThread extends Transmitter implements Runnable {

	// server performance statistics
	private int 	idleCount = 0;
	private int 	playOnCount = 0;
	private int 	deficitCount = 0;
	private double 	idleTimeSum = 0; 
	private double 	idleTimeSumOfSquares = 0; 


	public TransmitterThread(SoccerServerWorld soccerWorld, 
			SoccerRules soccerRules) {
		super(soccerWorld, soccerRules);
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
		CreateTeamsThread equipes = null;
		while ( getSoccerRules().gameCount <= TOS_Constants.GAMES_TO_PLAY ) {

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
			//	System.out.println("idling " + idleTime + " milliseconds");
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
						//HTMMFTObs = new JSONObserver();
						
						equipes  = new CreateTeamsThread();
		        		equipes.start();
						getSoccerRules().setPeriod( RefereeData.PRE_GAME );
						//SoccerTeamMain.StartWithPropertiesFile("sfu_team.ini");
												 
						//final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(1);
						//HTMMFTThreadPool executor = new HTMMFTThreadPool(1, 1, 10, TimeUnit.SECONDS, queue);
						//executor.execute(HTMMFTObs);
						//executor.afterExecute(HTMMFTObs, null);
			//			HTMMFTObs.init();
				//		HTMMFTObs.start(); 
					//	System.out.println("JSON Observer Start");
						

						if ( !isSituationSaved() ) {
							// initialize for the replication
							getSoccerWorld().initSavedSituation();
							setSituationSaved(true);
						}
					}
				}
			}
		}	// end while
	//	equipes.writeFile();
		System.out.println("======= Simulation done. " 
				+ (getSoccerRules().gameCount-1) + " games played =======." );
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



}
