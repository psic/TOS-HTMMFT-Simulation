package com.htmmft;



import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.htmmft.Equipe;
import com.htmmft.Joueur;
import com.htmmft.Match;

public class Base {

	final String BD_HTTMFT = "//localhost/howto";
	final String USER_HTMMFT ="root";
	final String PWD_HTMMFT = "cacapipi";
	private String dbURL = "";
	private String user = "";
	private String password = "";
	private java.sql.Connection dbConnect = null;
	private java.sql.Statement dbStatement = null;

	/**
	 * Constructeur
	 * @param url
	 * @param user
	 * @param password
	 * @return 
	 */
	public Base(String url, String user, String password) {
		this.dbURL = url;
		this.user = user;
		this.password = password;
	}

	public Base(){
		this.dbURL = BD_HTTMFT;
		this.user = USER_HTMMFT;
		this.password = PWD_HTMMFT;
	}

	/**
	 * Connecter à la base de donnée
	 * @return false en cas d'échec
	 */
	public Boolean connect() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			this.dbConnect = DriverManager.getConnection("jdbc:mysql:" + this.dbURL, this.user, this.password);

			return true;
		} catch (SQLException ex) {
			Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}

	/**
	 * Executer une requete SQL
	 * @param sql
	 * @return resultat de la requete
	 */
	public ResultSet exec(String sql) {

		try {
			this.dbStatement = this.dbConnect.createStatement();
			ResultSet rs = this.dbStatement.executeQuery(sql);
			return rs;
		} catch (SQLException ex) {
			Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	/**
	 * Fermer la connexion au serveur de DB
	 */
	public void close() {
		try {
			this.dbStatement.close();
			this.dbConnect.close();
		} catch (SQLException ex) {
			Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public ArrayList<Match> getMatch(String rMatch, String rTactiqueEquipe1, String rTactiqueEquipe2, String rDetailEquipe, String rPositions, String rPosition, String rJoueur) {
		ArrayList<Match> result = new ArrayList<Match>();
		try {
			ResultSet rs_match = this.exec(rMatch);
			if (rs_match != null) {
				while (rs_match.next()) {
					////System.out.println("Match ID: " + rs_match.getString(1));
					Match match = CreateMatch(rs_match.getInt(1),rs_match.getInt(2),rTactiqueEquipe1,rTactiqueEquipe2, rDetailEquipe, rPositions,rPosition,rJoueur);
					result.add(match);
				}
			}
		} catch (SQLException ex) {
			Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
		return result;
	}

	private Match CreateMatch(int id_match, int numjournee, String rTactiqueEquipe1, String rTactiqueEquipe2, String rDetailEquipe,String rPositions, String rPosition, String rJoueur) {
		Match match = new Match(id_match,numjournee);
		ResultSet rs_e1 = this.exec(rTactiqueEquipe1+id_match);
		ResultSet rs_e2 = this.exec(rTactiqueEquipe2+id_match);
		try {
			rs_e1.next();
			rs_e2.next();
			Equipe equipe1 = CreateEquipe(rs_e1.getInt(1),rPositions,rPosition,rJoueur,true, match);
			Equipe equipe2 = CreateEquipe(rs_e2.getInt(1),rPositions,rPosition,rJoueur,false,match);
			ResultSet rs_de1 = this.exec(rDetailEquipe+rs_e1.getInt(2));
			rs_de1.next();
			equipe1.setCouleur1(rs_de1.getInt(2));
			equipe1.setCouleur2(rs_de1.getInt(3));
			equipe1.setNom(rs_de1.getString(1));
			ResultSet rs_de2 = this.exec(rDetailEquipe+rs_e2.getInt(2));
			rs_de2.next();
			equipe2.setCouleur1(rs_de2.getInt(2));
			equipe2.setCouleur2(rs_de2.getInt(3));
			equipe2.setNom(rs_de2.getString(1));
			match.setEquipe1(equipe1);
			match.setEquipe2(equipe2);
			equipe1.setAdversaire(equipe2);
			equipe2.setAdversaire(equipe1);

		} catch (SQLException ex) {
			Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
		return match;

	}

	private Equipe CreateEquipe(int id_tactique, String rPositions, String rPosition, String rJoueur, boolean sens, Match match) {
		Equipe equipe = new Equipe(id_tactique,sens,match);
	//	//System.out.println("	tactique ID: " + id_tactique);
		ResultSet rs_positions = this.exec(rPositions+id_tactique);
		try {
			rs_positions.next();
			for (int i=1 ; i <= 11; i++){
				ResultSet rs_position = this.exec(rPosition+rs_positions.getInt(i));
				rs_position.next();
				Joueur joueur= CreateJoueur(rs_position.getInt(1),rs_position.getInt(2), rs_position.getInt(3),rJoueur,equipe);
				equipe.addJoueur(joueur);
			}


		} catch (SQLException ex) {
			Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
		return equipe;
	}

	private Joueur CreateJoueur(int id, int x, int y, String rJoueur, Equipe equipe) {
		Joueur joueur =new Joueur(id, x, y,equipe);//TODO
		if (id != 0){
		//	//System.out.println("		Joueur ID: " + id);
			ResultSet rs_joueur = this.exec(rJoueur+id);
			try {
				rs_joueur.next();

				joueur.setAge(rs_joueur.getInt("age"));
				joueur.setBlessure(rs_joueur.getInt("blessure"));
				joueur.setCond(rs_joueur.getInt("cond"));
				joueur.setCtr(rs_joueur.getInt("ctr"));
				joueur.setDef(rs_joueur.getInt("def"));
				joueur.setDrt(rs_joueur.getInt("drt"));
				joueur.setGch(rs_joueur.getInt("gch"));
				joueur.setMental(rs_joueur.getInt("mental"));
				joueur.setMoral(rs_joueur.getInt("moral"));
				joueur.setOff(rs_joueur.getInt("off"));
				joueur.setPhysique(rs_joueur.getInt("physique"));
				joueur.setTactique(rs_joueur.getInt("tactique"));
				joueur.setTalent(rs_joueur.getInt("talent"));
				joueur.setTechnique(rs_joueur.getInt("technique"));
				joueur.setVitesse(rs_joueur.getInt("vitesse"));
				joueur.setXp(rs_joueur.getInt("xp"));
			} catch (SQLException ex) {
				Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
				return null;
			}
		}
		else{
			//System.out.println("		ERREUR : Joueur ID");
		}
		return joueur;
	}

	/**
	 * Exemple d'utilisation de la class
	 * @param args
	 */
	//	    public static void main(String[] args) {
	//	        Base Base = new Base("//localhost", "", "");
	//	        if (Base.connect()) {
	//	            try {
	//	                ResultSet rs = Base.exec("SELECT * FROM table");
	//	                if (rs != null) {
	//	                    while (rs.next()) {
	//	                        //System.out.println("Valeur: " + rs.getString(1));
	//	                    }
	//	                }
	//	            } catch (SQLException ex) {
	//	                Logger.getLogger(Base.class.getName()).log(Level.SEVERE, null, ex);
	//	            }
	//	        } else {
	//	            //System.out.println("Mysql connection failed !!!");
	//	        }
	//	        Base.close();
	//	    }
	//	

}
