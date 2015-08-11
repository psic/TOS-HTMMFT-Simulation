package com.htmmft;

import java.util.ArrayList;

import com.htmmft.MatchIterator;

public class Equipe {
	private ArrayList<Joueur> joueurs;
	private int equipe_id;
	private int couleur1;
	private int couleur2;
	private String nom;
	private int tactique_id;
	private boolean sens; // true pour E1, false pour E2
	private Equipe adversaire;
	private Match match;

	
	public Equipe(int tac_id, boolean sens, Match match) {
		joueurs = new ArrayList<Joueur>();
		this.sens = sens;
		setTactique_id(tac_id);
		this.setMatch(match);
	}

	/**
	 * @return the tactique_id
	 */
	public int getTactique_id() {
		return tactique_id;
	}

	/**
	 * @param tactique_id the tactique_id to set
	 */
	public void setTactique_id(int tactique_id) {
		this.tactique_id = tactique_id;
	}

	public void addJoueur(Joueur joueur) {
		joueurs.add(joueur);

	}

	/**
	 * @return the sens
	 */
	public boolean isSens() {
		return sens;
	}

	/**
	 * @param sens the sens to set
	 */
	public void setSens(boolean sens) {
		this.sens = sens;
	}

	

	public void setAdversaire(Equipe equipe) {
		adversaire = equipe;
//		for (Joueur joueur : joueurs){
//			joueur.setAdversaire(equipe);
//		}
	}

	public ArrayList<Joueur> getJoueur(){
		return joueurs;
	}

	/**
	 * @return the match
	 */
	public Match getMatch() {
		return match;
	}

	/**
	 * @param match the match to set
	 */
	public void setMatch(Match match) {
		this.match = match;
	}

	public void setCouleur1(int int1) {
		couleur1 = int1;
		
	}

	public void setCouleur2(int int1) {
		couleur2 = int1;
		
	}

	public void setNom(String nom) {
		this.nom = nom;
		
	}

	public String getNom() {
		return nom;
	}
	
	public int getcouleur1() {
		return couleur1;
	}
	public int getcouleur2() {
		return couleur2;
	}
}
