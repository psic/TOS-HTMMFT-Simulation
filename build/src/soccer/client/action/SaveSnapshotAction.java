/* SaveSnapshotAction.java 
 *
 * This class implements the file selection dialog 
 * for saving a snapshot of current situation
 * to a text file
 *

   Added by Vadim Kyrylov
   January 2006 
   
   
*/

package soccer.client.action;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import soccer.client.ViewerClientMain;
import soccer.client.ViewerWorld;
import soccer.common.Player;

import java.awt.event.ActionEvent;


public class SaveSnapshotAction extends AbstractClientAction {
	
	private static final long serialVersionUID = 1761063940932902888L;

	public SaveSnapshotAction() {
		super();
		putValue(NAME, "Save situation to file ");
		URL imgURL = ViewerClientMain.class.getResource("/imag/csave.gif");
		ImageIcon defaultIcon = new ImageIcon(imgURL);
		putValue(SMALL_ICON, defaultIcon);
		
		//setAccelerator(KeyEvent.VK_Q, Event.CTRL_MASK);
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		try{
        	JFileChooser aJFileChooser = new JFileChooser();
        	
        	// open file selection dialog
            int returnVal = aJFileChooser.showSaveDialog( getSoccerMaster().getJToolBar() );
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File situationFile = aJFileChooser.getSelectedFile();
                System.out.println("Saving situation file: " + situationFile.getName() );
                writeFile( situationFile );
            } else {
                System.out.println("Save command cancelled by user." );
            }

		} catch(IOException ie) {
			System.out.println("Error saving the situation file " + ie );
		}
		getSoccerMaster().requestFocus();	// allow receiving key events		
	}
	
	// this method writes the state of the World into the file
	private void writeFile( File outputFile )throws IOException {  
		FileWriter aFileWriter = new FileWriter(outputFile);
		ViewerWorld aWorld = getSoccerMaster().getWorld();
		
		// write header/comments
		aFileWriter.write("# === A situation snapshot ===\r\n");
		aFileWriter.write("# \r\n");
		aFileWriter.write("#   All lines starting with '#' are comments.\r\n");
		aFileWriter.write("#   You can insert any number of such lines anywhere.\r\n");
		aFileWriter.write("#   An empty line is regarded the end of the file.\r\n");
		aFileWriter.write("#   Data are the items in the beginning of each line \r\n");
		aFileWriter.write("#   (one datum per line followed by optional comments). \r\n");
		aFileWriter.write("#   Lines begining with '>>' are dataset separators (do not remove or edit them). \r\n");
		aFileWriter.write("#   Data values and accompanying comments can be edited by hand. \r\n");
		aFileWriter.write("#\r\n");
		aFileWriter.write("#\r\n");

		// write the Ball state
		aFileWriter.write(">> BALL\r\n");
		aFileWriter.write("  " + aWorld.getBall().getPosition().getX() + "    		X-coordinate \r\n");
		aFileWriter.write("  " + aWorld.getBall().getPosition().getY() + "    		Y-coordinate \r\n");
		aFileWriter.write("  " + aWorld.getBall().controllerType + "    		Controller Type \r\n");
		aFileWriter.write("  " + aWorld.getBall().controllerId + "    		Controller ID \r\n");
		aFileWriter.write("  " + aWorld.getBall().isGrabbed + "    		is Grabbed or not \r\n");
		aFileWriter.write("#\r\n");
		aFileWriter.write("#\r\n");
		
		// the state of the left team
		aFileWriter.write(">> LEFT\r\n");
		aFileWriter.write("#\r\n");

		for ( int i=0; i<aWorld.getLeftTeam().size(); i++ ) {
			Player player = (Player)aWorld.getLeftTeam().elementAt( i );
			aFileWriter.write("  " + player.getId() + "    		The player's number \r\n");
			aFileWriter.write("  " + player.getPosition().getX() + "    		X-coordinate \r\n");
			aFileWriter.write("  " + player.getPosition().getY() + "    		Y-coordinate \r\n");
			aFileWriter.write("  " + player.getDirection() + "    		direction \r\n");
			aFileWriter.write("#\r\n"); 			 	
		}
		
		// the state of the right team
		aFileWriter.write("#\r\n");
		aFileWriter.write(">> RIGHT\r\n");
		aFileWriter.write("#\r\n");

		for ( int i=0; i<aWorld.getRightTeam().size(); i++ ) {
			Player player = (Player)aWorld.getRightTeam().elementAt( i );
			aFileWriter.write("  " + player.getId() + "    		The player's number \r\n");
			aFileWriter.write("  " + player.getPosition().getX() + "    		X-coordinate \r\n");
			aFileWriter.write("  " + player.getPosition().getY() + "    		Y-coordinate \r\n");
			aFileWriter.write("  " + player.getDirection() + "    		direction \r\n");
			aFileWriter.write("#\r\n"); 			 	
		}
			
		aFileWriter.write("\r\n"); 	// this is the end of the file.			 	
		
		aFileWriter.close();
	} 	
	
}
