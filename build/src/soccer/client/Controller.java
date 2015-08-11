/** Controller.java
 * 
 * by Vadim Kyrylov
 * 2006 - 2012
 * 
 * Object of this class provides mouse controls in two modes of operation:
 * 1) paused game mode (for moving the ball and players around between steps) and
 * 2) regular game play (for creating persistent player control command).  
 *
*/
 
package soccer.client;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;

import soccer.client.view.Field;
import soccer.client.view.j3d.FieldJ3D;

import soccer.common.Packet;
import soccer.common.Player;
import soccer.common.TOS_Constants;
import soccer.common.TeleportData;
import soccer.common.Vector2d;


public class Controller implements MouseListener, MouseMotionListener {
	
	private ViewerWorld world;				
	private ViewerClientMain soccerMaster;	
	private Field arena;

	// static variables are used because this is a singleton class
	public static final int DRIVE_COMMAND_DURATION = 50;
	public static final int KICK_COMMAND_DURATION = 50;
	
	private static int driveCommandCount = -1;
	private static Vector2d driveEndPoint;
	private static boolean isDrivenByUser = false;
	
	private static int kickCommandCount = -1;
	private static Vector2d kickEndPoint;
	private static boolean isChasingToKickBall = false;
	
	private static boolean ballGrabbed = false;
	private static boolean leftPlayerGrabbed = false;
	private static boolean rightPlayerGrabbed = false;
	private static int grabbedPlayerID = -1;
	
	private static final double  radius = 6;
	

	public Controller(ViewerWorld world, ViewerClientMain soccerMaster) {
		this.world = world;
		this.soccerMaster = soccerMaster;
		this.arena = soccerMaster.arena2D;
	}

	
	/**
	 *  this method returns true is either the ball or some player was grabbed;
	 *  it also sets the respective class instance variables
	 */
	private boolean objectGrabbed( double x, double y ) {
		
		clearGrabFlags();
		
		Vector2d cursor = new Vector2d( x, y ); 		
		Vector2d center = new Vector2d();
		
		if (!soccerMaster.isPlaying() || soccerMaster.getGState() == TOS_Constants.WAITING) {
			// do not grab the ball if there is a user controlled player 
			center.setXY( world.getBall().getPosition() );
			arena.soccer2user( center );
			if ( center.distance( cursor ) < radius ) {
				ballGrabbed = true;
				return true;
			}
		}
		
		for ( int i=0; i<world.getLeftTeam().size(); i++ ) {
			Player player = (Player)world.getLeftTeam().elementAt( i ); 
			center.setXY( player.getPosition() ); 
			arena.soccer2user( center );
			if ( center.distance( cursor ) < radius ) {
				leftPlayerGrabbed = true;
				grabbedPlayerID = i;	// this is the location in the vector, not ID
				//System.out.println("Left player " + i + " grabbed");
				return true;
			}
		}
		
		for ( int i=0; i<world.getRightTeam().size(); i++ ) {
			Player player = (Player)world.getRightTeam().elementAt( i ); 
			center.setXY( player.getPosition() ); 
			arena.soccer2user( center );
			if ( center.distance( cursor ) < radius ) {
				rightPlayerGrabbed = true;
				grabbedPlayerID = i;	// this is the location in the vector, not ID
				//System.out.println("Right player " + i + " grabbed");
				return true;
			}
		}
		
		return false;
	}

	public void clearGrabFlags() {
		ballGrabbed = false;
		leftPlayerGrabbed = false;
		rightPlayerGrabbed = false;
		grabbedPlayerID = -1;
	}	
	
	public void updateGrabFlags( boolean updateBall ) {
		if ( updateBall )
			// ball
			world.getBall().isGrabbed = ballGrabbed;
			
		// left team	
		for ( int i=0; i<world.getLeftTeam().size(); i++ ) {
			Player player = (Player)world.getLeftTeam().elementAt( i ); 
			player.setGrabbed( leftPlayerGrabbed && grabbedPlayerID == i );
		}
		// right team
		for ( int i=0; i<world.getRightTeam().size(); i++ ) {
			Player player = (Player)world.getRightTeam().elementAt( i ); 
			player.setGrabbed( rightPlayerGrabbed && grabbedPlayerID == i );
		}
	}
	
	/**
	 * forget about persistently driving player by user
	 */
	public static void resetDriveFlags() {
		isDrivenByUser = false;
		driveCommandCount = -1;
		driveEndPoint = null;		
	}
	
	/**
	 * forget about user controlled persistent chasing the ball
	 */
	public static void resetChaseKickFlags() {
		isChasingToKickBall = false;
		kickCommandCount = -1;
		kickEndPoint = null;		
	}
	
	/**
	 * cancel all persistent user controlled commands
	 */
	public static void resetFlags() {
		resetDriveFlags();
		resetChaseKickFlags();
	}

	//*** Handle mouse events ***
	
