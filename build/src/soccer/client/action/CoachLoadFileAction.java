/* CoachLoadFileAction.java 
 *
 * This class implements the file selection dialog 
 * for loading a snapshot of current situation
 * from a text file created beforehand
 *

   Added by Vadim Kyrylov
   January 2006 
   
   
*/

package soccer.client.action;

import java.net.URL;
import javax.swing.ImageIcon;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import soccer.client.dialog.SituationDialog;
import soccer.client.ViewerClientMain;


public class CoachLoadFileAction extends AbstractClientAction {
	
	private static final long serialVersionUID = 8129035119586179924L;

	public CoachLoadFileAction() {
		super();
		putValue(NAME, "Load situation from file");
		URL imgURL = ViewerClientMain.class.getResource("/imag/cload.gif");
		ImageIcon defaultIcon = new ImageIcon(imgURL);
		putValue(SMALL_ICON, defaultIcon);
		
		//setAccelerator(KeyEvent.VK_Q, Event.CTRL_MASK);
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		
    	JFileChooser aJFileChooser = new JFileChooser();
    	
    	// open file selection dialog
        int returnVal = aJFileChooser.showOpenDialog( getSoccerMaster().getJToolBar() );
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File situationFile = aJFileChooser.getSelectedFile();           
            System.out.println("Opening situation file: " + situationFile.getName() );
			
			if ( situationFile == null ||
					situationFile.getName().equals( "" ) ) {
				JOptionPane.showMessageDialog( getSoccerMaster().getJToolBar(),
					"Invalid File Name",
					"Invalid File Name",
					JOptionPane.ERROR_MESSAGE );   
				getSoccerMaster().requestFocus();	// allow receiving key events
				return;
//<=============
			}             
                
            // open dialogue to select the situation parameters
            // and to continue loading the selected file
            SituationDialog aSituationDialog = getSoccerMaster()
            			.getDialogManager().getSituationDialog( situationFile );
            aSituationDialog.display();
        } else {
            System.out.println("Open command cancelled by user." );
        }
		getSoccerMaster().requestFocus();	// allow receiving key events
	}
	
}