/* Sball.java
   
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
							(2006-2010)   
*/

package soccer.server;

import soccer.common.Ball;

/**
 *  This represents a ball object in the server world model 
 *  (ball properties, its state, and some behaviors).
 *  This is NOT what is transmitted over communication channels;
 *  only some parameters of the state vector are transmitted.
 */

public class Sball extends Ball {
	
	// place the ball at position(x,y)
	public void set(double x, double y) {
		this.getPosition().setXY(x,y);
		this.getVelocity().setXY(0,0);
		this.getAcceleration().setXY(0,0);
		controllerType = 'f';
		controllerId = 0;
		isFree = true;
		isGrabbed = false;
	}    
	
	// place the ball at the position of given player 
	public void setAtPlayerPos(Splayer player, boolean grabbed ) {
		this.getPosition().setXY( player.getPosition() );
		this.getVelocity().setXY( player.getVelocity() );
		this.getAcceleration().setXY(0,0);
		controllerType = player.getSide();
		controllerId = player.getId();	
		isGrabbed = grabbed;
		isFree = false;
	}   

	// reset Sball data using the input Ball object
	public void assign( Ball ball ) {
		this.set( ball.getPosition().getX(), ball.getPosition().getY() );
		controllerType = ball.controllerType; 
		controllerId = ball.controllerId; 
		isGrabbed = ball.isGrabbed; 
	}
	
	// copy Sball data using the input Sball oject
	public void copy( Sball sball ) {
		this.set( sball.getPosition().getX(), sball.getPosition().getY() );
		controllerType = sball.controllerType; 
		controllerId = sball.controllerId; 
		isGrabbed = sball.isGrabbed; 
	}
		
}
