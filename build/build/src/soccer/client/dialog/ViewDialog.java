/**
 * ViewDialog.java 
 * 
 * This class opens a dialog to get the required information
 * for viewing the game.
 * If this viewer client is first to connect with the server, 
 * it automatically assumes the Game Coordinator role that 
 * allows controlling the game. 
 * 
 * Copyright (C) 2001 Yu Zhang
 *
 *	Modifications by Vadim Kyrylov  
 *  2006-2012
 */

package soccer.client.dialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import soccer.client.action.*;
import soccer.client.Cviewer;
import soccer.client.CviewerThread;
import soccer.client.ViewerClientMain;
import soccer.common.ConnectData;
import soccer.common.InitData;
import soccer.common.Packet;
import soccer.common.TOS_Constants;


@SuppressWarnings("serial")
public class ViewDialog extends JPanel implements ActionListener {

	private ViewerClientMain m_client;
	private DialogManager m_dlgManager;
	private JDialog m_dialog;
	private JTextField hostName;
	private JTextField portNum;
	private JCheckBox coachButton;
	
	// this is used for running threads in the regular mode
	private ExecutorService executor; 
	
	private int count = 1;
	private int limit = 10;
	
	
	public ViewDialog(DialogManager mgr, ViewerClientMain soccerMaster) {
		m_client = soccerMaster;
		m_dlgManager = mgr;
		// create service process for executing threads
		executor =  Executors.newCachedThreadPool();
		setupViewPanel();
	}

	private void setupViewPanel() {

		setBorder(BorderFactory.createLoweredBevelBorder());
		TitledBorder title;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// get coach ability
		JPanel coachP = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Coach Ability:");
		coachP.setBorder(title);
		coachButton = new JCheckBox("Server Controls", true);
		coachP.add(coachButton);
		add(coachP);
		add(Box.createVerticalGlue());

		// get server name
		JPanel nameP = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Server IP address:");
		nameP.setBorder(title);
		hostName = new JTextField(20);
		hostName.setText(TOS_Constants.SERVER_ADDRESS);
		hostName.setActionCommand("Host");
		hostName.addActionListener(this);
		nameP.add(hostName);
		add(nameP);
		add(Box.createVerticalGlue());

		// get server port
		JPanel portP = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Server port:");
		portP.setBorder(title);
		portNum = new JTextField(20);
		portNum.setText(TOS_Constants.SERVER_PORT + "");
		portNum.setActionCommand("Port");
		portNum.addActionListener(this);
		portP.add(portNum);
		add(portP);
		add(Box.createVerticalGlue());

		// option buttons
		JButton OK = new JButton("OK");
		OK.setActionCommand("OK");
		OK.addActionListener(this);
		JButton Cancel = new JButton("Cancel");
		Cancel.setActionCommand("Cancel");
		Cancel.addActionListener(this);

		// option panel
		JPanel option = new JPanel();
		option.setLayout(new FlowLayout());
		option.add(OK);
		option.add(Cancel);
		add(option);
		add(Box.createVerticalGlue());

	}

	public void actionPerformed(ActionEvent e) {

		// get the server name and port
		if (e.getActionCommand().equals("OK")) {
			undisplay();
			// link to the server and initialize the player
			init();
		} else if (e.getActionCommand().equals("Cancel"))
			undisplay();
		
		m_client.requestFocus();	// allow receiving key events
	}

	
	/**
	 * Initialize the client and connect to server;
	 * return true if connected successfully.
	 */
	public boolean init() {
		try {
			m_client.setPort(Integer.parseInt(portNum.getText()));
			m_client.setAddress(InetAddress.getByName(hostName.getText()));
			
			//--- Send the connect packet to server ---
			char side = ConnectData.ANYSIDE;
			if (coachButton.isSelected()) {
				side++;
			}
			ConnectData aConnectData 
					= new ConnectData(ConnectData.VIEWER, side);
			m_client.sendToServer(Packet.CONNECT, aConnectData);							

			if (m_client.isDebugMode())
				return true;
//<=============		// stop half-way in debug mode 
			
			//--- wait for INIT message from server and start the thread ---
			m_client.getTransceiver().setTimeout(1000);
			boolean done = false;;
			while (count < limit) {
				if (isINITreceived()) {
					done = true;
					break;
		//<=========
				} else {
					m_client.sendToServer(Packet.CONNECT, aConnectData);
					count++;
				}
			}
			m_client.getTransceiver().setTimeout(0);
			if (!done) {
				JOptionPane.showMessageDialog(m_client,
						"Waiting time expired. Cannot connect to server.", "Error",
						JOptionPane.ERROR_MESSAGE);
				count = 0;
				return false;
			} else {
				return true;	
			}			
		} catch (Exception e) {
			undisplay();
			JOptionPane.showMessageDialog(m_client, e, 
					"Soccer Server may not be running.",
					JOptionPane.ERROR_MESSAGE );
			return false;
		}

	}
	
