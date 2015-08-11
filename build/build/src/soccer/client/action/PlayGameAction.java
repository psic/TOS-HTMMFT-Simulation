/** PlayGameAction.java
 * 
 * This class specifies client action to initialize 
 * a human controlled soccer player. 
 * 
 *   Copyright (C) 2004  Yu Zhang
 *
*/

package soccer.client.action;

import java.awt.event.ActionEvent;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import soccer.client.ViewerClientMain;
import soccer.common.TOS_Constants;

public class PlayGameAction extends AbstractClientAction {
	
	private static final long serialVersionUID = 1482344498655236140L;
	
	public PlayGameAction() {
		super();
		putValue(NAME, "Pick a Player");
		URL imgURL = ViewerClientMain.class.getResource("/imag/soccer.gif");
		ImageIcon icon = new ImageIcon(imgURL);
		putValue(SMALL_ICON, icon);
		//setAccelerator(KeyEvent.VK_Q, Event.CTRL_MASK);
		setEnabled(false);
	}
	
	public void actionPerformed(ActionEvent e) {
		if ( getSoccerMaster().getGState() == TOS_Constants.WAITING ) {
			JOptionPane.showMessageDialog(getSoccerMaster(),
					"Game Coordinator must resume simulation before you select a player", 
					"Error",
					JOptionPane.WARNING_MESSAGE);
			getSoccerMaster().requestFocus();	// allow receiving key events
		} else {
			getSoccerMaster().getDialogManager().getPlayDialog().display();
		}
	}
}
