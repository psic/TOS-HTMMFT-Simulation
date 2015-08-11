/* ShowPLayerCoordAction.java
   
 	by Vadim Kyrylov
 	2010
*/
package soccer.client.action;

public class ShowPlayerCoordAction  extends AbstractToggleAction {

	private static final long serialVersionUID = -7341164852259733351L;

	public ShowPlayerCoordAction() {
		super();
		putValue(NAME, "Display Player Coordinates");
		//setAccelerator( KeyEvent.VK_G, Event.CTRL_MASK );
		setEnabled(true);
		setToggledOn(false);
	}
	
	/**
	 * The toggle was changed
	 */
	protected void toggleStateChanged() {
		getSoccerMaster().setShowPlayerCoord( isToggledOn() );
	}
}
