/* SoccerServerMain.java

   Copyright (C) 2001  Yu Zhang

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the 
   Free Software Foundation, Inc., 
   59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/

package soccer.server;

import soccer.common.MyProperties;
import soccer.common.TOS_Constants;
import soccer.common.Transceiver;
import soccer.common.Util;
import soccer.debugger.DebugTransceiver;
import soccer.debugger.DebuggerRun;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.htmmft.JSONObserver.JSONObserver;

/**
 * SoccerServerMain is an application class that simulates the
 * soccer game world and communicates with the clients - 
 * players and viewers. From the player clients it receives
 * their action commands, processes them, updates the state of
 * the world and sends back to all clients the updates to this state.
 * From the viewer clients the server application receives control
 * commands and/or action commands from the players run by users. 
 * 
 * Only one instance of this class can be run as part of the 
 * TOS bundle. This limitation is not enforced programmatically, though.
 * If more than one instance is launched, the system would behave abnormally. 
 * (TOD: consider applying the Singleton design pattern to this class.) 
 *
 * The SoccerServer runs three main threads.  The first
 * is the Console thread.  It gives you a simple
 * interface that responds to text commands, and quit. Still this thread 
 * is not critical. 
 * The second is the critical Receiver thread.  It creates a
 * SocketServer and receives packets from the clients.  
 * The last is the critical Transmitter thread
 * that operates the main logic functions of the game and
 * sends updates to the clients.
 * 
 * In the debug mode, class SoccerServerMain is part of the monolithic 
 * debugger  application integrating all parts of TOS bundle. 
 * Method main is not used in in this mode and different constructor 
 * is executed. 
 * 
 * @author Yu Zhang; Vadim Kyrylov (since 2006)
 */

public class SoccerServerMain {

	public final static String APP_NAME = "*** Tao Of Soccer - Server ";

	//maximal size of received packet (may be critical for modifications) 
	public static final int MAX_PACKET_SIZE = 128;  // (default 1024)	
	

	// The transceiver to set up a UDP channel. This object 
	// is shared by the Receiver and the Transmitter threads.
	// No conflict arise because they are using different methods.
	public static Transceiver transceiver = null;
	
	//---------------------------------------------------------------------------
	// Private members
	/** The soccerWorld maintains the state of the soccer game environment. */
	protected SoccerServerWorld soccerWorld = null;
	protected SoccerRules soccerRules = null;
	/** Thread Transmitter operates the main logic of the game, and sends out data packets. */
	protected Transmitter transmitter = null;
	/** Thread Receiver receives incoming packets. */
	protected Receiver reciever = null;
	
	// this is used for running threads (in the regular mode only)
	protected ExecutorService executor; 

	//---------------------------------------------------------------------------
	/**
	 * regular constructor (transmitter and receiver threads are used)
	 */
	public SoccerServerMain() {
		runConstructorStuff();
		// create service process for executing threads
		executor =  Executors.newCachedThreadPool();
		// create the two threads
		transmitter = new TransmitterThread(soccerWorld, soccerRules);
		reciever = new ReceiverThread(soccerWorld, transmitter, soccerRules);	
		
		try {
			transceiver = new Transceiver(TOS_Constants.SERVER_PORT);
			transceiver.setSize( MAX_PACKET_SIZE );
		} catch (Exception e) {
			System.out.print(
				"\nSServer:start up at port("
					+ TOS_Constants.SERVER_PORT
					+ ")"
					+ " fails:"
					+ e);

			System.exit(1);
		}
	}

	/**
	 * constructor for the debug mode 
	 * (NO threads are used; NO inetAddress needed)
	 */
	public SoccerServerMain(DebuggerRun debugger) {
		runConstructorStuff();
		transmitter = new Transmitter(soccerWorld, soccerRules);
		reciever = new Receiver(soccerWorld, transmitter, soccerRules);
		boolean isServer = true;
		transceiver 
			= new DebugTransceiver(isServer, TOS_Constants.SERVER_PORT, debugger);
	}
	
