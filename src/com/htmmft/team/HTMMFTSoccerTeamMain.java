/*
 * SoccerTeamMain.java 
 * 
 * Copyright (C) 2004-2012 Vadim Kyrylov 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */


package com.htmmft.team;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import soccer.common.ConnectData;
import soccer.common.Field_Constants;
import soccer.common.InitData;
import soccer.common.Packet;
import soccer.common.TOS_Constants;
import soccer.common.Transceiver;
import soccer.common.Vector2d;
import soccer.debugger.DebugTransceiver;
import soccer.debugger.DebuggerRun;
import soccer.tos_teams.sfu.AIPlayer;
import soccer.tos_teams.sfu.Formation;
import soccer.tos_teams.sfu.Formation.FPlayer;
import soccer.tos_teams.sfu.WorldData;

import com.htmmft.Equipe;
import com.htmmft.Joueur;
import com.htmmft.Match;
import com.htmmft.team.player.HTMMFTThreadedPlayer;

/**
 * Class SoccerTeamMain represents up to two soccer teams or just some
 * players who belong to one or both teams. Several instances of this 
 * class could be run as part of TOS bundle. For running two different 
 * teams, normally at lest two instances needed, one per team running 
 * different code located in other classes. This class does not 
 * implement the way players behave in the soccer game. It is only 
 * necessary for establishing the communication with the server and 
 * launching the pre-specified number of players. For running groups 
 * or individual players on different computers, several copies of 
 * this class must be instantiated. (As of 2011-08-28, splitting teams 
 * in parts has not been tested properly yet.)     
 * 
 * When both teams are placed in one class instance, this configuration 
 * can be used primarily for testing, debugging, or the experimentation. 
 * 
 * This class can be run in the regular mode as a standalone application 
 * or in the debug mode as part of the monolithic application controlled 
 * by the debugger.
 * 
 * In the regular mode this application implements soccer players as a 
 * set of threads communicating with the soccer server. Therefore, 
 * this application can contain from 1 to 22 player threads belonging 
 * to one or both teams playing the game. The number of player on either
 * side can me determined by the property file read in by method main.  
 * A team may be split between several replicas of this application; 
 * by default, both full-size teams are implemented in a single 
 * application.
 * 
 * Variables 'address' and 'port' are used for connecting the client 
 * threads to the server.
 * The default setting allows running the server and the players on 
 * the same computer. In a distributed setting, or with just one team
 * running, these values must be initialized accordingly,
 * e.g. by using the properties file.
 * 
 * In the debug mode this class is not a standalone application; rather,
 * it becomes part of a monolithic debugger application.
 * For the purpose of debugging, method main is not executed and
 * different constructor is used to create an instance of this class. 
 * Communication with server is still maintained through a Transceiver 
 * object, yet this object is controlled by the debugger and has no 
 * access to the computer network. 
 * 
 * @author Vadim Kyrylov
 */

//public class SoccerTeamMainHTMMFT extends SoccerTeamMain {
public class HTMMFTSoccerTeamMain  {
	private static Match match;
	private ArrayList<Future<?>> player_futs = new ArrayList<Future<?>>();
	
	
	public final static String APP_NAME = "Tao Of Soccer - Player Client (soccer team) ";

	// network communication objects
	public static InetAddress address;		
	public static int port = TOS_Constants.SERVER_PORT;	
	protected static String host = TOS_Constants.SERVER_ADDRESS;

	// if true, this application has been terminated by one of its player threads
	public static boolean isTerminated = false;

	// this is used when running in the debug mode only
	protected DebuggerRun debugger;
	private Vector<AIPlayer> robots;

	// this is used for running threads in the regular mode
	protected ExecutorService executor; 

	// the team parameters are being set in setProperties() below
	protected static int leftSize = TOS_Constants.TEAM_FULL;	
	protected static int rightSize = TOS_Constants.TEAM_FULL;
	protected static boolean useGoalieL = true; 
	protected static boolean useGoalieR = true; 
	protected static String formationTypeL = "523"; 
	protected static String formationTypeR = "523"; 
	protected static String teamNameL = "";
	protected static String teamNameR = "";

