/* Modified to make Field class a subclass of Arena class.  Also, world and
   methods setFont, setWorld, getWorld, and isFocusTraversable were moved to 
   Arena.
						          jdm, June 7 2001
*/
/* Field.java
   This class shows the field and players and ball.
   
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

	Modifications by Vadim Kyrylov 
							January 2006
*/

package com.htmmft.video;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.ImageIcon;
import soccer.client.view.Arena;
import soccer.common.*;

public class HTMMMFTField 	extends Arena {
	final static BasicStroke wideStroke = new BasicStroke(4.0f);
	final static BasicStroke stroke = new BasicStroke(1.0f);
	private static final long serialVersionUID = -5297807339632591166L;
	private static Color bg = Color.green.darker();
	private static Color fg = Color.red;
	
	private ViewerClientLogMain aSoccerMaster;

	private int ballPassCount = 0;
	private final int BALL_PASS_DISPLAY_LIMIT = 25;

	// the center of the moving object
	//private Vector2d c = new Vector2d();

	// for loops
	private Player player = null;
	private Enumeration<Player> players = null;

	
	/**
	 * Constructor
	 * 
	 * @param aSoccerMaster -- the application class instance
	 */
	public HTMMMFTField(ViewerClientLogMain aSoccerMaster) {
		this.aSoccerMaster = aSoccerMaster;
		
		//ImageIcon background = new ImageIcon("bin/imag/terrain.jpg");
		//JLabel label = new JLabel("", background, JLabel.CENTER);
		//add( label, BorderLayout.CENTER );
		
		//Initialize drawing colors, border, opacity.
		setBackground(bg);
		setForeground(fg);

		Dimension d =
			new Dimension(
				(int)( (TOS_Constants.LENGTH + TOS_Constants.SIDEWALK * 2) 
								* TOS_Constants.METER + 0.5 ),
				(int)( (TOS_Constants.WIDTH + TOS_Constants.SIDEWALK * 2) 
								* TOS_Constants.METER + 0.5 ) +3 );
		setPreferredSize(d);
		setMaximumSize(d);
		setMinimumSize(d);
		//setBorder(BorderFactory.createRaisedBevelBorder());
	}

	public void paintComponent(Graphics g) {

		// clears the background
		super.paintComponent(g);
		ImageIcon background = new ImageIcon("image/terrain.jpg");
		//g.drawImage(background.getImage(), 0,0 , null);
		g.drawImage(background.getImage(),(int)(TOS_Constants.SIDEWALK  * TOS_Constants.METER)-53,(int)(TOS_Constants.SIDEWALK  * TOS_Constants.METER)-13 ,(int)(TOS_Constants.LENGTH * TOS_Constants.METER) + 107,(int)(TOS_Constants.WIDTH * TOS_Constants.METER)+26, null);
		//g.drawImage(background.getImage(),(int)(TOS_Constants.SIDEWALK  * TOS_Constants.METER)-53,(int)(TOS_Constants.SIDEWALK  * TOS_Constants.METER)-13 ,(int)(TOS_Constants.LENGTH * TOS_Constants.METER) + 107,(int)(TOS_Constants.WIDTH * TOS_Constants.METER)+26, null);

		// draw the soccer field
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);

		// draw all static elements
		//drawStatics( g2 );
		
		// set font size to the player size
		setFont(g2, (int)(TOS_Constants.METER * 2), 6);