	protected void runConstructorStuff() {
		// override main random factors if applicable
		if (!TOS_Constants.RANDOMNESS_ON) {
			// recommended for debugging or special experiments only
			TOS_Constants.DASHRANDOM = 0;
			TOS_Constants.KICKRANDOM = 0;
			TOS_Constants.BALLRANDOM = 0;
			System.out.println("--- Random factors turned OFF ---\n");
		} else {
			System.out.println("--- Random factors turned ON ---\n");
		}
		
		// Initialize the application.
		soccerWorld = new SoccerServerWorld();
		soccerRules = new SoccerRules(soccerWorld);
	}
	
	//---------------------------------------------------------------------------
	// Console loop methods 
	// Reads lines (words actually) and performs the appropriate actions
	protected static void console() {
		// A welcome message
		System.out.print(
			"\n"
				+ APP_NAME
				+ TOS_Constants.APP_VERSION
				+ "\nSServer:'quit' to end the server, or 'help' to get a list of commands.\nSServer>\n");

		// Drop into the loop.
		DataInputStream dis = new DataInputStream(System.in);

		boolean ok = true;

		while (ok) {
			try {
				StringTokenizer st;

				st = new StringTokenizer(Util.readLine(dis));

				if (st.hasMoreTokens()) {
					String command = st.nextToken();
					if (command.equals("quit")) {
						ok = false;
						System.out.print("\nSServer:Bye.\n");
					} else {
						System.out.print(
							"\nSServer:Help for SoccerServer application:"
								+ "\nSServer:left_name  XXX - Set left team name to XXX."
								+ "\nSServer:right_name XXX - Set right team name to XXX."
								+ "\nSServer:help           - Is this.\n"
								+ "\nSServer:quit           - Exits the server killing all games."
								+ "\nSServer>");
					}
				}
			} catch (Exception e) {
			}
		}
	}

