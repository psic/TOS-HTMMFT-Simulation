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

package soccer.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.IOException;
import java.io.RandomAccessFile;

import java.net.InetAddress;
import java.net.URL;

import java.util.Vector;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import soccer.client.action.AbstractClientAction;
import soccer.client.action.AbstractToggleAction;
import soccer.client.action.ActionMenuItem;
import soccer.client.action.Actions;
import soccer.client.action.CoachForwardAction;
import soccer.client.action.CoachLoadFileAction;
import soccer.client.action.CoachPlayAction;
import soccer.client.action.CoachStepAction;
import soccer.client.action.Display3DAction;
import soccer.client.action.ExitAction;
import soccer.client.action.FastForwardLogPlayAction;
import soccer.client.action.LoadLogAction;
import soccer.client.action.PauseLogPlayAction;
import soccer.client.action.PlayBackLogPlayAction;
import soccer.client.action.PlayGameAction;
import soccer.client.action.PlayLogPlayAction;
import soccer.client.action.RewindLogPlayAction;
import soccer.client.action.SaveSnapshotAction;
import soccer.client.action.SetUpAIAction;
import soccer.client.action.SetUpServerAction;
import soccer.client.action.StopGameAction;
import soccer.client.action.StopLogPlayAction;
import soccer.client.action.ToggleActionMenuItem;
import soccer.client.action.ViewGameAction;
import soccer.client.dialog.ActiveCommand;
import soccer.client.dialog.DialogManager;
import soccer.client.dialog.ViewDialog;
import soccer.client.view.Arena;
import soccer.client.view.Field;
import soccer.client.view.j3d.FieldJ3D;
import soccer.common.ByeData;
import soccer.common.Data;
import soccer.common.DriveData;
import soccer.common.KickData;
import soccer.common.Packet;
import soccer.common.Player;
import soccer.common.SndSystem;
import soccer.common.TOS_Constants;
import soccer.common.Transceiver;
import soccer.common.Util;

import soccer.debugger.DebugTransceiver;
import soccer.debugger.DebuggerRun;

/**
 * ViewerClientMain is a client application class
 * in the TOS bundle. Several instances of this class may run
 * simultaneously. It allows doing three things:
 * (1) just viewing the game (with some global control capabilities) or
 * (2) both viewing the game and controlling one player by the human.
 * (3) playing back the log file previously recorded during the game.
 *
 * (As of 2010-01-28, options (2) and (3) have not been tested properly yet.)
 * 
 * Several viewer clients can be running simultaneously, on same or 
 * separate computers communicating over a network. The first client that 
 * connects to the soccer server receives Game Controller designation. 
 * Only the Game Controller can change the game mode. All the rest clients 
 * can only be used for viewing the game and/or controlling one player. 
 * Viewer clients can join the game and leave it any time.   
 *
 * An instance of this class is a frame with the soccer field and
 * the controls that allow running the simulation.
 * The frame can display the soccer field in 2D and 3D.
 * The 3D view requires substantially more computing resources than 2D.
 *
 * This class can run in the regular or the debug mode. In the regular
 * mode it is a stand alone application started by method main. It
 * communicates with TOS server over the computer network using threaded
 * processes.
 *
 * In the debug mode, this class is part of the monolithic debugger
 * application integrating all parts of the TOS bundle. Method main is
 * not used in in this mode and different constructor is executed. Threads
 * that are normally invoked in the dialogue classes are not run in the
 * debug mode; rather, non-treaded versions of these objects are created.
 *
 * Known problem.
 * In a two-monitor configuration, 3D view may not be displayed at all.
 * This happens if the attempt is made to open 3D view on the secondary monitor
 * first. If this view is initially opened on the primary monitor, it works
 * on both monitors well. (Has been tested on just one computer only, though.)
 * Possible reasons:
 * (1) the 3D mode is using presumably obsolete Java libraries
 * that work incorrectly with two-monitor configuration.
 * (2) conflict with specific hardware (Dell Inspiron 6400).
 *
 * @author Yu Zhan; Vadim Kyrylov
 *
 */
public class ViewerClientMain extends JFrame implements KeyListener {
	
