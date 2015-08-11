package com.htmmft.JSONObserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import soccer.common.KickData;
import soccer.common.Packet;
import soccer.common.Player;
import soccer.common.ViewData;

public class JSONObserver extends soccer.tos_tools.Observer {
	
	public 	String 		JSONfilename = "match.json";
	protected PrintWriter json_writer; 
	private ArrayList<JSONMoveData> leftTeamMoves = new ArrayList<JSONMoveData>();
	private ArrayList<JSONMoveData> rightTeamMoves = new ArrayList<JSONMoveData>();
	private ArrayList<Move> ballMoves = new ArrayList<Move>();
	private ArrayList<KickData> kickdatas = new ArrayList<KickData>();
	
	public JSONObserver(){
		super();
	}
	
	
	@Override
	protected void handleKickPacket( Packet packet )   
	{
		KickData kickdata = (KickData)packet.data;
		kickdatas.add(kickdata);
		
	}
	// this method could be customized for colecting different data and
	// possibly preprocessing them before writing to the file.
	// as it, it records ball location and ball possession data only.
	// the 'println' method here writes a line to the text file
	@Override
	protected void writeFile( ViewData view )   
	{
		
		if ( recordCount == 0 ){
			//System.out.println("*******************************************************       CREATE ARRAY");
			createMoveArray(view);
		}
		//TODO verifier ça
		for (Player player : view.leftTeam){
			Move move= getMove(player, view.time);
			if (move!=null){
				for (JSONMoveData playermove : leftTeamMoves){
					if (playermove.getId() == player.getId())
						playermove.add(move);
				}
			}
		}
		
		for (Player player : view.rightTeam){
			Move move= getMove(player,view.time);
			if (move!=null){
				for (JSONMoveData playermove : rightTeamMoves){
					if (playermove.getId() == player.getId())
						playermove.add(move);
				}
			}
		}
		
		//ballmMoves.add(new Move(view.ball.getPosition().getX(),view.ball.getPosition().getY(),  view.time,'M'));
		Move ballmove = new Move(view.ball.getPosition().getX(),view.ball.getPosition().getY(),view.ball.getDirection(), view.ball.getSpeed(),  view.time);
		ballmove.setType('M');
		ballMoves.add(ballmove);
		//System.out.println(view.ball.getDirection());
		/**if (ballMoves.size() == 0){
			ballMoves.add(ballmove);
		}
		else{
			Move balllastmove = ballMoves.get(ballMoves.size() -1);
			balllastmove.setEndTime(view.time);
			ballmove.setType('M');
			ballMoves.add(ballmove);
		//	ballMoves.set(view.time, ballmove);
//			if (!(ballmove.sameBallMove(balllastmove))){
//				balllastmove.setEndTime(view.time);
//				//ballMoves.set(ballMoves.size() -1, balllastmove);
//				ballmove.setType('M');
//				ballMoves.add(ballmove);				
//			}
//			else{
//				System.out.println("last : " + balllastmove.debug());
//				System.out.println("current : " + ballmove.debug());
//
//			}
		}**/
		

		//new Move(x,y,dir,spd, time);
		String record = view.time + "     " +
						(int)( view.ball.getPosition().getX()*100 )  + "    " + 
						(int)( view.ball.getPosition().getY()*100 ) + "        " + 
						view.ball.controllerType; 
		//System.out.println(record);
		if ( recordCount == 0 )
			print_writer.println( "step   ball_X    ball_Y    side" );
		
		print_writer.println( record );
		
		if (print_writer.checkError ()) {
            System.out.println("An output error occurred!" );
        }

	}
	
	private void createMoveArray(ViewData view) {
		
		for (Player player : view.leftTeam){
			leftTeamMoves.add(new JSONMoveData(player.getId()));
		}
		
		for (Player player : view.rightTeam){
			rightTeamMoves.add(new JSONMoveData(player.getId()));
		}
		
	}
	
	/**
	 * Return the current move if is new, null either
	 * @param player
	 * @param time
	 * @return
	 */
	private Move getMove(Player player,int time) {
		double x = player.getPosition().getX();
		double y=  player.getPosition().getY();
		double dir = player.getDirection();
		double spd = player.getSpeed();
		Move move = new Move(x,y,dir,spd, time);
		Move lastmove = null;
		int playerIndex=0;
		int i=0;
		boolean leftteam = false;
		if (player.getSide() == 'r'){
			leftteam = false;
			for(JSONMoveData  playermove : rightTeamMoves){
				 i++;
				if (playermove.getId() == player.getId()){
					lastmove = playermove.getLastMove();
					playerIndex = i;
				}
			}
		}
		else{
			leftteam = true;
			for(JSONMoveData  playermove : leftTeamMoves){
				i++;
				if (playermove.getId() == player.getId()){
					lastmove = playermove.getLastMove();
					playerIndex = i;
				}
			}		
		}
		if (lastmove == null){
			move.setType('W');
			return move;
		}
		
		if (move.isWaiting(lastmove)){
			lastmove.setType('W');
			return null;
		}
		else{
			//lastmove.setEndTime(time);
			if (move.sameMove(lastmove))
				return null;			
			lastmove.setEndTime(time);
			move.setType('M');
			return move;
		}
	}
	
	protected boolean fileCreated() 
	{
		boolean result1 = super.fileCreated();
		boolean result2 = false;

		File logFile = new File( JSONfilename  );
		FileWriter file_writer;
		try {
			file_writer = new FileWriter( logFile );
			BufferedWriter buf_writer = new BufferedWriter ( file_writer) ;
			// this is a class variable
			json_writer = new PrintWriter ( buf_writer, true );
			System.out.println("Created file << " + JSONfilename + " >>\n");
			result2 = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (result1 && result2);
	}

	
	public static void main(String argv[]) throws IOException 
	{
		Properties properties = new Properties();
		String configFileName = null;

		System.out.println("HTMMFT Observer started \n");
		try {
			//	  First look for parameters
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
								+ "> does not exist.");
					}
				} else {
					System.out.println("Error parsing argument");
					throw new Exception();
				}
			}
		} catch (Exception e) {
			System.err.println("");
			System.err.println("USAGE: Observer -pf property_file_name]");
			return;
		}

		JSONObserver HTMMFTObs = new JSONObserver();
		if ( HTMMFTObs.init() ){
			final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(1);
			HTMMFTThreadPool executor = new HTMMFTThreadPool(1, 1, 10, TimeUnit.SECONDS, queue);
			executor.execute(HTMMFTObs);
			//executor.afterExecute(HTMMFTObs, null);
		}
			//HTMMFTObs.start();
	}
		

	
	public void writeJsonFile() {
		System.out.println("Saving JSON file");
		json_writer.print("{\"0\":\"");
		String ballmove="";
		boolean first = true;
		for(Move  move: ballMoves){
				ballmove += move.toString(true);
				first = false;
		}
		ballmove +="\",";
		json_writer.println(ballmove);
		for(JSONMoveData playermove : leftTeamMoves){
			json_writer.println(playermove.toString(false) );
		}
		String buff ="";
		for(JSONMoveData playermove : rightTeamMoves){
			buff += playermove.toString(true) + "\n";
		}
		if (buff.length() > 2)
			buff=buff.substring(0, buff.length()-2);
		json_writer.println(buff);
		json_writer.println("}");
		
		System.out.println("Saving JSON file finish");
	}
}
