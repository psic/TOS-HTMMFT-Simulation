/** 
 * PlayDialog.java
 * 
 * This class opens a dialog to get the required information for 
 * joining the game with a user controlled soccer player.
 * 
 * Copyright (C) 2012 Vadim Kyrylov  
 * October 2012
 */

package soccer.client.dialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Vector;

import soccer.client.ViewerClientMain;
import soccer.common.ConnectData;
import soccer.common.Packet;
import soccer.common.Player;


public class PlayDialog extends JPanel implements ActionListener {

	private static final long serialVersionUID = -6848205091394748038L;

	private ViewerClientMain m_client;
	private DialogManager m_dlgManager;
	private JDialog m_dialog;

	private char sideSelected;
	private int playerIDselected = 0;

	
	private JComboBox<String> cbxLeftTeam;
	private String[] leftTeam;
	private JComboBox<String> cbxRightTeam;
	private String[] rightTeam;
	private JButton btnOK = new JButton("OK");

	
	public PlayDialog(DialogManager mgr, ViewerClientMain soccerMaster) {
		m_client = soccerMaster;
		m_dlgManager = mgr;
		setupPlayPanel();
	}
	

	private void setupPlayPanel() {

		setBorder(BorderFactory.createLoweredBevelBorder());
		TitledBorder title;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		btnOK.setEnabled(false);

		// Create the combo boxes
		leftTeam = getPlayerIDs('l');
		cbxLeftTeam = new JComboBox<String>(leftTeam);
		cbxLeftTeam.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				sideSelected = 'l';
				playerIDselected = cbxLeftTeam.getSelectedIndex();
				if (playerIDselected > 0) {
					// some player has been selected;
					// cancel selection on the other combo box
					cbxRightTeam.setSelectedIndex(0);
					btnOK.setEnabled(true);
				} else {
					if (cbxRightTeam.getSelectedIndex() ==0)
						btnOK.setEnabled(false);
				}
			}
		});
		
		rightTeam = getPlayerIDs('r');	
		cbxRightTeam = new JComboBox<String>(rightTeam);
		cbxRightTeam.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				sideSelected = 'r';
				playerIDselected = cbxRightTeam.getSelectedIndex();
				if (playerIDselected > 0) {
					// some player has been selected;
					// cancel selection on the other combo box
					cbxLeftTeam.setSelectedIndex(0);
					btnOK.setEnabled(true);
				} else {
					if (cbxLeftTeam.getSelectedIndex() ==0)
						btnOK.setEnabled(false);
				}				
			}
		});
		
		
		// place combo boxes on panel
		JPanel teamsPanel = new JPanel();
		teamsPanel.setLayout(new FlowLayout());
		teamsPanel.add(cbxLeftTeam);
		teamsPanel.add(cbxRightTeam);

		title = BorderFactory.createTitledBorder("Select team and player:");
		teamsPanel.setBorder(title);
		add(teamsPanel);
		add(Box.createVerticalGlue());

		
		// option buttons
		btnOK.setActionCommand("OK");
		btnOK.addActionListener(this);
		JButton Cancel = new JButton("Cancel");
		Cancel.setActionCommand("Cancel");
		Cancel.addActionListener(this);

		// option panel
		JPanel option = new JPanel();
		option.setLayout(new FlowLayout());
		option.add(btnOK);
		option.add(Cancel);
		add(option);
		add(Box.createVerticalGlue());

	}
	
	/**
	 * This method returns a collection of player IDs in a team 
	 * playing on the given side and are not user controlled.
	 */
	private String[] getPlayerIDs(char side) {
		
		Vector<Player> team;
		String name;
		if (side == 'l') {
			team = m_client.getWorld().getLeftTeam();
			name = "Left team";
		} else {
			team = m_client.getWorld().getRightTeam();			
			name = "Right team";
		}
		Vector<String> myTeam = new Vector<String>();
		myTeam.add(name);
		for (int i=0; i<team.size(); i++) {
			Player p = team.get(i);
			if (!p.isUserControlled())
				myTeam.add("Player " + p.getId());
		}
		String[] myTeamStr = new String[myTeam.size()];
		for (int i=0; i<myTeam.size(); i++)
			myTeamStr[i] = myTeam.get(i);
		
		return myTeamStr;
	}
	
	/**
	 *  Process button pressed events
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("OK")) {
			if (playerIDselected > 0) {
			// connect to server and initialize the player
				undisplay();
				init();
			} else {
				// do nothing
			}
		} else if (e.getActionCommand().equals("Cancel")) {
			// cancel attempt to connect to server
			undisplay();
		}
		m_client.requestFocus();	// allow receiving key events
	}
	

	/**
	 * Attempt to initialize the player agent controlled by user
	 */
	private void init() {
		
		try {
			// Send connect packet to server
			ConnectData connectData = new ConnectData(ConnectData.VIEWER, 
											sideSelected, ConnectData.USER);
			connectData.playerID = playerIDselected;
			m_client.sendToServer(Packet.CONNECT, connectData);

			// wait for the connect message from server received by Cviewer
			m_client.getTransceiver().setTimeout(1000);
			int limit = 10;
			int count = 0;
			while (count < limit && !m_client.isPlaying()) {
				/**
				 *  wait; let Cviewer receive INIT and 
				 *  change m_client.isDenied() or m_client.isPlaying()
				 */
				Thread.sleep(70);
				System.out.println("m_client.isDenied() = " + m_client.isDenied());
				if (m_client.isDenied()) {
					return;
//<=================			
				}
				System.out.println("m_client.isPlaying() = " + m_client.isPlaying());
				if (m_client.isPlaying()) {
					break;
		//<=========			
				}
				m_client.sendToServer(Packet.CONNECT, connectData);
				count++;
			}

			m_client.getTransceiver().setTimeout(0);
			if (!m_client.isPlaying()) {
				JOptionPane.showMessageDialog(
					m_client,
					"Waiting time expired. Cannot get control of a player.",
					"Error",
					JOptionPane.ERROR_MESSAGE);
				return;
//<=============
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(
				m_client,
				e,
				"Error",
				JOptionPane.ERROR_MESSAGE);
			return;
//<=========
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * This method replaces all items in the combo box
	 */
	private void replaceItems(JComboBox<String> cbx, String[] items) {
		cbx.removeAllItems();
		for (int i=0; i<items.length; i++)
			cbx.addItem(items[i]);
	}
	

	public void display() {
		
		m_client.setDenied(false);
		
		if (m_dialog == null) {
			m_dialog = new JDialog((Frame) null, "Pick a Player to Control", true);
			m_dialog.getContentPane().setLayout(new BorderLayout());
			m_dialog.getContentPane().add(PlayDialog.this, BorderLayout.CENTER);
			m_dialog.setSize(250, 200);
			m_dialog.setResizable(false);
		}
		
		// refresh combo box contents
		leftTeam = getPlayerIDs('l');
		replaceItems(cbxLeftTeam, leftTeam);
		rightTeam = getPlayerIDs('r');	
		replaceItems(cbxRightTeam, rightTeam);

		m_dlgManager.showDialog(m_dialog);
	}

	public void undisplay() {
		m_dlgManager.hideDialog(m_dialog);
		m_client.requestFocus();	// allow receiving key events
	}

}
