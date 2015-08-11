package soccer.client.action;

/**
 *
 */
public class DisplayMoreAction extends AbstractToggleAction {
	
	private static final long serialVersionUID = 1702767197456842120L;
	
	public DisplayMoreAction() {
		super();
		putValue(NAME, "Display player reach area and ball passes");
		//setAccelerator( KeyEvent.VK_G, Event.CTRL_MASK );
		setEnabled( true );
		setToggledOn( true );
	}
	/**
	 * The toggle was changed
	 */
	protected void toggleStateChanged() {
		getSoccerMaster().setDisplayMore(isToggledOn());

	}
}
