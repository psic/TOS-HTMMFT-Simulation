/** InitData.java
 * 
 * Provides initialization data for server informing the newly connected client.
 *
 * @author Yu Zhang
 */


package soccer.common;

import java.util.*;


public class InitData implements Data {

  public static final char LEFT       		= 'l';
  public static final char LEFT_COACH       = 'm';  
  public static final char RIGHT      		= 'r';
  public static final char RIGHT_COACH      = 's';  
  public static final char USER       		= 'u';
  public static final char USER_DENIED 		= 'd';
  public static final char VIEWER     		= 'v';
  public static final char VIEWER_COACH     = 'w';  
  /**
   * Initialization token 'f' means that the client is denied of connection because there
   * is no available connection.
   */
  public static final char FULL       = 'f';  

  /**
   * Describes the type of client. If no connection available, 'f' is put here.
   */
  public char clientType;

  /**
   * the ID number assigned to the client.
   */
  public int num;
  
  /**
   * the client role (see class ConnectData).
   */
  public char role;

 /**
   * the server duration of the simulation step
   */
  public double simStepInSeconds;

  /**
   * the maximal number of steps goalie allowed to grab the ball
   */
  public int maxGrabSteps = 0;

  /**
   * Constructs an empty InitData for reading from an UDP packet.
   */
  public InitData() {
	this.clientType = ' ';
    num = 0;
  } 
  
  /**
   * Constructs an InitData for writing to an UDP packet.
   *
   * @param ct client type.
   * @param num player number.
   */

  public InitData(char ct, int num, char role, 
		  			double simStepInSeconds, int maxGrabSteps) {
	this.clientType = ct;
    this.num = num;
    this.role = role;
    this.simStepInSeconds = simStepInSeconds;
    this.maxGrabSteps = maxGrabSteps;
  }

  // a legacy constructor
   public InitData(char ct, int num, 
		   			double simStepInSeconds, int maxGrabSteps) {
	this.clientType = ct;
    this.num = num;
    this.role = ConnectData.ROLE_UNKNOWN;
    this.simStepInSeconds = simStepInSeconds;
    this.maxGrabSteps = maxGrabSteps;
  }

  // a legacy constructor
  public InitData(char ct, int num ) {
	this.clientType = ct;
    this.num = num;
    this.role = ConnectData.ROLE_UNKNOWN;
    this.simStepInSeconds = TOS_Constants.SIM_STEP_SECONDS;
    this.maxGrabSteps 
    	= (int)(TOS_Constants.MAX_GRABBED_TIME/TOS_Constants.SIM_STEP_SECONDS);
  }

  
  // Load its data content from a string.
  public void readData(StringTokenizer st) {
	  
    // Get the connection type.
    clientType = st.nextToken().charAt(0);
    
    // Get the " "
    st.nextToken();          

    // Get the player/viewer number.
    num = Integer.parseInt(st.nextToken()); 
       
    // Get the " "
    st.nextToken();          

    // Get the role.
    role = st.nextToken().charAt(0);
    
    // Get the " "
    st.nextToken(); 
    
   // Get the simulation step duration
    simStepInSeconds = ( Integer.parseInt(st.nextToken()) )/1000.0;    
    //System.out.println("Receiving heartRate = " + heartRate );

    // Get the " "
    st.nextToken();          

    // Get the number of steps
    maxGrabSteps = Integer.parseInt(st.nextToken()); 
       
  } 
  
  // Stream its data content to a string  heartRate
  public void writeData(StringBuffer sb)  {
    sb.append(Packet.INIT);
    sb.append(' ');
    sb.append(clientType);
    sb.append(' ');
    sb.append(num);  
    sb.append(' ');
    sb.append(role);  
    sb.append(' ');
    sb.append((int)(simStepInSeconds*1000));  
    sb.append(' ');
    sb.append(maxGrabSteps);  
    //System.out.println("Sending heartRate = " + heartRate + " sb = " + sb );
  } 
  
}
