package com.htmmft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.htmmft.server.HTMMFTSoccerServerMain;
import com.htmmft.video.ViewerThread;

public class MatchIterator  implements Iterator<Match>{
	final static String R_MATCH = "SELECT matchs.id, matchs.num_journee FROM matchs, annees WHERE matchs.num_journee = annees.journee";
	final static String R_TACTIQUE_EQUIPE1 = "SELECT tactique_equipe1, equipe1_id FROM matchs WHERE matchs.id =";
	final static String R_TACTIQUE_EQUIPE2 = "SELECT tactique_equipe2, equipe2_id FROM matchs WHERE matchs.id =";
	final static String R_DETAIL_EQUIPE = "SELECT equipes.nom, clubs.couleur1, clubs.couleur2 FROM equipes, clubs WHERE equipes.id = clubs.id AND equipes.id =";
	final static String R_POSITIONS = "SELECT position_j1_id, position_j2_id, position_j3_id, position_j4_id, position_j5_id, position_j6_id, " +
								"position_j7_id, position_j8_id, position_j9_id, position_j10_id, position_j11_id FROM tactiques " +
								"WHERE tactiques.id =";
	final static String R_JOUEUR = "SELECT age,xp,talent,tactique,technique,physique,vitesse,mental,off,def,drt,ctr,gch,cond,blessure,moral " +
									"FROM joueurs WHERE id =";
	final static String R_POSITION = "SELECT id_joueur, x, y FROM positions WHERE id =";
	//final static String R_UPD_SCORE = "SELECT id_joueur, x, y FROM positions WHERE id = ?";
	
	public final static int min_x = 30;
	public final static int min_y = 10 ;
	public final static int max_x = min_x + 660;
	public final static int max_y = min_y + 430;
	public final static int mid_x = (max_x - min_x) /2;
	public final static int mid_y = (max_y - min_y) /2;

	final static String MATCHS_FOLDER="./matchs";
	
	private static ArrayList<Match> matchs;
	private static Base BD_access;
	private int index =0;
	private int matchIndex =0;
	private static ExecutorService executor;
	
	public static void main(String[] args) throws IOException{
		 MatchIterator matchIt = new MatchIterator();
		// create service process for executing threads
			executor =  Executors.newCachedThreadPool();	
			
		 while(matchIt.hasNext()){
			Match currentMatch = matchIt.next();
			System.out.println("\n\n****************************************************************** Match Start " + currentMatch.getId() + " ************************************************************");

			//SoccerServerMainHTMMFT.startWithProperties("server.ini", currentMatch);
			HTMMFTSoccerServerMain server = new HTMMFTSoccerServerMain(currentMatch, "server.ini");
			server.start();
			while (server.isAlive()){
				
			}
			System.out.println("\n\n****************************************************************** Match Fini " + currentMatch.getId() + " ************************************************************");
			ViewerThread viewer = new ViewerThread(currentMatch);
			System.out.println("\n\n****************************************************************** Write Match Video" + currentMatch.getId() + " ************************************************************");
			//viewer.start();
			executor.submit(viewer);
		 }
		}
	
	
	public MatchIterator() throws IOException{
		 BD_access = new Base();
		 BD_access.connect();
		 matchs =new ArrayList<Match>();
		 matchs = BD_access.getMatch(R_MATCH, R_TACTIQUE_EQUIPE1, R_TACTIQUE_EQUIPE2,R_DETAIL_EQUIPE, R_POSITIONS, R_POSITION, R_JOUEUR);
		 jouerMatch();
		 BD_access.close();
		 index =0;
		 //System.out.println("FINI!!");
		}

	private static void jouerMatch() throws IOException {
		for (Match match :matchs){
			if (match.getId() == 114)
				//match.jouer();
				;
		}
	}

	@Override
	public boolean hasNext() {
//		if (index == matchs.size()-1)
//			return false;
		if(index >= 1)
			return false;
		return true;
	}

	@Override
	public Match next() {
		if (index == matchs.size()-1)
			return null;
		return matchs.get(index++);
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	public Match current() {
		return matchs.get(index);
	}



}