	// these variables could be used for testing and running experiments; 
	// they turn various player features on/off using the configuration 
	// file (see setProperties);
	// while using the Debugger, the default values should be changed by hand
	public static boolean isBallPassingAllowed = true; 
	public static boolean isFastBallDribblingAllowed = true; 
	public static boolean isModerateBallDribblingAllowed = true; 
	public static boolean isSlowBallDribblingAllowed = true; 
	public static boolean isBallHoldingAllowed = true; 
	public static boolean isAvoidingOffsideAllowed = true;	
	public static boolean isSmartPositioningAllowed = true; 
	public static boolean isMarkingOpponentAllowed = true; 
	public static boolean isOpeningForPassReceivingAllowed = true; 

	private HTMMFTCreateTeamsThread teamCreaterThread=null;
	protected Formation frmRight = null;
	protected Formation frmLeft = null;

	
	public static Properties StartWithPropertiesFile(Match match_){
		String configFileName ="HTMMFT_team.ini";
		Properties properties = new Properties();
		match = match_;
	
		File file = new File(configFileName);
		//File file = new File(String.valueOf(match.getId()));
		if (file.exists()) {
			System.out.println("Load properties from file: "
					+ configFileName);
			properties = new Properties();
			try {
				properties.load(new FileInputStream(configFileName));
			} catch (FileNotFoundException e) {
				System.err.println("");
				System.err.println("USAGE: SoccerTeamMain -pf property_file_name]");
				return null;
			} catch (IOException e) {
				System.err.println("");
				System.err.println("USAGE: SoccerTeamMain -pf property_file_name]");
				return null;
			}
		} else {
			System.out.println("Properties file <" + configFileName
					+ "> does not exist. Using defaults.");
			return null;
		}
		setProperties(properties);
		// run as a standalone application with a set of threaded players  
		// communicating with the rest of TOS bundle over the network
		leftSize = 11;
		rightSize = 11;
		useGoalieL = true;
		useGoalieR = true;
		formationTypeL = "CUSTOM";
		formationTypeR = "CUSTOM";
		teamNameL = match.getEquipe1().getNom();
		teamNameR = match.getEquipe2().getNom();
		new HTMMFTSoccerTeamMain();	
		
		return properties;
		
	}
	
	/**
	 * Constructor for the regular mode (distributed over a network)
	 */
	public HTMMFTSoccerTeamMain() {
		// create service process for executing threads
	//	executor =  Executors.newCachedThreadPool();
		//runConstructorSTuff();
		super();
		
		
	}
	
	/**
	 * Constructor for the regular mode (distributed over a network)
	 * @param createTeamsThreadHTMMFT 
	 */
	public HTMMFTSoccerTeamMain(Match match_, HTMMFTCreateTeamsThread createTeamsThreadHTMMFT) {
		// create service process for executing threads
	//	executor =  Executors.newCachedThreadPool();
		//runConstructorSTuff();
		//super();
		teamCreaterThread = createTeamsThreadHTMMFT;
		String configFileName ="HTMMFT_team.ini";
		Properties properties = new Properties();
		match = match_;
	
		File file = new File(configFileName);
		//File file = new File(String.valueOf(match.getId()));
		if (file.exists()) {
			System.out.println("Load properties from file: "
					+ configFileName);
			properties = new Properties();
			try {
				properties.load(new FileInputStream(configFileName));
			} catch (FileNotFoundException e) {
				System.err.println("");
				System.err.println("USAGE: SoccerTeamMain -pf property_file_name]");
			//	return null;
			} catch (IOException e) {
				System.err.println("");
				System.err.println("USAGE: SoccerTeamMain -pf property_file_name]");
				//return null;
			}
		} else {
			System.out.println("Properties file <" + configFileName
					+ "> does not exist. Using defaults.");
		//	return null;
		}
		setProperties(properties);
		// run as a standalone application with a set of threaded players  
		// communicating with the rest of TOS bundle over the network
		leftSize = 11;
		rightSize = 11;
		useGoalieL = true;
		useGoalieR = true;
		formationTypeL = "CUSTOM";
		formationTypeR = "CUSTOM";
		teamNameL = match.getEquipe1().getNom();
		teamNameR = match.getEquipe2().getNom();
		System.out.println( " Left team : " + teamNameL );
		System.out.println( " Right team : " + teamNameR );
		
		// create service process for executing threads
		executor =  Executors.newCachedThreadPool();
		runConstructorSTuff();
	}

