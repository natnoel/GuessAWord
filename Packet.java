import java.io.Serializable;

/**
 * CSCI213 Assignment 3
 * --------------------------------------------------
 * File name: GuessAWordClient.java
 * Author: Tan Shi Terng Leon
 * Registration Number: 4000602
 * Description: Packet class for the sending of messages between server and client
 */

/**
 * @author Leon
 *
 */
public class Packet implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public String control, data;
	
	public Packet(String control, String data) {
		// TODO Auto-generated constructor stub
		this.control = control;
		this.data = data;
	}

}
