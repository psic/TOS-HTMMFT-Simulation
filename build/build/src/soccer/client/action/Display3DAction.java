/* Display3DAction.java
   
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


import java.awt.BorderLayout;

import soccer.client.ViewerClientMain;

/**
 *
 */
public class Display3DAction extends AbstractToggleAction {
	
	private static final long serialVersionUID = 2947586949294383155L;

  public Display3DAction() {
    super();
    putValue( NAME, "3D Display On"  );
    //setAccelerator( KeyEvent.VK_G, Event.CTRL_MASK );
	setEnabled( true );	
	setToggledOn(false);
  }

  public Display3DAction(ViewerClientMain sm) {
    super();
    setSoccerMaster(sm);
  }
  
  
  /**
   * The toggle was changed
   */
  protected void toggleStateChanged() {
	getSoccerMaster().setIn3D(isToggledOn());
  	changeView(isToggledOn());
  }
  
  /**
   * A back door access to the change view feature
   * @param is3Dview
   */
  public void changeView(boolean is3Dview) {
	  	if(is3Dview) {
			getSoccerMaster().mainPane.remove(getSoccerMaster().arena2D);
			try {
				getSoccerMaster().mainPane.add(getSoccerMaster().arena3D, BorderLayout.CENTER);
			} catch (Exception e) {
				System.out.println("Unable to create 3D view. "
						+ "\nRestart the application and do not move the window before displaying 3D view."
						+ "\nException: " + e);
			}
		} else {
			getSoccerMaster().mainPane.remove(getSoccerMaster().arena3D);
			getSoccerMaster().mainPane.add(getSoccerMaster().arena2D, BorderLayout.CENTER);
		}
		
		getSoccerMaster().mainPane.invalidate();
		getSoccerMaster().mainPane.validate();	
		getSoccerMaster().mainPane.repaint();  
		setToggledOn(is3Dview);
  	}
  
}
