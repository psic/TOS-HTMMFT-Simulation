/* Robot.java
  
   Copyright (C) 2001  Fergus Crawshay Murray
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

/* Modified Player model, using spheres and cylinders instead of boxes
 * Yu Zhang, Nov 29 2004.
 */
/**
 * Implemented uniform numbers on players
 * Vadim Kyrylov May 2005
 * 
 */
package soccer.client.view.j3d;

import java.net.URL;

import javax.media.j3d.Appearance;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;

/**
 * This class implements a primitive humanoid robot.
 * 
 * Some information on classes can be found here:
 * http://java.sun.com/javase/technologies/desktop/java3d/1_2_api_beta
 * 						/j3dapi/javax/media/j3d/package-tree.html
 */

public class Robot extends TransformGroup implements TOSModel 
{
	public Robot() {
		this(0);
	}

	
	private Color3f colour;
	private String side;
	private int id;
	private Appearance appearance;
	private Appearance skin;
	private float phase = 0;
	private TransformGroup leftLeg,
		leftShin,
		rightLeg,
		rightShin,
		leftArm,
		leftForearm,
		rightArm,
		rightForearm,
		torso,
		head;
	
	private Transform3D llt, lst, rlt, rst, lat, lft, rat, rft, tt, ht;

	
	/**
	 * This constructor builds a humanoid robot out of cylinders and a sphere.
	 * 
	 * @param ID - player id (is put on the robot's torso)
	 */
	public Robot(int ID) 
	{
		if(ID<11)
		{
			colour = new Color3f(1f, 1f, 0f);
			side = "l";
			id = ID + 1;
		}
		else
		{
			colour = new Color3f(1f, 0f, 0f);
			side = "r";
			id = ID - 10;
		}
		this.appearance = createAppearance(colour);
		this.skin = createAppearance(new Color3f(0.77f, 0.48f, 0.3f));
		Transform3D partTransform3D;
		
		/**
		 * Build two legs (each a composite object itself)
		 */
		Transform3D lt = new Transform3D();
		lt.setTranslation(new Vector3f(0.0f, 0.0f, 1.6f));
		TransformGroup legs = new TransformGroup(lt);
		double lltm[] =
			{
				1.0,
				0.0,
				0.0,
				-0.5,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				-0.4,
				0.0,
				0.0,
				0.0,
				1.0 };
		llt = new Transform3D(lltm);
		leftLeg = new TransformGroup(llt);
		leftLeg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 2.0f, 1));
		partTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 1.57f));
		partTransform3D.setTranslation(new Vector3d(0.15, 0.0, 0.0));
		TransformGroup t_leftLeg = new TransformGroup(partTransform3D);
		t_leftLeg.addChild(new Cylinder(0.27f, -0.8f, appearance));		
		leftLeg.addChild(t_leftLeg);
		legs.addChild(leftLeg);

		double lstm[] =
			{
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				-1.2,
				0.0,
				0.0,
				0.0,
				1.0 };
		lst = new Transform3D(lstm);
		leftShin = new TransformGroup(lst);
		leftShin.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 2.0f, 1));
		partTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 1.57f));
		partTransform3D.setTranslation(new Vector3d(0.15, 0.0, 0.0));
		TransformGroup t_leftShin = new TransformGroup(partTransform3D);
		t_leftShin.addChild(new Cylinder(0.2f, 0.8f, skin));		
		leftShin.addChild(t_leftShin);
		leftLeg.addChild(leftShin);

		double rltm[] =
			{
				1.0,
				0.0,
				0.0,
				0.5,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				-0.4,
				0.0,
				0.0,
				0.0,
				1.0 };
		rlt = new Transform3D(rltm);
		rightLeg = new TransformGroup(rlt);
		rightLeg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 2.0f, 1));
		partTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 1.57f));
		partTransform3D.setTranslation(new Vector3d(-0.15, 0.0, 0.0));
		TransformGroup t_rightLeg = new TransformGroup(partTransform3D);
		t_rightLeg.addChild(new Cylinder(0.27f, 0.8f, appearance));		
		rightLeg.addChild(t_rightLeg);
		legs.addChild(rightLeg);
		addChild(legs);

		double rstm[] =
			{
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				-1.2,
				0.0,
				0.0,
				0.0,
				1.0 };
		rst = new Transform3D(rstm);
		rightShin = new TransformGroup(rst);
		rightShin.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 2.0f, 1));
		partTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 1.57f));
		partTransform3D.setTranslation(new Vector3d(-0.15, 0.0, 0.0));
		TransformGroup t_rightShin = new TransformGroup(partTransform3D);
		t_rightShin.addChild(new Cylinder(0.2f, 0.8f, skin));			
		rightShin.addChild(t_rightShin);
		rightLeg.addChild(rightShin);

		/**
		 * Build two arms (each a composite object itself)
		 */
		double latm[] =
			{
				1.0,
				0.0,
				0.0,
				-1.0,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				3.0,
				0.0,
				0.0,
				0.0,
				1.0 };
		lat = new Transform3D(latm);
		leftArm = new TransformGroup(lat);
		leftArm.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 2.0f, 1));
		partTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 1.57f));
		partTransform3D.setTranslation(new Vector3d(0.2, 0.0, 0.0));
		TransformGroup t_leftArm = new TransformGroup(partTransform3D);
		t_leftArm.addChild(new Cylinder(0.2f, 0.6f, appearance));		
		leftArm.addChild(t_leftArm);
		addChild(leftArm);

		double lftm[] =
			{
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				-0.8,
				0.0,
				0.0,
				0.0,
				1.0 };
		lft = new Transform3D(lftm);
		leftForearm = new TransformGroup(lft);
		leftForearm.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 2.0f, 1));
		partTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 1.57f));
		partTransform3D.setTranslation(new Vector3d(0.2, 0.0, 0.0));
		TransformGroup t_leftForeArm = new TransformGroup(partTransform3D);
		t_leftForeArm.addChild(new Cylinder(0.15f, 0.5f, skin));		
		leftForearm.addChild(t_leftForeArm);		
		leftArm.addChild(leftForearm);

		double ratm[] =
			{
				1.0,
				0.0,
				0.0,
				1.0,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				3.0,
				0.0,
				0.0,
				0.0,
				1.0 };
		rat = new Transform3D(ratm);
		rightArm = new TransformGroup(rat);
		rightArm.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 2.0f, 1));
		partTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 1.57f));
		partTransform3D.setTranslation(new Vector3d(-0.2, 0.0, 0.0));
		TransformGroup t_rightArm = new TransformGroup(partTransform3D);
		t_rightArm.addChild(new Cylinder(0.2f, 0.6f, appearance));			
		rightArm.addChild(t_rightArm);
		addChild(rightArm);

		double rftm[] =
			{
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				-0.8,
				0.0,
				0.0,
				0.0,
				1.0 };
		rft = new Transform3D(rftm);
		rightForearm = new TransformGroup(rft);
		rightForearm.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 2.0f, 1));
		partTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 1.57f));
		partTransform3D.setTranslation(new Vector3d(-0.2, 0.0, 0.0));
		TransformGroup t_rightForeArm = new TransformGroup(partTransform3D);
		t_rightForeArm.addChild(new Cylinder(0.15f, 0.5f, skin));			
		rightForearm.addChild(t_rightForeArm);
		rightArm.addChild(rightForearm);

		/**
		 * Build the torso with a uniform number on the skin
		 */
		double ttm[] =
			{
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				2.7,
				0.0,
				0.0,
				0.0,
				1.0 };
		tt = new Transform3D(ttm);
		torso = new TransformGroup(tt);
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 1.1, 0.6f));
		//partTransform3D.setRotation(new AxisAngle4f(1.0f, 0f, 0f, 1.57f));

		/**
		 *  with this orientation, player number images must be upside down;
		 *  in this case they are placed on the back side of the robot 
		 */
		partTransform3D.setRotation(new AxisAngle4f(
									1.0f, 0f, 0f, -(float)Math.PI/2f));
				
		TransformGroup t_torso = new TransformGroup(partTransform3D);
		
	    //Cylinder torsoCyl = new Cylinder(0.6f, 1.5f, appearance );
	    Cylinder torsoCyl = new Cylinder(0.6f, 1.5f, 
	    						Primitive.GENERATE_TEXTURE_COORDS, 
	    						appearance ); 

	    // create a texture with player uniform number (read from file)
		URL imgURL = getClass().getResource("/imag/num" + side + id + ".png");
		//URL imgURL = getClass().getResource("/imag/numr1.png");
		TextureLoader loader = new TextureLoader(imgURL, null);
		ImageComponent2D image = loader.getImage();

		if (image == null) {
			System.out.println("load failed for texture, player " + id + "-" + side );
		}		
	      // can't use parameterless constructor
	    Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
	                                      image.getWidth(), image.getHeight());
	    texture.setImage(0, image);
	    TextureAttributes textureAttrib = new TextureAttributes();
	    Appearance playNum = new Appearance();
	    playNum.setTexture(texture);
	    playNum.setTextureAttributes(textureAttrib);
	    
	    // put player number of the torso cylinder
	    torsoCyl.setAppearance( playNum );
	    /**
	     * TODO this technique eliminates the specular property of the torso
	     * skin; the texture appears to be absolutely matted, which looks
	     * somewhat unnatural. No solution found yet.
	     * To create a torso with specular surface without texture, comment 
	     * out the previous statement and use constructor Cylinder with 
	     * three parameters instead of four.
	     */

		t_torso.addChild( torsoCyl );		
		
		torso.addChild(t_torso);
		addChild(torso);

		/**
		 * Construct head
		 */
		double htm[] =
			{
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				0.0,
				0.0,
				0.0,
				0.0,
				1.0,
				4.0,
				0.0,
				0.0,
				0.0,
				1.0 };
		ht = new Transform3D(htm);
		head = new TransformGroup(ht);		
		partTransform3D = new Transform3D();
		partTransform3D.setScale(new Vector3d(1, 1, 1.3f));
		TransformGroup t_head = new TransformGroup(partTransform3D);
		t_head.addChild( new Sphere( 0.4f, appearance ) );		
		head.addChild(t_head);
		addChild(head);

		Transform3D rt = new Transform3D();
		rt.setRotation(new AxisAngle4f(0f, 0f, 1f, (float) (Math.PI * 0.5)));
		this.setTransform(rt);
	}

	/**
	 * This methods implements the appearance of robot stepping 
	 */
	public void step(float stepSize) 
	{
		//stepSize /= 2;
		phase += stepSize;
		//System.out.println(phase);
		if (phase > Math.PI * 2)
			phase -= Math.PI * 2;
		AxisAngle4f angt = new AxisAngle4f();

		// Crude trigonometric approximation of walking motion

		angt.set(1.0f, 0f, 0f, -0.25f + 0.5f * (float) Math.sin(phase));
		llt.setRotation(angt);
		leftLeg.setTransform(llt);

		angt.set(1.0f, 0f, 0f, -0.25f + 0.5f * (float) Math.sin(-phase));
		rlt.setRotation(angt);
		rightLeg.setTransform(rlt);

		// Shins and forearms are a little bit behind thighs and upper arms...
		angt.set(1.0f, 0f, 0f, 0.25f + 0.25f * (float) Math.sin(-phase + 0.4));
		rst.setRotation(angt);
		rightShin.setTransform(rst);

		angt.set(
			1.0f,
			0.3f,
			0f,
			-0.25f + 0.25f * (float) Math.sin(-phase + 0.4));
		rat.setRotation(angt);
		rightArm.setTransform(rat);
		rft.setRotation(angt);
		rightForearm.setTransform(rft);

		angt.set(1.0f, 0f, 0f, 0.25f + 0.25f * (float) Math.sin(phase - 0.4));
		lst.setRotation(angt);
		leftShin.setTransform(lst);

		angt.set(
			1.0f,
			0.3f,
			0f,
			-0.25f + 0.25f * (float) Math.sin(phase - 0.4));
		lat.setRotation(angt);
		leftArm.setTransform(lat);
		lft.setRotation(angt);
		leftForearm.setTransform(lft);

	}

	private Appearance createAppearance(Color3f colour) 
	{
		//Material(Color3f ambientColor, Color3f emissiveColor, 
		//			Color3f diffuseColor, Color3f specularColor, float shininess) 
		Appearance appearance = new Appearance();
		appearance.setMaterial(
			new Material(
				new Color3f(0.1f, 0.0f, 0.1f),
				new Color3f(0.2f, 0.0f, 0.0f),
				colour,
				colour,
				120f));
		PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		appearance.setPolygonAttributes(polyAttrib);

		return appearance;
	}

}
