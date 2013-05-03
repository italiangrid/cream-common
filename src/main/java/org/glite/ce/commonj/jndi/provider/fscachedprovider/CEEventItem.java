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

package org.glite.ce.commonj.jndi.provider.fscachedprovider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;

import org.apache.log4j.Logger;

public final class CEEventItem {

    private static Logger logger = Logger.getLogger(CEEventItem.class.getName());

    private HashMap children;
    private HashMap scopes;
    private int numberOfObjectScopes;
    private int numberOfOneLevelScopes;
    private int numberOfSubtreeScopes;
    private int dependencies;

    public CEEventItem(){

        children = new HashMap(0);
        scopes = new HashMap(0);

        numberOfObjectScopes = 0;
        numberOfOneLevelScopes = 0;
        numberOfSubtreeScopes = 0;
        dependencies = 0;
    }

    public boolean addListener(NamingListener lsnr,  int scope){

        boolean result = removeListener(lsnr) == 0;

        scopes.put(lsnr, new Integer(scope));

        if( scope==EventContext.OBJECT_SCOPE ){
            numberOfObjectScopes++;
        }else if( scope==EventContext.ONELEVEL_SCOPE ){
            numberOfOneLevelScopes++;
        }else{
            numberOfSubtreeScopes++;
        }

        return result;
    }

    public int removeListener(NamingListener lsnr){
        Integer scope = (Integer)scopes.remove(lsnr);
        if( scope!=null ){
            int sc = scope.intValue();
            if( sc==EventContext.OBJECT_SCOPE ){
                numberOfObjectScopes--;
            }else if( sc==EventContext.ONELEVEL_SCOPE ){
                numberOfOneLevelScopes--;
            }else{
                numberOfSubtreeScopes--;
            }
            return 1;
        }

        return 0;
    }

    public int removeAllListeners(){
        int result = scopes.size();
        scopes.clear();
        numberOfObjectScopes = 0;
        numberOfOneLevelScopes = 0;
        numberOfSubtreeScopes = 0;
        return result;
    }

    public NamingListener[] getListeners(int scope){

        NamingListener[] result = null;
        if( scope==EventContext.OBJECT_SCOPE ){
            result = new NamingListener[numberOfObjectScopes];
        }else if( scope==EventContext.ONELEVEL_SCOPE ){
            result = new NamingListener[numberOfOneLevelScopes];
        }else{
            result = new NamingListener[numberOfSubtreeScopes];
        }

        if( result.length==0 ) return result;

        Iterator lsnrItem = scopes.keySet().iterator();
        for(int k=0; lsnrItem.hasNext(); k++){
            NamingListener lsnr = (NamingListener)lsnrItem.next();
            int tmpsc = ((Integer)scopes.get(lsnr)).intValue();
            if( tmpsc==scope ) result[k] = lsnr;
        }

        return result;
    }

    public CEEventItem createSubItem(String child){
        CEEventItem result = (CEEventItem)children.get(child);
        if( result==null ){
            result = new CEEventItem();
            children.put(child, result);
        }
        return result;
    }

    public CEEventItem getItem(String child){
        return (CEEventItem)children.get(child);
    }

    public boolean removeItem(String child){
        return children.remove(child)!=null;
    }

    public void incrementDeps(int d){
        dependencies = dependencies + d;
    }

    public void decrementDeps(int d){
        if( dependencies>=d )
            dependencies = dependencies - d;
        else
            dependencies = 0;
    }

    public boolean hasDeps(){
        return dependencies>0;
    }

}
