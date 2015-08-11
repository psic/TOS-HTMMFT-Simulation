/* Replayer.java
   This class get display info from a log file, and display it.

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
 */

package com.htmmft.video;

import soccer.client.ViewerWorld;
import soccer.common.*;

import java.awt.event.WindowEvent;
import java.util.*;

import java.awt.Toolkit;
import java.io.IOException;

public class Replayer2D extends Thread {

	public static int RATE = 50;
	//public static int RATE = 100;

	public static final int PLAY = 1;
	public static final int FORWARD = 2;
	public static final int PAUSE = 3;
	public static final int BACK = 4;
	public static final int REWIND = 5;

	public static final int SKIP = 10;

	private static ViewerClientLogMain soccerMaster;
	private ViewerWorld world; //world view

	private int status = Replayer2D.PAUSE;
	private Stack<Long> positions = new Stack<Long>();
	private boolean end = false;

	private Packet info = new Packet();
	private String str = null;
	private VideoWriter videoW;


	public Replayer2D(ViewerClientLogMain soccerMaster) {
		this.soccerMaster = soccerMaster;
		world = new ViewerWorld();
		soccerMaster.setWorld(world);
		soccerMaster.arena2D.setWorld(world);
		videoW = new VideoWriter(soccerMaster);
		videoW.start();
	}

	public void run() {
		end = false;
		boolean start = false;

		//GOTO begining of the match
		try {
		while (!start) {
				str = soccerMaster.logFile.readLine();
				if (str != null) {
					info.readPacket(str);
				}
				else{
					start=true;
					System.out.println("Chelou fin de fichier");
				}
				start = isBeginingGame(info);		
		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		while (!end) {
			try {

				// get the time before the step
				long timeBefore = System.currentTimeMillis();
			/**	positions.push(
						new Long(soccerMaster.logFile.getFilePointer()));**/
				str = soccerMaster.logFile.readLine();

				if (str != null) {
					info.readPacket(str);
				}
				else{
					System.out.println("Fin prématuré du fichier input");
					break;
				}

				viewing(info);
				end = isEndingGame(info);
				
				// get the time after the step
				long timeAfter = System.currentTimeMillis();

				// figure out how long it takes to process
				long timeSpent = timeAfter - timeBefore;

				if (timeSpent < RATE)
					sleep((int) (RATE - timeSpent));

			} catch (Exception e) {
					System.out.println(e.getMessage());
			}

		}
		//System.out.println("ouo");
		videoW.end();
		videoW.write();
		//attention ça ferme tout ça!
		//TODO
		
		//WindowEvent wev = new WindowEvent(soccerMaster, WindowEvent.WINDOW_CLOSING);
         //Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
		
		//soccerMaster.getContentPane().dispatchEvent(new WindowEvent(soccerMaster, WindowEvent.WINDOW_CLOSING));
		
		this.interrupt();
		
	}

	public void setStatus(int s) {
		status = s;
	}

	public int getStatus() {
		return status;
	}

	public void end() {
		end = true;
	}

	private boolean isBeginingGame(Packet info){
		if (info.packetType == Packet.REFEREE) {
			RefereeData ref = (RefereeData) info.data;
			//System.out.println("periodPreGame : "+ref.period);
			if (ref.period == RefereeData.PRE_GAME){
				return true;
			}
			else
				return false;
		}
		return false;
	}

	private boolean isEndingGame(Packet info){
		if (info.packetType == Packet.REFEREE) {
			RefereeData ref = (RefereeData) info.data;
			//System.out.println("periodInGame : "+ref.period);
			if (ref.period == RefereeData.GAME_OVER ){
				return true;
			}
			if (ref.period == RefereeData.PRE_GAME){
				return true;
			}
		}
		return false;
	}


	private void viewing(Packet info) {

		int min;
		int sec;

		// process the info
		if (info.packetType == Packet.VIEW) {
			world.setViewData((ViewData) info.data);

			world.setMe(null);
			world.setBall(world.getViewData().ball);
			world.setLeftTeam(world.getViewData().leftTeam);
			world.setRightTeam(world.getViewData().rightTeam);

			// get the time information
			sec = world.getViewData().time / (1000 / RATE);
			min = sec / 60;
			sec = sec % 60;

			soccerMaster.timeJLabel.setText(min + ":" + sec);

			// find out if somebody has kicked the ball
			if (world.getBall().controllerType != world.getPreviousController()) {
				world.setPreviousController(world.getBall().controllerType);
				if (world.getBall().controllerType == 'f') {
					if (status == PLAY);
//						soccerMaster.getSoundSystem().playClip("kick");
				}

			}
			soccerMaster.arena2D.repaint();
		} else if (info.packetType == Packet.HEAR) {
			world.setHearData((HearData) info.data);
			if (world.getHearData().side == 'l')
				world.setLeftMhearData(world.getHearData());
			else if (world.getHearData().side == 'r')
				world.setRightMhearData(world.getHearData());
		} else if (info.packetType == Packet.REFEREE) {
			world.setRefereeData((RefereeData) info.data);

			// get the time information
			sec = world.getRefereeData().time / (1000 / RATE);
			min = sec / 60;
			sec = sec % 60;

			soccerMaster.periodJLabel.setText(
					RefereeData.periods[world.getRefereeData().period] );
			soccerMaster.timeJLabel.setText(min + ":" + sec);

			soccerMaster.leftName.setText(world.getRefereeData().leftName);
//			String scoreL = world.getRefereeData().score_L 
//					+ " ("+ world.getRefereeData().total_score_L + ")";
//			soccerMaster.leftScore.setText(":" + scoreL );
			
			String scoreL = Integer.toString(world.getRefereeData().score_L );					
			soccerMaster.leftScore.setText( scoreL );

			soccerMaster.rightName.setText(world.getRefereeData().rightName);
//			String scoreR = world.getRefereeData().score_R 
//					+ " ("+ world.getRefereeData().total_score_R + ")";
			String scoreR = Integer.toString(world.getRefereeData().score_R );
			soccerMaster.rightScore.setText( scoreR );

		}

	}
}