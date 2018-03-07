/**
 * CSCI213 Assignment 3
 * --------------------------------------------------
 * File name: GuessAWordClient.java
 * Author: Tan Shi Terng Leon
 * Registration Number: 4000602
 * Description: Server for the "Guess A Word" game
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

/**
 * @author Leon
 *
 */
public class GuessAWordServer {

	/**
	 * @param args
	 */
	
	private static final int DEFAULT_PORT = 4444;
	private static final String WORD_FILE = "words.txt";
	
	private static ServerSocket serverSock;
	private static Socket clientSock;
	
	private static ArrayList<String> easyWords, hardWords;
	private static Vector<Integer> clientIDList;
	private static Random randomGenerator;
	private static int clientID;
	private static DecimalFormat idFmt;
	
	public static void main(String[] args) {
		
		//Creating server socket
		try {
			serverSock = new ServerSocket(DEFAULT_PORT);
			
			System.out.println("Started server on port " + DEFAULT_PORT);
		}
		catch (IOException e) {
			System.out.println("Unable to create server socket! " + e);
			return;
		}
		
		//Store words into ArrayLists
		easyWords = new ArrayList<String>();
		hardWords = new ArrayList<String>();
		loadWords();
		
		//If not enough small words
		if (easyWords.size() < 20) {
			System.out.println("Not enough small words(<= 5 characters)! Need at least 20 small words. No of small words: " +
					easyWords.size() + "\nProgram terminating...");
			System.exit(0);
		}
		//If not enough big words
		if (hardWords.size() < 20) {
			System.out.println("Not enough big words(> 5 characters)! Need at least 20 big words. No of big words: "+
					hardWords.size() + "\nProgram terminating...");
			System.exit(0);
		}
		
		clientIDList = new Vector<Integer>();	//Create list of client IDs
		randomGenerator = new Random();			//Creates the random number generator
		idFmt = new DecimalFormat("000");		//Sets the format to display client ID
		
		while (true) {
			
			//Accept client connection
			try {
				clientSock = serverSock.accept();
				
			} catch (IOException e) {
				System.out.println("Error accepting client! " + e);
				continue;
			}
			
			clientID = getClientID();	//Creates a unique client ID
			
			//Creates a thread to handle each
			Thread handleClient = new Thread(new HandleClient(clientSock, clientID, easyWords, hardWords));
			handleClient.start();
			
			//Prints accepted client
			System.out.println("Accepting request from client " + idFmt.format(clientID) + ": " +
					clientSock.getInetAddress());
			
			//Adds client ID to the list of current clients
			clientIDList.add(clientID);
			
		}
	}
	
	//Generates a unique client ID
	private static int getClientID() {
		int clientID;
		
		do {
			clientID = randomGenerator.nextInt(1000);
		} while (clientIDList.contains(clientID));
		
		return clientID;
	}
	
	//Load words from file
	private static void loadWords() {
		
		try {
			BufferedReader fileReader = new BufferedReader(new FileReader(WORD_FILE));
			
			String line;
			while ((line = fileReader.readLine()) != null) {
				String[] wordArray = line.split(";");
				
				for (String word : wordArray) {
					//System.out.println(word);
					if (word.length() > 5)
						hardWords.add(word);
					else
						easyWords.add(word);
				}
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("Cannot find file: " + WORD_FILE + " " + e);
			System.exit(0);
		} catch (IOException e) {
			System.out.println("Error reading file! " + e);
			System.exit(0);
		}
		
	}
	
	//Remove a current client ID
	public static synchronized void removeClientID(Integer clientID) {
		clientIDList.remove(clientID);
	}

}

class HandleClient implements Runnable {
	
	private final int TOTAL_QNS = 20;
	
	private int[] qnWordIdx;
	private Socket sock;
	private ObjectInputStream request;
	private ObjectOutputStream response;
	private int clientID;
	private Packet msg;
	private ArrayList<String> easyWords, hardWords, wordList;
	private int questionNo, score;
	private String scrambledWord;
	private boolean gameStarted, wordGuessed;
	
	public HandleClient(Socket s, int clientID, ArrayList<String> easyWords, ArrayList<String> hardWords) {
		
		this.clientID = clientID;
		sock = s;
		
		//Creating streams
		try {
			response = new ObjectOutputStream(sock.getOutputStream());
			request = new ObjectInputStream(sock.getInputStream());
		}
		catch (IOException e) {
			System.out.println("Error creating streams for client " + clientID);
			return;
		}
		
		this.easyWords = easyWords;
		this.hardWords = hardWords;
		
		questionNo = 0;
		gameStarted = false;
		wordList = new ArrayList<String>();
	}
	
