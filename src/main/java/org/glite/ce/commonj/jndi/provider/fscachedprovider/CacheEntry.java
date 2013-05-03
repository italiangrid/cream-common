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
 * Authors: Luigi Zangrando, <luigi.zangrando@pd.infn.it>
 *
 */

package org.glite.ce.commonj.jndi.provider.fscachedprovider;

import org.glite.ce.commonj.CEResource;

public class CacheEntry implements Comparable {
    private long timestamp = 0;
  //  private int size = 0;
    private CEResource ceResource;



    public CacheEntry(long timestamp, CEResource resource) throws Exception {
        if(timestamp < 0) {
            throw (new Exception("The timestamp value must be > 0"));
        }

        if(resource == null) {
            throw (new Exception("The resource obj must be not null"));
        }

        this.timestamp = timestamp;
        this.ceResource = resource;
    //    this.size = resource.toString().getBytes().length;
    }



    public CEResource getCEResource() {
        return ceResource;
    }



    public int getSize() {
       // return size;
        return ceResource.toString().getBytes().length;
    }



    public long getTimestamp() {
        return timestamp;
    }



    public int compareTo(Object obj) {
        if(obj instanceof CacheEntry) {
            if(timestamp < ((CacheEntry) obj).getTimestamp()) {
                return -1;
            } else {
                return timestamp == ((CacheEntry) obj).getTimestamp() ? 0 : 1;
            }
        }
        return 0;
    }

}
