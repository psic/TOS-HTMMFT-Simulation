package soccer.server;

/**
 * This class adds an asynchronous thread functionality to
 * its super class. 
 * It is NOT used in the debug mode; facilitating the debug mode
 * is the only reason why this class was factored out. 
 * 
 * @author V KYRYLOV 2009
 */

public class ReceiverThread extends Receiver implements Runnable {
	
	@SuppressWarnings("unused")
	private int packetCounter = 0;	// household variable
	
	public ReceiverThread(SoccerServerWorld soccerWorld, 
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
		while(true)	{
			// this infinite loop terminates with the application
			super.runOneStep();
			packetCounter++;
			//if ( packetCounter%500 == 0 )
				//System.out.println("* packet count=" + packetCounter 
						//+ " ReceiverThread is still alive");
		}
	} 

}
