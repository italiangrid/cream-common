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
 *          Luigi Zangrando, <luigi.zangrando@pd.infn.it>
 *
 */

package org.glite.ce.commonj.jndi.provider.fscachedprovider;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import org.glite.ce.commonj.CEResource;

import org.apache.log4j.Logger;

public class CEGeneralAttributes extends BasicAttributes implements CEResource{

    private static Logger logger = Logger.getLogger(CEGeneralAttributes.class.getName());

    public CEGeneralAttributes(){
        super();
    }

    public CEGeneralAttributes(boolean ignoreCase){
        super(ignoreCase);
    }

    public CEGeneralAttributes(String attrID, Object val){
        super(attrID, val);
    }

    public CEGeneralAttributes(String attrID, Object val, boolean ignoreCase){
        super(attrID, val, ignoreCase);
    }

    public CEGeneralAttributes(Attributes inAttrs){

        super();

        if( inAttrs!=null ){
            NamingEnumeration allAttrs = inAttrs.getAll();
            while( allAttrs.hasMoreElements() ){
                this.put((Attribute)allAttrs.nextElement());
            }
        }
    }

    public boolean match(Attributes attrs){
        NamingEnumeration items = attrs.getAll();
        while( items.hasMoreElements() ){
            Attribute source = (Attribute)items.nextElement();
            Attribute target = this.get(source.getID());
            if( target==null ) return false;

            try{
                NamingEnumeration values = source.getAll();
                while( values.hasMoreElements() ){
                    if( !target.contains(values.nextElement()) )
                        return false;
                }
            }catch(NamingException nEx){
                logger.error(nEx.getMessage(), nEx);
                return false;
            }
        }
        return true;
    }

    public CEGeneralAttributes clone(String[] attributesRequired){

        if( attributesRequired==null )
            return (CEGeneralAttributes)this.clone();

        CEGeneralAttributes result = new CEGeneralAttributes();

        for(int k=0; k<attributesRequired.length; k++){
            Attribute tmpAttr = this.get(attributesRequired[k]);
            if( tmpAttr!=null ) result.put(tmpAttr);
        }

        return result;
    }
}