		// draws all moving objects
		if (world != null) {
			drawTeams( g2 );
			drawBall( g2 );
			drawBallPass(g2);
		}
	}
	

	
	/**
	 * draw ball pass, if any
	 */	
	private void drawBallPass(Graphics2D g2) {
		if (world.getReceiverID() >= 0 && aSoccerMaster.getDisplayMore()) {
			drawBallPassVector( g2 );
			ballPassCount++;
			if (ballPassCount > BALL_PASS_DISPLAY_LIMIT) {
				ballPassCount = 0;
				world.setReceiverID(-1);
			}
		}
	}

	
	// draw static elements
	private void drawStatics( Graphics2D g2 ) {
		// set line color
		Color lineColor = Color.WHITE;
		g2.setColor(lineColor);

		// boundary lines
		g2.draw(
			new Rectangle2D.Double(
					TOS_Constants.SIDEWALK * TOS_Constants.METER,
					TOS_Constants.SIDEWALK * TOS_Constants.METER,
					TOS_Constants.LENGTH * TOS_Constants.METER,
					TOS_Constants.WIDTH * TOS_Constants.METER));

		// halfway line
		g2.draw(
			new Line2D.Double(
				(TOS_Constants.LENGTH / 2.0 + TOS_Constants.SIDEWALK) 
							* TOS_Constants.METER,
							TOS_Constants.SIDEWALK * TOS_Constants.METER,
				(TOS_Constants.LENGTH / 2.0 + TOS_Constants.SIDEWALK) 
							* TOS_Constants.METER,
				(TOS_Constants.WIDTH + TOS_Constants.SIDEWALK) * TOS_Constants.METER));

		// center circle
		g2.draw(
			new Ellipse2D.Double(
				(TOS_Constants.LENGTH / 2.0 + TOS_Constants.SIDEWALK - TOS_Constants.RADIUS) * TOS_Constants.METER,
				(TOS_Constants.WIDTH / 2.0 + TOS_Constants.SIDEWALK - TOS_Constants.RADIUS) * TOS_Constants.METER,
				TOS_Constants.RADIUS * 2 * TOS_Constants.METER,
				TOS_Constants.RADIUS * 2 * TOS_Constants.METER));

		// left goal area
		g2.draw(
			new Rectangle2D.Double(
				TOS_Constants.SIDEWALK * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + (TOS_Constants.WIDTH - TOS_Constants.GOALAREA_WIDTH) / 2.0) * TOS_Constants.METER,
				TOS_Constants.GOALAREA_DEPTH * TOS_Constants.METER,
				TOS_Constants.GOALAREA_WIDTH * TOS_Constants.METER));

		// right goal area
		g2.draw(
			new Rectangle2D.Double(
				(TOS_Constants.SIDEWALK + TOS_Constants.LENGTH - TOS_Constants.GOALAREA_DEPTH) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + (TOS_Constants.WIDTH - TOS_Constants.GOALAREA_WIDTH) / 2.0) * TOS_Constants.METER,
				TOS_Constants.GOALAREA_DEPTH * TOS_Constants.METER,
				TOS_Constants.GOALAREA_WIDTH * TOS_Constants.METER));

		// left penalty area
		g2.draw(
			new Rectangle2D.Double(
				TOS_Constants.SIDEWALK * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + (TOS_Constants.WIDTH - TOS_Constants.PENALTY_WIDTH) / 2.0) * TOS_Constants.METER,
				TOS_Constants.PENALTY_DEPTH * TOS_Constants.METER,
				TOS_Constants.PENALTY_WIDTH * TOS_Constants.METER));

		// right penalty area
		g2.draw(
			new Rectangle2D.Double(
				(TOS_Constants.SIDEWALK + TOS_Constants.LENGTH - TOS_Constants.PENALTY_DEPTH) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + (TOS_Constants.WIDTH - TOS_Constants.PENALTY_WIDTH) / 2.0) * TOS_Constants.METER,
				TOS_Constants.PENALTY_DEPTH * TOS_Constants.METER,
				TOS_Constants.PENALTY_WIDTH * TOS_Constants.METER));

		// left penalty circle			       
		g2.draw(
			new Arc2D.Double(
				(TOS_Constants.PENALTY_CENTER - TOS_Constants.RADIUS + TOS_Constants.SIDEWALK) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2.0 - TOS_Constants.RADIUS) * TOS_Constants.METER,
				TOS_Constants.RADIUS * 2 * TOS_Constants.METER,
				TOS_Constants.RADIUS * 2 * TOS_Constants.METER,
				297,
				126,
				Arc2D.OPEN));

		// right penalty circle	
		g2.draw(
			new Arc2D.Double(
				(TOS_Constants.LENGTH - TOS_Constants.PENALTY_CENTER - TOS_Constants.RADIUS + TOS_Constants.SIDEWALK) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2.0 - TOS_Constants.RADIUS) * TOS_Constants.METER,
				TOS_Constants.RADIUS * 2 * TOS_Constants.METER,
				TOS_Constants.RADIUS * 2 * TOS_Constants.METER,
				117,
				126,
				Arc2D.OPEN));

		// left top corner
		g2.draw(
			new Arc2D.Double(
				(TOS_Constants.SIDEWALK - TOS_Constants.CORNER) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK - TOS_Constants.CORNER) * TOS_Constants.METER,
				TOS_Constants.CORNER * 2 * TOS_Constants.METER,
				TOS_Constants.CORNER * 2 * TOS_Constants.METER,
				270,
				90,
				Arc2D.OPEN));

		// left bottom corner
		g2.draw(
			new Arc2D.Double(
				(TOS_Constants.SIDEWALK - TOS_Constants.CORNER) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + TOS_Constants.WIDTH - TOS_Constants.CORNER) * TOS_Constants.METER,
				TOS_Constants.CORNER * 2 * TOS_Constants.METER,
				TOS_Constants.CORNER * 2 * TOS_Constants.METER,
				0,
				90,
				Arc2D.OPEN));

		// right top corner
		g2.draw(
			new Arc2D.Double(
				(TOS_Constants.SIDEWALK + TOS_Constants.LENGTH - TOS_Constants.CORNER) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK - TOS_Constants.CORNER) * TOS_Constants.METER,
				TOS_Constants.CORNER * 2 * TOS_Constants.METER,
				TOS_Constants.CORNER * 2 * TOS_Constants.METER,
				180,
				90,
				Arc2D.OPEN));

		// right bottom corner
		g2.draw(
			new Arc2D.Double(
				(TOS_Constants.SIDEWALK + TOS_Constants.LENGTH - TOS_Constants.CORNER) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + TOS_Constants.WIDTH - TOS_Constants.CORNER) * TOS_Constants.METER,
				TOS_Constants.CORNER * 2 * TOS_Constants.METER,
				TOS_Constants.CORNER * 2 * TOS_Constants.METER,
				90,
				90,
				Arc2D.OPEN));

		// center mark
		g2.fill(
			new Ellipse2D.Double(
				(TOS_Constants.SIDEWALK + TOS_Constants.LENGTH / 2.0) * TOS_Constants.METER - 2,
				(TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2.0) * TOS_Constants.METER - 2,
				5,
				5));

		// left penalty mark
		g2.fill(
			new Ellipse2D.Double(
				(TOS_Constants.SIDEWALK + TOS_Constants.PENALTY_CENTER) * TOS_Constants.METER - 2,
				(TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2.0) * TOS_Constants.METER - 2,
				5,
				5));

		// right penalty mark
		g2.fill(
			new Ellipse2D.Double(
				(TOS_Constants.SIDEWALK + TOS_Constants.LENGTH - TOS_Constants.PENALTY_CENTER) * TOS_Constants.METER - 2,
				(TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2.0) * TOS_Constants.METER - 2,
				5,
				5));

		// set goal color
		Color goalColor = Color.blue;
		g2.setColor(goalColor);

		// left goal
		g2.draw(
			new Rectangle2D.Double(
				(TOS_Constants.SIDEWALK - TOS_Constants.GOAL_DEPTH) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + (TOS_Constants.WIDTH 
						- TOS_Constants.GOAL_WIDTH) / 2.0) * TOS_Constants.METER,
				TOS_Constants.GOAL_DEPTH * TOS_Constants.METER,
				TOS_Constants.GOAL_WIDTH * TOS_Constants.METER));

		// right goal
		g2.draw(
			new Rectangle2D.Double(
				(TOS_Constants.SIDEWALK + TOS_Constants.LENGTH) * TOS_Constants.METER,
				(TOS_Constants.SIDEWALK + (TOS_Constants.WIDTH 
						- TOS_Constants.GOAL_WIDTH) / 2.0) * TOS_Constants.METER,
				TOS_Constants.GOAL_DEPTH * TOS_Constants.METER,
				TOS_Constants.GOAL_WIDTH * TOS_Constants.METER));
	}

	// draw ball 
	private void drawBall( Graphics2D g2 ) {
		
		if (world.getBall() != null) {

			// *** ball center in user (screen) coordinates                 
			Vector2d ballCenter = new Vector2d(world.getBall().getPosition());
			soccer2user(ballCenter);

			// the upper left corner of this moving object
			double xBall = ballCenter.getX() - TOS_Constants.BALLSIZE * TOS_Constants.METER;
			double yBall = ballCenter.getY() - TOS_Constants.BALLSIZE * TOS_Constants.METER;

			BufferedImage bi =
				new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
			Graphics2D big = bi.createGraphics();
			big.setColor(Color.WHITE);
			big.fillRect(0, 0, 4, 4);
			big.setColor(Color.BLACK);
			big.fillOval(0, 0, 4, 4);
			Rectangle r = new Rectangle(0, 0, 4, 4);
			g2.setPaint(new TexturePaint(bi, r));
			g2.fill(
				new Ellipse2D.Double(
					xBall,
					yBall,
					TOS_Constants.BALLSIZE * TOS_Constants.METER * 2.0,
					TOS_Constants.BALLSIZE * TOS_Constants.METER * 2.0));
		
			// draw ball coordinates and/or velocity
			if ( world.getBall().isGrabbed )
				drawSpot( g2, ballCenter.getX(), ballCenter.getY() );
			else {
				if ( aSoccerMaster.isShowBallCoord() ) 			
						drawObjCoordinates(g2, world.getBall());
				if ( aSoccerMaster.isShowVelocity() ) 			
					drawObjVelocity(g2, world.getBall(), Color.WHITE);
			}
		}		
	}
	
	// draw all players in both teams
	private void drawTeams( Graphics2D g2 )	{
		

		// *** draw left players
		if (world.getLeftTeam() != null) {
			players = world.getLeftTeam().elements();
			while (players.hasMoreElements()) {
				player = (Player) players.nextElement();
				drawPlayer( g2, player, aSoccerMaster.couleur11, aSoccerMaster.couleur12 );
			}
		}

		// *** draw right players
		if (world.getRightTeam() != null) {
			players = world.getRightTeam().elements();
			while (players.hasMoreElements()) {
				player = (Player) players.nextElement();
				drawPlayer( g2, player, aSoccerMaster.couleur21, aSoccerMaster.couleur22 );
			}
		}
	}
	

	// draw one player
	private void drawPlayer( Graphics2D g2, Player player, 
									Color teamColor1, Color teamColor2 ) {
		g2.setStroke( stroke );
		Vector2d pCenter = new Vector2d(player.getPosition());
		soccer2user(pCenter);
		// the upper left corner of this moving object
		double x = pCenter.getX() - TOS_Constants.PLAYERSIZE * TOS_Constants.METER;
		double y = pCenter.getY() - TOS_Constants.PLAYERSIZE * TOS_Constants.METER;

		// draw player (filled circle)
		g2.setColor(teamColor1);
		if ( player.isGoalie() ) {
			// mark the goalie
			g2.setColor(teamColor2);
			double scale = 0.7;
			x = pCenter.getX() - TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale;
			y = pCenter.getY() - TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale;
			g2.fill(
				new Ellipse2D.Double(
					x,
					y,
					TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale * 2,
					TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale * 2));
		}
		else{
		g2.fill(
			new Ellipse2D.Double(
				x,
				y,
				TOS_Constants.PLAYERSIZE * TOS_Constants.METER * 2,
				TOS_Constants.PLAYERSIZE * TOS_Constants.METER * 2));
		}
		
		// draw player ball reach circle 
		if (aSoccerMaster.getDisplayMore()) {
			//g2.setColor( Color.GREEN );
			g2.setColor( Color.WHITE );
			x = pCenter.getX() - TOS_Constants.BALLCONTROLRANGE * TOS_Constants.METER;
			y = pCenter.getY() - TOS_Constants.BALLCONTROLRANGE * TOS_Constants.METER;
			g2.draw(
				new Ellipse2D.Double(
					x,
					y,
					TOS_Constants.BALLCONTROLRANGE * TOS_Constants.METER * 2,
					TOS_Constants.BALLCONTROLRANGE * TOS_Constants.METER * 2));
		}
		
		// draw player ID (reuse xy-coordinates from ball reach)
		if (aSoccerMaster.isDisplayID()) {
			//g2.setColor(teamColor1);
			g2.setColor(Color.WHITE);
			g2.drawString(
				Integer.toString(player.getId()),
				(int) x,
				(int) y);
		}
				
		if ( player.isUserControlled() ) {
			// mark user controlled player
			g2.setColor(Color.BLUE);
			double scale = 0.4;
			x = pCenter.getX() - TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale;
			y = pCenter.getY() - TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale;
			g2.fill(
				new Ellipse2D.Double(
					x,
					y,
					TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale * 2,
					TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale * 2));
		}
				
		if ( player.isChasingBall() ) {
			// mark the player who is chasing the ball
			g2.setColor(Color.GREEN);
			double scale = 0.65;
			double x1 = pCenter.getX() - TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale;
			double y1 = pCenter.getY() - TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale;
			g2.fill(
				new Ellipse2D.Double(
					x1,
					y1,
					TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale * 2,
					TOS_Constants.PLAYERSIZE * TOS_Constants.METER * scale * 2));
		}

		// draw player coordinates and/or velocity
		if ( player.isGrabbed() )
			drawSpot( g2, pCenter.getX(), pCenter.getY() );
		else {
			if ( aSoccerMaster.isShowPlayerCoord() ) 			
				drawObjCoordinates(g2, player);
			if ( aSoccerMaster.isShowVelocity() ) 			
				drawObjVelocity(g2, player, teamColor1);
		}

		// draw dashing direction 
		if ( player.isGoalie() ) 
			g2.setColor(teamColor1);
		else
			g2.setColor(teamColor2);
		 g2.setStroke( wideStroke );
		g2.draw(
			new Line2D.Double(
				pCenter.getX(),
				pCenter.getY(),
				pCenter.getX()
					+ TOS_Constants.PLAYERSIZE
						* TOS_Constants.METER
						* Math.cos(Math.toRadians(player.getDirection())),
				pCenter.getY()
					- TOS_Constants.PLAYERSIZE 
						* TOS_Constants.METER
						* Math.sin(Math.toRadians(player.getDirection()))));
		g2.setStroke( stroke );
	}
	
	
	//=======   methods for drawing additional elements  =======
	
	/**
	 * This method draws a line displaying a vector from given object to 
	 * given point with or without receiver ID
	 */
	public void drawVector(Graphics2D g2, MovingObj obj, 
						Vector2d endPoint, boolean showID) {
		
		double r = 6.0;
		
		// draw the ball end point 
		Vector2d iPoint = new Vector2d(endPoint);
		soccer2user(iPoint);
		double xp = iPoint.getX() - r/2;
		double yp = iPoint.getY() - r/2;
		g2.draw( new Rectangle2D.Double( xp, yp, r, r ) );
		
		// draw the line between the ball and the end point                 
		Vector2d objCenter = new Vector2d(obj.getPosition());
		soccer2user(objCenter);
		double xb = objCenter.getX();
		double yb = objCenter.getY();
		g2.draw( new Line2D.Double( xp+r/2, yp+r/2, xb, yb ) );
		
		// show ball pass receiver ID, if any
		if (showID)
			g2.drawString(""+world.getReceiverID(), (float)xp+10, (float)yp-10 );		
	}


	// draw the ball interception point and the line from the ball to it
	public void drawBallPassVector( Graphics2D g2 ) {
		if (world.getReceiverSide() == 'l' )
			g2.setColor( Color.CYAN );
		else if (world.getReceiverSide() == 'r' )
			g2.setColor( Color.MAGENTA );
		else
			return;	// do nothing
		
		drawVector(g2, world.getBall(), world.getEndPoint(), true);
	}
	
	
	// draw coordinates of moving object
	private void drawObjCoordinates(Graphics2D g2, MovingObj obj) {
		g2.setColor(Color.WHITE);
		Vector2d v = new Vector2d(obj.getPosition());
		// soccer coordinates
		double x = v.getX();
		double y = v.getY();
		// convert to screen coordinates
		soccer2user(v);
		g2.drawString((float)x + " ", (float)v.getX()+15, (float)v.getY()-10 );
		g2.drawString((float)y + " ", (float)v.getX()+15, (float)v.getY()-0 );		
	}
	
	/**
	 * This method draws the velocity vector of moving object
	 * 
	 * @param g2	-- graphics context
	 * @param obj	-- moving object
	 * @param nSteps	-- number of steps between object position updates
	 */
	private void drawObjVelocity(Graphics2D g2, MovingObj obj, Color clr) {
		Vector2d pos = new Vector2d(obj.getPosition());
		double stepsPerSecond = 1/TOS_Constants.SIM_STEP_SECONDS;
		// rate of change per cycle
		Vector2d vel = new Vector2d(obj.getVelocity());
		// rate of change per second
		vel = vel.timesV(stepsPerSecond);
		//System.out.println(world.getViewData().time + " Obj velocity = " + vel);
		// velocity vector endpoint
		Vector2d end = Vector2d.add(pos, vel); 
		// convert the coordinates from soccer to user scale
		soccer2user(pos);
		soccer2user(end);
		g2.setColor(clr);
		// draw velocity vector on the screen
		g2.draw(new Line2D.Double(pos.getX(), pos.getY(), end.getX(), end.getY()));
	}

	
	// draw a circle with given center point and its coordinates
	public void drawSpot( Graphics2D g2, double xspot, double yspot ) {
		g2.setColor( Color.ORANGE );
		for ( double r = 16.0; r < 21.0; r=r+2.0 )
			g2.draw( new Ellipse2D.Double( xspot-r/2, yspot-r/2, r, r ) );
		Vector2d v = new Vector2d( xspot, yspot ); 
		user2soccer( v ); 
		g2.drawString((float)v.getX() + " ", (float)xspot+10, (float)yspot-15 );
		g2.drawString((float)v.getY() + " ", (float)xspot+10, (float)yspot-5 );
	}
	

	/**
	 *  Coordinate System Conversions
	 *  
	 *  The user space is a device-independent logical coordinate system.
	 *  the coordinate space that your program uses. All geometries passed 
	 *  into Java 2D rendering routines are specified in user-space 
	 *  coordinates. The origin of user space is the upper-left corner of 
	 *  the component's drawing area. The x coordinate increases to the 
	 *  right, and the y coordinate increases downward.
	 *  
	 *  The soccer space is used in the soccer server. The origin of soccer 
	 *  space is the center of the soccer field. The x coordinate increases 
	 *  to the right, and the y coordinate increases upward.
	 */

	/**
	 *  convert from Java 2d user space to soccer space
	 */
	public void user2soccer(Vector2d p) {
		double x = p.getX() / TOS_Constants.METER;
		double y = p.getY() / TOS_Constants.METER;

		double origin_x = TOS_Constants.SIDEWALK + TOS_Constants.LENGTH / 2;
		double origin_y = TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2;

		x = x - origin_x;
		y = - (y - origin_y);

		p.setXY(x, y);

		return;

	}

	/**
	 *  convert from soccer space to Java 2d user space 
	 */
	public void soccer2user(Vector2d p) {
		double x = p.getX();
		double y = p.getY();

		double origin_x = (-TOS_Constants.SIDEWALK - TOS_Constants.LENGTH / 2);
		double origin_y = TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2;

		x = (x - origin_x) * TOS_Constants.METER;
		y = - (y - origin_y) * TOS_Constants.METER;

		p.setXY(x, y);

		return;

	}

}
