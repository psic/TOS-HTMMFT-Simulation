package com.htmmft.video;

import com.htmmft.Match;

public class ViewerThread extends Thread {
	
	private Match currentMatch;
	public ViewerThread(Match match) {
		currentMatch = match;
	}

	public void run (){
		ViewerClientLogMain viewer= new ViewerClientLogMain(currentMatch);
		System.out.println("\n\n****************************************************************** Write Match Video end" + currentMatch.getId() + " ************************************************************");

	}
}
