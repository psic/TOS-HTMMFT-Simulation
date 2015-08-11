package soccer.tos_teams.sfu;

import soccer.common.Vector2d;

//----------------------------------------------------------
/**
 * A class for wrapping ball pass data
 * @author vkyrylov
 *
 */
public class BallPassData {
	
	private double direction;
	private double force;
	private double risk;
	private double gain;
	private Vector2d endPoint;
	private int receiverID = -1;
	
	public BallPassData() {}
	
	BallPassData(double d, double f, double r, double g, Vector2d ep)
	{
		this.setDirection(d);
		this.setForce(f);
		this.setRisk(r);
		this.setGain(g);
		this.setEndPoint(ep);
	}

	/**
	 * @return the risk
	 */
	public double getRisk() {
		return risk;
	}

	/**
	 * @param risk the risk to set
	 */
	public void setRisk(double risk) {
		this.risk = risk;
	}

	/**
	 * @return the direction
	 */
	public double getDirection() {
		return direction;
	}

	/**
	 * @param direction the direction to set
	 */
	public void setDirection(double direction) {
		this.direction = direction;
	}

	/**
	 * @return the receiverID
	 */
	public int getReceiverID() {
		return receiverID;
	}

	/**
	 * @param receiverID the receiverID to set
	 */
	public void setReceiverID(int receiverID) {
		this.receiverID = receiverID;
	}

	/**
	 * @return the gain
	 */
	public double getGain() {
		return gain;
	}

	/**
	 * @param gain the gain to set
	 */
	public void setGain(double gain) {
		this.gain = gain;
	}

	/**
	 * @return the endPoint
	 */
	public Vector2d getEndPoint() {
		return endPoint;
	}

	/**
	 * @param endPoint the endPoint to set
	 */
	public void setEndPoint(Vector2d endPoint) {
		this.endPoint = endPoint;
	}

	/**
	 * @return the force
	 */
	public double getForce() {
		return force;
	}

	/**
	 * @param force the force to set
	 */
	public void setForce(double force) {
		this.force = force;
	}
}
