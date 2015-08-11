package com.htmmft.server;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import soccer.server.SoccerRules;
import soccer.server.SoccerServerWorld;
import soccer.server.Transmitter;

/**
 * This class adds an asynchronous thread functionality to
 * its super class. 
 * It is NOT used in the debug mode; facilitating the debug mode
 * is the only reason why this class was factored out. 
 * 
 * @author V KYRYLOV 2009
 */

public class HTMMFTReceiverThread extends HTMMFTReceiver implements Runnable {
	
	@SuppressWarnings("unused")
	private int packetCounter = 0;	// household variable
	private boolean end = false;
	public HTMMFTReceiverThread(SoccerServerWorld soccerWorld, 
							Transmitter transmitter, 
							SoccerRules soccerRules) {
		super(soccerWorld, transmitter, soccerRules);
	}

	/**
	 * Run repeatedly the main Receiver step (receiving and 
	 * processing one data packet that may arrive any time).
	 * All functionality is implemented in the super class. 
	 */
	public void run() {	
		//while(true)	{
		System.out.println("*********************************************Receiver Thread start");
//		while(!Thread.currentThread().isInterrupted()){
		while(!getEnd()){
			// this infinite loop terminates with the application
				if(!getEnd()){
					super.runOneStep();
					packetCounter++;
				}
				else{
					System.out.println("*********************************************Receiver Thread stop");
					break;
				}
			if (Thread.currentThread().isInterrupted()){
				//System.out.println("RECiver INNNNNNNNNNNNNNTTTTTTTTTTEEEEEEEEEEEEERRRRRRRRRRRUUUUUPPPPPPPTTTTTTTIIIIIIOOOOOONNNNN");
				break;
			}
			//if ( packetCounter%500 == 0 )
				//System.out.println("* packet count=" + packetCounter 
						//+ " ReceiverThread is still alive");
		}
		System.out.println("*********************************************Receiver stop");
	} 

	public synchronized void   end() {
			end=true;
		//Thread.currentThread().interrupt();
		
	}
	
	public synchronized void   stop() {
		//end=true;
	Thread.currentThread().interrupt();
	
}
	
	public synchronized boolean getEnd(){
		return end;
	}

}