	//---------------------------------------------------------------------------
	/**
	 * Initialize the application, then drop into a console loop
	 * until it's time to quit.
	 * (NOT for the debug mode)
	 */
	public static void main(String args[]) {
		
		MyProperties 	properties 	= new MyProperties();
		boolean 	log 		= true;
		boolean 	fileexists 	= false;
		boolean 	verbose 	= false;
		String 		configFileName = null;

		configFileName = "server_copy.ini";
		
		try {
			// looking for config filename
			if (args[0].compareTo("-pf") == 0) {
				configFileName = args[1];
				File file = new File(configFileName);
				if (file.exists()) {
					fileexists = true;
				} else {
					System.out.println(
						"Properties file <"
							+ configFileName
							+ "> does not exist.");
				}
			} 
			// figure out about loading the file with verbous output
			try {
				if (args[2].compareTo("-verbose") == 0) 
					verbose = true;
			} catch (Exception e ) { }
			
			if ( fileexists ) {						
				System.out.println(
					"===  Load properties from file: " 
								+ configFileName + "  ===");
				verbose = true;
				properties = new MyProperties( verbose );
				try {
					properties.load(new FileInputStream(configFileName));
				} catch (Exception e ) {
					System.out.println("Error reading config file: " + e );	
					System.out.println("Using all default parameter values instead");	
				}
			}
			
		} catch (Exception e) {
			System.out.println("Error reading run parameters: " + e );	
			System.out.println("Using all default parameter values instead");	
			printStartMsg();  
		}
		
		SoccerServerMain server = new SoccerServerMain();

		try { 
			server.setProperties(properties);
		} catch (NumberFormatException e) {
			System.out.println( "Erorr reading property file."
					+ " Some propeties set to defaults. \n" + e  );
		}	
			
		if (log) {
			try {
				Calendar now = new GregorianCalendar();
				RandomAccessFile saved =
					new RandomAccessFile(
						"log_"
							+ now.get(Calendar.YEAR)
							+ (int)(now.get(Calendar.MONTH)+1)
							+ now.get(Calendar.DAY_OF_MONTH)
							+ now.get(Calendar.HOUR_OF_DAY)
							+ now.get(Calendar.MINUTE),
						"rw");
				server.transmitter.setSaved(saved);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			server.transmitter.log = true;
		}

		server.init();
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		// Drop into console loop.
		console();
		// Do an exlpicit exit to terminate any running treads.
		System.exit(0);
	}

	protected static void printStartMsg() {
			System.out.println("\n ***  Starting Tao of Soccer Server. *** \n");
			System.out.print(
				"\nSServer:USAGE: java SoccerServer [-key1 value [-key2] ]");
			System.out.print("\nSServer:");
			System.out.print("\nSServer:Key    value           default");
			System.out.print("\nSServer:-------------------------------------");
			System.out.print("\nSServer:pf   property_file_name  ./properties");
			System.out.print("\nSServer:verbose                  ./print out property values flag");
			System.out.print("\nSServer:");
			System.out.print("\nSServer:Examples:");
			System.out.print("\nSServer:java soccer.server.SoccerServer -pf server.ini");
			System.out.print("\nSServer:java soccer.server.SoccerServer -pf server.ini -verbous");
			System.out.println();
			System.out.println();
	}

	//---------------------------------------------------------------------------
	/**
	 * Initialize server
	 */
	public void init() 	{
		// Transmitter like Heart of World begins to pump 'blood'
		// execute thread
		executor.execute((TransmitterThread)transmitter);

		// Receiver begins to take care of visitors
		// execute thread
		executor.execute((ReceiverThread)reciever);
	}

	//----------------  getters and setters -------------------

	public SoccerServerWorld getWorld() {
		return soccerWorld;
	}

	//---------------------------------------------------------------------------
	
	public Transmitter getTransmitter() {
		return transmitter;
	}

	public Receiver getReciever() {
		return reciever;
	}

	/**
	 * set properties for this application
	 * 
	 * note: to assure consistency, the String default values in method 
	 * getProperty calls must be same as the default values assigned in class 
	 * TOS_Constants and its super entities 
	 */
	public void setProperties(MyProperties properties) {
		
		TOS_Constants.SERVER_PORT = 			
			Integer.parseInt(
				properties.getProperty(
						"port_number",
						"7777"));

		TOS_Constants.TEAM_FULL =
			Integer.parseInt(
				properties.getProperty(
					"team_full",
					String.valueOf(TOS_Constants.TEAM_FULL)));

		TOS_Constants.VIEWER_FULL =
			Integer.parseInt(
				properties.getProperty(
					"viewer_full",
					String.valueOf(TOS_Constants.VIEWER_FULL)));

		TOS_Constants.KICK_OFF_TIME =
			Integer.parseInt(
				properties.getProperty(
					"kick_off_time",
					String.valueOf(TOS_Constants.KICK_OFF_TIME))); 
					
		TOS_Constants.MAX_GRABBED_TIME =
			Double.parseDouble(
				properties.getProperty(
					"max_grabbed_time",
					String.valueOf(TOS_Constants.MAX_GRABBED_TIME))); 

		TOS_Constants.GAMES_TO_PLAY = 
			Integer.parseInt(
				properties.getProperty(
					"games_to_play",
					String.valueOf(TOS_Constants.GAMES_TO_PLAY)));  
		
		TOS_Constants.RESET_SCORE = 
			Boolean.parseBoolean(
				properties.getProperty(
					"reset_score",
					String.valueOf(TOS_Constants.RESET_SCORE)));  

		TOS_Constants.SIM_STEP_SECONDS =
			Double.parseDouble(
				properties.getProperty(
					"sim_step_seconds",
					String.valueOf(TOS_Constants.SIM_STEP_SECONDS)));

		TOS_Constants.RANDOMNESS_ON =
			Boolean.parseBoolean(
				properties.getProperty(
					"randomness_on",
					String.valueOf(TOS_Constants.RANDOMNESS_ON)));

		TOS_Constants.NO_GAME_SECONDS =
			Double.parseDouble(
				properties.getProperty(
					"no_game_seconds",
					String.valueOf(TOS_Constants.NO_GAME_SECONDS)));

		TOS_Constants.HALF_TIME_MINUTES =
			Double.parseDouble(
				properties.getProperty(
					"half_time",
					String.valueOf(TOS_Constants.HALF_TIME_MINUTES)));

		TOS_Constants.IDLE_MINUTES =
			Double.parseDouble(
				properties.getProperty(
					"idle",
					String.valueOf(TOS_Constants.IDLE_MINUTES)));

		TOS_Constants.PLAYERMAXSPEED =
			Double.parseDouble(
				properties.getProperty(
					"maxspeed_p",
					String.valueOf(TOS_Constants.PLAYERMAXSPEED)));

		TOS_Constants.TIMETOMAX =
			Double.parseDouble(
				properties.getProperty(
					"timetomax_p",
					String.valueOf(TOS_Constants.TIMETOMAX)));

		TOS_Constants.MAXDASH =
			Double.parseDouble(
				properties.getProperty(
					"maxdash",
					String.valueOf(TOS_Constants.MAXDASH)));

		TOS_Constants.MINDASH =
			Double.parseDouble(
				properties.getProperty(
					"mindash",
					String.valueOf(TOS_Constants.MINDASH)));

		TOS_Constants.MAXKICK =
			Double.parseDouble(
				properties.getProperty(
					"maxkick",
					String.valueOf(TOS_Constants.MAXKICK)));

		TOS_Constants.MINKICK =
			Double.parseDouble(
				properties.getProperty(
					"minkick",
					String.valueOf(TOS_Constants.MINKICK)));

		TOS_Constants.DASHRANDOM =
			Double.parseDouble(
				properties.getProperty(
					"random_p",
					String.valueOf(TOS_Constants.DASHRANDOM)));

		TOS_Constants.KICKRANDOM =
			Double.parseDouble(
				properties.getProperty(
					"kick_random",
					String.valueOf(TOS_Constants.KICKRANDOM)));

		TOS_Constants.DRIBBLEFACTOR =
			Double.parseDouble(
				properties.getProperty(
					"dribble_factor",
					String.valueOf(TOS_Constants.DRIBBLEFACTOR)));

		TOS_Constants.NOWORD =
			Double.parseDouble(
				properties.getProperty(
					"noword",
					String.valueOf(TOS_Constants.NOWORD)));

		TOS_Constants.COLLIDERANGE =
			Double.parseDouble(
				properties.getProperty(
					"collide_range",
					String.valueOf(TOS_Constants.COLLIDERANGE)));

		TOS_Constants.NOBALL =
			Double.parseDouble(
				properties.getProperty(
					"noball",
					String.valueOf(TOS_Constants.NOBALL)));

		TOS_Constants.BALLCONTROLRANGE =
			Double.parseDouble(
				properties.getProperty(
					"control_range",
					String.valueOf(TOS_Constants.BALLCONTROLRANGE)));

		TOS_Constants.BALLMAXSPEED =
			Double.parseDouble(
				properties.getProperty(
					"maxspeed_b",
					String.valueOf(TOS_Constants.BALLMAXSPEED)));

		TOS_Constants.BALLRANDOM =
			Double.parseDouble(
				properties.getProperty(
					"random_b",
					String.valueOf(TOS_Constants.BALLRANDOM)));

		TOS_Constants.FRICTIONFACTOR =
			Double.parseDouble(
				properties.getProperty(
					"friction_factor",
					String.valueOf(TOS_Constants.FRICTIONFACTOR)));
		
		TOS_Constants.OFFSIDERULE_ON =
			Boolean.valueOf(
				properties.getProperty(
					"offside_on",
					"true")).booleanValue();
		
		SoccerServerWorld.log =
			Boolean.valueOf(
				properties.getProperty(
					"log_on",
					"false")).booleanValue();
		
	}

}