	/**
	 * This method works differently in the regular game mode and in 
	 * the paused mode. In the paused mode, it only concerns the Game 
	 * Coordinator and determines if any object was grabbed. 
	 * In the regular mode, this method sets command parameters for 
	 * the user controlled player, if any, running and kicking the ball. 
	 */
	public void mousePressed(MouseEvent e) { 
		
		if (soccerMaster.isDebugMode())
			return;
//<=========
		
		double x = e.getX();
		double y = e.getY();
		
		if ( soccerMaster.getGState() == TOS_Constants.WAITING 
									&& soccerMaster.isCoordinator() ) {
			/**
			 *  moving objects around on the field in the paused state
			 */
			if(soccerMaster.isIn3D()) {
				soccerMaster.arena3D.requestFocus();
				((FieldJ3D)(soccerMaster.arena3D)).myCanvas3D.requestFocus();
			} else {
				arena.requestFocus();
			} 
			
			if ( e.getModifiers() == InputEvent.BUTTON1_MASK ) {
				// left button clicked
				if ( objectGrabbed( x, y ) ) {
					// do nothing	
				}
			} 
		} else if (soccerMaster.getGState() != TOS_Constants.WAITING
									&& soccerMaster.isPlaying()) {
			/**
			 * persistently controlling a player by user commands
			 */
			if ( e.getModifiers() == InputEvent.BUTTON1_MASK ) {
				// left button clicked: set the target point to run to
				driveEndPoint = new Vector2d(x, y);
				arena.user2soccer(driveEndPoint);
				isDrivenByUser = true;
				driveCommandCount = 0;
			} else if ( e.getModifiers() == InputEvent.BUTTON3_MASK ) {
				// right button clicked:  set the target point to kick ball to 
				kickEndPoint =  new Vector2d(x, y);
				arena.user2soccer(kickEndPoint);				
				resetDriveFlags();	// cancel user controlled drive
				isChasingToKickBall = true;
				kickCommandCount = 0;
			}
		}
		// just for the immediate display, clear all isGrabbed flags 
		updateGrabFlags( true );						
		arena.repaint();
	}

	/**
	 *  If the game is paused, once mouse is dragged, the Game 
	 *  Coordinator sends message to server to move the grabbed object
	 */
	public void mouseDragged(MouseEvent e) {
		
		if (soccerMaster.isDebugMode())
			return;
//<=========

		char objType ='?';
		int playerID = -1;
		Player player = null;
		
		if ( soccerMaster.getGState() == TOS_Constants.WAITING 
				&& soccerMaster.isCoordinator() ) {
			
			if ( ballGrabbed || leftPlayerGrabbed || rightPlayerGrabbed ) {
				/**
				 * teleporting object in the paused state of the game
				 */
				if ( ballGrabbed ) {
					objType = TeleportData.BALL;
				} else {					
					if ( leftPlayerGrabbed ) {
						objType = TeleportData.LEFT_PLAYER;
						player = (Player)world.getLeftTeam().elementAt( grabbedPlayerID ); 
					} else if ( rightPlayerGrabbed ) {
						objType = TeleportData.RIGHT_PLAYER;
						player = (Player)world.getRightTeam().elementAt( grabbedPlayerID ); 
					}	
					playerID = player.getId();						
				}
				
				// determine the new position of the grabbed object
				Vector2d newpos = new Vector2d( e.getX(), e.getY() ); 
				arena.user2soccer( newpos ); 
				TeleportData sentData = 
						new TeleportData( objType,
										  playerID,
										  newpos.getX(),
										  newpos.getY()  );
				
				try {
					soccerMaster.sendToServer(Packet.TELEPORT, sentData);
				} catch (IOException e1) {
					System.out.println("Error sending Packet.TELEPORT " + e1 );
				}
			}
		}
	}
	
	public void mouseReleased(MouseEvent e) { }
	public void mouseClicked(MouseEvent e) { }
	public void mouseMoved(MouseEvent e) { }
	public void mouseEntered(MouseEvent arg0) { }
	public void mouseExited(MouseEvent arg0) { }


	public static int getDriveCommandCount() {
		return driveCommandCount;
	}


	public static void setDriveCommandCount(int driveCommandCount) {
		Controller.driveCommandCount = driveCommandCount;
	}


	public static int getKickCommandCount() {
		return kickCommandCount;
	}


	public static void setKickCommandCount(int kickCommandCount) {
		Controller.kickCommandCount = kickCommandCount;
	}


	public static Vector2d getDriveEndPoint() {
		return driveEndPoint;
	}


	public static Vector2d getKickEndPoint() {
		return kickEndPoint;
	}


	public static boolean isChasingToKickBall() {
		return isChasingToKickBall;
	}


	public static void setChasingToKickBall(boolean isChasingToKickBall) {
		Controller.isChasingToKickBall = isChasingToKickBall;
	}


	public static boolean isDrivenByUser() {
		return isDrivenByUser;
	}


	public static void setDrivenByUser(boolean isDrivenByUser) {
		Controller.isDrivenByUser = isDrivenByUser;
	}

}
