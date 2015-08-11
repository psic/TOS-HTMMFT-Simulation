/* InfoData.java
   
   by Vadim Kyrylov 
   January 2006
*/

package soccer.common;

import java.util.*;

/**
 * Provides feedback info sent by server to clients about its actions
 *
 */
public class InfoData implements Data
{

  /**
   * server command identifiers (info)
   */
  public static final int WHO_ARE_THE_GOALIES  = 10;	// info about who the goalies 
  public static final int WHO_IS_CHASING_BALL  = 15;	// info about who is chasing ball 
  public static final int WAIT_NEXT  = 20;		// game paused
  public static final int RESUME     = 30;		// game resumed
  public static final int REPLICA    = 40;		// a replica of the game is being played
  public static final int BALL_KICK    = 50;	// info about a ball kick
  public static final int BALL_PASS    = 60;	// info about a ball pass
  public static final int SOUND_ON_OFF = 70;	// info about sound on/off toggle seting
  public static final int COLLISION = 80;		// info about player collision

  // info
  public int info;
  public int info1;
  public int info2;
  public int info3;
  public String extraInfo = "";


  /**
   * Constructs an empty InfoData for reading from an UDP packet.
   */
  public InfoData()
  {
    this.info = 0;
    this.info1 = 0;
    this.info2 = 0;
  }
  
  /** 
   * Constructs a InfoData for writing to an UDP packet.
   * @param info the server feedback info
   */
  public InfoData(int info)
  {
    this.info = info;
    this.info1 = 0;
    this.info2 = 0;
  } 

  /** 
   * Constructs a InfoData for writing to an UDP packet.
   * @param info the server feedback info
   * @param info1 the server feedback info
   * @param info2 the server feedback info
  */
  public InfoData(int info, int info1, int info2)
  {
    this.info = info;
    this.info1 = info1;
    this.info2 = info2;
  } 
  
  /** 
   * Constructs a InfoData for writing to an UDP packet.
   * @param info the server feedback info
   * @param info1 the server feedback info
   * @param info2 the server feedback info
   * @param extraInfo the server feedback info
  */
  public InfoData(int info, int info1, int info2, String extraInfo)
  {
    this.info = info;
    this.info1 = info1;
    this.info2 = info2;
    this.extraInfo = extraInfo;
  } 

  // Load its data content from a string.
  public void readData(StringTokenizer st)
  {
    // Get the info.
    info = Integer.parseInt(st.nextToken()); 
    // Get the " "
    st.nextToken();       
    // Get the info2.
    info1 = Integer.parseInt(st.nextToken()); 
    // Get the " "
    st.nextToken();       
    // Get the info2.
    info2 = Integer.parseInt(st.nextToken()); 
    // Get the " "
    st.nextToken();       
    // Get the info2.
    info3 = Integer.parseInt(st.nextToken()); 
    // Get the " "
        st.nextToken();       
    // Get the extraInfo.
    extraInfo = st.nextToken(); 
  } 
  
  // Stream its data content to a string.
  public void writeData(StringBuffer sb)
  {
    sb.append(Packet.INFO);
    sb.append(' ');
    sb.append(info);
    sb.append(' ');
    sb.append(info1);
    sb.append(' ');
    sb.append(info2);
    sb.append(' ');
    sb.append(info3);
    sb.append(' ');
    sb.append(extraInfo);
 } 
  
}
