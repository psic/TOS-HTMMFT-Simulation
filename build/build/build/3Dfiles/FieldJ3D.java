/* FieldJ3D.java
   This class shows the field and players and ball using Java3D.
   A modified version of Yu Zhang's Field.java by Fergus C. Murray, August 29 2001. 
   
   Copyright (C) 2001  Yu Zhang
   Modified 254

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

/* Modified to make Field class a subclass of Arena class.  Also, world and
   methods setFont, setWorld, getWorld, and isFocusTraversable were moved to 
   Arena.
						          jdm, June 7 2001
*/

/* Replaced pitch-drawing and sprite-drawing with routines to update Java3D world.
   Also set panel not to be opaque, in order to allow Java3D to be used.

                                  fcm, August 29 2001.
*/

/* Fixed player, ball displayed bug. Fixed Player running direction bug.
 * added background sky, added ground, add code to read VRML model files.
 * Currently 2 vrml balloons are loaded and displayed.
 * 
 * Use Hashtable to store team players' 3D data instead of Vectors
 * 
 * Add a self indicator
 * 
 * fix speed bug
 * 
 * Yu Zhang, Nov 29 2004.
 */
/**
 * To get rid of rounding errors, all integer dimensions 
 * replaced by double.
 * PLayer ID added on the back of his torso. 
 * 
 * Vadim Kyrylov, March 2006
 */
/**
 * All constants factored out into class TOS_Constants.
 * TODO: the many magic numbers should be replaced by the constants 
 * 		defined in class TOS_Constants.  
 * Vadim Kyrylov, January 2010
 */

// EVERYTHING DISABLED BECAUSE THIS CLASS REQUIRES JAVA 3D LIBRARIES.
// INSTALLING THESE LIBRARIES AND FINE TUNING JDK AND ECLIPSE FOR USING 
// THEM IS A TRICKY TASK, ESPECIALLY ON 64-BIT PLLATFORM.
// This class can be easily restored by removing all block comments (/* ... */)
// Without Java 3D libraries, classes MouseRotateXZ, MouseZoomOnRightClick, 
// and Robot do not compile. They are all used in Field3D only and could 
// be removed if this class is left disabled. 

package soccer.client.view.j3d;
/*
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.web3d.j3d.loaders.VRML97Loader;	// critical for 3d view

import com.sun.j3d.loaders.Scene;

import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.MultiTransformGroup;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;

*/
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import soccer.client.ViewerClientMain;
import soccer.client.view.*;
import soccer.common.*;


/**
 * This class displays the soccer field in 3D.
 * Some references to classes used here could be found at
 * http://java.sun.com/javase/technologies/desktop/java3d
 * 		/forDevelopers/J3D_1_2_API/j3dguide/AppendixUtilities.html
 * http://www.cs.txstate.edu/~hd01/CS1319/Resources/Java%20Notes
 *  		/java3d-1_4_0-doc/com/sun/j3d/utils/geometry/package-summary.html
 * http://java.sun.com/javase/technologies/desktop/java3d/forDevelopers
 * 			/J3D_1_3_API/j3dapi/javax/vecmath/package-tree.html
 * 
 * @author vkyrylov
 */

public class FieldJ3D extends Arena {

