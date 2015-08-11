/* CoachPlayAction.java
   
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
								
	Modifications by Vadim Kyrylov 
							January 2006
*/

package soccer.client.action;

//import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import soccer.client.ViewerClientMain;
import soccer.common.Packet;
import soccer.common.PeriodData;

import java.io.IOException;

public class CoachPlayAction extends AbstractClientAction {
	
	private static final long serialVersionUID = -5407001146183213456L;
	private ImageIcon pauseIcon;
	
	
	public CoachPlayAction() {
		super();
		putValue(NAME, "Start/Resume the game");
		URL imgURL = ViewerClientMain.class.getResource("/imag/cplay.gif");
		ImageIcon playIcon = new ImageIcon(imgURL);
		putValue(SMALL_ICON, playIcon);
		//setAccelerator(KeyEvent.VK_Q, Event.CTRL_MASK);
		setEnabled(false);
			
		imgURL = ViewerClientMain.class.getResource("/imag/cpause.gif");
		pauseIcon = new ImageIcon(imgURL);
	}

	
	public void actionPerformed(ActionEvent e) {
		
		// disable the Play Game button (self)
		getSoccerMaster().getAction((Class<?>) CoachPlayAction.class).setEnabled(false);

		// disable the Load button 
		getSoccerMaster().getAction((Class<?>) CoachLoadFileAction.class).setEnabled(false);
		
		// disable the Save button 
		getSoccerMaster().getAction((Class<?>) SaveSnapshotAction.class).setEnabled(false);

		// enable the Forward button 
		getSoccerMaster().getAction((Class<?>) CoachForwardAction.class)
							.setEnabled( getSoccerMaster().isCoordinator() );					

		// enable the Step button and change its icon (using special access to this control)
		JToolBar jtb = getSoccerMaster().getJToolBar();
		JButton aJButton;  
		aJButton = (JButton)jtb.getComponentAtIndex( getSoccerMaster().getStepBtnIdx() ); 
		aJButton.setEnabled(true);
		aJButton.setIcon( pauseIcon );

		//----- start/resume game ----
		
		PeriodData serverControl = new PeriodData(PeriodData.PLAY);
		Packet command = new Packet(Packet.PERIOD, 
									serverControl, getSoccerMaster().getAddress(), 
									getSoccerMaster().getPort());
		try{
			getSoccerMaster().getTransceiver().send(command);
		} catch(IOException ie) { 
			System.out.println("CoachPlayAction error sending command " + ie);
		}
		getSoccerMaster().requestFocus();	// allow receiving key events
	}
}
