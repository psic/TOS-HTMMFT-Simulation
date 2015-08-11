package soccer.debugger;

import java.util.Vector;
import javax.swing.JOptionPane;

import soccer.client.ViewerClientMain;
import soccer.client.action.CoachForwardAction;
import soccer.client.action.CoachPlayAction;
import soccer.client.action.CoachStepAction;
import soccer.client.action.PlayGameAction;
import soccer.client.action.SetUpAIAction;
import soccer.client.action.SetUpServerAction;
import soccer.server.SoccerServerMain;
import soccer.tos_teams.sfu.AIPlayer;
import soccer.tos_teams.sfu.SoccerTeamMain;
import soccer.common.Game_Constants;
import soccer.common.Packet;
import soccer.common.TOS_Constants;
import soccer.common.Util;

/**
 * This is the debugger application class. It could be used for three purposes:
 * 1) debugging the TOS bundle on single computer (main purpose)
 * 2) demonstrating the soccer game
 * 3) running experiments on a single computer with the increased pace by 
 * 		utilizing up to 99% of the processor speed. 
 * 
 * Threaded applications are difficult to debug. Concurrently running
 * threads cannot be executed stepwise with the debugging tools like Eclipse.
 * That is why we need the Debugger.  
 * 
 * DebuggerMain glues together different TOS applications that normally 
 * are being run concurrently as stand alone programs and communicate over 
 * the network. 
 * The debugger allows running these parts as a whole without starting
 * the threads contained in these applications. Neither communication 
 * over the network, nor threading is used in this application. 
 * This substantially simplifies debugging.
 *  
 * By default, the number of soccer players on each side is set to three; this 
 * makes it easier to run this application in the debug mode in Eclipse. User 
 * can change these settings when the application starts. User can also cancel 
 * idling which maintain the simulation steps exactly as preset in 
 * TOS_Constants. Eliminating idling speeds up the simulation which helps 
 * gathering statistics for research purposes. 
 *  
 * The extras needed for bypassing multi-threading and communicating over the 
 * network are mostly contained in this class and in the dedicated class 
 * DebugTransceiver. Also these extras include the dedicated debug constructors 
 * in each TOS bundle application and a few methods in class SoccerTeamMain.
 * 
 * Very limited fraction of the code that is used solely in the regular 
 * mode is not run in this debugger. This includes methods main(), 
 * constructors in the application classes, most part of class Transceiver,  
 * and the small 'threaded' classes CviewerThread, ReceiverThread, 
 * TransmitterThread, and ThreadedPlayer. All this code must be tested 
 * and debugged separately.
 * 
 * @author Vadim Kyrylov
 */

public class DebuggerRun {
	
	// the four hard-coded variables below can be modified as necessary
	protected int leftTeamSize = 3;			// any number between 0 and 11
	protected boolean useGoalieL = true;	// or false if no goalie
	protected int rightTeamSize = 3;		// any number between 0 and 11
	protected boolean useGoalieR = true;	// or false if no goalie
	protected boolean useIdling = true;		// or false if no idling
	
	private ViewerClientMain viewerClient;
	private SoccerServerMain server;
	private SoccerTeamMain playerClient;
	
	// the pool of all sent communication packets 
	private Vector<Packet> packets = new Vector<Packet>();
	
	// average execution time of the whole bundle (household item)
	private double avgExecTime;	
	
		
	/**
	 * This method starts the TOS bundle as a monolithic
	 * application 
	 * @param args
	 */
	public static void main(String[] args) {
		DebuggerRun app = new DebuggerRun();
		/**
		 *  TODO - for unknown reason, DebuggerFrame does
		 *  not work properly with this application
		 */
		//DebuggerFrame  frame = new DebuggerFrame(app);
		//frame.requestFocus();	
		//-- the two lines below are not used with DebuggerFrame
		app.init();
		app.runSimulation();	
	}
	
	/**
	 * This method initializes the TOS bundle applications that all 
	 * run as part of this monolithic application. It also establishes 
	 * "communication" between them using class instance variable Vector 'packets'.
	 * 
	 * The "communication protocol" used is slightly different from the regular 
	 * mode, but it does not change the way how all all applications work.
	 */
	private void init() {
		
		//--- create viewer client and send CONNECT to the server
		viewerClient = 	new ViewerClientMain(this);
		viewerClient.setIn3D(false);		// toggle into 2D view
		//viewerClient.toggleMenuItem("Sound On");
		
		//-- allow user change some settings
		changeSettings();
		
		//--- create player client application with no players yet; only
		// send CONNECT packets to server
		playerClient = 	new SoccerTeamMain(this, leftTeamSize, useGoalieL, 
											rightTeamSize, useGoalieR);
		
		// start the server and let it receive the CONNECT packets and 
		// acknowledge this by sending INIT packets to player clients 
		server = 		new SoccerServerMain(this);
		runServerReceive();
		// send all INIT and other applicable packets
		server.getTransmitter().stepForward();
		
		// receive INIT packets and create player objects
		// (if idling is disallowed, so is the sound)
		playerClient.initPlayersDebug(useIdling);
		viewerClient.getSoundSystem().setSoundOn(useIdling);	
		runServerReceive();
		
		// send all INIT and other applicable packets
		server.getTransmitter().stepForward();
		// receive INIT packet and create the viewer object 
		runViewerClientINIT();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) { }
		
