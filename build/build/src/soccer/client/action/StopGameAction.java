/**
 *  StopGameAction.java
 *  
 *  This action stops participation in the game by this viewer client.
 *  If this client is controlling a player, its control is passed back to AI.
 *  If this client is Game Coordinator, leaving the game may result in 
 *  sending the command to server to terminate all TOS applications.  
 *  
 *  Copyright (C) 2004  Yu Zhang
 *  Modifications by Vadim Kyrylov (since 2005)
*/

package soccer.client.action;

import java.awt.event.ActionEvent;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import soccer.client.ViewerClientMain;
import soccer.common.ByeData;
import soccer.common.TOS_Constants;

public class StopGameAction extends AbstractClientAction {

	private static final long serialVersionUID = -3234634334328950082L;
	
	public StopGameAction() {
		super();
		putValue(NAME, "Leave Playing/Viewing Game");
		URL imgURL = ViewerClientMain.class.getResource("/imag/stop.gif");
		ImageIcon icon = new ImageIcon(imgURL);
		putValue(SMALL_ICON, icon);
		//setAccelerator(KeyEvent.VK_Q, Event.CTRL_MASK);
		setEnabled(true);
	}
	
	
	public void actionPerformed(ActionEvent e) {
		
		int option = JOptionPane.NO_OPTION;
		//System.out.println("getSoccerMaster().isPlaying() = " + getSoccerMaster().isPlaying());

		if (getSoccerMaster().isPlaying()) {
			// give chance to release player without closing application
			option = JOptionPane.showConfirmDialog(
					getSoccerMaster(),
					"Do you want just to release the player?",
					ViewerClientMain.APP_NAME,
					JOptionPane.YES_NO_OPTION);	
			if (option == JOptionPane.YES_OPTION) {
				getSoccerMaster().setPlaying(false);
				// send release message
				if (getSoccerMaster().getViewer() != null) {
					getSoccerMaster().getViewer().end(ByeData.RELEASE);
				}
				getSoccerMaster().requestFocus();	// allow receiving key events
				return;
//<=============
			}
		}
		
		getSoccerMaster().setClientStopped(false);
		
		option = JOptionPane.NO_OPTION;
		
		if (getSoccerMaster().isCoordinator()) {
			option = JOptionPane.showConfirmDialog(
				getSoccerMaster(),
				"This will terminate the whole TOS bundle.\n"
						+ "Are you sure?",
				ViewerClientMain.APP_NAME,
				JOptionPane.YES_NO_OPTION);		
		
			if ( getSoccerMaster().getGState() == TOS_Constants.WAITING ) {
				JOptionPane.showMessageDialog(getSoccerMaster(),
						"Game Coordinator must resume simulation before quitting", 
						"Error",
						JOptionPane.WARNING_MESSAGE);
				getSoccerMaster().requestFocus();	// allow receiving key events
				return;
//<=============			
			}
		
		} else {
			// regular Viewer Client
			option = JOptionPane.showConfirmDialog(
					getSoccerMaster(),
					"Do you really want to leave?",
					ViewerClientMain.APP_NAME,
					JOptionPane.YES_NO_OPTION);		
		}

		if (option == JOptionPane.YES_OPTION) {
			
			// terminate the local viewer thread, if any, gracefully
			if (getSoccerMaster().getViewer() != null) {
				getSoccerMaster().getViewer().end(ByeData.DISCONNECT);
				getSoccerMaster().setViewer(null);
			}
			
			// restore the initial states of controls
			getSoccerMaster().getAction(
					(Class<?>) SetUpServerAction.class).setEnabled(true);					
			getSoccerMaster().getAction(
					(Class<?>) SetUpAIAction.class).setEnabled(true);					
			getSoccerMaster().getAction(
					(Class<?>) CoachLoadFileAction.class).setEnabled(false);
			getSoccerMaster().getAction(
					(Class<?>) CoachStepAction.class).setEnabled(false);
			getSoccerMaster().getAction(
				(Class<?>) CoachPlayAction.class).setEnabled(false);
			getSoccerMaster().getAction(
				(Class<?>) PlayGameAction.class).setEnabled(false);
			getSoccerMaster().getAction(
				(Class<?>) ViewGameAction.class).setEnabled(true);
			getSoccerMaster().getAction(
				(Class<?>) LoadLogAction.class).setEnabled(true);
			getSoccerMaster().getAction(
					(Class<?>) SaveSnapshotAction.class).setEnabled(false);
			
			// update the arena
			if (getSoccerMaster().isIn3D()) {
				getSoccerMaster().arena3D.setWorld(null);
				getSoccerMaster().arena3D.repaint();
			} else {
				getSoccerMaster().arena2D.setWorld(null);
				getSoccerMaster().arena2D.repaint();
			}

			if (getSoccerMaster().isCoordinator()) {
				//confirm closing the Viewer Client window
				getSoccerMaster().setClientStopped(true);
				// terminate processes, if any started from within this application
				getSoccerMaster().quit(ByeData.TERMINATE);
			} else {
				// terminate processes, if any started from within this application
				getSoccerMaster().quit(ByeData.DISCONNECT);				
			}
						
			getSoccerMaster().setCoordinator(false);
			getSoccerMaster().setTitle(ViewerClientMain.APP_NAME 
					+ TOS_Constants.APP_VERSION);
		}
		getSoccerMaster().requestFocus();	// allow receiving key events
	}
	
}
