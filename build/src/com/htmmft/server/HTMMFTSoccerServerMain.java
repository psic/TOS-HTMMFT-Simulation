package com.htmmft.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.htmmft.Match;

import soccer.common.MyProperties;
import soccer.common.TOS_Constants;
import soccer.common.Transceiver;
import soccer.server.SoccerRules;
import soccer.server.SoccerServerWorld;
import soccer.server.Transmitter;

public class HTMMFTSoccerServerMain extends Thread {

	public static final String logFolder = "./logs/";
	public final static String APP_NAME = "*** HTMMFT -- Tao Of Soccer - Server ";
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
	protected HTMMFTTransmitterThread transmitter = null;
	/** Thread Receiver receives incoming packets. */
	protected HTMMFTReceiverThread reciever = null;

	// this is used for running threads (in the regular mode only)
	protected ExecutorService executor; 
	public  boolean end = false;
	private String configFileName ;
	private Future<?> fut_transmitter;
	private Future<?> fut_reciever;

	public HTMMFTSoccerServerMain(Match currentMatch,String config){
		//super();
		//transmitter = new TransmitterThreadHTMMFT(soccerWorld, soccerRules);
		configFileName = config;
		runConstructorStuff();
		// create service process for executing threads
		executor =  Executors.newCachedThreadPool();	
		
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
	
		// create the two threads
		//transmitter = new TransmitterThread(soccerWorld, soccerRules);
		transmitter = new HTMMFTTransmitterThread(soccerWorld, soccerRules,currentMatch);
		reciever = new HTMMFTReceiverThread(soccerWorld, transmitter, soccerRules);
		
		MyProperties 	properties 	= new MyProperties();
		boolean 	log 		= true;
		boolean 	fileexists 	= false;

		

		if (log) {
			try {
				String SaveFile = String.valueOf(HTMMFTSoccerServerMain.logFolder + currentMatch.getId());
				RandomAccessFile saved =	new RandomAccessFile(SaveFile,"rw");
				transmitter.setSaved(saved);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			transmitter.log = true;
		}
		//init();
	}

//	public static void main(String args[]) {
//
//		MyProperties 	properties 	= new MyProperties();
//		boolean 	log 		= true;
//		boolean 	fileexists 	= false;
//		boolean 	verbose 	= false;
//		String 		configFileName = null;
//
//		configFileName = "server_copy.ini";
//		int numjournee = 0;
//		try {
//			// looking for config filename
//			if (args[0].compareTo("-pf") == 0) {
//				configFileName = args[1];
//				File file = new File(configFileName);
//				if (file.exists()) {
//					fileexists = true;
//				} else {
//					System.out.println(
//							"Properties file <"
//									+ configFileName
//									+ "> does not exist.");
//				}
//			} 
//
//
//			// figure out about loading the file with verbous output
//			try {
//				if (args[2].compareTo("-verbose") == 0) 
//					verbose = true;
//			} catch (Exception e ) { }
//
//			if ( fileexists ) {						
//				System.out.println(
//						"===  Load properties from file: " 
//								+ configFileName + "  ===");
//				verbose = true;
//				properties = new MyProperties( verbose );
//				try {
//					properties.load(new FileInputStream(configFileName));
//				} catch (Exception e ) {
//					System.out.println("Error reading config file: " + e );	
//					System.out.println("Using all default parameter values instead");	
//				}
//			}
//
//		} catch (Exception e) {
//			System.out.println("Error reading run parameters: " + e );	
//			System.out.println("Using all default parameter values instead");	
//			printStartMsg();  
//		}
//
//		SoccerServerMainHTMMFT server = new SoccerServerMainHTMMFT();
//
//		try { 
//			server.setProperties(properties);
//		} catch (NumberFormatException e) {
//			System.out.println( "Erorr reading property file."
//					+ " Some propeties set to defaults. \n" + e  );
//		}	
//
//		if (log) {
//			try {
//				Calendar now = new GregorianCalendar();
//				RandomAccessFile saved =
//						new RandomAccessFile(
//								"log_"
//										+ now.get(Calendar.YEAR)
//										+ (int)(now.get(Calendar.MONTH)+1)
//										+ now.get(Calendar.DAY_OF_MONTH)
//										+ now.get(Calendar.HOUR_OF_DAY)
//										+ now.get(Calendar.MINUTE),
//								"rw");
//				server.transmitter.setSaved(saved);
//			} catch (IOException e) {
//				e.printStackTrace();
//				System.exit(-1);
//			}
//
//			server.transmitter.log = true;
//		}
//
//		server.init();
//		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
//		// Drop into console loop.
//		console();
//		// Do an exlpicit exit to terminate any running treads.
//		System.exit(0);
//	}


	//static public void startWithProperties(String config, Match currentMatch) {
	public void run(){
		File file = new File(configFileName);
		MyProperties 	properties 	= new MyProperties();
		boolean 	log 		= true;
		boolean 	fileexists 	= false;
		if (file.exists()) {
			fileexists = true;
		} else {
			System.out.println(
					"Properties file <"
							+ configFileName
							+ "> does not exist.");
		}


		if ( fileexists ) {						
			System.out.println(
					"===  Load properties from file: " 
							+ configFileName + "  ===");
			properties = new MyProperties(true  );
			try {
				properties.load(new FileInputStream(configFileName));
			} catch (Exception e ) {
				System.out.println("Error reading config file: " + e );	
				System.out.println("Using all default parameter values instead");	
			}
		}

		try { 
			setProperties(properties);
		} catch (NumberFormatException e) {
			System.out.println( "Erorr reading property file."
					+ " Some propeties set to defaults. \n" + e  );
		}	

		init();
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		console();
		// Do an exlpicit exit to terminate any running treads.
		//System.exit(0);

	}

	//---------------------------------------------------------------------------
	/**
	 * Initialize server
	 */
	public void init() 	{
		// Transmitter like Heart of World begins to pump 'blood'
		// execute thread
		//fut_transmitter= executor.execute((TransmitterThreadHTMMFT)transmitter);
		fut_transmitter= executor.submit((HTMMFTTransmitterThread)transmitter);
		transmitter.setServer(this);
		// Receiver begins to take care of visitors
		// execute thread
		fut_reciever= executor.submit((HTMMFTReceiverThread)reciever);
		reciever.setServer(this);
		//executor.execute((ReceiverThreadHTMMFT)reciever);
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
	protected  void console() {
	
		//while (!end) {
			//while ( soccerRules.gameCount <= TOS_Constants.GAMES_TO_PLAY ) {
		//while(!executor.isTerminated()){
		while(!transceiver.isClosed()){
		//while(!Thread.interrupted()){
			//System.out.println(end);
		}
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

	//----------------  getters and setters -------------------

	public SoccerServerWorld getWorld() {
		return soccerWorld;
	}

	//---------------------------------------------------------------------------

	public Transmitter getTransmitter() {
		return transmitter;
	}

	public HTMMFTReceiver getReciever() {
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

	public void end() {
		System.out.println("LA FIIIIIIIIIIN **********************");
		//executor.shutdownNow();
		reciever.end();
		boolean fut1 = fut_reciever.cancel(true);
		boolean fut2 = fut_transmitter.cancel(true);
		executor.shutdown(); // Disable new tasks from being submitted
   	 java.util.List<Runnable> task = executor.shutdownNow(); // Cancel currently executing tasks
   	 System.out.println("Tache : " + task.size() + " " + executor.isShutdown() + " " + executor.isTerminated()+ " " + fut1 + " " + fut2);
   	 Thread.currentThread().interrupt();
		   try {
		     // Wait a while for existing tasks to terminate
		     if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
		    	 task = executor.shutdownNow(); // Cancel currently executing tasks
		    	 System.out.println("Tache : " + task.size());
		    	 Thread.currentThread().interrupt();
//		    	 for (Runnable tache : task){
//		    		 tache.toString();
//		    	 }
		    	 
		       // Wait a while for tasks to respond to being cancelled
		       if (!executor.awaitTermination(1, TimeUnit.SECONDS))
		           System.err.println("Pool did not terminate");
		       else
		    	   Thread.currentThread().interrupt();
		     }
		   } catch (InterruptedException ie) {
		     // (Re-)Cancel if current thread also interrupted
			   executor.shutdownNow();
		     // Preserve interrupt status
			   Thread.currentThread().interrupt();
		   }
		this.end = true;
		//System.exit(0);
		
	}

	public void arret() {
		System.out.println("On esssaye de tout arrete  ^_^");
		//transmitter.end();
		//System.out.println("On esssaye de tout arrete  ^_^");

		//reciever.end();
		reciever.stop();
		System.out.println("On esssaye de tout arrete  ^_^2");
		//transceiver.disconnect();
	}

}