	/**
	 * This method attempts to receive packet from server.
	 * If packet INIT received, this method starts the viewer 
	 * client thread and returns true; 
	 * if no INIT received, it returns false.
	 * If this viewer client is first to connect to the server, 
	 * it automatically assumes the Game Coordinator role. 
	 */
	public boolean isINITreceived() {
		
		Packet receivedPacket = null;
		
		try {
			receivedPacket = m_client.getTransceiver().receive();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(m_client,
					"Soccer Server may not be running.  Attempt " + count + " of " + limit, 
					"Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (receivedPacket.packetType == Packet.INIT) {
			InitData aInitData = (InitData) receivedPacket.data;
			// synchronize simulation step with that of the server
			m_client.setSimulationStep( aInitData.simStepInSeconds ); 
			if (m_client.isDebugMode()) {
				// thread is not run in the debug mode; rather, it is
				// emulated by the debugger application
				m_client.setViewer(new Cviewer( m_client ));
				
				// disable unnecessary buttons
				m_client.getAction((Class<?>) PlayGameAction.class)
						.setEnabled(false);
				m_client.getAction((Class<?>) ViewGameAction.class)
						.setEnabled(false);
				m_client.getAction((Class<?>) LoadLogAction.class).setEnabled(
						false);
			} else {
				System.out.println("Server assigned client num = " + aInitData.num );
				// regular mode; create a threaded viewer client
				m_client.setViewer(new CviewerThread( m_client ));
				// start the thread
				executor.execute((CviewerThread)m_client.getViewer());	
				if ( aInitData.num > 1 ) {
					m_client.setCoordinator(false);
					m_client.setTitle(ViewerClientMain.APP_NAME 
							+ TOS_Constants.APP_VERSION + "  user #" + aInitData.num);
					// disable unnecessary buttons for the regular viewer
					// (this prevents from starting server and AI players)
					m_client.getAction((Class<?>) SetUpServerAction.class)
							.setEnabled(false);					
					m_client.getAction((Class<?>) SetUpAIAction.class)
							.setEnabled(false);					
					// (this prevents from controlling the game)
					m_client.getAction((Class<?>) CoachStepAction.class)
							.setEnabled(false);					
					m_client.getAction((Class<?>) CoachPlayAction.class)
							.setEnabled(false);					
					m_client.getAction((Class<?>) CoachForwardAction.class)
							.setEnabled(false);					
				} else {
					m_client.setCoordinator(true);
					m_client.setTitle(ViewerClientMain.APP_NAME 
							+ TOS_Constants.APP_VERSION + "  user #" 
							+ aInitData.num + "  GAME COORDINATOR");
					// allow the coordinator controlling the game
					m_client.getAction((Class<?>) CoachPlayAction.class)
							.setEnabled(true);					
				}
				m_client.setViewerID( aInitData.num );
			}
					
			// enable Join/Play  Game control
			m_client.getAction((Class<?>) PlayGameAction.class)
					.setEnabled(true);
			// disable View  Game control
			m_client.getAction((Class<?>) ViewGameAction.class)
					.setEnabled(false);
			// enable leaving the game
			m_client.getAction((Class<?>) StopGameAction.class)
					.setEnabled(true);
			
			m_client.setGState( TOS_Constants.CONNECTED );

			return true;
		}
	
		return false;
	}

	
	public void display() {
		
		if (m_dialog == null) {
			m_dialog = new JDialog((Frame)null, "View TOS", true);
			m_dialog.getContentPane().setLayout(new BorderLayout());
			m_dialog.getContentPane().add(ViewDialog.this, BorderLayout.CENTER);
			m_dialog.setSize(250, 150);
			m_dialog.setResizable(false);

		}

		m_dlgManager.showDialog(m_dialog);
	}

	public void undisplay() {
		m_dlgManager.hideDialog(m_dialog);
		m_client.requestFocus();	// allow receiving key events
	}

}