/* CoachForwardAction.java
   
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

import java.awt.event.ActionEvent;
import java.net.URL;
import javax.swing.ImageIcon;
import soccer.client.ViewerClientMain;
import soccer.common.Packet;
import soccer.common.PeriodData;
import soccer.common.TOS_Constants;

import java.io.IOException;

public class CoachForwardAction extends AbstractClientAction {
	
	private static final long serialVersionUID = 8015805368779755698L;

	public CoachForwardAction() {
		super();
		putValue(NAME, "Forward the game");
		//URL imgURL = SoccerMaster.class.getResource("/imag/cfwd_disabled.gif");
		URL imgURL = ViewerClientMain.class.getResource("/imag/cforward.gif");
		ImageIcon icon = new ImageIcon(imgURL);
		putValue(SMALL_ICON, icon);
		//setAccelerator(KeyEvent.VK_Q, Event.CTRL_MASK);
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		
		if ( getSoccerMaster().getGState() != TOS_Constants.INIT ) {

			// enable the Play Game button 
			getSoccerMaster().getAction((Class<?>) CoachPlayAction.class).setEnabled(true);
			
			// disable the Load button 
			getSoccerMaster().getAction((Class<?>) CoachLoadFileAction.class).setEnabled(false);
	
			PeriodData serverControl = new PeriodData(PeriodData.FORWARD);
			Packet command = new Packet(Packet.PERIOD, 
										serverControl, 
										getSoccerMaster().getAddress(), 
										getSoccerMaster().getPort());
			command.senderIDdebug = 100;
			
			try{
				getSoccerMaster().getTransceiver().send(command);
			} catch(IOException ie) { }	
		}
		getSoccerMaster().requestFocus();	// allow receiving key events
	}
}