	public void run() {
			
		while (true) {
			
			//Accepts a request
			try {
				msg = (Packet)request.readObject();
			} catch (IOException | ClassNotFoundException e) {
				System.out.println("Error receiving data from client " + clientID + "!");
				break;
			}
			
			/***************************************If requesting for new game***************************************/
			if (msg.control.equals("New Game")) {
				
				//If difficulty level not chosen send back error message
				if (wordList.isEmpty()) {
					if (!sendResponse("Error", "Difficulty level must be set!", "Error sending data!"))
						break;
					continue;
				}
				
				//Initializes or resets the question number and score
				questionNo = 0;
				score = 0;
				
				//Get the indexes of the random words chosen for the game
				getQuestionWordIndex(wordList);
				
				//Gets the first word scrambled
				scrambledWord = getScrambledWord(wordList.get(qnWordIdx[questionNo]));
				
				//Sends the scrambled word over to client
				if (!sendResponse("Word", scrambledWord, "Error sending data!"))
					break;
				
				//Game has started
				gameStarted = true;
				
				wordGuessed = false;
			}
			/***************************************If requesting for new word***************************************/
			else if (msg.control.equals("New Word")) {
				
				//If difficulty level not chosen, send back error message
				if (wordList.isEmpty()) {
					if (!sendResponse("Error", "Difficulty level must be set!", "Error sending data!"))
						break;
					continue;
				}
				
				//Reject request if game has not yet started
				if (gameStarted == false) {
					if (!sendResponse("Error", "Game has not started", "Error sending data!"))
						break;
					continue;
				}
				
				if (questionNo >= TOTAL_QNS - 1) {
					if (!sendResponse("Error", "This is the last word!", null))
						break;
					continue;
				}
				
				questionNo++;	//Increments question number to the next question
				scrambledWord = getScrambledWord(wordList.get(qnWordIdx[questionNo]));	//Gets scrambled word
				if (!sendResponse("Word", scrambledWord, "Error sending data!"))		//Sends the new scrambled word
					break;
				wordGuessed = false;	//Waiting for word to be guessed
			}
			/******************************************If submitting a guess******************************************/
			else if (msg.control.equals("Guess")) {
				
				/**********************************Invalid Guess**********************************/
				//If difficulty level not chosen, send back error message
				if (wordList.isEmpty()) {
					if (!sendResponse("Error", "Difficulty level must be set!", "Error sending data!"))
						break;
					continue;
				}
				//If game not yet started, sends an error message over
				else if (gameStarted == false) {
					if (!sendResponse("Error", "Game has not started!", "Error sending data"))
						break;
					continue;
				}
				//If word has already been guessed, sends an error message over
				else if (wordGuessed == true) {
					if (!sendResponse("Error", "Word already guessed!", "Error sending data"))
						break;
					continue;
				}
				//If submitted the same scrambled word
				else if (msg.data.equals(scrambledWord)) {
					if (!sendResponse("Same Word!", null, "Error sending data"))
						break;
					continue;
				}
				
				/**********************************Valid Guess**********************************/
				//If correct
				if (msg.data.equals(wordList.get(qnWordIdx[questionNo]))) {
					if (!sendResponse("Correct", wordList.get(qnWordIdx[questionNo]), "Error sending data"))
						break;
					
					//if (!receiveRequest())
					//	break;
					
					score += 10;
				}
				//If wrong
				else {
					if (!sendResponse("Wrong", wordList.get(qnWordIdx[questionNo]), "Error sending data"))
						break;
					
					//if (!receiveRequest())
					//	break;
					
					score -= 10;
				}
				
				if (!receiveRequest())
					break;
				
				if (!sendResponse("Update Score", Integer.toString(score), "Error sending data"))
					break;
				
				wordGuessed = true;
				
				//Receive request whether it is the last question (Only if guess is valid)
				if (!receiveRequest())
					break;
				
				if (msg.control.equals("Finished?")) {
					if (questionNo < TOTAL_QNS - 1) {
						if (!sendResponse("No", null, "Error send data"))
							break;
					}
					else {
						if (!sendResponse("Yes", null, "Error sending data"))
							break;
						gameStarted = false;
					}
				}
				
			}
			/*************************************Client sets difficulty level*************************************/
			else if (msg.control.equals("Set Difficulty Level")) {
				
				//Initializing for a new game
				score = 0;
				questionNo = 0;
				gameStarted = false;
				
				if (msg.data.equals("Easy") || msg.data.equals("Hard"))
					wordList = easyWords;
				else if (msg.data.equals("Medium") || msg.data.equals("Very Hard"))
					wordList = hardWords;
				else {
					if (!sendResponse("Error", "Did not receive any one of the difficulty levels : Easy or Hard", null))
						break;
					System.out.println("Incorrect data received when requesting to set difficulty level");
				}
			}
			/*****************************If time out (for Hard and Very Hard mode)*********************************/
			else if (msg.control.equals("TimeOut!")) {
				//If correct
				if (msg.data.equals(wordList.get(qnWordIdx[questionNo]))) {
					if (!sendResponse("Correct", wordList.get(qnWordIdx[questionNo]), "Error sending data"))
						break;
					score += 10;
				}
				//If wrong
				else {
					if (!sendResponse("Wrong", wordList.get(qnWordIdx[questionNo]), "Error sending data"))
						break;
					score -= 10;
				}
				
				wordGuessed = true;
				
				//Updates score
				if (!receiveRequest())
					break;
				if (!sendResponse("Update Score", Integer.toString(score), "Error sending data"))
					break;
				
				//Receive request whether it is the last question (Only if guess is valid)
				if (!receiveRequest())
					break;
				
				if (msg.control.equals("Finished?")) {
					if (questionNo < TOTAL_QNS - 1) {
						if (!sendResponse("No", null, "Error send data"))
							break;
					}
					else {
						if (!sendResponse("Yes", null, "Error sending data"))
							break;
						gameStarted = false;
					}
				}
			}
			/******************************************Client request to exit******************************************/
			else if (msg.control.equals("Exit")) {
				break;
			}
			/**********************Receives something not known (or in wrong sequence) to server**********************/
			else {
				System.out.println("Received something weird from client " + clientID + ":" + sock.getInetAddress().getHostName());
			}
			
		}
		
		try {
			sock.close();
		} catch (IOException e) {
			System.out.println("Error closing socket of client: " + clientID + ":" + sock.getInetAddress().getHostName());
		}
		
		System.out.println("Disconnected from client " + clientID + ": " + sock.getInetAddress().getHostAddress());
		GuessAWordServer.removeClientID(clientID);
		
	}
	
