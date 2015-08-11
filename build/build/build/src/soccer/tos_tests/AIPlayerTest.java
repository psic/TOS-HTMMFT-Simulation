/*
 * @(#)AIPLayerTest.java 1.0 06/01/08
 *
 * 
 */
package soccer.tos_tests;

import soccer.common.*;
import soccer.tos_teams.sfu.*;

// An exampe of a harness for testing some features in the SFU basic team.
// In particular, this class tests the WorldModel.isInOwnPenaltyArea() method.
// Other methods can be tested in the similar way before running them together
// with the soccer server. 

// TODO: must be re-tested after the modifications made in January 2010

class AIPLayerTest  
{
	private PlayerWorld aWorldModel;
	//private 
	
	public AIPLayerTest() {
		init(); 
		
	}

	public static void main(String args[]) {
		System.out.println("Starting AIPLayerTest...");
		new AIPLayerTest();
	}
	
	
	private void init() 
	{
		Transceiver transceiver = new Transceiver(false);
		boolean isGoalie = false;
		aWorldModel = new PlayerWorld( transceiver, 'r', 8, isGoalie );
		System.out.println("===  World Model created. side = " + aWorldModel.getMySide() );
		
		boolean inside; 
		Vector2d point = null; 
		
		for (int i=0; i<8; i++ ) {
			switch ( i ) {
				case 0:
					point = new Vector2d( 0.0, 0.0 );
				break;
				case 1:
					point = new Vector2d( 45.0, 10.0 );
				break;
				case 2:
					point = new Vector2d( 35.0, -18.5 );
				break;
				case 3:
					System.out.println();
					aWorldModel = new PlayerWorld( transceiver, 'l', 8, isGoalie );
					System.out.println("===  World Model created. side = " + aWorldModel.getMySide() );
					point = new Vector2d( -5.0, -20.0 );
				break;
				case 4:
					point = new Vector2d( -45.0, 10.0 );
				break;
				case 5:
					point = new Vector2d( -32.3, -17.0 );
				break;
				case 6:
					point = new Vector2d( -35.0, -13.5 );
				break;
				case 7:
					point = new Vector2d( -55.0, -13.5 );
				break;
			}
			
			System.out.println();
			inside = World.inPenaltyArea('l', point, 0.0001 );
			System.out.println("point = " + point + " is inside = " + inside );
			System.out.println();
			inside = World.inPenaltyArea('l',  point, 0.0 );
			System.out.println("check in right, point = " + point + " is inside = " + inside );
			inside = World.inPenaltyArea('l',  point, 0.0 );
			System.out.println("check in left, point = " + point + " is inside = " + inside );
		}
	}
}
