package com.htmmft;

public class Joueur {
	private int id;
	protected int x_tactique;
	protected int y_tactique;

	private int age; 
	private int xp;
	private int talent=0;;
	private int tactique=0;;
	private int technique=0;; 
	protected int physique=0;;
	private int vitesse =0;
	private int mental=0;;
	private int off;
	private int def;
	private int drt;
	private int ctr;
	private int gch;
	private int cond;
	private int blessure;
	private int moral;
	private int numero;

	public Joueur(int id, int x, int y, Equipe equipe) {
		this.setId(id);
		this.x_tactique = x;
		this.y_tactique = y;
	}
	
	/******************* ACCESSOR *****************/
	
			/**
			 * @return the x
			 */
			public int getX() {
				return x_tactique;
			}
			/**
			 * @param x the x to set
			 */
			public void setX(int x) {
				this.x_tactique = x;
			}
			/**
			 * @return the y
			 */
			public int getY() {
				return y_tactique;
			}
			/**
			 * @param y the y to set
			 */
			public void setY(int y) {
				this.y_tactique = y;
			}
			/**
			 * @return the age
			 */
			public int getAge() {
				return age;
			}
			/**
			 * @param age the age to set
			 */
			public void setAge(int age) {
				this.age = age;
			}
			/**
			 * @return the xp
			 */
			public int getXp() {
				return xp;
			}
			/**
			 * @param xp the xp to set
			 */
			public void setXp(int xp) {
				this.xp = xp;
			}
			/**
			 * @return the talent
			 */
			public int getTalent() {
				return talent;
			}
			/**
			 * @param talent the talent to set
			 */
			public void setTalent(int talent) {
				this.talent = talent;
			}
			/**
			 * @return the tactique
			 */
			public int getTactique() {
				return tactique;
			}
			/**
			 * @param tactique the tactique to set
			 */
			public void setTactique(int tactique) {
				this.tactique = tactique;
			}
			/**
			 * @return the physique
			 */
			public int getPhysique() {
				return physique;
			}
			/**
			 * @param physique the physique to set
			 */
			public void setPhysique(int physique) {
				this.physique = physique;
			}
			/**
			 * @return the moral
			 */
			public int getMoral() {
				return moral;
			}
			/**
			 * @param moral the moral to set
			 */
			public void setMoral(int moral) {
				this.moral = moral;
			}
			/**
			 * @return the blessure
			 */
			public int getBlessure() {
				return blessure;
			}
			/**
			 * @param blessure the blessure to set
			 */
			public void setBlessure(int blessure) {
				this.blessure = blessure;
			}
			/**
			 * @return the cond
			 */
			public int getCond() {
				return cond;
			}
			/**
			 * @param cond the cond to set
			 */
			public void setCond(int cond) {
				this.cond = cond;
			}
			/**
			 * @return the gch
			 */
			public int getGch() {
				return gch;
			}
			/**
			 * @param gch the gch to set
			 */
			public void setGch(int gch) {
				this.gch = gch;
			}
			/**
			 * @return the ctr
			 */
			public int getCtr() {
				return ctr;
			}
			/**
			 * @param ctr the ctr to set
			 */
			public void setCtr(int ctr) {
				this.ctr = ctr;
			}
			/**
			 * @return the drt
			 */
			public int getDrt() {
				return drt;
			}
			/**
			 * @param drt the drt to set
			 */
			public void setDrt(int drt) {
				this.drt = drt;
			}
			/**
			 * @return the off
			 */
			public int getOff() {
				return off;
			}
			/**
			 * @param off the off to set
			 */
			public void setOff(int off) {
				this.off = off;
			}
			/**
			 * @return the def
			 */
			public int getDef() {
				return def;
			}
			/**
			 * @param def the def to set
			 */
			public void setDef(int def) {
				this.def = def;
			}
			/**
			 * @return the mental
			 */
			public int getMental() {
				return mental;
			}
			/**
			 * @param mental the mental to set
			 */
			public void setMental(int mental) {
				this.mental = mental;
			}
			/**
			 * @return the vitesse
			 */
			public int getVitesse() {
				return vitesse;
			}
			/**
			 * @param vitesse the vitesse to set
			 */
			public void setVitesse(int vitesse) {
				this.vitesse = vitesse;
			}
			/**
			 * @return the technique
			 */
			public int getTechnique() {
				return technique;
			}
			/**
			 * @param technique the technique to set
			 */
			public void setTechnique(int technique) {
				this.technique = technique;
			}
			public int getId() {
				return id;
			}
			public void setId(int id) {
				this.id = id;
			}
			
			public int getNumero() {
				return numero;
			}
			public void setNumero(int num) {
				this.numero = num;
			}
			
}
