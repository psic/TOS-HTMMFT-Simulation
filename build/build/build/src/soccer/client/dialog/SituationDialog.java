/** SituationDialog.java 
 *
 * This class implements a dialog 
 * for repeating any number of times a situation 
 * loaded from a text file created beforehand
 *
 * Added by Vadim Kyrylov
 * January 2006

*/

package soccer.client.dialog;


import java.util.Vector;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import soccer.client.ViewerClientMain;
import soccer.client.ViewerWorld;
import soccer.common.Ball;
import soccer.common.Packet;
import soccer.common.Player;
import soccer.common.RefereeData;
import soccer.common.SituationData;
import soccer.common.ViewData;


public class SituationDialog extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = -9109982926995033097L;

	private DialogManager 	aDialogManager;
	private ViewerClientMain 	aSoccerMaster;
	private File 		 	situationFile;
	private JDialog 		aJDialog;
	private JTextField 		filenameJTextField;
	private JTextField 		replicasJTextField;
	private JTextField 		stepsJTextField;
	
	// times to repeat this situation
	private int 			numOfReplicas = 1;	
	
	// number of steps to run after setting this situation
	private int 			numOfSteps = 100;		

	public SituationDialog(	DialogManager aDialogManager,
							ViewerClientMain aSoccerMaster, 
							File situationFile ) {
		super();
		this.setName("Situation Setup");
		this.aDialogManager = aDialogManager;   
		this.aSoccerMaster = aSoccerMaster;
		this.situationFile = situationFile;
		setupViewPanel();
	}


	private void setupViewPanel() {
		
		setBorder(BorderFactory.createLoweredBevelBorder());
		TitledBorder title;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// display situation file name
		JPanel nameJPanel = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Situation file:");
		nameJPanel.setBorder(title);
		filenameJTextField = new JTextField( situationFile.getName() );
		filenameJTextField.setEditable( false );
		nameJPanel.add(filenameJTextField);
		add(nameJPanel);
		add(Box.createVerticalGlue());

		// get number of replications
		JPanel numJPanel = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Times to repeat:");
		numJPanel.setBorder(title);
		replicasJTextField = new JTextField(4);
		replicasJTextField.setText("1");
		replicasJTextField.setActionCommand("Replicas");
		replicasJTextField.addActionListener(this);
		numJPanel.add(replicasJTextField);
		add(numJPanel);
		add(Box.createVerticalGlue());

		// get number of steps to run
		JPanel stepsJPanel = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Duration in steps:");
		stepsJPanel.setBorder(title);
		stepsJTextField = new JTextField(4);
		stepsJTextField.setText("100");
		stepsJTextField.setActionCommand("Steps");
		stepsJTextField.addActionListener(this);
		stepsJPanel.add(stepsJTextField);
		add(stepsJPanel);
		add(Box.createVerticalGlue());

		// option buttons
		JButton okJButton = new JButton("OK");
		okJButton.setActionCommand("OK");
		okJButton.addActionListener(this);
		JButton cancelJButton = new JButton("Cancel");
		cancelJButton.setActionCommand("Cancel");
		cancelJButton.addActionListener(this);

		// option panel
		JPanel option = new JPanel();
		option.setLayout(new FlowLayout());
		option.add(okJButton);
		option.add(cancelJButton);
		add(option);
		add(Box.createVerticalGlue());

	}

	// process the button click
	public void actionPerformed(ActionEvent e) {
		
		if (e.getActionCommand().equals("OK")) {
			undisplay();
			// read the situation file and update the server
			setSituation();
		} else if (e.getActionCommand().equals("Cancel"))
			undisplay();
		
		aSoccerMaster.requestFocus();	// allow receiving key events
	}


	
		
	// read the file and send data to the server
	private void setSituation() {
		try{
            numOfReplicas = Integer.parseInt( replicasJTextField.getText() ); 
        } catch (NumberFormatException nfe ) { }
            
		try{
            numOfSteps = Integer.parseInt( stepsJTextField.getText() ); 
        } catch (NumberFormatException nfe ) { }

        ViewerWorld aWorld = new ViewerWorld(); 
        aWorld.init();								// set default values
        aWorld.setPreviousMode(RefereeData.PLAY_ON);

		try{
            if ( readFileOK( situationFile, aWorld ) )
            	updateServer( aWorld );				// send new situation -> server
		} catch(IOException ie) {
			System.out.println("Error opening file " + ie );
		}
		
	}

	public void display() {
		if (aJDialog == null) {
			aJDialog = new JDialog((Frame) null, "Situation Setup", true);
			aJDialog.getContentPane().setLayout(new BorderLayout());
			aJDialog.getContentPane().add(SituationDialog.this, BorderLayout.CENTER);
			aJDialog.setSize(250, 150);
			aJDialog.setResizable(false);

		}

		aDialogManager.showDialog(aJDialog);
	}

	public void undisplay() {
		aDialogManager.hideDialog(aJDialog);
		aSoccerMaster.requestFocus();	// allow receiving key events
	}


	/*******************************************************************
	 *
	 *	methods for reading the situation file and updating the server
	 *
	 *******************************************************************/
	
	// this method loads the state of the World from the file
	// returns true if no error occurred
	private boolean readFileOK( File inputFile, ViewerWorld aWorld ) throws IOException {  
		//System.out.println( "Entering readFile()" );	
		
		boolean readingOK = false;
		
		try {
			FileReader frdr = new FileReader( inputFile );
			BufferedReader input = new BufferedReader( frdr );
			boolean done = false;
			boolean isData = false;
			int lineCount = -1;
			int recCount = 0;
			char key = '?';
			
			while ( !done ) {
				String line = input.readLine(); // Get the next line 
				//System.out.println("=== Line read:" + line );
				
				// if there are no more
				if ( line == null || line.equals("") )  // exit the loop when there are no more lines
					break;
				
				if ( line.charAt(0) != '#' ) {		// skip a comment line
				
					line = removeDelimiters( line );
					isData = true; 
					
					if ( line.equals(">> BALL") ) {
						key = 'B'; 
						lineCount = 0;
						isData = false;
					} else if ( line.equals(">> LEFT") ) {
						key = 'L';
						lineCount = 0;
						isData = false;
					} else if ( line.equals(">> RIGHT") ) {
						key = 'R';
						lineCount = 0;
						isData = false;
					}
					
					if ( isData ) {
						done = parseString( line, lineCount, key, aWorld );			// returns true if error
						lineCount++;
						recCount++;
					}
				}
			}
			
			input.close();
			frdr.close();
			System.out.println( "Total records read from file: " + recCount  );	
			readingOK = true;
			
		} catch ( Exception e ) {
			
			JOptionPane.showMessageDialog( aSoccerMaster.getJToolBar(),
					"Unable to read file. \n IOException:"+e, "Error Opening/Reading File",
					JOptionPane.ERROR_MESSAGE );		         
		}  
		
		return readingOK;    	
	}					
		
		
	// this method gets one datum and assigns it to the object attribute
	// @returns true if error
	private boolean parseString( String line, 
								int lineCount, 
								char key, 
								ViewerWorld aWorld ) throws IOException {		
		//System.out.println( "Entering parseString() line = |" + line + "|" + " key = " + key );	
		
		// get rid of the comment on the right of the parameter value
		int spaceidx = line.indexOf(" ");
		line = line.substring( 0, spaceidx ); 		// get the parameter
		//System.out.println( "trimmed line = |" + line + "|" + " lineCount = " + lineCount );	
		
		switch ( key ) {
			
			case 'B':	//---------------------------
				
				switch ( lineCount ) {
					
					case 0:
						aWorld.getBall().getPosition().setX( Double.parseDouble(line) );
					break;	
					
					case 1:
						aWorld.getBall().getPosition().setY( Double.parseDouble(line) );					
					break;
						
					case 2:
						aWorld.getBall().controllerType = line.charAt(0);
					break;	
					
					case 3:
						aWorld.getBall().controllerId = Integer.parseInt(line);
					break;	
					
					case 4:
						aWorld.getBall().isGrabbed = Boolean.getBoolean( line );
					break;	
					
					default:
						System.out.println("Error parsing BALL line = " + line );
						throw new IOException();
				}
				
			break;

			default:
				parseTeamString( line, lineCount, key, aWorld ); 				
		}
		return false;		
		
	} 	

	// this method gets one datum and assigns it to the Player attribute
	// @returns true if error
	private boolean parseTeamString( String line, 
								int lineCount, 
								char key, 
								ViewerWorld aWorld ) throws IOException {
		//System.out.println( "Entering parseTeamString() line = |" + line + "|"  + " key = " + key + " lineCount = " + lineCount );	

		int numOfPlayers;
		Vector<?> team;
		String keyword;
		char side;
		
		if ( key == 'L' ) {
			numOfPlayers = aWorld.getLeftTeam().size();
			keyword = "LEFT";
			team = aWorld.getLeftTeam();
			side = 'l';
		} else if ( key == 'R' ) {
			numOfPlayers = aWorld.getRightTeam().size();
			keyword = "RIGHT";
			team = aWorld.getRightTeam();
			side = 'r';
		} else {
			System.out.println("Error parsing line = " 
				+ line + " key must be either LEFT or RIGHT" );
			throw new IOException();
		}
			
		if ( lineCount >= numOfPlayers*4 ) {
			System.out.println("Error parsing " + keyword + " Too many lines; line = " 
				+ line + " lineCount = " + lineCount + " numOfPlayers = " + numOfPlayers );
			throw new IOException();
		}
		
		Player player = (Player)team.elementAt( (int)(lineCount/4) );
		
		int idx = lineCount%4; 
		
		switch ( idx ) {
			case 0:
				player.setId(Integer.parseInt(line));
				player.setSide(side);
			break;	
			
			case 1:
				player.getPosition().setX( Double.parseDouble(line) );					
			break;
				
			case 2:
				player.getPosition().setY( Double.parseDouble(line) );					
			break;	
			
			case 3:
				player.setDirection(Double.parseDouble(line));
			break;	
								
			default:
				System.out.println("Error parsing  " + keyword + " line = " + line );
				throw new IOException();
		}
		return false; 
	}
	
		
	/** 
	 * this method replaces delimiting characters in the input String 
	 * with spaces and trims the leading and ending spaces
	 * 
	 * @param instr  - input string
	 * @return
	 */  
	private String removeDelimiters( String instr ) {
		/*
		final String space 	= "" + (char)32; 	
		final String tab   	= "" + (char)9; 	
		final String endln  = "" + (char)13; 	
		final String cr   	= "" + (char)10; 
		final String comma 	= "" + (char)44; 
		*/
		String outstr = instr;	
		
		outstr = outstr.replace( (char)9,   (char)32 );			// get rid of tabs, if any
		outstr = outstr.replace( (char)13, (char)32 );			// get rid of endlns, if any
		outstr = outstr.replace( (char)10, 	(char)32 );			// get rid of crs, if any
		outstr = outstr.replace( (char)44,	(char)32 );			// get rid of commas, if any
		
		return outstr.trim();			
	}
		
	// this method updates Sever by sending a VIEW packet 
    public void updateServer( ViewerWorld aWorld ) {
    	
		System.out.println( "Entering updateServer()" );	
		//aWorld.printBall();
		//aWorld.printTeams();
		
		// send the VIEW packet with the ball and up to two players at a time. 
		// total number of packet is 12. 
		
		// figure out how many messages to send
		int sizeL = aWorld.getLeftTeam().size();
		int sizeR = aWorld.getRightTeam().size();
		int size = Math.max( sizeL, sizeR );
		int count = 0;
		
		for ( int i =0; i< size; i++ ) {
			
			Vector<Player> playersL = new Vector<Player>();
			if ( i < sizeL ) {
				Player plr = (Player)aWorld.getLeftTeam().elementAt( i ); 
				playersL.addElement( plr ); 
			}
			
			Vector<Player> playersR = new Vector<Player>();
			if ( i < sizeR ) {
				Player plr = (Player)aWorld.getRightTeam().elementAt( i ); 
				playersR.addElement( plr ); 
			}
			
			if ( !sendViewPacket( aWorld.getBall(), playersL, playersR ) )
				return;
			count++;
		}
		
		System.out.println("Total " + count + " Packet.VIEW sent." );
		
		// this packet just indicates that the file has been loaded and 
		// the server must update clients and start running the situation
		
		int newTime = 1500;	// magic number: new simulation step to start the game over
		
		sendSituationPacket( newTime );
		System.out.println("Packet.SITUATION sent." );
		
    }

	/**
	 * This method sends a VIEW data packet to the viewing client
	 * @param ball		- ball data
	 * @param leftT		- left soccer team data
	 * @param rightT	- right soccer team data
	 * @return			- true if packet sent successfully
	 */
	private boolean sendViewPacket( Ball ball, Vector<Player> leftT, 
											Vector<Player> rightT ) {
		
        boolean success = false;
        
        ViewData aViewData = 
        		new ViewData( 	100, 			// dummy time (is not used, anyway)
        						ball,			
                  				leftT,			// up to one player from left Team 
                  				rightT );		// up to one player from right Team 
		
		Packet infoToSend = new Packet( Packet.VIEW, 
										aViewData, 
										aSoccerMaster.getAddress(), 
										aSoccerMaster.getPort() );
        
		try{
			//System.out.println("sending Packet.VIEW = " + infoToSend.writePacket() );
			aSoccerMaster.getTransceiver().send( infoToSend );
			success = true;
		} catch(IOException ie) {
			System.out.println("Error sending Packet.VIEW " + ie );
		}
		return success;
	}

	
	private boolean sendSituationPacket( int time ) {
        boolean success = false;
        
        SituationData aSituationData = 
        			new SituationData( 	time, 
        								numOfSteps, 		
        								numOfReplicas );			
		
		Packet infoToSend = new Packet( Packet.SITUATION, 
										aSituationData, 
										aSoccerMaster.getAddress(), 
										aSoccerMaster.getPort() );
        
		try{
			//System.out.println("sending Packet.VIEW = " + infoToSend.writePacket() );
			aSoccerMaster.getTransceiver().send( infoToSend );
			success = true;
		} catch(IOException ie) {
			System.out.println("Error sending Packet.SITUATION " + ie );
		}
		return success;
	}
}
