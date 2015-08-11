/* SoccerMaster.java
   This class presents a gui to player,... it can be run as an application or an applet

   Copyright (C) 2001  Yu Zhang

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

/* This file modifed to fix bug #430844
							jdm, June 7 2001
*/

/* Modified to add functionality of the Arena class that can be instantiated
   with either the Field class or the Rink class.  The Rink class is used if
   the command line parameter "-hockey" is used.
						        jdm, June 7 2001
*/

/* Modified to add Java3D capabilities - the -3D option now starts it with
   a FieldJ3D for its Arena.
   In addition to calling FieldJ3D rather than just Field, this meant making
   the menus 'heavyweight' so that the Java3D part is not rendered over them.

                                fcm, August 29 2001
*/

/* Cleaned up, mainly move actions, dialogs and sounds out of this messy class
								Yu Zhang, Nov 18 2004

	Modifications by Vadim Kyrylov
							January 2006
*/

package com.htmmft.video;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.htmmft.Match;
import com.htmmft.server.HTMMFTSoccerServerMain;


import soccer.client.ViewerClientMain;
import soccer.client.ViewerWorld;
import soccer.common.SndSystem;
import soccer.common.TOS_Constants;

/**
 *
 *Just load a log file play it and save a an avi file
 *
 * @author psic
 *
 */
public class ViewerClientLogMain extends JFrame {
	
	public static final String vidFolder = "./vids/";
	//private static final long serialVersionUID = 7700997438558437499L;
	public final static String APP_NAME = "Tao Of Soccer - Log Viewer Client ";


	// log file viewer client thread for communicating with the server
	private Replayer2D replayer = null;

	// game data
	private ViewerWorld world;
	private int state;

	private boolean displayID = true;
	private boolean displayMore = true;
	private boolean showPlayerCoord = false;
	private boolean showBallCoord = false;
	private boolean showVelocity = false;

	private ViewerClientLogMain frame;	// this frame
	private SndSystem m_soundSystem;
  
 // log file
 	public RandomAccessFile logFile = null;

 	// game interface
 	public JPanel mainPane;
 	public HTMMMFTField arena2D;

 	public JLabel leftName;
 	public JLabel leftScore;
 	public JLabel periodJLabel;
 	public JLabel modeJLabel;
 	public JLabel timeJLabel;
 	public JLabel gameJLabel;

 	public JLabel rightName;
 	public JLabel rightScore;
 	
 	public Color couleur11 = new Color(1);
 	public Color couleur12 = new Color(6);
 	public Color couleur21 = new Color(8);
 	public Color couleur22 = new Color(10);
 	public String LeftName;
 	public String RightName;
 	
 	private Match currentMatch = null;
	/**
	 * This method starts this class as a standalone application.
	 *
	 * @param s - not used
	 */
	public static void main(String s[]) {
		System.out.println("\n ***  Starting " + APP_NAME
				+ TOS_Constants.APP_VERSION + "  *** \n");
		// regular mode as standalone application communicating
		// with the rest of TOS bundle over the network
		new ViewerClientLogMain();
	}

	/**
	 * Default constructor (is used in the regular mode).
	 * When this frame is about to close, the whole TOS bundle is terminated
	 * if only this client is Game Coordinator
	 */
	public ViewerClientLogMain() {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); 		
		this.frame = this;	// make it visible from the inner listener class
		float demi = 0.511f;
		float c11 = 0.223f;
		float c12 = 0.644f;
		float c21 = 0.413f;
		float c22 = 0.835f;
	
		
		//System.out.println("COULEUR 11: " + c11 + " " + c12 + " / " + c21 + " " + c22 + " / " + demi);
		
		couleur11 = Color.getHSBColor(c11, demi, demi);
		couleur12 = Color.getHSBColor(c12, demi , demi);
		couleur21 = Color.getHSBColor(c21, demi, demi);
		couleur22 = Color.getHSBColor(c22, demi, demi);
		
		LeftName = "Tata";
		RightName = "Yoyo";
		this.initGUI();
		
	} // end constructor



	public ViewerClientLogMain(Match currentMatch) {
		this.currentMatch = currentMatch;	
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); 		
		this.frame = this;	// make it visible from the inner listener class
		//System.out.println("COULEUR 1: " +currentMatch.getEquipe1().getcouleur1() + " " + currentMatch.getEquipe1().getcouleur2() + " " + currentMatch.getEquipe2().getcouleur1() + " " + currentMatch.getEquipe2().getcouleur2());
		float demi = 0.5f;
		float c11 = currentMatch.getEquipe1().getcouleur1()/360.0f;
		float c12 = currentMatch.getEquipe1().getcouleur2()/360.0f;
		float c21 = currentMatch.getEquipe2().getcouleur1()/360.0f;
		float c22 = currentMatch.getEquipe2().getcouleur2()/360.0f;
	
		
		//System.out.println("COULEUR 11: " + c11 + " " + c12 + " / " + c21 + " " + c22 + " / " + demi);
		
		couleur11 = Color.getHSBColor(c11, demi, demi);
		couleur12 = Color.getHSBColor(c12, demi , demi);
		couleur21 = Color.getHSBColor(c21, demi, demi);
		couleur22 = Color.getHSBColor(c22, demi, demi);
		