	protected void runConstructorSTuff() {
		initFormations();
		// regular game simulation with standalone applications
		// (at least server and player clients are standalone)
		try {
			address = InetAddress.getByName(host);
		} catch (Exception e) {
			System.out.println("Network error:" + e);
			System.exit(1);
		}
		initPlayers();	
	}

	/**
	 * This method creates two soccer team formations based on values of 
	 * class variables formationTypeL and formationTypeR.
	 * Class variables frmLeft and frmRight are the outputs. 
	 */
	private void initFormations() {
		// create team formations, as necessary
		// leftSize, rightSize could be read from the initialization file 
		System.out.println();
		int index_gardien = 0;
		int index_kicker = 0;
		if ( leftSize > 0 ) {
			System.out.println( "Set formation for the LEFT team: " + formationTypeL );
			frmLeft = new Formation(formationTypeL.trim(), leftSize, useGoalieL);
			index_gardien = trouveGardien(match.getEquipe1());
			index_kicker = trouveKicker(match.getEquipe1());
			System.out.println(":::::::::::::::::::::: IGoolie : " + index_gardien + " IKicker : " + index_kicker);
			int i =0;
			boolean kicker = false;
			boolean goalie = false;
			for (Joueur joueur : match.getEquipe1().getJoueur()){
				FPlayer player = null;
				goalie = false;
				kicker = false;
				if (i == index_kicker){
					player = PostionneJoueur(joueur, frmLeft,true);
					kicker = true;
				}
				else
					player = PostionneJoueur(joueur, frmLeft,false);
				if (i == index_gardien){
					player.goalie =true;
					goalie = true;
				}
				System.out.println(":::::::::::::::::::::: Kicker : " +  kicker + " Goalie : "+ goalie + " ID : " + match.getEquipe1().getJoueur().get(i).getNumero());
//				frmLeft.add(player);
				frmLeft.add(player,match.getEquipe1().getJoueur().get(i).getNumero());
				i++;
			}
			System.out.println(i + " player(s) are created for left team : " + teamNameL );
		}
		if ( rightSize > 0 ) {
			System.out.println( "Set formation for the RIGHT team: " + formationTypeR );
			frmRight = new Formation(formationTypeR.trim(), rightSize, useGoalieR);
			index_gardien = trouveGardien(match.getEquipe2());
			index_kicker = trouveKicker(match.getEquipe2());
			System.out.println(":::::::::::::::::::::: IGoolie : " + index_gardien + " IKicker : " + index_kicker);
			int i =0;
			boolean kicker = false;
			boolean goalie = false;
			for (Joueur joueur : match.getEquipe2().getJoueur()){
				FPlayer player = null;
				goalie = false;
				kicker = false;
				
				if (i == index_kicker){
					player = PostionneJoueur(joueur, frmRight,true);
					kicker = true;
				}
				else
					player = PostionneJoueur(joueur, frmRight,false);
				if (i == index_gardien){
					player.goalie =true;
					goalie = true;
				}
				System.out.println(":::::::::::::::::::::: Kicker : " +  kicker + " Goalie : "+ goalie + " ID : " + match.getEquipe2().getJoueur().get(i).getNumero());
				//frmRight.add(player);
				frmRight.add(player,match.getEquipe2().getJoueur().get(i).getNumero());
				i++;
			
			}
			System.out.println(i + " player(s) are created for right team : "+ teamNameR);
		}
		System.out.println();		
	}

