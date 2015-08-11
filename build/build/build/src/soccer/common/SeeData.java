/** SeeData.java
 * 
 * An instance of this class represents data received by the visual 
 * sensor of a player client. Server decides what the AI player can see 
 * and sends these data to him. 
 * 
 * Copyright (C) 2001  Yu Zhang
 * modifications by Vadim Kyrylov (since 2004)
 */

package soccer.common;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;


public class SeeData implements Data {
	/**
	* status NO_OFFSIDE indicates no players are at Offside position.
	*/
	public static final int NO_OFFSIDE     = 0;
	/**
	* status OFFSIDE indicates I'm at Offside position.
	*/
	public static final int OFFSIDE     = 1;
	/**
	* status T_OFFSIDE indicates some my teammates are at Offside position.
	*/
	public static final int T_OFFSIDE     = 2;
	
	/**
	* the current simulation step.
	*/
	public int time;
	/**
	* the player who perceives this information
	*/
	public Player player;
	/**
	* the player's Offside position status. Its permitted values listed above
	*/  
	public int status;
	/**
	* the ball.
	*/
	public Ball ball;  
	
	/**
	* a list of positions for the left team. 
	*/
	public Vector<Player> leftTeam; 
	/**
	* a list of positions for the right team. 
	*/
	public Vector<Player> rightTeam; 
	
	
	/**
	* Constructs an empty SeeData object for reading from an UDP packet.
	*/
	public SeeData() {
		this.time = 	0;
		this.player = 	new Player();
		this.status = 	0;
		this.ball = 	new Ball();
		this.leftTeam = new Vector<Player>();
		this.rightTeam = new Vector<Player>();
	} 
	
	/** 
	* Constructs a SeeData object for writing to an UDP packet.
	*
	* @param time the current simulation step.
	* @param player the player.
	* @param status the player's Offside position status.
	* @param ball the ball.
	* @param leftTeam a list of positions for the left team.
	* @param rightTeam a list of positions for the right team.
	*/
	public SeeData(int time, Player player, int status, Ball ball, 
							Vector<Player> leftTeam, Vector<Player> rightTeam) {
		this.time = time;
		this.player = player;
		this.status = status;
		this.ball = ball;
		this.leftTeam = leftTeam;
		this.rightTeam = rightTeam;
	} 
	
	
	// Load its data content from a string
	public void readData(StringTokenizer st) {
		double x,y;
		int dataid = 0;
		
		try {
			// Get the time.
			time = Integer.parseInt(st.nextToken());
			dataid++;
			// Get the " "
			st.nextToken();
			
			//-----  Player  -----
			// Get player side
			player.setSide(st.nextToken().charAt(0));
			dataid++;
			// Get the " "
			st.nextToken();
			
			// Get player id
			player.setId(Integer.parseInt(st.nextToken()));
			dataid++;
			// Get the " "
			st.nextToken();    
			
			// Get player position x
			x = Double.parseDouble(st.nextToken()) / 100;
			dataid++;
			// Get the " "
			st.nextToken();
			
			// Get player position y
			y = Double.parseDouble(st.nextToken()) / 100;
			dataid++;
			// Get player position
			player.getPosition().setXY(x,y);
			
			// Get the " "
			st.nextToken();
			
			// Get player direction
			player.setDirection(Double.parseDouble(st.nextToken()));
			dataid++;
			
			// Get the " "
			st.nextToken();
			player.setUserControlled(Boolean.parseBoolean(st.nextToken()));
			dataid++;
			
			// Get the " "
			st.nextToken();

			// Get the status
			status = Integer.parseInt(st.nextToken());
			dataid++;
			// Get the " "
			st.nextToken();
			
			//-----  Ball  -----
			// Get ball's position x
			x = Double.parseDouble(st.nextToken()) / 100;
			dataid++;
			// Get the " "
			st.nextToken();
			
			// Get ball's position y
			y = Double.parseDouble(st.nextToken()) / 100;
			dataid++;
			// Get ball's position
			ball.getPosition().setXY(x,y);
			
			// Get the " "
			st.nextToken();
			
			// Get ball's controller type.
			ball.controllerType = st.nextToken().charAt(0);
			dataid++;
			// Get the " "
			st.nextToken();
			
			// Get ball's controller id.
			ball.controllerId = Integer.parseInt(st.nextToken());
			dataid++;
			// Get the " "
			st.nextToken();
			
			// Get ball grabbed status
			char grabbed = st.nextToken().charAt(0);
			ball.isGrabbed = ( grabbed == 'g' );
			dataid++;
			// Get the " "
			st.nextToken();
			
			// Read each object information in the teams
			while(st.nextToken().charAt(0) == Packet.OPEN_TOKEN) {
			
				Player obj = new Player();
				
				// Get the side info.
				obj.setSide(st.nextToken().charAt(0));
				dataid++;
				// Get the " "
				st.nextToken();
				
				obj.setId(Integer.parseInt(st.nextToken()));
				dataid++;
				// Get the " "
				st.nextToken();   	
				
				// Get the position and facing
				x = Double.parseDouble(st.nextToken()) / 100;
				dataid++;
				// Get the " "
				st.nextToken();  	
				
				y = Double.parseDouble(st.nextToken()) / 100;
				obj.getPosition().setXY(x,y);
				dataid++;
				// Get the " "
				st.nextToken();  
				
				obj.setDirection(Double.parseDouble(st.nextToken()));	
				dataid++;
				// Get the " "
				st.nextToken();  
				
				obj.setUserControlled(Boolean.parseBoolean(st.nextToken()));
				
				// add it to its team
				if(obj.getSide() == 'l') 
					leftTeam.addElement(obj);
				else 
					rightTeam.addElement(obj);
				
				// Get the " "
				st.nextToken();      
				
				// Get the ")"
				st.nextToken(); 
				
				// Get the " "
				st.nextToken();       
			}
		} 
		catch ( Exception e ) {
			// we get here when the packet is truncated by the sender if 
			// the limit on its length is exceeded. 
			System.out.println("Error in SeeData.readData(" + e );
			System.out.println("dataid = " + dataid + "  st = " );
			System.out.println();
		}
	}
	
