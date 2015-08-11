/* Actions.java
   This class stores all the client actions of TOS

   Copyright (C) 2004  Yu Zhang

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

package soccer.client.action;

import soccer.client.ViewerClientMain;
import java.util.HashMap;

public class Actions {
  private ViewerClientMain m_s;
  private HashMap<Class<?>, AbstractClientAction> m_mapActions;
  
  public Actions( ViewerClientMain s ) {
    m_s = s;
    m_mapActions = new HashMap<Class<?>, AbstractClientAction>();

  }
  
  /**
   * Gets the named action
   */
  public AbstractClientAction getAction( Class<?> clazz ) {
    AbstractClientAction a = (AbstractClientAction)m_mapActions.get( clazz );
    if( a == null )
    {
      try {
        a = (AbstractClientAction)clazz.newInstance();
        a.setSoccerMaster( m_s );
        m_mapActions.put( clazz, a );
      } catch( Exception e ) {
        throw new RuntimeException( "Couldn't find action " + clazz + " (exception was " + e + ")" );
      }
    }
    return a;
  }
}