	private FPlayer PostionneJoueur(Joueur joueur, Formation f,boolean isKicker) {
		int defline = 660/3;
		int midline = 2 * defline;
		boolean def = false;
		boolean mid = false;
		boolean att= false;
		int x = joueur.getX();
		int y = joueur.getY();
		
		Vector2d vec = TranslateCoord(x,y);
		
		if (x < defline)
			def = true;
		if (x > midline)
			att = true;
		if (x > defline && x<midline)
			mid =true;
		System.out.println("::::::::::::::::::::::X: "+ vec.getX() + "/ Y: " + vec.getY() + " // " + def + " " + mid + " " + att);
		FPlayer player = f.new FPlayer ( vec, def, mid, att, isKicker );
		return player;
	}

	private Vector2d TranslateCoord(int x, int y) {
		//int xx = (int) (( (x+Field_Constants.LENGTH/2 ) / Field_Constants.LENGTH*660)+30 );
		//int xx = (int)(  ((Field_Constants.LENGTH*660)* (  x -30 )   )- Field_Constants.LENGTH/2);
		// pour le cercle du milieu
		int ymin = (int) ( - Field_Constants.RADIUS);
		int ymax =  (int) ( Field_Constants.RADIUS);
		int xx = (int)(  -(Field_Constants.LENGTH /2) - (Field_Constants.LENGTH/660) *(30 -x)  );
		if (xx > 0){
			xx = (int) -2;
		}
		
		//int yy = (int) (( (y+Field_Constants.WIDTH/2) / Field_Constants.WIDTH *430) +10 ) ;
		//int yy = (int)( ((Field_Constants.WIDTH*430)* (  x -10 )   )- Field_Constants.WIDTH/2);
		int yy = (int)(   -(Field_Constants.WIDTH/2) - (Field_Constants.WIDTH/430) *(10 -y) ) * -1;
		if (yy > ymin){
			if(yy < ymax){
				if (xx > 0)
				xx = (int) ( - Field_Constants.RADIUS -2);
			}
		}
		return new Vector2d(xx,yy);
	}
	
	public int trouveGardien(Equipe equipe) {
		ArrayList<com.htmmft.Joueur> joueurs = equipe.getJoueur();
		int x_gardien = joueurs.get(0).getX();
		int index_gardien =0;
		int index =0;
		for (Joueur joueur :joueurs){
				if ( joueur.getX() < x_gardien){
					x_gardien = joueur.getX();
					index_gardien = index;
				}
				index ++;
			}
		return index_gardien;
	}
	
	public int trouveKicker(Equipe equipe) {
		ArrayList<com.htmmft.Joueur> joueurs = equipe.getJoueur();
		int x_kcker = joueurs.get(0).getX();
		int index_kicker =0;
		int index =0;
		for (Joueur joueur :joueurs){
				if (joueur.getX() >  x_kcker ){
					x_kcker = joueur.getX();
					index_kicker = index;
				}
				index ++;
			}
		return index_kicker;
	}


