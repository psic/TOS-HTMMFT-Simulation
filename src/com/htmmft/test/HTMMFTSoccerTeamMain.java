/* AIPlayers.java
   The AI players start program

   Copyright (C) 2001  Yu Zhang
   Modified by Vadim Kyrylov (October 2004)

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

package com.htmmft.test;

import java.io.*;
import java.util.*;
import java.net.*;

import soccer.tos_teams.sfu.AIPlayer;
import soccer.tos_teams.sfu.Formation;
import soccer.tos_teams.sfu.SoccerTeamMain;
import soccer.common.*;


public class HTMMFTSoccerTeamMain extends SoccerTeamMain
{
	
	public HTMMFTSoccerTeamMain()
	{
		super();
	} 



	public static void main(String argv[]) throws IOException
	{
		System.out.print("Starting HTMMFTAIPlayers v.1.2.4 ... argv[] = ");
		for( int i = 0 ; i < argv.length ; i ++ )
			System.out.print( argv[i] + ", ");
		System.out.println();
		Properties properties = new Properties();
		String configFileName = null;

		try {
			// First parse the parameters, if any
			for (int c = 0; c < argv.length; c += 2) {
				if (argv[c].compareTo("-pf") == 0) {
					configFileName = argv[c + 1];
					File file = new File(configFileName);
					if (file.exists()) {
						System.out.println("Load properties from file: "
								+ configFileName);
						properties = new Properties();
						properties.load(new FileInputStream(configFileName));
					} else {
						System.out.println("Properties file <" + configFileName
								+ "> does not exist. Using defaults.");
					}
				} else {
					System.out.println("Wrong arguments for the Properties file. Using defaults.");
					throw new Exception();
				}
			}
		} catch (Exception e) {
			System.err.println("");
			System.err.println("USAGE: SoccerTeamMain -pf property_file_name]");
			return;
		}

		setProperties(properties);
		// run as a standalone application with a set of threaded players  
		// communicating with the rest of TOS bundle over the network
		new HTMMFTSoccerTeamMain();

	}    
}
