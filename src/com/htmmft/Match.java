package com.htmmft;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.htmmft.MatchIterator;

public class Match{

	private Equipe equipe1;
	private Equipe equipe2;
	private int id;
	private int num_journee;
	
	
	public Match(int id, int numjournee) {
		this.id =id;
		this.setNum_journee(numjournee);
	}
	
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * @return the equipe1
	 */
	public Equipe getEquipe1() {
		return equipe1;
	}
	/**
	 * @param equipe1 the equipe1 to set
	 */
	public void setEquipe1(Equipe equipe1) {
		this.equipe1 = equipe1;
	}
	/**
	 * @return the equipe2
	 */
	public Equipe getEquipe2() {
		return equipe2;
	}
	/**
	 * @param equipe2 the equipe2 to set
	 */
	public void setEquipe2(Equipe equipe2) {
		this.equipe2 = equipe2;
	}
	public int getNum_journee() {
		return num_journee;
	}
	public void setNum_journee(int num_journee) {
		this.num_journee = num_journee;
	}
}