	private static final long serialVersionUID = 444641009596202940L;
	private static Color bg = Color.GREEN.darker();
	private static Color fg = Color.RED;
/*
	public Canvas3D myCanvas3D;

	// the upper left corner for moving objects
	private double xObj;
	private double yObj;

	// the center of the moving object
	private Vector2d objCenter = new soccer.common.Vector2d();

	// for loop
	private Player player = null;
	private Enumeration<Player> players = null;

	private Transform3D myTransform3D, thisTransform3D;
	private Vector3d 	thisVector3d;
	private AxisAngle4d thisAxisAngle4d;
	private Transform3D viewRotation, viewTranslation;
	private MouseRotateXZ myMouseRotateXZ;
	private MouseZoomOnRightClick myMouseZoomOnRightClick;
	private TransformGroup 	viewRotationGroup,
							viewTranslationGroup,
							ballTransformGroup,
							selfTransformGroup,
							thisTransformGroup;
	private SimpleUniverse myUniverse;
	private Hashtable<String, TransformGroup> leftTeamTransform 
							= new Hashtable<String, TransformGroup>();
	private Hashtable<String, TOSModel> leftTeamGeometry 
							= new Hashtable<String, TOSModel>();
	private Hashtable<String, TransformGroup> rightTeamTransform 
							= new Hashtable<String, TransformGroup>();
	private Hashtable<String, TOSModel> rightTeamGeometry 
							= new Hashtable<String, TOSModel>();
	
	//  Slave myBehaviour;
	private TOSModel curPlayer;
	private boolean stepping = true;
	private double oldlx[] = new double[11];
	private double oldly[] = new double[11];
	private double oldrx[] = new double[11];
	private double oldry[] = new double[11];

	// There's no guarantee that a loaded model will be a sensible size or height for the pitch, 
	// so use these parameters to compensate.
	private double scale1 = 0.55;
	private double scale2 = 0.55;
	private double height1 = 0;
	private double height2 = 0;
	private float speed;
*/
	public FieldJ3D(ViewerClientMain soccerMaster) {
		//Initialize drawing colors, border, opacity.
		setBackground(bg);
		setForeground(fg);

		Dimension d =
			new Dimension(
				(int)( (TOS_Constants.LENGTH + TOS_Constants.SIDEWALK * 2.0) 
									* TOS_Constants.METER + 0.5 ),
				(int)( (WIDTH + TOS_Constants.SIDEWALK * 2.0) 
									* TOS_Constants.METER + 0.5 ) );
		//    JPanel jpanel = new JPanel();
		setPreferredSize(d);
		setMaximumSize(d);
		setMinimumSize(d);
		setBorder(BorderFactory.createRaisedBevelBorder());
		this.setLayout(new BorderLayout());
		this.setOpaque(false);
		// (Otherwise the Java3D panel is hidden by green.)

		System.out.println("Java3D initialisation begins.");
/*
		myCanvas3D =
			new Canvas3D( 	com.
							sun.
							j3d.
							utils.
							universe.
							SimpleUniverse.
							getPreferredConfiguration() );

		this.add(myCanvas3D);
		thisTransform3D = new Transform3D();
		thisVector3d = new Vector3d();
		thisAxisAngle4d = new AxisAngle4d();
		myUniverse = new SimpleUniverse(myCanvas3D, 2);
		ViewingPlatform myView = myUniverse.getViewingPlatform();
		myUniverse.getViewer().getView().setBackClipDistance(150.0);
		MultiTransformGroup mtg = myView.getMultiTransformGroup();
		viewRotationGroup = mtg.getTransformGroup(0);
		viewTranslationGroup =
			mtg.getTransformGroup(mtg.getNumTransforms() - 1);
		viewRotation = new Transform3D();
		viewRotation.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 0f));
		viewTranslation = new Transform3D();
		viewTranslation.setTranslation(new Vector3d(0.0f, 0.0f, 131.5f));
		viewTranslationGroup.setTransform(viewTranslation);
		viewRotationGroup.setTransform(viewRotation);
		BranchGroup scene = constructContentBranch();
		myUniverse.addBranchGraph(scene);
*/
	}

/*
	private BranchGroup constructContentBranch() {

		//Font myFont = new Font("TimesRoman",Font.BOLD,1);
		//Font3D myFont3D = new Font3D(myFont,new FontExtrusion());
		//Text3D myText3D = new Text3D(myFont3D, "O");

		System.out.println("Constructing Content Branch");

		// Create teams first
		BranchGroup scene = new BranchGroup();
		TOSModel player;

		myMouseRotateXZ = new MouseRotateXZ();
		myMouseRotateXZ.setTransformGroup(viewRotationGroup);
		myMouseRotateXZ.setSchedulingBounds(
					new BoundingSphere(new Point3d(0, 0, 0), 300.0));
		scene.addChild(myMouseRotateXZ);

		myMouseZoomOnRightClick = new MouseZoomOnRightClick();
		myMouseZoomOnRightClick.setTransformGroup(viewTranslationGroup);
		myMouseZoomOnRightClick.setSchedulingBounds(
					new BoundingSphere(new Point3d(0, 0, 0), 300.0));
		scene.addChild(myMouseZoomOnRightClick);

		for (int i = 0; i < 22; i++) {
			myTransform3D = new Transform3D();
			if (i < 11) {

				// player = new LoadedModel("d:\\jdk1.3.1\\lascasmn.3ds");
				player = new Robot(i);
				leftTeamGeometry.put(Integer.toString(i), player);
				myTransform3D.setScale(scale1);
				myTransform3D.setTranslation(
					new Vector3d(
						-TOS_Constants.LENGTH / 2.0 - 3.0,
						6.0 * i - TOS_Constants.WIDTH / 2.0 + 2.0,
						height1));
				thisTransformGroup = new TransformGroup(myTransform3D);
				thisTransformGroup.addChild((Node) player);
				thisTransformGroup.setCapability(
					TransformGroup.ALLOW_TRANSFORM_WRITE);
				scene.addChild(thisTransformGroup);
				leftTeamTransform.put(Integer.toString(i), thisTransformGroup);

			} else {

				//player = new LoadedModel("Simplist.3ds");
				player = new Robot(i);
				rightTeamGeometry.put(Integer.toString(i - 11), player);
				myTransform3D.setScale(scale2);
				myTransform3D.setTranslation(
					new Vector3d(
							TOS_Constants.LENGTH / 2.0 + 3.0,
							TOS_Constants.WIDTH / 2.0 - 2.0 - 6.0 * (i - 11),
						height2));
				thisTransformGroup = new TransformGroup(myTransform3D);
				thisTransformGroup.addChild((Node) player);
				thisTransformGroup.setCapability(
					TransformGroup.ALLOW_TRANSFORM_WRITE);
				scene.addChild(thisTransformGroup);
				rightTeamTransform.put(
					Integer.toString(i - 11),
					thisTransformGroup);
			}

		}

		// Now the ball
		Appearance ballAppearance = new Appearance();
		Material ballMaterial = new Material();
		ballMaterial.setDiffuseColor(1f, 0f, 1f);
		ballAppearance.setMaterial(ballMaterial);
		com.sun.j3d.utils.geometry.Sphere mySphere =
			new com.sun.j3d.utils.geometry.Sphere(0.5f, ballAppearance);
		myTransform3D = new Transform3D();
		myTransform3D.setScale(0.5);
		myTransform3D.setTranslation(new Vector3d(0.0, 0.0, 0.0));
		ballTransformGroup = new TransformGroup(myTransform3D);
		ballTransformGroup.addChild(mySphere);
		ballTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		scene.addChild(ballTransformGroup);
		
		// Now the self indicator
		Appearance selfAppearance = new Appearance();
		Material selfMaterial = new Material();
		selfMaterial.setDiffuseColor(0.0f, 0.0f, 1.0f);
		selfAppearance.setMaterial(selfMaterial);
		com.sun.j3d.utils.geometry.Sphere selfSphere =
			new com.sun.j3d.utils.geometry.Sphere(1.0f, selfAppearance);
		myTransform3D = new Transform3D();
		myTransform3D.setScale(new Vector3d(1, 1, 0.1f));
		myTransform3D.setTranslation(new Vector3d(0.0, 0.0, -200.0));
		selfTransformGroup = new TransformGroup(myTransform3D);
		selfTransformGroup.addChild(selfSphere);
		selfTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		scene.addChild(selfTransformGroup);

		BranchGroup pitch = constructPitch();
		scene.addChild(pitch);

		BranchGroup ground = constructGround();
		scene.addChild(ground);

		BranchGroup left = constructGoal();
		myTransform3D = new Transform3D();
		myTransform3D.setTranslation(
			new Vector3f(- ((float) TOS_Constants.LENGTH / 2), 0.5f, 0.0f));
		TransformGroup leftGoalPole = new TransformGroup(myTransform3D);
		leftGoalPole.addChild(left);
		scene.addChild(leftGoalPole);

		BranchGroup right = constructGoal();
		myTransform3D = new Transform3D();
		myTransform3D.setTranslation(
			new Vector3f(((float) TOS_Constants.LENGTH / 2), 0.5f, 0.0f));
		TransformGroup rightGoalPole = new TransformGroup(myTransform3D);
		rightGoalPole.addChild(right);
		scene.addChild(rightGoalPole);

		loadObjects(scene);

		//FrameBehaviour frameRateGetter = new FrameBehaviour ();
		//frameRateGetter.setSchedulingBounds(new BoundingSphere());
		//scene.addChild(frameRateGetter);

		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setDirection(0.1f, 0.5f, -0.5f);
		lightD1.setInfluencingBounds(
			new BoundingSphere(new Point3d(0, 0, 0), 256));
		scene.addChild(lightD1);

		DirectionalLight lightD2 = new DirectionalLight();
		lightD2.setDirection(-0.1f, -0.5f, -0.5f);
		lightD2.setInfluencingBounds(
			new BoundingSphere(new Point3d(0, 0, 0), 256));
		scene.addChild(lightD2);

		BoundingSphere bounds =
			new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 200.0);
		Background background = new Background();
		URL imgURL = getClass().getResource("/imag/bg.jpg");
		TextureLoader loader = new TextureLoader(imgURL, null);
		ImageComponent2D image = loader.getImage();

		if (image == null) {
			System.out.println("--- load failed for texture: bg.jpg");
		}
		background.setImage(image);
		background.setApplicationBounds(bounds);
		scene.addChild(background);

		scene.compile();
		System.out.println("Scene constructed.");
		return (scene);
	}

	private BranchGroup constructPitch() {

		BranchGroup scene = new BranchGroup();

		// Now the pitch
		Appearance pitchAppearance = new Appearance();
		//Material pitchMaterial=new Material();
		QuadArray plane =
			new QuadArray(
				4,
				GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);

		float halfLength = (float)( TOS_Constants.SIDEWALK + TOS_Constants.LENGTH / 2.0 );
		float halfWidth = (float)( TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2.0 );
		Point3f p = new Point3f(-halfLength, halfWidth, 0.0f);
		plane.setCoordinate(0, p);
		p.set(-halfLength, -halfWidth, 0.0f);
		plane.setCoordinate(1, p);
		p.set(halfLength, -halfWidth, 0.0f);
		plane.setCoordinate(2, p);
		p.set(halfLength, halfWidth, 0.0f);
		plane.setCoordinate(3, p);

		TexCoord2f q = new TexCoord2f(0.0f, 1.0f);
		plane.setTextureCoordinate(0, 0, q);
		q.set(0.0f, 0.0f);
		plane.setTextureCoordinate(0, 1, q);
		q.set(0.735f, 0.0f);
		plane.setTextureCoordinate(0, 2, q);
		q.set(0.735f, 1.0f);
		plane.setTextureCoordinate(0, 3, q);
		URL imgURL = getClass().getResource("/imag/pitchf.gif");
		TextureLoader loader = new TextureLoader(imgURL, null);
		ImageComponent2D image = loader.getImage();

		if (image == null) {
			System.out.println("load failed for texture: pitchf.gif");
		}
		// can't use parameterless constuctor
		Texture2D texture =
			new Texture2D(
				Texture.BASE_LEVEL,
				Texture.RGBA,
				image.getWidth(),
				image.getHeight());
		texture.setImage(0, image);
		//texture.setEnable(false);

		pitchAppearance.setTexture(texture);

		//pitchMaterial.setDiffuseColor(0.0f,0.5f,0.0f);
		//pitchAppearance.setMaterial(pitchMaterial);
		//Box pitch = new Box(60f, 36f, 0.1f, pitchAppearance);
		Shape3D pitch = new Shape3D(plane, pitchAppearance);
		myTransform3D = new Transform3D();
		myTransform3D.setTranslation(new Vector3f(0f, 0f, -0.5f));
		TransformGroup pitchTransformGroup = new TransformGroup(myTransform3D);
		pitchTransformGroup.addChild(pitch);
		scene.addChild(pitchTransformGroup);

		return (scene);
	}

	private BranchGroup constructGround() {

		BranchGroup scene = new BranchGroup();

		Appearance groundAppearance = new Appearance();

		QuadArray plane =
			new QuadArray(
				4,
				GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);

		Point3f p = new Point3f(-300, 300, 0.0f);
		plane.setCoordinate(0, p);
		p.set(-300, -300, 0.0f);
		plane.setCoordinate(1, p);
		p.set(300, -300, 0.0f);
		plane.setCoordinate(2, p);
		p.set(300, 300, 0.0f);
		plane.setCoordinate(3, p);

		TexCoord2f q = new TexCoord2f(-10.0f, 10.0f);
		plane.setTextureCoordinate(0, 0, q);
		q.set(-10.0f, -10.0f);
		plane.setTextureCoordinate(0, 1, q);
		q.set(10.0f, -10.0f);
		plane.setTextureCoordinate(0, 2, q);
		q.set(10.0f, 10.0f);
		plane.setTextureCoordinate(0, 3, q);
		URL imgURL = getClass().getResource("/imag/grass.gif");
		TextureLoader loader = new TextureLoader(imgURL, null);
		ImageComponent2D image = loader.getImage();

		if (image == null) {
			System.out.println("load failed for texture: grass.gif");
		}
		// can't use parameterless constructor
		Texture2D texture =
			new Texture2D(
				Texture.BASE_LEVEL,
				Texture.RGBA,
				image.getWidth(),
				image.getHeight());
		texture.setBoundaryModeS(Texture.WRAP);
		texture.setBoundaryModeT(Texture.WRAP);
		texture.setImage(0, image);
		//texture.setEnable(false);

		groundAppearance.setTexture(texture);

		//pitchMaterial.setDiffuseColor(0.0f,0.5f,0.0f);
		//pitchAppearance.setMaterial(pitchMaterial);
		//Box pitch = new Box(60f, 36f, 0.1f, pitchAppearance);
		Shape3D ground = new Shape3D(plane, groundAppearance);
		myTransform3D = new Transform3D();
		myTransform3D.setTranslation(new Vector3f(0f, 0f, -1f));
		TransformGroup groundTransformGroup = new TransformGroup(myTransform3D);
		groundTransformGroup.addChild(ground);
		scene.addChild(groundTransformGroup);

		return (scene);
	}

	private BranchGroup constructGoal() {

		BranchGroup scene = new BranchGroup();

		 // for the unknown reason, the cylinders (but not the ball) turn to be shifted 
		 // along y-axis; this adjustment parameter eliminates this shift
		 
		float adjustmentY = -0.50f;

		float poleRadius = 0.15f;
		// top bar		
		Cylinder c1Obj = new Cylinder(poleRadius, TOS_Constants.GOAL_WIDTH + 0.25f);
		myTransform3D = new Transform3D();
		myTransform3D.setTranslation(new Vector3f( 0f, adjustmentY, TOS_Constants.GOAL_HEIGHT));
		TransformGroup c1TransformGroup = new TransformGroup(myTransform3D);
		c1TransformGroup.addChild(c1Obj);
		scene.addChild(c1TransformGroup);

		float poleHeight = TOS_Constants.GOAL_HEIGHT + 0.4f;
		// right pole (if looking from back)	
		Cylinder c2Obj = new Cylinder(poleRadius, poleHeight);
		myTransform3D = new Transform3D();
		myTransform3D.setTranslation(
				new Vector3f(0f, -TOS_Constants.GOAL_WIDTH/2 + adjustmentY, TOS_Constants.GOAL_HEIGHT/2 - 0.25f));
		// rotate about the non-zero coordinate (x-axis)
		myTransform3D.setRotation(new AxisAngle4f(1f, 0f, 0f, (float)Math.PI/2f));
		TransformGroup c2TransformGroup = new TransformGroup(myTransform3D);
		c2TransformGroup.addChild(c2Obj);
		scene.addChild(c2TransformGroup);

		// left pole  (if looking from back)
		Cylinder c3Obj = new Cylinder(poleRadius, poleHeight);
		myTransform3D = new Transform3D();
		myTransform3D.setTranslation(
				new Vector3f(0f, TOS_Constants.GOAL_WIDTH/2 + adjustmentY, TOS_Constants.GOAL_HEIGHT/2 - 0.25f));
		// rotate about the non-zero coordinate (x-axis)
		myTransform3D.setRotation(new AxisAngle4f(1f, 0f, 0f, (float)Math.PI/2f));
		TransformGroup c3TransformGroup = new TransformGroup(myTransform3D);
		c3TransformGroup.addChild(c3Obj);
		scene.addChild(c3TransformGroup);

		return (scene);
	}

	private void loadObjects(BranchGroup scene) {

		//Lw3dLoader modLoader = new Lw3dLoader();
		//ObjectFile modLoader = new ObjectFile();
		VRML97Loader modLoader = new VRML97Loader(); 
		Scene model = null;

		// balloon
		URL modURL = getClass().getResource("/model/balloon.wrl");
		try {
			model = modLoader.load(modURL);  
		} catch (Exception e) {
			System.out.println("model loading failed:" + e);
			System.exit(1);
		}
		BranchGroup balloon = model.getSceneGroup();
		myTransform3D = new Transform3D();
		myTransform3D.setScale(2.5);
		myTransform3D.setTranslation(new Vector3f(-50, 50, 40.0f));
		myTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 2.72f));
		TransformGroup tballoon = new TransformGroup(myTransform3D);
		tballoon.addChild(balloon);
		scene.addChild(tballoon);

		BranchGroup balloon2 = (BranchGroup) balloon.cloneTree();
		myTransform3D = new Transform3D();
		myTransform3D.setScale(2.5);
		myTransform3D.setTranslation(new Vector3f(80, -80, 40.0f));
		myTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 2.72f));
		TransformGroup tballoon2 = new TransformGroup(myTransform3D);
		tballoon2.addChild(balloon2);
		scene.addChild(tballoon2);

	}

	public void paintComponent(Graphics g) {

		// draws all moving objects
		if (world != null) {

			
			// draw messages from player clients
			if(world.leftM != null && soccerMaster.chat.isSelected())
			{
			  g2.setColor(Color.yellow);
			  g2.drawString("(" + world.leftM.id + "):" + world.leftM.message,
			           SIDEWALK * METER, 2 * METER);
			}
			if(world.rightM != null && soccerMaster.chat.isSelected())
			{
			  g2.setColor(Color.red);
			  g2.drawString("(" + world.rightM.id + "):" + world.rightM.message,
			           (SIDEWALK + LENGTH / 2) * METER, 2 * METER);
			}

			if (world.getBall() != null) {

				// draw the ball                 
				objCenter.setXY(world.getBall().getPosition());
				//soccer2user(c);

				xObj = objCenter.getX(); // - ballSize * METER;
				yObj = objCenter.getY(); // - ballSize * METER;
				thisVector3d.set(xObj, yObj, 0.0f);
				thisTransform3D.setTranslation(thisVector3d);
				thisTransform3D.setScale(0.5);
				ballTransformGroup.setTransform(thisTransform3D);

			}

			if (world.getLeftTeam() != null) {
				// draw left players
				players = world.getLeftTeam().elements();

				while (players.hasMoreElements()) {
					player = (Player) players.nextElement();
					objCenter.setXY(player.getPosition());
					//soccer2user(c);
					xObj = objCenter.getX();
					yObj = objCenter.getY();

					curPlayer =
						(TOSModel) leftTeamGeometry.get(
							Integer.toString(player.getId() - 1));

					if (stepping) {
						speed =
							(float) Math.sqrt(
								(xObj - oldlx[player.getId() - 1])
									* (xObj - oldlx[player.getId() - 1])
									+ (yObj - oldly[player.getId() - 1])
										* (yObj - oldly[player.getId() - 1]));
						curPlayer.step(speed);
					}

					thisVector3d.set(xObj, yObj, height1);
					thisTransform3D.setTranslation(thisVector3d);
					thisTransform3D.setScale(scale1);
					//thisAxisAngle4d.set(0, 0, 1, (player.direction) / 6.28);	// replaced by the following
					//thisAxisAngle4d.set(0, 0, 1, Util.Deg2Rad(player.direction));
					
					double directionDeg = Util.normal_dir( player.getDirection() );
					double directionRad = Util.deg2Rad( directionDeg );
					thisAxisAngle4d.set(0, 0, 1, directionRad);
					
					thisTransform3D.setRotation(thisAxisAngle4d);

					thisTransformGroup =
						(TransformGroup) leftTeamTransform.get(
							Integer.toString(player.getId() - 1));
					thisTransformGroup.setTransform(thisTransform3D);
					oldlx[player.getId() - 1] = xObj;
					oldly[player.getId() - 1] = yObj;

				}
			}

			if (world.getRightTeam() != null) {
				// draw right players
				players = world.getRightTeam().elements();
				while (players.hasMoreElements()) {
					player = (Player) players.nextElement();
					objCenter.setXY(player.getPosition());
					//soccer2user(c);
					xObj = objCenter.getX(); // - playerSize * METER;
					yObj = objCenter.getY(); // - playerSize * METER;

					curPlayer =
						(TOSModel) rightTeamGeometry.get(
							Integer.toString(player.getId() - 1));

					if (stepping) {
						speed =
							(float) Math.sqrt(
								(xObj - oldrx[player.getId() - 1])
									* (xObj - oldrx[player.getId() - 1])
									+ (yObj - oldry[player.getId() - 1])
										* (yObj - oldry[player.getId() - 1]));
						curPlayer.step(speed);
					}

					thisVector3d.set(xObj, yObj, height2);
					thisTransform3D.setTranslation(thisVector3d);
					thisAxisAngle4d.set(0, 0, 1, Util.deg2Rad(player.getDirection()));
					thisTransform3D.setScale(scale2);
					thisTransform3D.setRotation(thisAxisAngle4d);

					thisTransformGroup =
						(TransformGroup) rightTeamTransform.get(
							Integer.toString(player.getId() - 1));
					thisTransformGroup.setTransform(thisTransform3D);
					oldrx[player.getId() - 1] = xObj;
					oldry[player.getId() - 1] = yObj;

				}
			}

			// identify myself on the field
			if (world.getMe() != null) {
				objCenter.setXY(world.getMe().getPosition());
				//soccer2user(c);

				xObj = objCenter.getX();
				yObj = objCenter.getY();
				if (world.getMe().getSide() == 'l') {
					curPlayer =
						(TOSModel) leftTeamGeometry.get(
							Integer.toString(world.getMe().getId() - 1));
					if (stepping) {
						speed =
							(float) Math.sqrt(
								(xObj - oldlx[world.getMe().getId() - 1])
									* (xObj - oldlx[world.getMe().getId() - 1])
									+ (yObj - oldly[world.getMe().getId() - 1])
										* (yObj - oldly[world.getMe().getId() - 1]));
						curPlayer.step(speed);
					}
					thisTransformGroup =
						(TransformGroup) leftTeamTransform.get(
							Integer.toString(world.getMe().getId() - 1));
					oldlx[world.getMe().getId() - 1] = xObj;
					oldly[world.getMe().getId() - 1] = yObj;
				} else {
					curPlayer =
						(TOSModel) rightTeamGeometry.get(
							Integer.toString(world.getMe().getId() - 1));
					if (stepping) {
						speed =
							(float) Math.sqrt(
								(xObj - oldrx[world.getMe().getId() - 1])
									* (xObj - oldrx[world.getMe().getId() - 1])
									+ (yObj - oldry[world.getMe().getId() - 1])
										* (yObj - oldry[world.getMe().getId() - 1]));
						curPlayer.step(speed);
					}
					thisTransformGroup =
						(TransformGroup) rightTeamTransform.get(
							Integer.toString(world.getMe().getId() - 1));
					oldrx[world.getMe().getId() - 1] = xObj;
					oldry[world.getMe().getId() - 1] = yObj;
				}

				thisVector3d.set(xObj, yObj, height2 + 0.3);
				thisTransform3D.setTranslation(thisVector3d);
				thisAxisAngle4d.set(0, 0, 1, Util.deg2Rad(world.getMe().getDirection()));
				thisTransform3D.setRotation(thisAxisAngle4d);

				thisTransformGroup.setTransform(thisTransform3D);

				// draw the self indicator under the player                
				thisVector3d.set(xObj, yObj, 0.0f);
				thisTransform3D.setTranslation(thisVector3d);
				thisTransform3D.setScale(new Vector3d(1, 1, 0.1f));
				selfTransformGroup.setTransform(thisTransform3D);

			} else {

				// draw the self indicator far away from view                
				thisVector3d.set(0, 0, -200.0f);
				thisTransform3D.setTranslation(thisVector3d);
				thisTransform3D.setScale(new Vector3d(1, 1, 0.1f));
				selfTransformGroup.setTransform(thisTransform3D);
			}
		}

	}

	public void enableMouseNavigation(boolean enabled) {
		myMouseRotateXZ.setEnable(enabled);
		myMouseZoomOnRightClick.setEnable(enabled);
	}

	public void viewReset() {
		ViewingPlatform myView = myUniverse.getViewingPlatform();
		MultiTransformGroup mtg = myView.getMultiTransformGroup();
		viewRotationGroup = mtg.getTransformGroup(0);
		viewTranslationGroup =
			mtg.getTransformGroup(mtg.getNumTransforms() - 1);
		viewRotation = new Transform3D();
		viewRotation.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 0f));
		viewTranslation = new Transform3D();
		viewTranslation.setTranslation(new Vector3d(0.0f, 0.0f, 131.5f));
		viewTranslationGroup.setTransform(viewTranslation);
		viewRotationGroup.setTransform(viewRotation);
	}
*/
	
