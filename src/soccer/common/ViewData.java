/**
 * Provides visual data for viewer clients.
 *
 * Copyright (C) 2001  Yu Zhang
 * Vadim Kyrylov (2006-2010)
 */

package soccer.common;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;


public class ViewData implements Data {  
	/**
	* the current simulation step.
	*/
	public int time;
	
	/**
	* the ball.
	*/
	public Ball ball;  
	/**
	* a list of positions for left team. 
	*/
	public Vector<Player> leftTeam; 
	/**
	* a list of positions for right team. 
	*/
	public Vector<Player> rightTeam; 
	
	
	/**
	* Constructs an empty ViewData for reading from an UDP packet.
	*/
	public ViewData() {
		this.time = 0;
		this.ball = new Ball();
		this.leftTeam = new Vector<Player>();
		this.rightTeam =new Vector<Player>();
	} 
	
	/** 
	* Constructs a ViewData for writing to an UDP packet.
	*
	* @param time the current simulation step.
	* @param ball the ball.
	* @param leftTeam a list of positions for the left team.
	* @param rightTeam a list of positions for the right team.
	*/
	public ViewData(int time, Ball ball, 
				Vector<Player> leftTeam, Vector<Player> rightTeam) {
		this.time = time;
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
			
			// Get ball position x
			x = Double.parseDouble(st.nextToken()) / 100;
			dataid++;
			// Get the " "
			st.nextToken();
			
			// Get ball position y
			y = Double.parseDouble(st.nextToken()) / 100;
			dataid++;
			// set ball position
			ball.getPosition().setXY(x,y);
			
			// Get the " "
			st.nextToken();
			
			// Get ball controller type.
			ball.controllerType = st.nextToken().charAt(0);
			dataid++;
			// Get the " "
			st.nextToken();
			
			// Get ball controller id.
			ball.controllerId = Integer.parseInt(st.nextToken());
			dataid++;
			// Get the " "
			st.nextToken();
			
			
			// Read each object information
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
				
				//--- Get the position and facing direction
				x = Double.parseDouble(st.nextToken()) / 100;
				dataid++;
				// Get the " "
				st.nextToken();  	
				
				y = Double.parseDouble(st.nextToken()) / 100;
				dataid++;
				obj.getPosition().setXY(x,y);
				
				// Get the " "
				st.nextToken();  
				
				// Get direction
				obj.setDirection(Double.parseDouble(st.nextToken()));	
				dataid++;
				// add it to its team
				if(obj.getSide() == 'l') 
					leftTeam.addElement(obj);
				else 
					rightTeam.addElement(obj);
				
				// Get the " "
				st.nextToken();    
				
				// Get user controlled flag
				obj.setUserControlled(Boolean.parseBoolean(st.nextToken()));
				
				// Get the " "
				st.nextToken();  
				
				
				// Get user kicker flag
				obj.setKicker(Boolean.parseBoolean(st.nextToken()));
				
				// Get the " "
				st.nextToken();
				
				boolean isGoal =Boolean.parseBoolean(st.nextToken());
				obj.setGoalie(isGoal);
				// Get user goalie flag
				//obj.setGoalie(Boolean.parseBoolean(st.nextToken()));
				
				// Get the " "
				st.nextToken();
				
				// Get the ")"
				st.nextToken(); 
				
				// Get the " "
				st.nextToken();    
			}
			
		} catch ( Exception e ) {
			// we get here when the packet is trucated by the sender is the limit on its
			// length is exceeded. 
			System.out.println("Error in ViewData.readData(" + e );
			System.out.println("dataid = " + dataid + "  st = " );
			System.out.println();
		}
	}
	
	// Stream its data content to a string.
	public void writeData(StringBuffer sb) {
		Player obj;
		Enumeration<Player> players;
		
		// send packet name and time
		sb.append(Packet.VIEW);
		sb.append(' ');
		sb.append(time);
		sb.append(' ');
		
		// send ball information
		sb.append((int)(ball.getPosition().getX() * 100.0));
		sb.append(' ');
		sb.append((int)(ball.getPosition().getY() * 100.0));
		sb.append(' ');
		sb.append(ball.controllerType);
		sb.append(' ');
		sb.append(ball.controllerId);
		sb.append(' ');

		
		// send left team information, if any
		try {
			players = leftTeam.elements();
			while (players.hasMoreElements()) {
				
				obj = (Player) players.nextElement();
				
				sb.append('(');
				sb.append(obj.getSide());
				sb.append(' ');
				sb.append(obj.getId());
				sb.append(' ');
				sb.append((int)(obj.getPosition().getX() * 100.0));
				sb.append(' ');
				sb.append((int)(obj.getPosition().getY() * 100.0));
				sb.append(' ');
				sb.append((int)obj.getDirection());
				sb.append(' ');
				sb.append(obj.isUserControlled());
				sb.append(' ');
				sb.append(obj.isKicker());
				sb.append(' ');

				boolean isGoal =obj.isGoalie();
				sb.append(obj.isGoalie());
				sb.append(' ');
				sb.append(")");
				sb.append(" ");
			}
		} catch (Exception e ) {
			System.out.println("View packet sent with empty left team");	
		}
			
		// send right team information, if any
		try {
			players = rightTeam.elements();
			while (players.hasMoreElements()) { 
				
				obj = (Player) players.nextElement();
				
				sb.append('(');
				sb.append(obj.getSide());
				sb.append(' ');
				sb.append(obj.getId());
				sb.append(' ');
				sb.append((int)(obj.getPosition().getX() * 100.0));
				sb.append(' ');
				sb.append((int)(obj.getPosition().getY() * 100.0));
				sb.append(' ');
				sb.append((int)obj.getDirection());
				sb.append(' ');
				sb.append(obj.isUserControlled());
				sb.append(' ');
				sb.append(obj.isKicker());
				sb.append(' ');
				sb.append(obj.isGoalie());
				sb.append(' ');
				sb.append(")");
				sb.append(" ");
			}    
		} catch (Exception e ) {
			System.out.println("View packet sent with empty right team");	
		}
	
	} 
	
}
