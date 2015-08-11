/* TurnOnSoundAction.java
   
   Copyright (C) 2004  Yu Zhang

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

package soccer.client.action;

import soccer.common.InfoData;
import soccer.common.Packet;

/**
 *
 */
public class TurnOnSoundAction extends AbstractToggleAction {
	
	private static final long serialVersionUID = 8660192283121931424L;
	
	public TurnOnSoundAction() {
		super();
		putValue(NAME, "Sound On");
		//setAccelerator( KeyEvent.VK_G, Event.CTRL_MASK );
		setEnabled(true);
		setToggledOn(true);
	}
	/**
	 * Process the toggle state change action
	 */
	protected void toggleStateChanged() {
		getSoccerMaster().getSoundSystem().setSoundOn(isToggledOn());
		sendInfoPacket(isToggledOn());
	}
	
	private boolean sendInfoPacket( boolean setting ) {
        boolean success = false;
        int onOff = (isToggledOn()) ? 1 : -1;
        InfoData aInfoData = 
        			new InfoData( InfoData.SOUND_ON_OFF, onOff, 0, "" );			
		
		Packet infoToSend = new Packet( Packet.INFO, 
										aInfoData, 
										getSoccerMaster().getAddress(), 
										getSoccerMaster().getPort() );
        
		try{
			System.out.println("sending Packet.INFO = " + infoToSend.writePacket() );
			getSoccerMaster().getTransceiver().send( infoToSend );
			success = true;
		} 
		catch(Exception ie)
		{
			System.out.println("Error sending Packet.INFO " + ie );
		}
		return success;
	}
}