	/**
	 * This method is used in the regular mode only. It waits 
	 * until a packet arrives and processes it.
	 * If this is an INIT packet, a new player client thread is created.
	 *  
	 * @param transceiver
	 * @param tmpName
	 * @param side
	 * @param agentRole
	 * @param formation
	 * @return - true if the packet is INIT and no exception was thrown.
	 */
	private boolean initPlayerAgent(Transceiver transceiver, String tmpName, 
			char side, char agentRole, Formation formation) {
		Packet packet;
		try {
			packet = transceiver.receive();	// wait until a packet arrives

//			System.out.println(tmpName + " received packet: " 
//					+ " addr: " + packet.address + " port: " + packet.port + " ::::    "
//					+ packet.writePacket() );

		} catch (IOException e) {
			// we get here if things are really bad
			System.out.println("player " + tmpName 
					+ " fails to receive from server.\n" + e);
			return false;
		}

		if (packet.packetType == Packet.INIT) {
			// The received packet is the player client registration confirmation;
			// in particular, it tells the player ID assigned by server.
			/**
			 * TODO: The server also must tell whether this player is confirmed
			 * as the goalie. More than one goalie can be mistakenly assigned   
			 * if the soccer team is split in several applications; the
			 * server allows only one goalie per team.
			 */
			InitData initData = (InitData) packet.data;
			// create a thread for the new player; server decides which player ID 
			// must be assigned (this becomes critical when the team is spread 
			// across different applications possibly running on different computers)
			HTMMFTThreadedPlayer robot = new HTMMFTThreadedPlayer(
					transceiver, 
					side, 
					initData.num,	// player ID sent back by server
					(agentRole == ConnectData.GOALIE), // true or false
					formation );  
			robot.getWorldModel().setFormation( formation ); 
			// execute the new thread
			player_futs.add(executor.submit(robot));
			//executor.execute(robot);
			return true;
		}
		return false;
	}

	
	/**
	 * In the regular mode, this method creates a threaded soccer player.
	 * The connection is established once the acknowledging
	 * INIT packet is received from the server in the response.
	 *  
	 * In the debug mode this method does half of this job by only sending 
	 * to the server the CONNECT packet on behalf of one player.  
	 *  
	 * @param side
	 * @param id
	 * @param formation
	 */
	private void initPlayer(char side,	// determines the team 
			// this is the suggested player id that can be overridden by the server
			// in the case when the team is spread across different applications;
			// it also determines the role (1 could be the goalie);
			// in the debug mode, server does not change this player id. 
			int id,			
			Formation formation) {

		String tmpName = id + "-" + side;	// used in error messages

		char agentRole;

//		if ( id == 1 && (useGoalieL && side=='l' || useGoalieR && side=='r') )
//			agentRole = ConnectData.GOALIE;
//		System.out.println( "ID : " +id + " "+  formation.getFormationArraySize() + " side : " + side);
		if (formation.isGoalie(id)){
			agentRole = ConnectData.GOALIE;
		}
		else {
			if ( formation.isKicker(id) )
				agentRole = ConnectData.FIELD_PLAYER_KICKER;
			else
				agentRole = ConnectData.FIELD_PLAYER;
		}

		// each player object has its own Transceiver
		boolean isServer = false;
		Transceiver transceiver;

		if (debugger == null) {
			// regular mode; create real transceiver communicating over a network
			transceiver = new Transceiver(isServer);
		} else {
			// debug mode; created an emulated transceiver without using a network 
			int myport = (side=='r') ? id : -id;
			transceiver = new DebugTransceiver(isServer, myport, debugger);
			// just send one packet in this mode
			connectToServer(transceiver, tmpName, id, side, agentRole, formation);
			return;	
			//<=====
		}

		// regular mode only; trying to connect to server by repeatedly 
		// sending the CONNECT packet
		int limit = 0;	
		while (limit < 60) {	
			// send CONNECT packet 
			connectToServer(transceiver, tmpName, id, side, agentRole, formation);
			// wait response from the server and create player agent
			if (initPlayerAgent(transceiver, tmpName, side, agentRole, formation)) {
				try {
					transceiver.setTimeout(0);
				} catch (Exception e) {
					System.out.println(tmpName + " initPlayer fatal error02: " + e);
				}
				return;
				//<=========
			}
			limit++;	
		}	
		// we get here if things are really bad
		System.out.println(tmpName + " initPlayer failed to connect to server");
	}

	/**
	 * In the regular mode, this method creates threaded soccer player objects.
	 * In the debug mode, this method does half of this job; to complete
	 * the initialization, another method is called by the debugger later.   
	 * Class variables frmLeft and frmRight are the inputs. 
	 */
	protected void initPlayers() {
		// initialize teams 
		System.out.println(" HTMMFT ---  connecting players  ---\n");
		int maxSize = Math.max( leftSize, rightSize );
		for (int i=1; i <maxSize+1; i++ ) {
			for ( int k=0; k < 2; k++ ) {
				if ( k == 0 ) {
					if ( i <= leftSize ) 
						//initPlayer('l', i, frmLeft );
						initPlayer('l',match.getEquipe1().getJoueur().get(i-1).getNumero(), frmLeft );
				} else {	
					if ( i <= rightSize ) 
						initPlayer('r',match.getEquipe2().getJoueur().get(i-1).getNumero(), frmRight );
				}
			}
		}
		teamCreaterThread.setFuturePlayersThread(player_futs);
		System.out.println();			
	}
	
