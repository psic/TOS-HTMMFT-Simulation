/* MovingObj.java

   Copyright (C) 2001  Yu Zhang
   Modifications by Vadim Kyrylov (2004-2010)

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

package soccer.common;

/**
 * This is a generic template for moving 
 * objects such as ball and players.
 * It represents the state vector of a point in 2D 
 */

public abstract class MovingObj {
	
	protected Vector2d position;
	private Vector2d velocity;	
	private Vector2d acceleration;
	private double direction;	// velocity vector direction in degrees
	private double speed;		// velocity vector magnitude

	public MovingObj() 	{
		position = new Vector2d();
		velocity = new Vector2d();
		acceleration = new Vector2d();
		direction = 0;
		speed = 0;
	}

	public Vector2d getPosition() {
		return position;
	}

	public void setPosition(Vector2d position) {
		this.position = position;
	}
	
	public Vector2d getVelocity() {
		return velocity;
	}
	
	public void setVelocity(Vector2d velocity) {
		this.velocity = velocity;
		calcDirectionAndSpeed();
	}
	
	public Vector2d getAcceleration() {
		return acceleration;
	}
	
	public void setAcceleration(Vector2d acceleration) {
		this.acceleration = acceleration;
	}

	public double getDirection() {
		return direction;
	}

	/**
	 * set new direction and update velocity keeping speed intact
	 * @param direction
	 */
	public void setDirection(double direction) {
		this.direction = direction;
		velocity.setX(speed*Math.cos(Math.toRadians(direction))); 
		velocity.setY(speed*Math.sin(Math.toRadians(direction))); 
	}

	public double getSpeed() {
		return speed;
	}

	/**
	 * set new speed and update velocity keeping direction intact
	 * @param speed
	 */
	public void setSpeed(double speed) {
		this.speed = speed;
		velocity.setX(speed*Math.cos(Math.toRadians(direction))); 
		velocity.setY(speed*Math.sin(Math.toRadians(direction))); 
	}

	/**
	 * transform the velocity vector into polar coordinates
	 * @return
	 */
	public void calcDirectionAndSpeed() {
		direction = Vector2d.polar_dir(velocity);
		speed = Vector2d.polar_dist(velocity);
	}
	
}

