package soccer.client;

import soccer.common.ByeData;
import soccer.common.Packet;
import soccer.common.ViewData;

/**
 * This class adds the asynchronous thread functionality to
 * its super class. 
 * It is not used in the debug mode; facilitating the debug mode
 * is the only reason why this class was factored out. 
 * 
 * Copyright (C) 2012 Vadim Kyrylov
 */
public class CviewerThread extends Cviewer implements Runnable {
	
	// packet statistics
	@SuppressWarnings("unused")
	private int 	receivedPacketCount = 0;
	private	int 	previousReceivedPacketID = -1;	
	private	int 	lostPacketCount = 0;
	private double 	lostPacketFactor = 0;
	private static final int  MODULUS = 1000;
	private boolean stop = false;
	
	/**
	 * constructor
	 * @param soccerMaster
	 */
	public CviewerThread(ViewerClientMain soccerMaster) {
		super(soccerMaster);
	}

	/**
	 * Run repeatedly the Cviewer main step.
	 * All functionality is implemented in the super class. 
	 */
	public void run() {
		
		while (!stop) {
			
			Packet receivedPacket = super.runOneStep();
			
			checkLostPackets(receivedPacket);
			receivedPacketCount++;

			//try {
			// a stub for testing packet loss by introducing delay	
				//sleep( 80 );	
			//} catch (Exception e ) {}
		}
	}
	
	/**
	 * this method collects lost packet statistics that could be 
	 * useful for determining whether the client is running too slow.
	 */
	private void checkLostPackets( Packet aPacket ) {
		
		if( aPacket.packetType == Packet.VIEW ) {
			// as the the VIEW packets are sent on each simulation step 
			// and the server inserts in them the step ID, this parameter
			// can be used to detect packets losses 

			ViewData aViewData = (ViewData)aPacket.data;
			
			if ( previousReceivedPacketID < 0 ) {
				// skip the first packet
			} else {
				
				
				int delta = aViewData.time  
						- ( previousReceivedPacketID + 1 );
				if ( delta <0 )
					delta = MODULUS - delta; 
	
				if ( delta > MODULUS/2 )
					delta = 0; 	// just ignore too big losses
				
				// this is the exponential smoothening method
				double weight = 0.5;			// a magic number
				lostPacketFactor = weight * delta + 
									(1 - weight) * lostPacketFactor;
				
				if ( lostPacketFactor > 0.7 ) {
					// print a warning that packets are being lost
					lostPacketCount = lostPacketCount + delta;
					System.out.println("** CviewerThread lost " + delta 
						+ " packets" //+ " lostPacketCount = " + lostPacketCount 
						+ "  lostPacketFactor = " 
						+ ((int)(1000.0*lostPacketFactor))/1000.0 + "  **" );	
				}
			}
			previousReceivedPacketID = aViewData.time;
		}
	}

	/**
	 * This method ends this thread if necessary
	 */
	public void end(char actionType) {
		super.end(actionType);
		if (actionType == ByeData.RELEASE) {
			// keep the thread running
			System.out.println("soccerMaster.isPlaying() = " + soccerMaster.isPlaying());
		} else {
			// raise flag
			stop = true;
			System.out.println("-- CviewerThread terminated");
		}
	}

}
