package com.htmmft.JSONObserver;


import soccer.tos_teams.sfu.SoccerTeamMain;

public class CreateTeamsThread extends Thread {
	JSONObserver HTMMFTObs = null;
	public void run(){
		System.out.println("***************** HTMMFT Starting thread for creating teams");
		SoccerTeamMain.StartWithPropertiesFile("sfu_team.ini");
//		HTMMFTObs = new JSONObserver();
//		HTMMFTObs.init();
//		HTMMFTObs.start(); 
//		System.out.println("JSON Observer Start");
	}
	
	public void writeFile(){
		HTMMFTObs.writeJsonFile();
	}
	
}
