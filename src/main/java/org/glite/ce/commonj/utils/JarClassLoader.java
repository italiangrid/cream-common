/*
 * Copyright (c) Members of the EGEE Collaboration. 2004. 
 * See http://www.eu-egee.org/partners/ for details on the copyright
 * holders.  
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

/*
 *
 * Authors: Luigi Zangrando (zangrando@pd.infn.it)
 *
 */

package org.glite.ce.commonj.utils;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;


/**
 * A class loader for loading jar files, both local and remote.
 * @author Luigi Zangrando (zangrando@pd.infn.it)
 */
public class JarClassLoader extends URLClassLoader {

   /**
    * Creates a new JarClassLoader for the specified url.
    *
    * @param url the url of the jar file
    */
   public JarClassLoader( String href, ClassLoader parent ) throws MalformedURLException {
      this( new URL( href ), parent );
   }


   public JarClassLoader( URL url, ClassLoader parent ) {
      super( new URL[] { url }, parent );
   }


   public void addJarURL( URL url ) throws MalformedURLException {
      addURL( url );
   }


   public void addJarURL( String href ) throws MalformedURLException {
      if( href != null ) {
         addURL( new URL( href ) );
      }
   }


   /**
    * Returns the name of the jar file main class, or null if
    * no "Main-Class" manifest attributes was defined.
    * @param sensorURL
    */
   public String getMainClassName( URL sensorURL ) throws IOException {
      URL u = new URL( "jar", "", sensorURL + "!/" );
      JarURLConnection uc = ( JarURLConnection )u.openConnection( );
      Attributes attr = uc.getMainAttributes( );

      return attr != null ? attr.getValue( Attributes.Name.MAIN_CLASS ) : null;
   }


   /**
    * Invokes the application in this jar file given the name of the
    * main class and an array of arguments. The class must define a
    * static method "main" which takes an array of String arguemtns
    * and is of return type "void".
    *
    * @param name the name of the main class
    * @param args the arguments for the application
    * @exception ClassNotFoundException if the specified class could not be found
    * @exception NoSuchMethodException if the specified class does not contain a "main" method
    * @exception InvocationTargetException if the application raised an exception
    */
   public void invokeClass( String name, String[] args ) throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
      Class c = loadClass( name );
      Method m = c.getMethod( "main", new Class[] { args.getClass( ) } );
      m.setAccessible( true );
      
      int mods = m.getModifiers( );

      if( m.getReturnType( ) != void.class || !Modifier.isStatic( mods ) || !Modifier.isPublic( mods ) ) {
         throw new NoSuchMethodException( "main" );
      }

      try {
         m.invoke( null, new Object[] { args } );
      } catch( IllegalAccessException e ) {// This should not happen, as we have disabled access checks
      }
   }

}
