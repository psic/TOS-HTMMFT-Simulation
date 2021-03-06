/* RewindLogPlayAction.java
   
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
import soccer.client.Replayer;
import soccer.client.ViewerClientMain;

public class RewindLogPlayAction extends AbstractClientAction {

	private static final long serialVersionUID = -1243942070351629133L;

	public RewindLogPlayAction() {
		super();
		putValue(NAME, "Rewind the log file");
		URL imgURL = ViewerClientMain.class.getResource("/imag/vrewind.gif");
		ImageIcon icon = new ImageIcon(imgURL);
		putValue(SMALL_ICON, icon);
		//setAccelerator(KeyEvent.VK_Q, Event.CTRL_MASK);
		setEnabled(true);
	}

	public void actionPerformed(ActionEvent e) {
		if (getSoccerMaster().getReplayer() != null)
			getSoccerMaster().getReplayer().setStatus(Replayer.REWIND);
		getSoccerMaster().requestFocus();	// allow receiving key events
	}
}