//		couleur11 = new Color (Color.HSBtoRGB(c11, demi, demi));
//		couleur12 = new Color (Color.HSBtoRGB(c12, demi , demi));
//		couleur21 = new Color (Color.HSBtoRGB(c21, demi, demi));
//		couleur22 = new Color (Color.HSBtoRGB(c22, demi, demi));
		
	//	System.out.println("COULEUR 2: " + couleur11.toString() + " " + couleur12.toString() + " / " + couleur21.toString() + " " + couleur22.toString());

		
		LeftName = currentMatch.getEquipe1().getNom();
		RightName = currentMatch.getEquipe2().getNom();
		this.initGUI();
	}

	/**
	 * Creates viewer GUI; because no threads are invoked in this method,
	 * it is equally good for the regular mode and the debug mode.
	 */
	public void initGUI() {
		// set windows icon
		ImageIcon img =
			new ImageIcon(
				ViewerClientMain.class.getClass().getResource("/imag/icon.gif"));
		this.setIconImage(img.getImage());

		//runSplashScreen(true);
		//setupSoundSystem();

		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {

		}
 		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		getContentPane().setLayout(new BorderLayout());

		setupMainPane();
		state = TOS_Constants.INIT;

		//this.setEnabled();
		//this.setSize(894, 736);
		//this.setSize(TOS_Constants.DISPLAYWIDTH, TOS_Constants.DISPLAYHEIGHT);
		this.pack();
		this.setResizable(false);
		//this.setVisible(true);
	    this.requestFocus();	
	    String LogFile = "114";
	    if (currentMatch != null)
	    	LogFile = Integer.toString(currentMatch.getId());
	    
	    try {
			logFile =new RandomAccessFile(HTMMFTSoccerServerMain.logFolder+LogFile, "r");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    replayer = new Replayer2D(this);
	    replayer.start();
	    replayer.setStatus(Replayer2D.PLAY);
	}
	

	/**
	 * Set up the main window panel.
	 */
	private void setupMainPane() {
		
	//	System.out.println("COULEUR :" + couleur11.toString() + " " + couleur12.toString() + " / " + couleur21.toString() + " " + couleur22.toString());
		
		JPanel statusPane;
		JPanel leftStatusPanel;
		//JPanel gameStatusPanel;
		JPanel timePane;
		JPanel rightStatusPanel;

		// Create mainPane
		mainPane = new JPanel();
		mainPane.setLayout(new BorderLayout());
		mainPane.setBackground(Color.GRAY);

		// Create arena Pane
        arena2D = new HTMMMFTField(this);
        arena2D.setSize(TOS_Constants.DISPLAYWIDTH, TOS_Constants.FIELDHEIGHT);


		// Create status Pane
		statusPane = new JPanel();
		statusPane.setLayout(new GridLayout(1, 3, 3, 3));
		Dimension d = new Dimension(TOS_Constants.DISPLAYWIDTH, TOS_Constants.STATUSPANEHEIGHT);
		statusPane.setSize(d);
		statusPane.setBackground(Color.gray);
		//statusPane.setBorder(BorderFactory.createRaisedBevelBorder());

		int fontSize = (int)(0.37*statusPane.getHeight());
		Font aFont = new Font( Font.DIALOG, Font.BOLD, fontSize );
		Font bFont = new Font( Font.DIALOG, Font.BOLD, (int)(1.5*fontSize) );

		// left team status
		leftStatusPanel = new JPanel();
		//leftStatusPanel.setLayout(new FlowLayout());
		leftStatusPanel.setLayout( new GridLayout(1, 2, 2, 0) );
		//leftStatusPanel.setBackground(Color.yellow);
		leftStatusPanel.setBackground(couleur11);
		//leftStatusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		//leftName = new JLabel("Alliance:", SwingConstants.CENTER);
		leftName = new JLabel(LeftName, SwingConstants.CENTER);
		leftName.setBackground(couleur11);
		//leftName.setBackground(couleur11);

		leftName.setForeground(Color.BLACK);
		leftName.setFont( aFont );

		leftScore = new JLabel("0", SwingConstants.CENTER);
		leftScore.setBackground(couleur11);
		leftScore.setForeground(Color.BLACK);
		leftScore.setFont( bFont );

		leftStatusPanel.add(leftName);
		leftStatusPanel.add(leftScore);

		// general game status

//		gameStatusPanel = new JPanel();
//		gameStatusPanel.setLayout( new GridLayout(1, 4, 5, 0) );
//		gameStatusPanel.setBackground(Color.ORANGE);
//		gameStatusPanel.setBorder(BorderFactory.createLoweredBevelBorder());

		periodJLabel = new JLabel("Before Match", SwingConstants.CENTER);
		periodJLabel.setBackground(Color.GRAY);
		periodJLabel.setForeground(Color.BLACK);
		periodJLabel.setFont( aFont );

//		modeJLabel = new JLabel("Before Kick Off:", SwingConstants.CENTER);
//		modeJLabel.setBackground(Color.GRAY);
//		modeJLabel.setForeground(Color.BLACK);
//		modeJLabel.setFont( aFont );

//		gameStatusPanel.add(periodJLabel);
//		gameStatusPanel.add(modeJLabel);

		timePane = new JPanel();
		timePane.setLayout( new GridLayout(1, 2, 5, 0) );
		timePane.setBackground(Color.ORANGE);
		//timePane.setBorder(BorderFactory.createLoweredBevelBorder());

		timeJLabel = new JLabel("00:00", SwingConstants.CENTER);
		timeJLabel.setBackground(Color.GRAY);
		timeJLabel.setForeground(Color.BLACK);
		timeJLabel.setFont( aFont );

		//gameJLabel = new JLabel("Game:  0", SwingConstants.CENTER);
		//gameJLabel.setBackground(Color.GRAY);
		//gameJLabel.setForeground(Color.BLACK);
		//gameJLabel.setFont( aFont );
		//gameJLabel.setVisible(true);
		timePane.add(periodJLabel);
		timePane.add(timeJLabel);
		//timePane.add(gameJLabel);

		// right team status
		rightStatusPanel = new JPanel();
		//rightStatusPanel.setLayout(new FlowLayout());
		rightStatusPanel.setLayout(new GridLayout(1, 2, 2, 0) );
		rightStatusPanel.setBackground(couleur21);
		//rightStatusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		//rightName = new JLabel("Empire:",SwingConstants.CENTER);
		rightName = new JLabel(RightName,SwingConstants.CENTER);
		rightName.setBackground(couleur21);
		rightName.setForeground(Color.BLACK);
		rightName.setFont( aFont );

		rightScore = new JLabel("0",SwingConstants.CENTER);
		rightScore.setBackground(couleur21);
		rightScore.setForeground(Color.BLACK);
		rightScore.setFont( bFont );
		rightStatusPanel.add(rightName);
		rightStatusPanel.add(rightScore);

		statusPane.add(leftStatusPanel);
	//	statusPane.add(gameStatusPanel);
		statusPane.add(timePane);
		statusPane.add(rightStatusPanel);

		mainPane.add(statusPane, BorderLayout.NORTH);	
		mainPane.add(arena2D, BorderLayout.CENTER);
		getContentPane().add(mainPane, BorderLayout.CENTER);	
		//System.out.println("Main pane aded; size = " + mainPane.getSize());
	}


	private void setupSoundSystem() {		
		//m_soundSystem = new SndSystem();
	}	

	//=============  getter and setters  ===============
	public int getGState() {
		return state;
	}

	public void setGState( int newstate ) {
		state = newstate;
	}

	public void setSimulationStep( double simStep ) {
		TOS_Constants.SIM_STEP_SECONDS = simStep;
	}

	/**
	 * @return
	 */
	public boolean isDisplayID() {
		return displayID;
	}

	/**
	 * @param b
	 */
	public void setDisplayID(boolean b) {
		displayID = b;
	}

	/**
	 * @param b
	 */
	public void setDisplayMore(boolean b) {
		displayMore = b;
	}

	public boolean getDisplayMore() {
		return displayMore;
	}

	/**
	 * Get the sound system.
	 */
	public SndSystem getSoundSystem() {
//		if (m_soundSystem == null) {
//			m_soundSystem = new SndSystem();
//		}
//		return m_soundSystem;
		return null;
	}

	/**
	 * Terminates the client or disconnects it from server. 
	 * Before terminating, this method terminates processes,
	 * if any, launched from this application.
	 */
	public synchronized void quit(char actionType) {
		
		
		System.out.println("####  LOG Viewer client quits  ####");
	}	

	/**
	 * @return
	 */
	public ViewerWorld getWorld() {
		return world;
	}

	/**
	 * @param world
	 */
	public void setWorld(ViewerWorld world) {
		this.world = world;
	}

	public void setShowPlayerCoord( boolean show ) {
		showPlayerCoord = show;
	}

	public boolean isShowPlayerCoord() {
		return showPlayerCoord;
	}

	public void setShowBallCoord( boolean show ) {
		showBallCoord = show;
	}

	public boolean isShowBallCoord() {
		return showBallCoord;
	}

	public boolean isShowVelocity() {
		return showVelocity;
	}

	public void setShowVelocity(boolean showVelocity) {
		this.showVelocity = showVelocity;
	}

	public Match getMatch() {
		return currentMatch;
	}
	


}