	private static final long serialVersionUID = 7700997438558437499L;
	public final static String APP_NAME = "Tao Of Soccer - Viewer Client ";

	// this makes a difference between running in the regular mode as a
	// standalone application or in the debug mode as part of the debugger application
	private boolean isDebugMode = false;
	// this is used when running in the debug mode only
	private DebuggerRun debugger;
	// this is used in the debug mode only
	private ViewDialog debugViewDialog;
	
	// if true, this client is Game Coordinator  (see ViewDialog)
	private boolean isCoordinator;
	// viewer ID assigned by server  (see ViewDialog)
	private int viewerID;
	// if true, this client is controlling a soccer player
	private boolean isPlaying = false;
	// if true, permission to control a soccer player is denied
	private boolean isDenied = false;
	private Player controlledPlayer = null; 
	private boolean isSendingCommandAllowed;

	// if true, this Viewer client has been stopped 
	private boolean isClientStopped = false;

	// server and AI processes setup
	public static Runtime runtime = Runtime.getRuntime();
	public static Process serverProcess = null;
	public static int maxCommands = 22; // max commands can be started
	public static Vector<Object> activeCommands = new Vector<Object>(maxCommands);

	//--- server interface threads
	// game viewer client thread for communicating with the server
	private Cviewer viewer = null;
	// log file viewer client thread for communicating with the server
	private Replayer replayer = null;

	// log file
	public RandomAccessFile logFile = null;

	// game interface
	public JPanel mainPane;
	public Field arena2D;
	public Arena arena3D;

	public JLabel leftName;
	public JLabel leftScore;
	public JLabel periodJLabel;
	public JLabel modeJLabel;
	public JLabel timeJLabel;
	public JLabel gameJLabel;

	public JLabel rightName;
	public JLabel rightScore;

	public JTextField replicaJTextField;
	public JTextField input;	// input text field for chat

	// game data
	private ViewerWorld world;
	private int state;

	// networking
	private InetAddress address = null;	// server IP address (see ViewDialog)
	private int port;					// server listening port
	private Transceiver transceiver;

	private boolean in3D = false;	// force showing 3D view first TODO
	private boolean displayID = true;
	private boolean displayMore = true;
	private boolean displayChat = false;
	private boolean showPlayerCoord = false;
	private boolean showBallCoord = false;
	private boolean showVelocity = false;

	private ViewerClientMain frame;	// this frame
	private Actions m_actions;
	private DialogManager m_dlgManager;
	private SndSystem m_soundSystem;

    private JWindow splashScreen = null;
    private JLabel splashLabel = null;