	// Coordinate System Conversions

	// User space is a device-independent logical coordinate system. 
	// the coordinate space that your program uses. All geometries passed into 
	// Java 2D rendering routines are specified in user-space coordinates.
	// the origin of user space is the upper-left corner of the component's drawing
	// area. The x coordinate increases to the right, and the y coordinate increases downward.

	// Soccer space is used in soccer server.
	// the origin of soccer space is the center of the soccer field. The x coordinate increases 
	// to the right, and the y coordinate increases upward.

	// convert from Java 2d user space to soccer space
	public void user2soccer(soccer.common.Vector2d p) {
		double x = p.getX() / TOS_Constants.METER;
		double y = p.getY() / TOS_Constants.METER;

		double origin_x = TOS_Constants.SIDEWALK + TOS_Constants.LENGTH / 2.0;
		double origin_y = TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2.0;

		x = x - origin_x;
		y = - (y - origin_y);

		p.setXY(x, y);

		return;

	}

	// convert from soccer space to Java 2d user space 
	public void soccer2user(soccer.common.Vector2d p) {
		double x = p.getX();
		double y = p.getY();

		double origin_x = (-TOS_Constants.SIDEWALK - TOS_Constants.LENGTH / 2.0);
		double origin_y = TOS_Constants.SIDEWALK + TOS_Constants.WIDTH / 2.0;

		x = (x - origin_x) * TOS_Constants.METER;
		y = - (y - origin_y) * TOS_Constants.METER;

		p.setXY(x, y);

		return;

	}

	
}
