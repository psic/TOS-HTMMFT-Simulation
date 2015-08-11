package com.htmmft.JSONObserver;

import java.io.PrintWriter;
import java.util.ArrayList;

public class JSONMoveData {
	
	private int id;
	private ArrayList<Move> moves = new ArrayList<Move>();
	
	public JSONMoveData(int id) {
		this.id=id;
	}

	public int getId() {
		return id;
	}

	public void add(Move move) {
		moves.add(move);	
	}

	public Move getLastMove() {
		if(moves.size()!=0){
			return moves.get(moves.size()-1);			
		}
		return null;
	}

	public String toString(boolean isright) {
		String result="";
		int thisID =id;
		if (isright)
			thisID +=11;
		result ="\"" + thisID + "\":\"";
		boolean first = true;
		for(Move move : moves){
			result += move.toString(first);
			first=false;
		}
		result += "\",";
		return result;
	}
}