    private JToolBar jtb;
	private int stepBtnIdx;	// index of Step Button/Action (it is special)
	private JMenuBar jmb;


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
		new ViewerClientMain();
	}

	/**
	 * Default constructor (is used in the regular mode).
	 * When this frame is about to close, the whole TOS bundle is terminated
	 * if only this client is Game Coordinator
	 */
	public ViewerClientMain() {
		
		super(APP_NAME + TOS_Constants.APP_VERSION);
		
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); 		
		this.frame = this;	// make it visible from the inner listener class

        WindowListener exitListener = new WindowAdapter() {  
            @Override 
            public void windowClosing(WindowEvent e) { 
            	// run exit dialog and update isClientStopped
    			frame.getAction(
    					(Class<?>) StopGameAction.class).actionPerformed(null);   			
            	if (frame.isClientStopped)	{
            		System.out.println("Closing server and AI player applications");
                	frame.dispose();	// close the window
            	} else {
                	// do not close the window
                	System.out.println("NO selected");
            	}
            }
            
            @Override 
            public void windowClosed(WindowEvent e) { 
            	if (frame.isCoordinator()) {
					// destroy processes, if any started from this application
	            	System.out.println("Window closed, destroying processes, exit");
					//quit(ByeData.TERMINATE);
            	} else {
					// just disconnect from server
	            	System.out.println("Disconnecting Viewer client from server, exit");
					//quit(ByeData.DISCONNECT); 
					System.exit(0);
            	}
           }
        }; 
        
        this.addWindowListener(exitListener);		
		this.initGUI();
		this.addKeyListener(this); 	// make it receiving key events 
		
	} // end constructor


	/**
	 * Constructor (is used in the debug mode only);
	 * the class instance becomes part of the debugger application.
	 * Closing the viewer client window terminates the whole application.
	 *
	 * @param debug - used only to distinguish from the default constructor
	 */
	public ViewerClientMain(DebuggerRun debugger) {
		
		super(APP_NAME + TOS_Constants.APP_VERSION + " DEBUG MODE");
		
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); 		
		
        WindowListener exitListener = new WindowAdapter() {  
            @Override 
            public void windowClosed(WindowEvent e) {
            	System.exit(0);
            }
        };
		
        this.addWindowListener(exitListener);		
		
		this.isDebugMode = true;
		this.debugger = debugger;
		initGUI();

		// connect to server
		ViewGameAction myaction = new ViewGameAction();
		myaction.setSoccerMaster(this);
		myaction.actionPerformed(null);
		// create dummy dialog object
		debugViewDialog = new ViewDialog(null, this);		
	}

	/**
	 * This method is used in the debug mode only.
	 * It moves forward the game period by emulating pressing Forward button
	 */
	public void debugForwardPeriod() {
		CoachForwardAction  myaction = new CoachForwardAction();
		myaction.setSoccerMaster(this);
		myaction.actionPerformed(null);
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

		runSplashScreen(true);
		setupSoundSystem();

		boolean isServer = false;
		if (isDebugMode) {
			int myport = 100;
			// use debug transceiver
			transceiver = new DebugTransceiver(isServer, myport, debugger); 	
		} else
			transceiver = new Transceiver(isServer);	// regular transceiver

		m_actions = new Actions(this);
		m_dlgManager = new DialogManager(this);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {

		}
 		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		getContentPane().setLayout(new BorderLayout());

		setupMenus();
		setupToolbar();
		setupMainPane();
		state = TOS_Constants.INIT;

		this.setEnabled();
		//this.setSize(894, 736);
		//this.setSize(TOS_Constants.DISPLAYWIDTH, TOS_Constants.DISPLAYHEIGHT);
		this.pack();
		this.setResizable(false);
		this.setVisible(true);
	    runSplashScreen(false);
	    this.requestFocus();	
	}
	

	/**
	 * Set up the main window panel.
	 */
	private void setupMainPane() {
		
		JPanel statusPane;
		JPanel leftStatusPanel;
		JPanel gameStatusPanel;
		JPanel timePane;
		JPanel rightStatusPanel;

		// Create mainPane
		mainPane = new JPanel();
		mainPane.setLayout(new BorderLayout());
		mainPane.setBackground(Color.GRAY);

		// Create arena Pane
        arena2D = new Field(this);
        arena2D.setSize(TOS_Constants.DISPLAYWIDTH, TOS_Constants.FIELDHEIGHT);
		//System.out.println("Field size = " + arena2D.getSize());
        arena3D = new FieldJ3D(this);
        arena3D.setSize(arena2D.getSize());

		// Create status Pane
		statusPane = new JPanel();
		statusPane.setLayout(new GridLayout(1, 4, 3, 3));
		Dimension d = new Dimension(TOS_Constants.DISPLAYWIDTH, TOS_Constants.STATUSPANEHEIGHT);
		statusPane.setSize(d);
		statusPane.setBackground(Color.gray);
		statusPane.setBorder(BorderFactory.createRaisedBevelBorder());

		int fontSize = (int)(0.35*statusPane.getHeight());
		Font aFont = new Font( Font.DIALOG, Font.BOLD, fontSize );
		Font bFont = new Font( Font.DIALOG, Font.BOLD, (int)(1.5*fontSize) );

		// left team status
		leftStatusPanel = new JPanel();
		leftStatusPanel.setLayout(new FlowLayout());
		leftStatusPanel.setBackground(Color.yellow);
		leftStatusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		leftName = new JLabel("Alliance:", SwingConstants.CENTER);
		leftName.setBackground(Color.YELLOW);
		leftName.setForeground(Color.BLACK);
		leftName.setFont( aFont );

		leftScore = new JLabel("0", SwingConstants.CENTER);
		leftScore.setBackground(Color.YELLOW);
		leftScore.setForeground(Color.BLACK);
		leftScore.setFont( bFont );

		leftStatusPanel.add(leftName);
		leftStatusPanel.add(leftScore);

		// general game status

		gameStatusPanel = new JPanel();
		gameStatusPanel.setLayout( new GridLayout(1, 4, 5, 0) );
		gameStatusPanel.setBackground(Color.ORANGE);
		gameStatusPanel.setBorder(BorderFactory.createLoweredBevelBorder());

		periodJLabel = new JLabel("Before Match:", SwingConstants.CENTER);
		periodJLabel.setBackground(Color.GRAY);
		periodJLabel.setForeground(Color.BLACK);
		periodJLabel.setFont( aFont );

		modeJLabel = new JLabel("Before Kick Off:", SwingConstants.CENTER);
		modeJLabel.setBackground(Color.GRAY);
		modeJLabel.setForeground(Color.BLACK);
		modeJLabel.setFont( aFont );

		gameStatusPanel.add(periodJLabel);
		gameStatusPanel.add(modeJLabel);

		timePane = new JPanel();
		timePane.setLayout( new GridLayout(1, 2, 5, 0) );
		timePane.setBackground(Color.ORANGE);
		timePane.setBorder(BorderFactory.createLoweredBevelBorder());

		timeJLabel = new JLabel("00:00", SwingConstants.CENTER);
		timeJLabel.setBackground(Color.GRAY);
		timeJLabel.setForeground(Color.BLACK);
		timeJLabel.setFont( aFont );

		gameJLabel = new JLabel("Game:  0", SwingConstants.CENTER);
		gameJLabel.setBackground(Color.GRAY);
		gameJLabel.setForeground(Color.BLACK);
		gameJLabel.setFont( aFont );
		gameJLabel.setVisible(true);

		timePane.add(timeJLabel);
		timePane.add(gameJLabel);

		// right team status
		rightStatusPanel = new JPanel();
		rightStatusPanel.setLayout(new FlowLayout());
		rightStatusPanel.setBackground(Color.RED);
		rightStatusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		rightName = new JLabel("Empire:");
		rightName.setBackground(Color.RED);
		rightName.setForeground(Color.BLACK);
		rightName.setFont( aFont );

		rightScore = new JLabel("0");
		rightScore.setBackground(Color.RED);
		rightScore.setForeground(Color.BLACK);
		rightScore.setFont( bFont );
		rightStatusPanel.add(rightName);
		rightStatusPanel.add(rightScore);

		statusPane.add(leftStatusPanel);
		statusPane.add(gameStatusPanel);
		statusPane.add(timePane);
		statusPane.add(rightStatusPanel);

		mainPane.add(statusPane, BorderLayout.NORTH);	

		if (!in3D) {
			mainPane.add(arena2D, BorderLayout.CENTER);
		} else {
			mainPane.add(arena3D, BorderLayout.CENTER);
			//mainPane.setSize(arena2D.getSize());
		}

		getContentPane().add(mainPane, BorderLayout.CENTER);	
		//System.out.println("Main pane aded; size = " + mainPane.getSize());
	}


	/**
	 * Set up the main window menus.
	 */
	private void setupMenus() {
		
		jmb = new JMenuBar();
		
		for (int i = 0; i < MenuDefinitions.MENUS.length; i++) {
			JMenu menu = new JMenu((String) MenuDefinitions.MENUS[i][0]);
			for (int j = 1; j < MenuDefinitions.MENUS[i].length; j++) {
				if (MenuDefinitions.MENUS[i][j] == null) {
					menu.addSeparator();
				} else {
					Action a = getAction((Class<?>) MenuDefinitions.MENUS[i][j]);
					if (a instanceof AbstractToggleAction) {
						menu.add(
							new ToggleActionMenuItem((AbstractToggleAction) a));
					} else {
						menu.add(new ActionMenuItem((AbstractClientAction) a));
					}
				}
			}
			jmb.add(menu);
		}
		setJMenuBar(jmb);	
		//System.out.println("Menu bar added; size = " + jmb.getSize());
	}
	

	/**
	 * Set up the main window toolbar.
	 */
	private void setupToolbar() {
		
		JLabel comm;

		JButton button = null;
		Action a = null;

		jtb = new JToolBar();
		jtb.setFloatable(false);
		jtb.setRollover(true);
		jtb.setBorderPainted(true);

		a = getAction((Class<?>) SetUpServerAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Start Local Server");

		a = getAction((Class<?>) SetUpAIAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Start Local AI players");

		jtb.addSeparator();

		a = getAction((Class<?>) PlayGameAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Pick a Player");

		a = getAction((Class<?>) ViewGameAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Connect and View");

		a = getAction((Class<?>) StopGameAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Release Player/Quit");

		jtb.addSeparator();

		a = getAction((Class<?>) LoadLogAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Load the log file");

		a = getAction((Class<?>) RewindLogPlayAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Rewind the log file");

		//jtb.addSeparator();

		a = getAction((Class<?>) PlayBackLogPlayAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Play Back the log file");

		a = getAction((Class<?>) PauseLogPlayAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Pause the log file");

		a = getAction((Class<?>) PlayLogPlayAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Play the log file");

		a = getAction((Class<?>) FastForwardLogPlayAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Fast forward the log file");

		a = getAction((Class<?>) StopLogPlayAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Stop replaying the log file");

		jtb.addSeparator();

        a = getAction((Class<?>) CoachLoadFileAction.class);
        button = jtb.add(a);
        button.setText(""); //an icon-only button
        button.setToolTipText("Load situation file");

		a = getAction((Class<?>) CoachStepAction.class);
		button = jtb.add(a);
		stepBtnIdx = jtb.getComponentIndex(button); // index of this button must be saved
		//System.out.println("stepBtnIdx = " + stepBtnIdx );
		button.setText(""); //an icon-only button
		button.setToolTipText("Pause/Step forward the Game");

		a = getAction((Class<?>) CoachPlayAction.class);
		button = jtb.add(a);
		button.setText(""); //an icon-only button
		button.setToolTipText("Play the Game");

        a = getAction((Class<?>) CoachForwardAction.class);
        button = jtb.add(a);
        button.setText(""); //an icon-only button
        button.setToolTipText("Forward the Game Period");

        a = getAction((Class<?>) SaveSnapshotAction.class);
        button = jtb.add(a);
        button.setText(""); //an icon-only button
        button.setToolTipText("Save a game snapshot");
		jtb.addSeparator();

		JLabel replicaJLabel = new JLabel("Replica:");
		jtb.add(replicaJLabel);
		replicaJTextField = new JTextField(5);
		replicaJTextField.setEditable(false);
		replicaJTextField.setText( "" ); // not used yet
		jtb.add(replicaJTextField);

		comm = new JLabel("Chat:");
		jtb.add(comm);
		input = new JTextField(25);
		jtb.add(input);
		
		getContentPane().add(jtb, BorderLayout.NORTH);
		//System.out.println("Toolbar aded; size = " + jtb.getSize());
	}


	public void setEnabled() 	{
		getAction((Class<?>) ExitAction.class).setEnabled(true);
		getAction((Class<?>) SetUpServerAction.class).setEnabled(true);
		getAction((Class<?>) SetUpAIAction.class).setEnabled(true);
		getAction((Class<?>) LoadLogAction.class).setEnabled(true);
	}

	/**
	 * Create and throw the splash screen up. Since this will
	 * physically throw bits on the screen, we need to do this
	 * on the GUI thread using invokeLater.
	 */
	private void runSplashScreen(boolean run) 	{
		
        if (run) {
			// create and show the splash screen
        	URL imgURL;
        	if (isDebugMode)
        		imgURL = ViewerClientMain.class.getResource("/imag/debug.png");
        	else
        		imgURL = ViewerClientMain.class.getResource("/imag/splash.png");
	        ImageIcon icon = new ImageIcon(imgURL);
	        splashLabel = new JLabel(icon);
	        splashScreen = new JWindow();
	        splashScreen.getContentPane().add(splashLabel);
	        splashScreen.pack();
	        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	        splashScreen.setLocation(
	        			screenSize.width/2 - splashScreen.getSize().width/2,
	        			screenSize.height/2 - splashScreen.getSize().height/2);

	        // do the following on the gui thread
	        SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	showSplashScreen();
	            }
	        });
        } else {
	        // Remove the splash screen. Note that
	        // we again must do this on the GUI thread using invokeLater.
	        SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	hideSplash();
	            }
	        });
        }
	}


	private void setupSoundSystem() {		
		m_soundSystem = new SndSystem();
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
	 * Get the action instance for the specified action class.
	 *
	 * @param actionClass an action class
	 * @return the stored instance of the specified action.
	 */
	public AbstractClientAction getAction(Class<?> actionClass) {
		return m_actions.getAction(actionClass);
	}

	/**
	 * Get the dialog manager.
	 */
	public DialogManager getDialogManager() {
		if (m_dlgManager == null) {
			m_dlgManager = new DialogManager(this);
		}
		return m_dlgManager;
	}

	/**
	 * Get the sound system.
	 */
	public SndSystem getSoundSystem() {
		if (m_soundSystem == null) {
			m_soundSystem = new SndSystem();
		}
		return m_soundSystem;
	}

	/**
	 * Terminates the client or disconnects it from server. 
	 * Before terminating, this method terminates processes,
	 * if any, launched from this application.
	 */
	public synchronized void quit(char actionType) {
		
		if (serverProcess != null) {
			serverProcess.destroy();
			serverProcess = null;
		}
		if (activeCommands != null) {
			for (int i = 0; i < activeCommands.size(); i++) {
				ActiveCommand ac = (ActiveCommand) activeCommands.elementAt(i);
				ac.getProcess().destroy();
			}
		}
		
		System.out.println("####  Viewer client quits  ####");
		
		// send BYE packet to server
		try {
			sendToServer(Packet.BYE, new ByeData(actionType) );			
		} catch (IOException e1) {
			System.out.println("Error sending Packet.BYE " + e1 );
		}
		this.dispose();
		if (actionType == ByeData.TERMINATE) {
			System.exit(0);
		} else if (actionType == ByeData.DISCONNECT) {
			address = null;
		}

	}
	
	/**
	 * This method sends to server a packet of given type containing 
	 * given data
	 */	
	public void sendToServer(char packetType, Data data) throws IOException {
		if (address == null)
			return;
	//<=====			server not connected yet
		Packet packet =
			new Packet(packetType, data, address, port);
			packet.senderIDdebug = 100;
			transceiver.send(packet);
	}

	/**
	 * @return
	 */
	public boolean isDisplayChat() {
		return displayChat;
	}

	/**
	 * @param b
	 */
	public void setDisplayChat(boolean b) {
		displayChat = b;
	}

	/**
	 * @return
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * @return
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param address
	 */
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * @param i
	 */
	public void setPort(int i) {
		port = i;
	}

	/**
	 * @return
	 */
	public Transceiver getTransceiver() {
		return transceiver;
	}

	/**
	 * @return
	 */
	public boolean isIn3D() {
		return in3D;
	}

	/**
	 * Set the requested view of the arena
	 */
	public void setIn3D(boolean b) {
		in3D = b;
		Display3DAction myaction = new Display3DAction(this);
		myaction.changeView(in3D);
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

    public void showSplashScreen() {
        splashScreen.setVisible(true);		//.show();
    }

    /**
     * pop down the spash screen
     */
    public void hideSplash() {
        splashScreen.setVisible(false);
        splashScreen = null;
        splashLabel = null;
    }

	public JToolBar getJToolBar() {
		return jtb;
	}

	public int getStepBtnIdx() {
		return stepBtnIdx;
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

	public Cviewer getViewer() {
		return viewer;
	}

	public void setViewer(Cviewer viewer) {
		this.viewer = viewer;
	}

	public Replayer getReplayer() {
		return replayer;
	}

	public void setReplayer(Replayer replayer) {
		this.replayer = replayer;
	}

	public boolean isDebugMode() {
		return isDebugMode;
	}

	public DebuggerRun getDebugger() {
		return debugger;
	}

	public ViewDialog getDebugViewDialog() {
		return debugViewDialog;
	}

	public boolean isCoordinator() {
		return isCoordinator;
	}

	public void setCoordinator(boolean isCoordinator) {
		this.isCoordinator = isCoordinator;
	}

	public int getViewerID() {
		return viewerID;
	}

	public void setViewerID(int viewerID) {
		this.viewerID = viewerID;
	}

	public boolean isDenied() {
		return isDenied;
	}

	public void setDenied(boolean isDenied) {
		this.isDenied = isDenied;
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public Player getControlledPlayer() {
		return controlledPlayer;
	}

	public void setControlledPlayer(Player controlledPlayer) {
		this.controlledPlayer = controlledPlayer;
	}

	public void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
	}

	public void setClientStopped(boolean isClientStopped) {
		this.isClientStopped = isClientStopped;
	}
	
	public void setSendingCommandAllowed(boolean isSendingCommandAllowed) {
		this.isSendingCommandAllowed = isSendingCommandAllowed;
	}

	//==========================================================================================
	@Override
	public void keyPressed(KeyEvent e) {
		
		if (!this.isPlaying || controlledPlayer == null || !isSendingCommandAllowed)
			return;
//<=========
		
		//System.out.println("keyPressed KeyEvent " + e);
		
		Player player = world.getPlayer(controlledPlayer.getSide(), controlledPlayer.getId());
		//System.out.println("Player dir = " + player.getDirection());
		
		try {		
			synchronized (world) {
				// set appropriate action command
				switch (e.getKeyCode()) {
				
					case KeyEvent.VK_UP :
						// dash forward
						world.setDashForce(100);
						world.setDashDirection(player.getDirection());
						world.setActionType(TOS_Constants.DRIVE);
					break;
					
					case KeyEvent.VK_DOWN :
						// turn around
						world.setDashForce(100);
						world.setDashDirection( Util.normal_dir(player.getDirection() + 180) );
						world.setActionType(TOS_Constants.DRIVE);
					break;
					
					case KeyEvent.VK_LEFT :
						// turn left
						world.setDashForce(100);
						world.setDashDirection( Util.normal_dir(player.getDirection() + 90) );
						world.setActionType(TOS_Constants.DRIVE);
					break;
					
					case KeyEvent.VK_RIGHT :
						// turn right
						world.setDashForce(100);
						world.setDashDirection( Util.normal_dir(player.getDirection() - 90) );
						world.setActionType(TOS_Constants.DRIVE);
					break;
					
					case KeyEvent.VK_ENTER :
						if (player.getPosition().distance(
								world.getBall().getPosition()) <= TOS_Constants.BALLCONTROLRANGE) {
							// kick the ball
							if (e.isShiftDown())
								world.setDashForce(30);	// used as the kick force
							else 
								world.setDashForce(100);	// used as the kick force
							world.setDashDirection(player.getDirection());
							world.setActionType(TOS_Constants.KICK);
						}
					break;
				}
			}
			// execute action command by sending a data packet
			if (world.getActionType() == TOS_Constants.DRIVE) {
				// send packet to the server
				DriveData driveData = new DriveData(world.getDashDirection(), world.getDashForce(), false);
				try {
					//System.out.println("keyPressed sending DRIVE packet" + driveData);
					this.sendToServer(Packet.DRIVE, driveData);
					Controller.resetDriveFlags();	// cancel the persistent drive, if any
				} catch (IOException e1) {
					System.out.println("keyPressed cannot send DRIVE packet " + e1);
				}
			} else if (world.getActionType() == TOS_Constants.KICK) {
				// send packet to the server
				KickData kickData = new KickData(world.getDashDirection(), world.getDashForce());
				try {
					this.sendToServer(Packet.KICK, kickData);
					Controller.resetChaseKickFlags();	// cancel the persistent chase/kick, if any
				} catch (IOException e1) {
					System.out.println("keyPressed cannot send KICK packet " + e1);
				}
			}

			isSendingCommandAllowed = false;	// make it wait until the next step
			Controller.resetFlags();	// cancel all other commands
			arena2D.repaint();

		} catch (Exception ex) {
			System.out.println("Wrong key press event " + ex);
		}
	}


	@Override
	public void keyReleased(KeyEvent event) {
		//System.out.println("\nKeyEvent: " + event);
	}

	@Override
	public void keyTyped(KeyEvent event) {
		//System.out.println("\nKeyEvent: " + event);
	}	
}