		// forward the game period from No Game to Before Kickoff
		viewerClient.debugForwardPeriod();
		runServerReceive();
		// clean up the data exchange object from unnecessary packets
		packets.removeAllElements();
		
		// disable game control buttons on the viewer GUI
		// (buttons are not used in the debug mode)
		viewerClient.getAction((Class<?>) SetUpServerAction.class).setEnabled(false);					
		viewerClient.getAction((Class<?>) SetUpAIAction.class).setEnabled(false);					
		viewerClient.getAction((Class<?>) PlayGameAction.class).setEnabled(false);					
		viewerClient.getAction((Class<?>) CoachPlayAction.class).setEnabled(false);
		viewerClient.getAction((Class<?>) CoachStepAction.class).setEnabled(false);
		viewerClient.getAction((Class<?>) CoachForwardAction.class).setEnabled(false);
	}
	
	/**
	 * This method runs a set of dialogs that allow user 
	 * changing some settings, such as idling, team size, etc
	 */
	private void changeSettings() {
		
		// allow/disallow random factors
		int choice0 = JOptionPane.showConfirmDialog(
				viewerClient, 
				"Allow major random factors in this simulation?\n"
			    + "(player dash, kick, and ball movement)\n"
			    + "Other minor randomness is always present.",
			    "Turn random factors ON/OFF",
			    JOptionPane.YES_NO_OPTION);
		TOS_Constants.RANDOMNESS_ON = (choice0 == JOptionPane.YES_OPTION); 
		
		// set the number of games
		Object[] gamesCount = new Object[10];
		double count = 1;
		for (int i=0; i<gamesCount.length; i++) {
			gamesCount[i] = "" + (int)count;	// between 1 and 512
			count = count * 2;
		}
		String s1 = (String)JOptionPane.showInputDialog(
				viewerClient,
                "Select number of games to play:", 
                "Number of games",
                JOptionPane.PLAIN_MESSAGE,
                null,
                gamesCount,
                "4");
		if (s1 != null)
			Game_Constants.GAMES_TO_PLAY = Integer.parseInt(s1);
		
		// allow/disallow running the simulation without idling
		int choice1 = JOptionPane.showConfirmDialog(
				viewerClient, 
				"Run the simulation as fast as possible?\n"
			    + "(idling and sound turned off)",
			    "Reject or allow idling and sound",
			    JOptionPane.YES_NO_OPTION);
		useIdling = (choice1 == JOptionPane.NO_OPTION); 
		
		// set the number of players on the left side
		Object[] teamSize = new Object[12];
		for (int i=0; i<teamSize.length; i++)
			teamSize[i] = "" + i;	// between 0 and 11
		String s2 = (String)JOptionPane.showInputDialog(
				viewerClient,
                "Select left team size:", 
                "Left team size",
                JOptionPane.PLAIN_MESSAGE,
                null,
                teamSize,
                "3");
		if (s2 != null)
			leftTeamSize = Integer.parseInt(s2);
		
		// allow a goalie on the left side
		int choice2 = JOptionPane.showConfirmDialog(
				viewerClient, 
				"Do you want to have a goalie\n"
			    + "in the left team?",
			    "Allow goalie or reject",
			    JOptionPane.YES_NO_OPTION);
		useGoalieL = (choice2 == JOptionPane.YES_OPTION); 

		// set the number of players on the right side
		String s3 = (String)JOptionPane.showInputDialog(
				viewerClient,
                "Select right team size:", 
                "Right team size",
                JOptionPane.PLAIN_MESSAGE,
                null,
                teamSize,
                "3");
		if (s3 != null)
			rightTeamSize = Integer.parseInt(s3);
		
		// allow a goalie on the right side
		int choice3 = JOptionPane.showConfirmDialog(
				viewerClient, 
				"Do you want to have a goalie\n"
			    + "in the right team?",
			    "Allow goalie or reject",
			    JOptionPane.YES_NO_OPTION);
		useGoalieR = (choice3 == JOptionPane.YES_OPTION); 
		
	}
	
	
	/**
	 * This method runs the predetermined number of soccer simulation games.
	 * It is meant that the programmer uses this simulation 
	 * in the debug mode and manually terminates the application
	 * when needed.
	 */
	public void runSimulation() {
		
		int count = 0;	// step counter
		
		while ( server.getTransmitter().getSoccerRules().gameCount 
							<= TOS_Constants.GAMES_TO_PLAY ) {
			long timeBefore = System.currentTimeMillis();
			// display average execution time of one simulation step in milliseconds
			if (count%1000 == 0)
				System.out.println( "Step count = " + count 
						+ " avgExecTime=" + Util.round(avgExecTime, 2) );
			
			if (count == 280)		// modify this magic number as necessary
				count = count+0;	// put a break point here for debugging
			
			runOneSimulationStep();			
			count++;
			long timeAfter = System.currentTimeMillis();
			long dt = timeAfter - timeBefore;
			// smoothened average execution time for one step
			avgExecTime = 0.01*dt + 0.99*avgExecTime;
			
			// make this program wait some time
			long indlingTimeMs = 1;	
			if (useIdling) {
				// time until the simulation cycle ends
				indlingTimeMs = (long)(1000*TOS_Constants.SIM_STEP_SECONDS) - dt;
			}
			// wait at least 1 ms to allow multiprocessing on the host computer
			indlingTimeMs = Math.max(indlingTimeMs, 1);
			try {
				Thread.sleep(indlingTimeMs);
			} catch (InterruptedException e) { }
		}
		System.out.println("\n======  Simulation done. All games are over.  =======");
		System.exit(0);
	}

	/**
	 * This method implements the sequence of data packets exchanges 
	 * between different TOS applications in one simulation step. 
	 * From the real mode, this sequence differs in that it is 
	 * synchronized; asynchronous behavior of each client is accomplished by 
	 * repeatedly running the client receiver until all applicable packets
	 * are processed by this client. 
	 */
	private void runOneSimulationStep() {
		
		server.getTransmitter().stepForward(); // send all applicable packets to clients
		runPlayerClient();	// perceive world, make decisions, and send action commands
		runViewerClient();	// receive updates and send action commands, if any
		runServerReceive();	// receive and execute action commands
		// clean up the exchange object from unnecessary packets
		packets.removeAllElements();
	}

	/**
	 * This method repeatedly runs each player "thread" until all
	 * received packets for this thread are processed and action
	 * commands "sent" in response to the server.  
	 */
	private void runPlayerClient() {
		
		for (int i = 0; i < playerClient.getRobots().size(); i++) {
			AIPlayer player = playerClient.getRobots().elementAt(i);
			// process all packets, if any, for this player
			Packet packet = new Packet();	// dummy packet
			while (packet != null) {
				packet = player.receiveInfoAndUpdate();
				if (packet != null) {
					// execute actions, if any, by sending back data packets
					player.planAndExecute(null);
				}
			} 
		}
	}

	/**
	 * This method processes INIT packet sent by server to the viewer client.  
	 */
	private void runViewerClientINIT() {
		// look through all packets, if any, and find INIT for this viewer
		int count = packets.size();
		boolean done = false;
		while (!done) {
			 if (viewerClient.getDebugViewDialog().isINITreceived())
				 return;
	//<----------
			 count--;
			 done = (count == 0);	// false if some packets remained to check
		}
		System.out.println("Error: No INIT packet found for the viewer");
	}

	/**
	 * This method repeatedly processes all received packets for 
	 * the viewer client; action commands, if any, are sent in response 
	 * to the server.  
	 */
	private void runViewerClient() {
		// process all packets, if any, for this viewer
		Packet packet = new Packet();	// dummy packet
		 while (packet != null) {
			 try {
				 packet = viewerClient.getViewer().runOneStep();
			 } catch (NullPointerException e) {
				 // this happens when user quits the game
				 System.out.println("Program runViewerClient terminated. Exception caught:\n" + e);
				 System.exit(1);
			 }
		}
	}

	/**
	 * This method receives packets, if any, one by one and processed 
	 * each; the responses, if any, are placed in the Transmitter
	 */
	private void runServerReceive() {
		Packet receivedPacket = new Packet();	// dummy packet
		while (receivedPacket != null) {	 
			receivedPacket = server.getReciever().runOneStep();
		}  		
	}
	
	/**
	 * This method returns count of packets of the specified
	 * type in the packets pool.
	 * 
	 * @param packetType
	 * @return
	 */
	public int getPacketCount(char type) {
		int count = 0;
		for (int i=0; i<packets.size(); i++) {
			Packet p = packets.elementAt(i);
			if (p.packetType == type) 
				count++;
		}
		return count;
	}

	public Vector<Packet> getPackets() {
		return packets;
	}
	
	/**
	 * This method terminates the application
	 */
	public void stop() {
		System.exit(1);
	}
	
}
