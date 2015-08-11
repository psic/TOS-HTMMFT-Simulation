/* CoachStepAction.java
   
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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import javax.swing.ImageIcon;
import soccer.client.ViewerClientMain;
import soccer.common.Packet;
import soccer.common.PeriodData;

public class CoachStepAction extends AbstractClientAction {
	
	private static final long serialVersionUID = -8241496537384488944L;

	private JToolBar jtb; 
	JButton aJButton;
	private ImageIcon pauseIcon; 
	private ImageIcon stepIcon; 
	
	public CoachStepAction() {
		super();
		putValue(NAME, "Pause/Step forward");
		URL imgURL = ViewerClientMain.class.getResource("/imag/cstep.gif");
		stepIcon = new ImageIcon(imgURL);
		imgURL = ViewerClientMain.class.getResource("/imag/cpause.gif");
		pauseIcon = new ImageIcon(imgURL);
		
		putValue(SMALL_ICON, pauseIcon);
		setEnabled(false);

	}

	public void actionPerformed(ActionEvent e) {
		
		jtb = getSoccerMaster().getJToolBar();

		// change icon on this button
		aJButton = (JButton)jtb.getComponentAtIndex( getSoccerMaster().getStepBtnIdx() ); 
		aJButton.setIcon( stepIcon );
		
		// enable the Play Game button 
		getSoccerMaster().getAction((Class<?>) CoachPlayAction.class).setEnabled(true);

		// enable the Save button 
		getSoccerMaster().getAction((Class<?>) SaveSnapshotAction.class).setEnabled(true);

		// enable the Load button 
		getSoccerMaster().getAction((Class<?>) CoachLoadFileAction.class).setEnabled(true);
			
		PeriodData serverControl = new PeriodData(PeriodData.STEP);
		Packet command = new Packet(Packet.PERIOD, serverControl, 
									getSoccerMaster().getAddress(), 
									getSoccerMaster().getPort());									
		try{
			getSoccerMaster().getTransceiver().send(command);
		} catch(IOException ie) {
		}
		getSoccerMaster().requestFocus();	// allow receiving key events
	}
}