	//Fills and array of size 20 with indexes of random words of the list
	private void getQuestionWordIndex(ArrayList<String> wordList) {
		
		qnWordIdx = new int[TOTAL_QNS];
		int numOfWords = wordList.size();
		Random generator = new Random();
		int wordIdx;
		
		for (int i = 0; i < TOTAL_QNS; i++) {
			do {
				wordIdx = generator.nextInt(numOfWords);
			}while (indexExist(qnWordIdx, i, wordIdx));
			
			qnWordIdx[i] = wordIdx;
		}
	}
	
	//If a number already exist in the array qnWordIdx from position 0 to max - 1
	private boolean indexExist(int[] qnWordIdx, int max, int wordIdx) {
		
		for (int i = 0; i < max; i++) {
			if (wordIdx == qnWordIdx[i])
				return true;
		}
		return false;
	}
	
	//Sends a response to client
	private boolean sendResponse(String control, String data, String errorMsg) {
		try {
			response.writeObject(new Packet(control, data));
			response.flush();
			response.reset();
			return true;
		} catch (IOException e) {
			System.out.println("Error in client " + clientID + "! " + errorMsg + " " + e);
			return false;
		}
	}
	
	//Received request from client and stores it in msg variable
	private boolean receiveRequest() {
		try {
			msg = (Packet)request.readObject();
			return true;
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error receiving data from client " + clientID + "!");
			return false;
		}
	}
	
	//Scrambles a word
	private String scramble(String word) {
		
		String scrambledWord;
		Random randomGenerator = new Random();
		int length = word.length();
		char wordArray[] = word.toCharArray();
		
		for (int i = 0; i < length; i++) {
			//Swap characters with a random position
			int j = randomGenerator.nextInt(length);
			char temp = wordArray[i];
			wordArray[i] = wordArray[j];
			wordArray[j] = temp;
		}
		
		scrambledWord = new String(wordArray);
		return scrambledWord;
	}
	
	//Gets a scrambled version of the word and makes sure it is not the same
	private String getScrambledWord(String word) {
		String scrambledWord;
		
		do {
			scrambledWord = scramble(word);
		} while (scrambledWord.equals(word));
		
		return scrambledWord;
	}
	
}
