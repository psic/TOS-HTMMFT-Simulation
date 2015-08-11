/* AIDialog.java
   This class opens a dialog to set up the AI players

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

package soccer.client.dialog;


import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import soccer.client.ViewerClientMain;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.PrintStream;

//import java.io.*;


@SuppressWarnings("serial")
public class AIDialog extends JPanel implements ActionListener {

	private ViewerClientMain m_client;
	private DialogManager m_dlgManager;
	JDialog m_dialog;
	private String command = "Both_Teams.bat";
	//"java -cp soccer.jar tos_teams.graviton.AIPlayers -l 11 -r 11";	// TODO

	private JTextField commandField;
	private JComboBox<ActiveCommand> commandBox = new JComboBox<ActiveCommand>();
	private JTextField inputField;
	private JTextArea outputArea;

	// ai player runtime control
	private PrintStream ps; // process input stream
	private DataInputStream ds; // process output stream
	private ActiveCommand current = null; // current selected one  

	public AIDialog(DialogManager mgr, ViewerClientMain soccerMaster) {
		m_client = soccerMaster;
		m_dlgManager = mgr;

		setupAIPanel();

	}

	private void setupAIPanel() {

		setBorder(BorderFactory.createLoweredBevelBorder());
		TitledBorder title;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// command line field
		JPanel p1 = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Command Line:");
		p1.setBorder(title);
		commandField = new JTextField(40);
		commandField.setActionCommand("Command");
		commandField.setText(command);
		commandField.addActionListener(this);
		p1.add(commandField);
		add(p1);
		add(Box.createVerticalGlue());
		
		// button for opening file chooser 
		JButton btnMoreFiles = new JButton("Choose different *.bat file");
		btnMoreFiles.addActionListener(this);
		add(btnMoreFiles);
		add(Box.createVerticalGlue());

		JPanel p2 = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Current Active Command:");
		p2.setBorder(title);
		commandBox = new JComboBox<ActiveCommand>();
		commandBox.setEnabled(false);		// TODO
		commandBox.setActionCommand("Active");
		commandBox.addActionListener(this);
		commandBox.setEditable(false);
		commandBox.setAlignmentX(LEFT_ALIGNMENT);
		p2.add(commandBox);
		add(p2);
		add(Box.createVerticalGlue());

		// set input field
		JPanel p3 = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Input:");
		p3.setBorder(title);
		// input field
		inputField = new JTextField(40);
		inputField.setEnabled(false);		// TODO
		inputField.setActionCommand("Input");
		inputField.addActionListener(this);
		p3.add(inputField);
		add(p3);
		add(Box.createVerticalGlue());

		// set output field
		JPanel p4 = new JPanel(new BorderLayout());
		title = BorderFactory.createTitledBorder("Output:");
		p4.setBorder(title);
		// output field
		outputArea = new JTextArea(6, 80);
		outputArea.setEditable(false);
		JScrollPane scrollPane =
			new JScrollPane(
				outputArea,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		p4.add(scrollPane);
		add(p4);
		add(Box.createVerticalGlue());

		// option buttons
		JButton on = new JButton("On");
		on.setActionCommand("On");
		on.addActionListener(this);
		JButton off = new JButton("Off");
		off.setActionCommand("Off");
		off.addActionListener(this);
		JButton close = new JButton("Close");
		close.setActionCommand("Close");
		close.addActionListener(this);

		// option panel
		JPanel option = new JPanel();
		option.setLayout(new FlowLayout());
		option.add(on);
		option.add(off);
		option.add(close);
		add(option);
		add(Box.createVerticalGlue());

	}

	public void actionPerformed(ActionEvent e) {
		//   
		if (e.getActionCommand().equals("On")) {
			if (ViewerClientMain.activeCommands.size()
				< ViewerClientMain.maxCommands) {
				command = commandField.getText();
				try {
					Process p = ViewerClientMain.runtime.exec(command);
					PrintStream p_s = new PrintStream(p.getOutputStream());
					ds = new DataInputStream(p.getInputStream());
					OutputUpdater ou = new OutputUpdater(ds, 50);
					ou.start();
					if (ou.isAlive()) {
						ActiveCommand ac =
							new ActiveCommand(command, p, ou, p_s);
						commandBox.addItem(ac);
						ViewerClientMain.activeCommands.addElement(ac);
					}

				} catch (Exception ex) {
					JOptionPane.showMessageDialog(
						m_client,
						ex,
						"Error",
						JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			} else
				JOptionPane.showMessageDialog(
					m_client,
					"Max number has been reached. No more process.",
					"Error",
					JOptionPane.ERROR_MESSAGE);
			
		} else if (e.getActionCommand().equals("Off")) {
			ActiveCommand ac = (ActiveCommand) commandBox.getSelectedItem();
			if (ac != null) {
				try {
					ac.getProcess().destroy();
					ac.getOutputUpdater().setOK(false);
					commandBox.removeItem(ac);
					ViewerClientMain.activeCommands.removeElement(ac);
				} catch (Exception ex) {
				}
			}
			
		} else if (e.getActionCommand().equals("Active")) {
			ActiveCommand ac = (ActiveCommand) commandBox.getSelectedItem();
			if (ac != null && !ac.equals(current)) {

				if (current != null)
					current.getOutputUpdater().setOutput(null);
				ps = ac.getPrintStream();
				outputArea.setText("");
				ac.getOutputUpdater().setOutput(outputArea);
				current = ac;
			}
			
		} else if (e.getActionCommand().equals("Input")) {
			if (ps != null) {
				ps.println(inputField.getText());
				ps.flush();
			}
			
		} else if (e.getActionCommand().equals("Close")) {
			undisplay();
			
		} else if (e.getActionCommand().equals("Choose different *.bat file")) {
			System.out.println("btnMoreFiles pressed");
			chooseFile();
		}
		m_client.requestFocus();	// allow receiving key events
	}
	
	
	private void chooseFile() {
	
		JFileChooser aJFileChooser = new JFileChooser();
		aJFileChooser.addChoosableFileFilter(new MyFilter());
		
		// open file selection dialog
	    int returnVal = aJFileChooser.showOpenDialog( m_client.getJToolBar() );
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	        File batFile = aJFileChooser.getSelectedFile();           
			
			if ( batFile == null ||
					batFile.getName().equals( "" ) ) {
				JOptionPane.showMessageDialog( m_client.getJToolBar(),
					"Invalid File Name",
					"Invalid File Name",
					JOptionPane.ERROR_MESSAGE );   
				return;
			} else {
				// place filename in text box
				commandField.setText(batFile.getName());
			}
	            

	    } else {
	        System.out.println("Open command cancelled by user." );
	    }
	}
	

	public void display() {
		if (m_dialog == null) {
			m_dialog = new JDialog((Frame) null, "Start AI Players", true);
			m_dialog.getContentPane().setLayout(new BorderLayout());
			m_dialog.getContentPane().add(AIDialog.this, BorderLayout.CENTER);
			m_dialog.setSize(400, 300);
			m_dialog.setResizable(false);

		}

		m_dlgManager.showDialog(m_dialog);
	}

	public void undisplay() {
		m_dlgManager.hideDialog(m_dialog);
		m_client.requestFocus();	// allow receiving key events
	}

	
	// this inner class allows filtering out *.bat files
	private class MyFilter extends javax.swing.filechooser.FileFilter {
	    public boolean accept(File file) {
	        String filename = file.getName();
	        return filename.endsWith(".bat");
	    }
	    public String getDescription() {
	        return "*.bat";
	    }
	}

}
