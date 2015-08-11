/* ByeData.java

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


package soccer.common;

import java.util.*;

/**
 * This class wraps a packet used by a player/viewer client to inform 
 * the sever that the client is going to leave.
 *
 * @author Yu Zhang
 */
    
public class ByeData implements Data {
	
	public static char TERMINATE 	= 't';	// terminate all TOS applications
	public static char DISCONNECT 	= 'd';	// disconnect sender client
	public static char RELEASE 		= 'r';	// release user controlled player
	
	public char actionType = DISCONNECT;
  
	// default constructor
	public ByeData() {
	} 
	  
	// constructor with Action Type
	public ByeData(char actionType) {
		this.actionType = actionType;
	} 

	// Load its data content from a string.
	public void readData(StringTokenizer st) {
	    // Get action type.
	    actionType = st.nextToken().charAt(0);
	} 
	  
	// Stream its data content to a string
	public void writeData(StringBuffer sb) {
		sb.append(Packet.BYE);
	    sb.append(' ');
	    sb.append(actionType);
	} 
  
}
