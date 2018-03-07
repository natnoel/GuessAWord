/**
 * CSCI213 Assignment 3
 * --------------------------------------------------
 * File name: GuessAWordClient.java
 * Author: Tan Shi Terng Leon
 * Registration Number: 4000602
 * Description: Connects to a server to play a "guess a word" game!
 * 				Also contains ConnectionGUI class providing the GUI for entering host and port
 */

/**
 * @author Leon
 *
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class GuessAWordClient {
	
	public static void main(String[] args) {
		
		new ConnectionGUI();
		
	}

}

class ConnectionGUI extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private JPanel p;
	private JTextField hostTF, portTF;
	private JButton connectButton;
	private JLabel msgLabel;
	
	private GridBagLayout layout;
	private GridBagConstraints c;
	
	private Dimension screenSize;
	
	private String host;
	private int port;
	private Socket sock;
	ObjectInputStream response;
	ObjectOutputStream request;
	
	public ConnectionGUI() {
		
		super("Welcome to Online Word Guessing Game!");
		
		p = new JPanel();
		
		layout = new GridBagLayout();
		c = new GridBagConstraints();
		
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		p.setLayout(layout);
		
		addToPanel(new JLabel("To connect to Server, enter the following."), 0, 0, 2, 1);
		
		c.fill = GridBagConstraints.NONE;
		addToPanel(new JLabel("Hostname:"), 1, 0, 1, 1);
		
		//Text field to enter host
		c.fill = GridBagConstraints.HORIZONTAL;
		hostTF = new JTextField();
		hostTF.addActionListener(new ConnectionListener());
		addToPanel(hostTF, 1, 1, 1, 1);
		
		c.fill = GridBagConstraints.NONE;
		addToPanel(new JLabel("Port No."), 2, 0, 1, 1);
		
		//Text field to enter port
		c.fill = GridBagConstraints.HORIZONTAL;
		portTF = new JTextField();
		portTF.addActionListener(new ConnectionListener());
		addToPanel(portTF, 2, 1, 1, 1);
		
		//Connect button
		connectButton = new JButton("Connect");
		connectButton.addActionListener(new ConnectionListener());
		addToPanel(connectButton, 3, 1, 1, 1);
		
		//Label to display error messages
		msgLabel = new JLabel();
		addToPanel(msgLabel, 4, 0, 2, 1);
		
		p.setPreferredSize(new Dimension(500, 200));
		
		//Sets up frame
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(p);
		pack();
		setVisible(true);
		
		//Makes frame appear in the center of the screen
		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
	}
	
	private void addToPanel(Component comp, int row, int col, int width, int height) {
		
		c.gridy = row;
		c.gridx = col;
		c.gridwidth = width;
		c.gridheight = height;
		
		layout.setConstraints(comp, c);
		p.add(comp);
	}
	
	class ConnectionListener implements ActionListener {
		
		//Establish connection to server
		public void actionPerformed(ActionEvent event) {
			
			//Getting the host
			host = hostTF.getText().trim();
			
			//Getting the port
			try {
				port = Integer.parseInt(portTF.getText().trim());
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(null, "Port No. must be a number!", "Invalid port no.!", JOptionPane.ERROR_MESSAGE);
				msgLabel.setText("Port No. must be a number!");
				pack();
				return;
			}
			
			//Setting the socket
			try {
				sock = new Socket(host, port);
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Port No. " + port + " is out of range!" , 
						"Unable to connect!", JOptionPane.ERROR_MESSAGE);
				msgLabel.setText("<HTML>Cannot get connected. Port No. " + port + " is out of range!<BR>Please try again.</HTML>");
				pack();
				return;
			}
			
			setVisible(false);			//Hides current frame
			new GuessAWordGUI(sock);	//Calls a new frame to start the game
		}
	}
}