	// Stream its data content to a string.
	public void writeData(StringBuffer sb) {
		
		// send packet name and time
		sb.append(Packet.SEE);
		sb.append(' ');
		sb.append(time);
		sb.append(' ');
		
		// send the player information (the receiver)
		sb.append(player.getSide());
		sb.append(' ');
		sb.append(player.getId());
		sb.append(' ');
		sb.append((int)(player.getPosition().getX() * 100));
		sb.append(' ');
		sb.append((int)(player.getPosition().getY() * 100));
		sb.append(' ');
		sb.append((int)player.getDirection());
		sb.append(' ');
		sb.append(player.isUserControlled());
		sb.append(' ');
		sb.append(status);
		sb.append(' ');
		
		// send ball information
		sb.append((int)(ball.getPosition().getX() * 100));
		sb.append(' ');
		sb.append((int)(ball.getPosition().getY() * 100));
		sb.append(' ');
		sb.append(ball.controllerType);
		sb.append(' ');
		sb.append(ball.controllerId);
		sb.append(' ');
		char grabbed = 'n';
		if ( ball.isGrabbed )
			grabbed = 'g';
		sb.append( grabbed );
		sb.append(' ');
		
		// add left and team information to buffer in turns for symmetry
		if ( time%2 == 0 ) {
			addTeamInfo( leftTeam, sb );	
		}
		addTeamInfo( rightTeam, sb );	
		if ( time%2 != 0 ) {
			addTeamInfo( leftTeam, sb );	
		}

		
	} 

	private void addTeamInfo( Vector<Player> team, StringBuffer sb ) {
		
		Enumeration<Player> players = team.elements();
		
		while (players.hasMoreElements()) {
			
			Player obj = (Player) players.nextElement();

			sb.append('(');
			sb.append(obj.getSide());
			sb.append(' ');
			sb.append(obj.getId());
			sb.append(' ');
			sb.append((int)(obj.getPosition().getX() * 100));
			sb.append(' ');
			sb.append((int)(obj.getPosition().getY() * 100));
			sb.append(' ');
			sb.append((int)obj.getDirection());
			sb.append(' ');
			sb.append(obj.isUserControlled());
			sb.append(' ');
			sb.append(")");
			sb.append(" ");
		}	
	}

}
