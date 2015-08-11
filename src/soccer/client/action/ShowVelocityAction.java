package soccer.client.action;

public class ShowVelocityAction  extends AbstractToggleAction {
	
	private static final long serialVersionUID = -6461018577218413819L;

	public ShowVelocityAction() {
		super();
		putValue(NAME, "Display Velocities");
		//setAccelerator( KeyEvent.VK_G, Event.CTRL_MASK );
		setEnabled(true);
		setToggledOn(false);
	}
	/**
	 * The toggle was changed
	 */
	protected void toggleStateChanged() {
		getSoccerMaster().setShowVelocity( isToggledOn() );
	}
}
