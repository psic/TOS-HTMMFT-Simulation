/** ConnectData.java
 * 
 * Provides connection data for client connecting to server.
 *
 * @author Yu Zhang 
 * with modification by Vadim Kyrylov 
 * 
*/

package soccer.common;

import java.util.StringTokenizer;


public class ConnectData implements Data {

  /**
   * Connection client Type 
   */
  public char clientType;
  public static final char PLAYER   = 'p';
  public static final char VIEWER   = 'v';

  /**
   * Side type/role.
   */
  public char sideType;
  public static final char ANYSIDE   = 'a';
  public static final char ANYSIDE_COACH   = 'b';  
  public static final char LEFT     = 'l';
  public static final char LEFT_COACH     = 'm';  
  public static final char RIGHT    = 'r';
  public static final char RIGHT_COACH    = 's';
  
  // 'home' position coordinates
  public Vector2d pos = new Vector2d();
  private double x, y;
  
  // team name
  public String teamName = "";

  /**
   * Player role 
   */
  public char playerRole;	
  public static final char COACH    		= 'c';
  public static final char GOALIE    		= 'g';
  public static final char FIELD_PLAYER    	= 'f';
  public static final char FIELD_PLAYER_KICKER    	= 'k';
  public static final char ROLE_UNKNOWN    	= 'n';
  public static final char USER		    	= 'u';	// user controlled player
  
  /**
   * Player ID (optional)
   */
  public int playerID = 0;
  
  /**
   * Constructs an empty ConnectData for reading from an UDP packet.
   */
  public ConnectData() {
    clientType = ' ';
    sideType   = ' ';
    playerRole = FIELD_PLAYER;	// not a goalie by default
  } 

  
  /**
   * Constructs a ConnectData for writing to an UDP packet.
   *
   * @param ct client type.
   * @param st side type.
   */
  public ConnectData(char ct, char st, char role ) {
    clientType = ct;
    sideType = st;
    playerRole = role;
  } 

  public ConnectData(char ct, char st, char role, Vector2d pos ) {
    clientType = ct;
    sideType = st;
    playerRole = role;
    this.pos = new Vector2d( pos );	
  }
   
  public ConnectData(char ct, char st, char role, 
		  				Vector2d pos, String teamName )  {
    clientType = ct;
    sideType = st;
    playerRole = role;
    this.pos = new Vector2d( pos );
    this.teamName = teamName;
  }
   
 /**
   * this is a legacy constructor
   * Constructs a ConnectData for writing to an UDP packet.
   *
   * @param ct client type.
   * @param st side type.
   */
  public ConnectData(char ct, char st ) {
    clientType = ct;
    sideType = st;
    playerRole = ROLE_UNKNOWN;	
  }
    
  // Load its data content from a string.
  public void readData(StringTokenizer st) {
	  
    // Get the connection type.
    clientType = st.nextToken().charAt(0);

    // Get the " "
    st.nextToken();    
    
    // Get the side type.
    sideType = st.nextToken().charAt(0);   

    // Get the " "
    st.nextToken();    
    
    // Get the side type.
    playerRole = st.nextToken().charAt(0);   

    // Get the " "
    st.nextToken();          

    // Get x
    x = ( Integer.parseInt(st.nextToken()) )/100.0;    

    // Get the " "
    st.nextToken();          

    // Get y
    y = ( Integer.parseInt(st.nextToken()) )/100.0;  
    
    pos = new Vector2d( x, y );  
    
    // Get the " "
    st.nextToken();          

    // Get player ID
    playerID = Integer.parseInt(st.nextToken());    

    // Get the " "
    st.nextToken();          

   // Get leftTeamName
    teamName = ( st.nextToken() );  
    
  } 
  
  // Stream its data content to a string.
  public void writeData(StringBuffer sb) {
    sb.append(Packet.CONNECT);
    sb.append(' ');
    sb.append(clientType);
    sb.append(' ');
    sb.append(sideType);
    sb.append(' ');
    sb.append(playerRole);
    sb.append(' ');
    sb.append( (int)(pos.getX()*100) );
    sb.append(' ');
    sb.append( (int)(pos.getY()*100) );
    sb.append(' ');
    sb.append(playerID);
    sb.append(' ');
    sb.append( teamName );
  } 
  
}
