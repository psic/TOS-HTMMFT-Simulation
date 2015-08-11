/* Util.java

   Copyright (C) 2001  Yu Zhang

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

	Modifications by Vadim Kyrylov 
							(2006-2010)
*/

package soccer.common;

import java.io.*;
import java.util.*;


/**
 * Utility static class provides conversion, normalization...
 *
 * @author Yu Zhang
 * @author V KYRYLOV (since 2006)
 */
public class Util {
  
  /**
   * @deprecated
   * converts degrees to radians
   *
   * @param  degree the angle in degree.
   * @return the same angle in radian.
   */
  public static double deg2Rad(double degree) {
    return Math.toRadians(degree);
  }
  
  /**
   * converts any angle value into range -180 -- +180.
   * @param dir the angle value
   * @return the same angle in range -180 -- +180.
   */
  public static double normal_dir(double dir) {
    double tmp = dir;
    if(tmp > 180)
      tmp -= 360;
    else if(tmp < -180)
      tmp += 360;
    return tmp;
  }
  
  /** 
   * square. 
   * 
   * @param value the input value.
   * @return the square of the input value.
   */
  public static double pow2(double value) {
    return( value * value );
  }
  
  /**
   * @deprecated
   * convert radian to degree.
   * 
   * @param radian the angle value in radians.
   * @return the same angle in degree.
   */
  public static double rad2Deg(double radian) {
    return Math.toDegrees(radian);
  }

  /**
   * read a line from InputStreamReader, it will block until a '\n' is read.
   *
   * @param ds the DataInputStream.
   * @return the String contains the last line read from the DataInputStream.
   * @exception IOException If any IO problems occured.
   */
  public static String readLine(DataInputStream ds) throws IOException {
    StringBuffer sb = new StringBuffer();
    int c = ds.readByte();
    while(c != '\n' && c != -1)
    {
      sb.append((char)c);
      c = ds.readByte();
    }
    
    return sb.toString();	    
  }

	// this method returns the sign of the input value
	public static int sign ( double number ) {
		if ( number <  0 ) return -1;
		if ( number == 0 ) return  0;
		if ( number >  0 ) return  1;
		return 0;
	}

	
	// copies elements of source into the respective elements of dist;
	// both vectors are assumed to store objects of same class
	// if source.size()>dest.size(), the exrta elements are ignored;
	// if source.size()<dest.size(), only available elements of source are copied
	
	public void copy( Vector<Object> source, Vector<Object> dest ) {
		for ( int i = 0; i < dest.size(); i++ ) {
			try {
				Object obj = (Object)source.elementAt(i);
				dest.setElementAt( obj, i );
			} catch (Exception e ) {}
		}
	}
	
	/**
	 * This method returns x rounded to n digits after the decimal point
	 */
	public static double round(double x, int n) {
		int k = 1;
		for (int i=0; i<n; i++) 
			k = k * 10;
		int y = (int)(Math.abs(x * k) + 0.5);
		return Math.signum(x) * y/(double)k;
	}
	
	/**
	 * This methods blends the values of two functions of some variable on 
	 * given interval and ensures that the blended value has a continuous 
	 * first derivative at the end points of the interval
	 * @param f
	 * 		first value f(x)
	 * @param g
	 * 		second value g(x)
	 * @param x
	 * 		independent variable
	 * @param x1
	 * 		left end point of the interval
	 * @param x2
	 * 		right end point of the interval 
	 * @return
	 */
	public static double blend_(double f, double g, double x, double x1, double x2) {		
		if ( x < x1 )
			return f;
		else if ( x > x2 )
			return g;
		else if (Math.abs(x1 - x2) < 1e-8)
			return (f + g)/2.0;		// protect from division by zero
		else {
			//--- blend f(x) and g(x) on the interval [x1, x2]  
			// this normalized variable belongs to [-PI/2, PI/2]
			double z = Math.PI * ( -0.5 + (x - x1)/(x2 - x1) );
			double weight = 0.5 * ( 1.0 - Math.sin( z ) );
			return f * weight + g * ( 1.0 - weight );
		}
	}
	
	/**
	 * This methods blends the values of two functions of some variable on 
	 * given interval and ensures that the blended value has a continuous 
	 * first derivative at the end points of the interval and in its midpoint
	 * @param f
	 * 		first value f(x)
	 * @param g
	 * 		second value g(x)
	 * @param x
	 * 		independent variable
	 * @param x1
	 * 		left end point of the interval
	 * @param x2
	 * 		right end point of the interval 
	 * @return
	 */
	public static double blend(double f, double g, double x, double x1, double x2) {		
		if ( x < x1 )
			return f;
		else if ( x > x2 )
			return g;
		else if (Math.abs(x1 - x2) < 1e-8)
			return (f + g)/2.0;		// protect from division by zero
		else {
			//--- blend f(x) and g(x) on the interval [x1, x2]  
			// this normalized variable belongs to [-1.0, 1.0]
			double z =  2.0 * (x - x1)/(x2 - x1) - 1.0;
			double weight;
			if ( z < 0 ) 
				weight = 1.0 - 0.5 * (z + 1)*(z + 1);
			else 
				weight = 0.5 * (z - 1)*(z - 1);
			return f * weight + g * ( 1.0 - weight );
		}
	}

	
	// local tester method
	public static void main(String[] s) {
		/*
		System.out.print("--- Testing method round ---\n\tx\t\tk = ");
		for(int k=0; k<6; k++) 
			System.out.print("\t" + k);
		System.out.println();
		double x = Math.PI;
		for (int i=0; i<10; i++) {
			System.out.print(x + "\t");
			for(int k=0; k<6; k++) 
				System.out.print("\t" + Util.round(x, k) );
			System.out.println();
		}
		System.out.println();
		*/
		
		System.out.println("--- Testing method blend ---\n  x\t  y ");
		double f = -2.0;
		double g = 4.0;
		int n = 3;
		for ( double x=-2; x <=6; x=x+0.2) {
			double y = blend(f, g, x, -1, 3);
			System.out.print(Util.round(x, n) + "\t" + Util.round(y, n) + "\n");			
		}	
		System.out.println();
		
		/*
		System.out.println("--- Testing method blend near the left end ---\n  x\t  y ");
		double f = -2.0;
		double g = 4.0;
		int n = 8;
		for ( double x=-1.010; x <=-0.990; x=x+0.0005) {
			double y = blend(f, g, x, -1, 3);
			System.out.print(Util.round(x, n) + "\t" + Util.round(y, n) + "\n");			
		}	
		System.out.println();
		*/
		/*
		System.out.println("--- Testing method blend near the right end ---\n  x\t  y ");
		double f = -2.0;
		double g = 4.0;
		int n = 8;
		for ( double x=2.990; x <=3.010; x=x+0.0005) {
			double y = blend(f, g, x, -1, 3);
			System.out.print(Util.round(x, n) + "\t" + Util.round(y, n) + "\n");			
		}	
		System.out.println();
		*/
		
	}
}
