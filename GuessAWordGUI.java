/**
 * CSCI213 Assignment 3
 * --------------------------------------------------
 * File name: GuessAWordClient.java
 * Author: Tan Shi Terng Leon
 * Registration Number: 4000602
 * Description: Contains GuessAWordGUI class providing the GUI for playing the "Guess A Word" game
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.*;


/**
 * @author Leon
 *
 */
public class GuessAWordGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private final String[] DIFFLEVEL = {"Easy", "Medium", "Hard", "Very Hard"};
	private final int INTERVAL = 1000;	//One second interval
	private final int TIMELIMIT = 21;	//Time limit of 20 seconds
	
	Socket sock;
	ObjectInputStream response;
	ObjectOutputStream request;
	
	private JPanel mainPanel, diffPanel;
	private WordPanel wordPanel;
	private JLabel scoreLabel, msgLabel, diffLabel, countDownLabel, qnNoLabel;
	private JButton confirmButton, newWordButton, newGameButton, clearButton;
	private JComboBox<String> difficultyCB;
	
	private GridBagLayout layout;
	private GridBagConstraints c;
	
	private int score, questionNo;
	private Packet msg;
	private String word, guess, diffLevel;
	private Timer timer;
	private int secondsLeft;
	private Dimension screenSize;
	private ImageIcon smileyIcon, keepItUpIcon;
	
	public GuessAWordGUI(Socket s) {
		super("Game has started");
		
		sock = s;
		
		//Setting the streams
		response = null;
		request = null;
		try {
			request = new ObjectOutputStream(sock.getOutputStream());
			response = new ObjectInputStream(sock.getInputStream());
			request.flush();
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Error creating streams! Program terminating..." , 
					"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		
		mainPanel = new JPanel();
		
		layout = new GridBagLayout();
		c = new GridBagConstraints();
		
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		mainPanel.setLayout(layout);
		
		//Displays the score
		scoreLabel = new JLabel("Score: 0");
		addToMainPanel(scoreLabel, 0, 0, 1, 1);
		
		//Combo box to set difficulty level
		diffPanel = new JPanel();
		diffLabel = new JLabel("Difficulty Level");
		diffPanel.add(diffLabel);
		difficultyCB = new JComboBox<String>(DIFFLEVEL);
		difficultyCB.setSelectedIndex(-1);
		difficultyCB.addActionListener(new DifficultyLevelListener());
		diffPanel.add(difficultyCB);
		addToMainPanel(diffPanel, 0, 1, 1, 1);
		
		//Displays the question number
		qnNoLabel = new JLabel();
		addToMainPanel(qnNoLabel, 0, 2, 1, 1);
		
		//The scrambled word
		wordPanel = new WordPanel();
		addToMainPanel(wordPanel, 1, 0, 3, 1);
		
		//Confirm button
		confirmButton = new JButton("Confirm");
		confirmButton.addActionListener(new ConfirmListener());
		confirmButton.setEnabled(false);
		addToMainPanel(confirmButton, 3, 0, 1, 1);
		
		//New Word button
		newWordButton = new JButton("New Word");
		newWordButton.addActionListener(new NewWordListener());
		newWordButton.addKeyListener(new CharKeyListener());
		addToMainPanel(newWordButton, 3, 1, 1, 1);
		
		//New Game button
		newGameButton = new JButton("New Game");
		newGameButton.addActionListener(new NewGameListener());
		newGameButton.addKeyListener(new CharKeyListener());
		addToMainPanel(newGameButton, 3, 2, 1, 1);
		
		//Clear button (resets the characters allowing users to re-enter)
		clearButton = new JButton("Clear");
		clearButton.addActionListener(new ClearListener());
		clearButton.addKeyListener(new CharKeyListener());
		addToMainPanel(clearButton, 4, 1, 1, 1);
		
		//Shows whether guess is correct or wrong
		msgLabel = new JLabel();
		addToMainPanel(msgLabel, 5, 0, 2, 1);
		
		//Counts down from the given time limit (20 seconds)
		countDownLabel = new JLabel();
		addToMainPanel(countDownLabel, 5, 2, 1, 1);
		
		//Sets frame
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		mainPanel.setPreferredSize(new Dimension(700, 200));
		add(mainPanel);
		pack();
		addWindowListener(new ExitListener());
		
		//Creates timer for count down
		timer = new Timer(INTERVAL, new CountDownListener());
		secondsLeft = TIMELIMIT;
		
		//Smiley image
		smileyIcon = new ImageIcon("smiley.jpg");
		smileyIcon.setImage(smileyIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
		
		//Keep it up image
		keepItUpIcon = new ImageIcon("keepup.jpg");
		keepItUpIcon.setImage(keepItUpIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
		
		//Makes frame appear in the middle of the screen
		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
		
	}
	
	private void addToMainPanel(Component comp, int row, int col, int width, int height) {
		
		c.gridy = row;
		c.gridx = col;
		c.gridwidth = width;
		c.gridheight = height;
		
		layout.setConstraints(comp, c);
		mainPanel.add(comp);
	}
	
	//Allows user to type instead of clicking the buttons of the scrambled word
	class CharKeyListener implements KeyListener {
		
		public void keyPressed(KeyEvent arg0) {
			
		}
		
		public void keyReleased(KeyEvent arg0) {
			
		}
		
		public void keyTyped(KeyEvent event) {
			String letter = String.valueOf(event.getKeyChar()).toUpperCase();
			int currIdx;
			
			//If backspace and word filled is not empty
			if ((wordPanel.charIdx > 0 && letter.equals("\b"))) {
				currIdx = --wordPanel.charIdx;
				for (JButton b : wordPanel.charButton) {
					if (!b.isEnabled() && b.getText().equals(wordPanel.charChosen[currIdx].getText())) {
						b.setEnabled(true);
						break;	//To prevent more than 1 button of the same letter to be reset
					}
				}
				wordPanel.charChosen[currIdx].setText("");
			}
			//Types a character
			else {
				for (JButton b : wordPanel.charButton) {
					if (b.isEnabled() && (b.getText().equals(letter))) {
						b.setEnabled(false);
						wordPanel.charChosen[wordPanel.charIdx++].setText(letter);
						break;	//To prevent more than 1 button of the same letter to be selected
					}
				}
			}
			
		}
		
	}
	
	class CountDownListener implements ActionListener {
		
		public void actionPerformed(ActionEvent event) {
			
			secondsLeft -= 1;
			countDownLabel.setText("Time left: " + secondsLeft + " seconds");
			
			if (secondsLeft == 0) {
				timer.stop();
				
				guess = wordPanel.getGuess();
				
				if (!sendRequest("TimeOut!", guess, "Error sending guess to server!"))
					return;
				if (!receiveResponse())
					return;
				
				//If guessed word is valid
				if (msg.control.equals("Correct") || msg.control.equals("Wrong")) {
					
					msgLabel.setText("Time Out! " + msg.control + "! The word is " + msg.data);
					pack();
					
					//Request for score
					if (!sendRequest("Score?", null, "Error requesting for updated score"))
						return;
					
					//Getting updated score from server
					if (!receiveResponse())
						return;
					
					//Updating score
					if (msg.control.equals("Update Score")) {
						try {
							score = Integer.parseInt(msg.data);
						} catch (NumberFormatException e) {
							JOptionPane.showMessageDialog(null, "Error received wrong data from server! ", 
									"Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						scoreLabel.setText("Score: " + score);
					}
					else
						JOptionPane.showMessageDialog(null, "Error received wrong data from server!\n" +
								msg.control + ": " + msg.data, 
								"Error", JOptionPane.ERROR_MESSAGE);
					
					//Ask server if the game has ended
					if (!sendRequest("Finished?", null, "Error sending data to server!"))
						return;
					
					if (!receiveResponse())
						return;
					
					//If it is the last question
					if (msg.control.equals("Yes")) {
						//Pop out frame
						//Another game or Terminate
						if (score == 200)
							JOptionPane.showMessageDialog(null, "Congratulations, you have won!\n" +
									"Your score: " + score, "You won the game!!", JOptionPane.INFORMATION_MESSAGE, smileyIcon);
						else
							JOptionPane.showMessageDialog(null, "Yay, you have completed!\n" +
								"Your score: " + score, "Finish!", JOptionPane.INFORMATION_MESSAGE, keepItUpIcon);
					}
				}
				//If received anything else from server
				else {
					JOptionPane.showMessageDialog(null, "Error! Message from server: " + msg.data, 
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
			}
		}
		
	}
	
	//Clears all selected characters
	class ClearListener implements ActionListener {
		
		public void actionPerformed(ActionEvent event) {
			wordPanel.reset();
			pack();
		}
		
	}
	
	//Sets difficulty level
	class DifficultyLevelListener implements ActionListener {
		
		public void actionPerformed(ActionEvent event) {
			int idx = difficultyCB.getSelectedIndex();
			diffLevel = DIFFLEVEL[idx];
			if (!sendRequest("Set Difficulty Level", diffLevel, "Sending request to set difficulty level"))
				return;
			
			if (diffLevel.equals("Easy") || diffLevel.equals("Medium"))
				countDownLabel.setVisible(false);
			else
				countDownLabel.setVisible(true);
			
			//Resets the components in the frame
			countDownLabel.setText("");
			qnNoLabel.setText("");
			msgLabel.setText("");
			scoreLabel.setText("");
			timer.stop();
			wordPanel.removeAll();
			pack();
		}
		
	}
	
	//Starts a new game
	class NewGameListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			
			score = 0;
			scoreLabel.setText("Score: " + score);
			questionNo = 1;
			qnNoLabel.setText("Word " + Integer.toString(questionNo) + " of 20");
			msgLabel.setText("");
			
			//Request for new game
			if (!sendRequest("New Game", null, "Error requesting for new game!"))
				return;
			/*try {
				request.writeObject(new Packet("New Game", null));
				request.flush();
				request.reset();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Error requesting for new game! " + e.getMessage(), 
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}*/
			
			//Receive response
			if (!receiveResponse())
				return;
			/*try {
				msg = (Packet)response.readObject();
			} catch (ClassNotFoundException | IOException e) {
				JOptionPane.showMessageDialog(null, "Error receiving data from server! " + e.getMessage(), 
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}*/
			
			//If erroneous data received from server
			if (!msg.control.equals("Word")) {
				qnNoLabel.setText("");
				JOptionPane.showMessageDialog(null, "Server message: " + msg.data, 
						msg.control, JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			word = msg.data;
			wordPanel.setWord(word);
			pack();
			
			if (diffLevel.equals("Hard") || diffLevel.equals("Very Hard")) {
				secondsLeft = TIMELIMIT;
				timer.start();
			}
			
			confirmButton.setEnabled(true);
		}
	}
	
	//Ask for a new word or skips the current question
	class NewWordListener implements ActionListener {

		public void actionPerformed(ActionEvent event) {
			msgLabel.setText("");
			
			if(!sendRequest("New Word", null, "Error requesting for new word!"))
				return;

			if (!receiveResponse())
				return;
			
			//if (msg.control == null)
			//	return;
			if (!msg.control.equals("Word")) {
				JOptionPane.showMessageDialog(null, msg.data, 
						msg.control, JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			
			word = msg.data;
			wordPanel.setWord(word);
			pack();
			
			if (diffLevel.equals("Hard") || diffLevel.equals("Very Hard")) {
				secondsLeft = TIMELIMIT;
				timer.start();
			}
			
			questionNo++;
			qnNoLabel.setText("Word " + Integer.toString(questionNo) + " of 20");
			
			confirmButton.setEnabled(true);
		}
		
	}
	
	//Confirms the guess and submits it to server
	class ConfirmListener implements ActionListener {

		public void actionPerformed(ActionEvent event) {
			confirmButton.setEnabled(false);
			
			if (diffLevel.equals("Hard") || diffLevel.equals("Very Hard")) {
				timer.stop();
			}
			
			guess = wordPanel.getGuess();
			
			//Submits the guessed word
			if (!sendRequest("Guess", guess, "Error sending guess to server"))
				return;
			
			//Receives response from server
			if (!receiveResponse())
				return;
			
			//If guessed word is valid
			if (msg.control.equals("Correct") || msg.control.equals("Wrong")) {
				
				msgLabel.setText(msg.control + "! The word is " + msg.data);
				pack();
				
				//Request for score
				if (!sendRequest("Score?", null, "Error requesting for updated score"))
					return;
				
				//Getting updated score from server
				if (!receiveResponse())
					return;
				
				//Updating score
				if (msg.control.equals("Update Score")) {
					try {
						score = Integer.parseInt(msg.data);
					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(null, "Error received wrong data from server! ", 
								"Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					scoreLabel.setText("Score: " + score);
				}
				else {	//Receive incorrect data from server
					JOptionPane.showMessageDialog(null, "Error received wrong data from server! ", 
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				//Ask server if the game has ended
				if (!sendRequest("Finished?", null, "Error sending data to server!"))
					return;
				
				if (!receiveResponse())
					return;
				
				//If it is the last question
				if (msg.control.equals("Yes")) {
					//Pop out frame
					//Another game or Terminate
					if (score == 200)
						JOptionPane.showMessageDialog(null, "Congratulations, you have won!\n" +
								"Your score: " + score, "You won the game!!", JOptionPane.INFORMATION_MESSAGE, smileyIcon);
					else
						JOptionPane.showMessageDialog(null, "Yay, you have completed!\n" +
							"Your score: " + score, "Finish!", JOptionPane.INFORMATION_MESSAGE, keepItUpIcon);
				}
			}
			//If guess word is the same as itself
			else if (msg.control.equals("Same Word!")) {
				if (diffLevel.equals("Hard") || diffLevel.equals("Very Hard"))
					timer.start();
				
				JOptionPane.showMessageDialog(null, "Please do not submit the same word as the given! ", 
						"Same word detected!", JOptionPane.ERROR_MESSAGE);
				
				wordPanel.reset();
				pack();
				
				confirmButton.setEnabled(true);
				
				return;
			}
			//If received anything else from server
			else {
				JOptionPane.showMessageDialog(null, "Error! Message from server: " + msg.data, 
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
	}
	
	private boolean sendRequest(String control, String data, String errorMsg) {
		try {
			request.writeObject(new Packet(control, data));
			request.flush();
			request.reset();
			return true;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, errorMsg + " " + e.getMessage(), 
					"Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
	
	private boolean receiveResponse() {
		try {
			msg = (Packet)response.readObject();
			return true;
		} catch (IOException | ClassNotFoundException e) {
			JOptionPane.showMessageDialog(null, "Error receiving data from server! " + e.getMessage(), 
					"Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
	
	//Disconnects from server and close frame
	class ExitListener implements WindowListener {
		
		public void windowClosed(WindowEvent event) {
			
		}
		
		public void windowClosing(WindowEvent event) {
			
			try {
				request.writeObject(new Packet("Exit", null));
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Unable to send exit message" + " " + e.getMessage(), 
						"Error", JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
			
			try {
				sock.close();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Error closing socket! " + e.getMessage(), 
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		public void windowOpened(WindowEvent event) {
			
		}
		
		public void windowActivated(WindowEvent event) {
			
		}
		
		public void windowDeactivated(WindowEvent event) {
			
		}
		
		public void windowIconified(WindowEvent event) {
			
		}
		
		public void windowDeiconified(WindowEvent event) {
			
		}
		
	}
	
	//Panel that contains the scrambled word and the guess
	class WordPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		
		JButton charButton[];
		JTextField charChosen[];
		int length;
		int charIdx;
		
		//Sets up the panel given a word
		public void setWord(String word) {
			charIdx = 0;
			removeAll();
			setLayout(new GridLayout(2, word.length()));
			
			length = word.length();
			
			charButton = new JButton[word.length()];
			charChosen = new JTextField[word.length()];
			
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < word.length(); j++) {
					if (i == 0) {
						//Sets up the buttons
						charButton[j] = new JButton(String.valueOf(word.charAt(j)));
						charButton[j].addActionListener(new CharListener());
						add(charButton[j]);
					}
					else {
						//Sets up the text fields for the guess
						charChosen[j] = new JTextField();
						charChosen[j].setEditable(false);
						charChosen[j].setBackground(Color.WHITE);
						charChosen[j].setFont(new Font("Arial", Font.BOLD, 14));
						charChosen[j].setHorizontalAlignment(JTextField.CENTER);
						add(charChosen[j]);
						
					}
				}
			}
		}
		
		//Returns the guess
		public String getGuess() {
			String guess = new String();
			
			for (int i = 0; i < length; i++) {
				guess += charChosen[i].getText();
			}
			
			return guess;
		}
		
		//Enters the corresponding letter when a button is clicked
		class CharListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				JButton b = (JButton)e.getSource();
				charChosen[charIdx++].setText(b.getText());
				b.setEnabled(false);
			}
			
		}
		
		//Resets the buttons and text fields
		public void reset() {
			for (int i = 0; i < length; i++) {
				charButton[i].setEnabled(true);
				charChosen[i].setText("");
			}
			charIdx = 0;
		}
	}

}

