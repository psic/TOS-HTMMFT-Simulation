package com.htmmft.JSONObserver;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import soccer.common.Field_Constants;

public class Move {
	
	private char type;
	private double x;
	private double y;
	private double dir;
	private double spd;
	private int beginTime;
	private int endTime;
	private DecimalFormat df;
	
	
	public Move(double x2, double y2, double dir, double spd,int time) {
		x=x2;
		y=y2;
		this.dir=dir;
		this.spd=spd;
		beginTime = time;
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');
		//otherSymbols.setGroupingSeparator('.'); 
		//df = new DecimalFormat ("#######0.00",otherSymbols ) ; 
		df = new DecimalFormat ("#######0",otherSymbols ) ; 
		
	}

	public Move(double x2, double y2, int time, char c) {
		x=x2;
		y=y2;
		beginTime = time;
		endTime = time *2;
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');
		//otherSymbols.setGroupingSeparator('.'); 
		//df = new DecimalFormat ("########0.00",otherSymbols ) ; 
		df = new DecimalFormat ("#######0",otherSymbols ) ; 
		type=c;
	}

	public void setEndTime(int end){
		endTime = end;
	}

	public boolean sameMove(Move lastmove) {
		if (lastmove != null){
		//if (lastmove.spd == spd && lastmove.dir == dir)
		if (lastmove.dir == dir){

			//System.out.println("Same Move");
			return true;
		}
		else
			return false;
		}
		return false; 
	}

	public boolean isWaiting(Move lastmove) {
		if (lastmove != null){
		if (lastmove.x == x && lastmove.y == y && lastmove.spd == spd && lastmove.dir == dir)
			return true;
		else
			return false;
		}
		return false; 
	}

	public void setType(char c) {
		type=c;
		
	}
	
	public int length(){
		return endTime - beginTime;
	}
	
	public String toString(boolean first){
		String result="";
		if (first){
			result = "M" + df.format(( (x+Field_Constants.LENGTH/2 ) / Field_Constants.LENGTH*660)+30 )  + "," + df.format(( (y+Field_Constants.WIDTH/2) / Field_Constants.WIDTH *430) +10 ) ;
			return result;
		}
		result += type;
		if (type == 'M')
			result += df.format(( (x+Field_Constants.LENGTH/2 ) / Field_Constants.LENGTH*660)+30 ) + "," + df.format(( (y+Field_Constants.WIDTH/2) / Field_Constants.WIDTH *430) +10) + "," ;
		//result += df.format(length()/10.0);
		result +=  length();
		return result;
		
	}

	public int getTime() {
		return beginTime;
	}

	public String debug() {
	   return " " + x + " / " + y + " : " + spd + " : " + dir;
	}

	public boolean sameBallMove(Move balllastmove) {
		if (balllastmove != null){
			if (balllastmove.x == x && balllastmove.y == y)
				return true;
			else
				return false;
			}
			return false; 
	}
}
