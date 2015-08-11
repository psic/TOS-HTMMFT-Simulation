package com.htmmft.team;

import java.util.ArrayList;
import java.util.concurrent.Future;

import com.htmmft.Match;

public class HTMMFTCreateTeamsThread extends Thread{
		private Match match;
		private ArrayList<Future<?>> player_futs;
		public HTMMFTCreateTeamsThread(Match match) {
		this.match = match;
	}

		public void run(){
			System.out.println("HTMMFT ************************************************************ Starting thread for creating teams");
			//SoccerTeamMainHTMMFT.StartWithPropertiesFile(match);
			HTMMFTSoccerTeamMain team = new HTMMFTSoccerTeamMain(match, this);

		}

		public void setFuturePlayersThread(ArrayList<Future<?>> player_futs) {
			this.player_futs =player_futs; 			
		}

		/**
		 * @return the player_futs
		 */
		public ArrayList<Future<?>> getFuturePlayersThread() {
			return player_futs;
		}

		public void disconnect() {
			// TODO Auto-generated method stub
			Thread.currentThread().interrupt();
			
		}


}