	/**
	 * set properties (in the regular mode only)
	 *
	 * this method could be enhanced by setting whatever properties you want
	 * using the parameter initialization file
	 */
	public static void setProperties(Properties properties) {

		System.out.println("Properties:");
		leftSize = Integer.parseInt(properties
				.getProperty("left_team_size", "11"));
		System.out.println("left_team_size   \t" + leftSize);

		rightSize = Integer.parseInt(properties
				.getProperty("right_team_size", "11"));
		System.out.println("right_team_size \t" + rightSize);

		useGoalieL = Boolean.parseBoolean(properties
				.getProperty("use_goalie_l", "true"));
		System.out.println("use_goalie_l \t" + useGoalieL);

		useGoalieR = Boolean.parseBoolean(properties
				.getProperty("use_goalie_r", "true"));
		System.out.println("use_goalie_r \t" + useGoalieR);

		formationTypeL = properties.getProperty("left_formation", "523");
		System.out.println("left_formation \t" + formationTypeL);

		formationTypeR = properties.getProperty("right_formation", "523");
		System.out.println("right_formation \t" + formationTypeR);

		teamNameL = properties.getProperty("left_team_name", "Unknown_L");
		System.out.println("left_team_name \t" + teamNameL);

		teamNameR = properties.getProperty("right_team_name", "Unknown_R");
		System.out.println("right_team_name \t" + teamNameR);

		host = properties.getProperty("host_address", TOS_Constants.SERVER_ADDRESS);
		System.out.println("host_address \t" + host);

		port = Integer.parseInt(properties.getProperty("port_number", TOS_Constants.SERVER_PORT+""));
		System.out.println("port_number \t" + port + "\n");

	}

	/**
	 * This method sends CONNECT packet to server in order to get
	 * registered as a player client. 
	 * In this implementation, the first player is registered 
	 * as the goalie; the communication protocol allows 
	 * making goalie registration even from different application; 
	 * server must take care of getting registered of only one goalie per team
	 * 
	 * @param transceiver
	 * @param tmpName
	 * @param id
	 * @param side
	 * @param agentRole
	 * @param formation
	 */
	protected void connectToServer(Transceiver transceiver, String tmpName, int id, 
			char side, 	char agentRole, Formation formation) {

		// tell the server on what side this player is, his role, what his 
		// real home position is and place all this in a data packet
		char teamside;
		String name;
		if (side == 'l') {
			teamside = ConnectData.LEFT;
			name = teamNameL;
		} else {
			teamside = ConnectData.RIGHT;
			name = teamNameR;
		}		
		
//		System.out.println("My team : " + name  + " : " + agentRole);

		ConnectData connectData = new ConnectData(ConnectData.PLAYER, 
				teamside,
				agentRole, 
				WorldData.getRealPosOrVel(teamside, 
						formation.getHome(id)),
						name 
				);			
		connectData.playerID = id;
		Packet connectPacket = new Packet(	Packet.CONNECT, 
				connectData, 
				address,
				port);

		// attach sender ID for debug purposes 
		if (side == 'l')
			connectPacket.senderIDdebug = -id;
		else
			connectPacket.senderIDdebug = id;

		// send CONNECT packet to server
		try {
			transceiver.send(connectPacket);


//			System.out.println(tmpName + " sent connectPacket: " 
//					+ " addr: " + address + " port: " + port + " ::::::  "
//					+ connectPacket.writePacket() );


			// wait for the acknowledging INIT message from the server
			transceiver.setTimeout(1000);
			return;
		} catch (IOException e) {
			// we get here if things are really bad
			System.out.println("player " + tmpName + " fails to send to server.\n" + e);
			return;
		}		
	}
}