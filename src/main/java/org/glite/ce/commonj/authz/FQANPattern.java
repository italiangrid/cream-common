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
 * Authors: Paolo Andreetto, <paolo.andreetto@pd.infn.it>
 *
 */

package org.glite.ce.commonj.authz;

import java.util.regex.Pattern;

public class FQANPattern {
    
    private String pattern;
    private Pattern group;
    private Pattern role;

    public FQANPattern(String str) throws IllegalArgumentException {        
        pattern = canonicalize(str);
        
        int idx = pattern.indexOf("/Role=");
        if( idx<0 ){
            group = Pattern.compile("^" + pattern.replaceAll("\\*", ".*").replaceAll("\\?", ".") + "$");
            role = null;
        }else{
            group = Pattern.compile("^" + pattern.substring(0,idx).replaceAll("\\*", ".*").replaceAll("\\?", ".") + "$");
            role = Pattern.compile("^" + pattern.substring(idx).replaceAll("\\*", ".*").replaceAll("\\?", ".") + "$");
        }
    }

    public boolean matches(String str){
        String tmps = canonicalize(str);
        String groupStr = null;
        String roleStr = null;
        
        int idx = tmps.indexOf("/Role=");
        if( idx<0 ){
            groupStr = tmps;
            roleStr = null;
        }else{
            groupStr = tmps.substring(0,idx);
            roleStr = tmps.substring(idx);
        }
        
        if( group.matcher(groupStr).matches() ){
            if( role==null ){
                return roleStr==null;
            }
            if( roleStr==null )
                return false;
            return role.matcher(roleStr).matches();
        }
        
        return false;
    }
    
    public boolean equals(Object obj){
        if( obj==null || !(obj instanceof FQANPattern) )
            return false;
        
        return pattern.equals(((FQANPattern)obj).pattern);
    }
    
    public String toString(){
        return new String(pattern);
    }
    
    public static String canonicalize(String in) throws IllegalArgumentException {
        String str = in.trim();
        
        if( str.length()<2 || str.charAt(0)!='/' )
            throw new IllegalArgumentException("Malformed FQAN " + str);

        int idx = str.indexOf('/', 1);
        if( idx<0 )
            idx = str.length();
        if( str.indexOf('=')<idx && str.indexOf('=')>0 ){
            throw new IllegalArgumentException("Malformed FQAN " + str);
        }

        idx = str.indexOf("/Role=NULL");
        if( idx>=0 ){
            str = str.substring(0, idx);
        }else{
            idx = str.indexOf("Capability");
            if( idx>=0 ){
                str = str.substring(0, idx);
            }
        }
        
        if( str.endsWith("/") )
            str = str.substring(0, str.length()-1);
        
        return str;
    }
    
}
