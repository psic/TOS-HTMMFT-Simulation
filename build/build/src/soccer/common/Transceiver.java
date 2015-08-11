/**  
 * This class defines the UDP networking tasks, such as sending and
 * receiving UDP packets over the computer network.
 * One or more instances of this class belong to each stand alone 
 * application in the TOS bundle.
 *
 * @author Yu Zhang
 * modifications by Vadim Kyrylov 
 * since 2011
 */


package soccer.common;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class Transceiver {

	private  int  size = 1024;
	private DatagramSocket    socket;

	/**
	 * Default constructor; used for compatibility with sub classes, if any 
	 */
	public Transceiver() {}

	/**
	 * creates a transceiver with default settings. If the transceiver
	 * is used for the server, the default port is used. If the
	 * transceiver is used for the client, no port number is needed.
	 *
	 * @param isServer the flag to indicate if this transceiver is used for server or not
	 */
	public Transceiver(boolean isServer) {
		//System.out.println("Transceiver Start 1");
		try {
			if(!isServer) {
				// bind to any available port
				socket = new DatagramSocket();
			}
			else {
				// bind to the default server port
				socket = new DatagramSocket(7777);
			}
		} catch(Exception e) {
			System.err.println("Transceiver:socket creating error:" + e);
			System.exit(1);
		}
	}

	/**
	 * sets up a transceiver for server with specified port number.
	 *
	 * @param port the server port number
	 */
	public Transceiver(int port) {
		//System.out.println("Transceiver Start 2: " + port);
		try {
			socket = new DatagramSocket(port);
		} catch(Exception e) {
			System.err.println("Transceiver:socket creating error:" + e);
			System.exit(1);      
		}
	}

	/**
	 * Disconnect the UDP socket, close this transceiver.
	 */
	public void disconnect() {
		socket.close();
	}

	/**
	 * Sends soccer data packet over UDP socket.
	 * The DatagramPacket includes information indicating the 
	 * data to be sent, its length, the IP address of the 
	 * remote host, and the port number on the remote host. 
	 * 
	 * @param p the soccer data packet
	 * @exception IOException If any UDP errors occured.
	 * 
	 * @see Packet
	 */
	public void send(Packet p) throws IOException {

		byte[] buffer = p.writePacket().getBytes();
		DatagramPacket packet = 
				new DatagramPacket(	buffer, 
						buffer.length, 
						p.address, 
						p.port);
		socket.send(packet);

	}

	/**
	 * Receives soccer data packet over UDP socket.
	 * 
	 * @return    the soccer data packet
	 * @exception IOException If any UDP errors occured.
	 * 
	 * @see Packet
	 */

	public Packet receive() throws IOException {

		if (socket.isClosed() || socket == null )
			return null ;
		else{
			byte[] buffer = new byte[size];
			DatagramPacket packet = new DatagramPacket(buffer, size);
			/**
			 * When this method returns, the DatagramPacket's buffer 
			 * is filled with the data received. The datagram packet 
			 * also contains the sender's IP address, and the port 
			 * number on the sender's machine.
			 */


			socket.receive(packet);

			String message = new String(buffer);
			Packet p = new Packet();
			p.readPacket(message);
			p.address = packet.getAddress();
			p.port = packet.getPort();
			return p;
		}
	}

	/**
	 * Sets the UDP receiving buffer size.
	 *
	 * @param size the UDP receiving buffer size
	 */
	public void setSize(int size) {
		this.size = size;  
	}

	/**
	 * Gets the UDP receiving buffer size.
	 *
	 * @return the UDP receiving buffer size
	 */

	public int getSize() {
		return size;  
	}

	/**
	 * Sets the UDP socket block timeout.
	 * 
	 * Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds.
	 * With this option set to a non-zero timeout, a call
	 * to receive() for this DatagramSocket will block for only this amount of time.
	 * If the timeout expires, a <b>java.io.InterruptedIOException</b> 
	 * is raised, though the DatagramSocket is still valid. The option must be enabled 
	 * prior to entering the blocking operation to have effect.
	 * A timeout of zero is interpreted as an infinite timeout.
	 *	
	 * @param timeout  the specified timeout in milliseconds.
	 * @exception IOException If there is an error in the underlying protocol.
	 */
	public void setTimeout(int timeout) throws IOException  {
		socket.setSoTimeout(timeout);
	}

	/**
	 * Gets the UDP socket block timeout.
	 * <p>
	 * Retrieve setting for SO_TIMEOUT. 0 returns implies that the option is disabled 
	 * (i.e., timeout of infinity).
	 *
	 * @return the UDP socket block timeout.
	 */
	public int getTimeout() throws IOException {
		return socket.getSoTimeout();  
	}

	public boolean isClosed() {
		return socket.isClosed();
	}  
}

