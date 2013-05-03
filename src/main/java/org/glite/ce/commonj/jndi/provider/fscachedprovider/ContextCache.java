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

import java.util.TreeMap;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.glite.ce.commonj.CEResource;

public class ContextCache {
    private static Logger logger = Logger.getLogger(ContextCache.class.getName());
    private static final long serialVersionUID = 1L;
    private TreeMap cache;
  //  private int cacheMaxSize = 10485760; // 10 MB
    private int cacheMaxSize = 100; 
  //  private int cacheSize = 0;
    private String name;



    public ContextCache() {
        this(null);
    }


    public ContextCache(final String name) {
        this.name = name;
        cache = new TreeMap();
    }

    public ContextCache(final int size) {
        if(size > 0) {
            cacheMaxSize = size;
        }

        cache = new TreeMap();
    }



    public String getName() {
        return name;        
    }
    

    public void setName(final String name) {
        this.name = name;        
    }
    
    
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }



    public void setCacheMaxSize(final int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }



    public int getCacheSize() {
        return cache.size();
    }


/*
    private synchronized void allocate(int bytes) throws Exception {
        if(bytes < 0) {
            throw (new Exception("Cannot allocate a negative amount of bytes (" + bytes + ")"));
        }

        if(bytes > cacheMaxSize) {
            throw (new Exception("Cannot allocate an amount of bytes > cacheMaxSize"));
        }

        int bytesToFree = cacheSize + bytes - cacheMaxSize;

        if(bytesToFree > 0) {
            while(bytesToFree > 0) {
                String entryKey = (String) cache.firstKey();
                CacheEntry entry = (CacheEntry) cache.remove(entryKey);
                
           //     System.out.println("**************** " + getName() + "  removing key \"" + entryKey + "\" freeing " + entry.getSize() + " bytes");
                bytesToFree -= entry.getSize();
                cacheSize -= entry.getSize();
            }
            
            for(int i=0; i<(cache.size()*0.1); i++) {
                String entryKey = (String) cache.firstKey();
                CacheEntry entry = (CacheEntry) cache.remove(entryKey);
           //     System.out.println("**************** " + getName() + "  removing key \"" + entryKey + "\" freeing " + entry.getSize() + " bytes");
                bytesToFree -= entry.getSize();
                cacheSize -= entry.getSize();            
            }
        }
    //    System.out.println("**************** cacheSize size = " + cacheSize   + "  cacheMaxSize = " + cacheMaxSize + "  objects = " + cache.size());
    }
*/


    public void load(final String key, final CEResource obj) throws Exception {
        if(obj == null) {
            return;
        }

        if(cache.containsKey(key)) {
            throw (new Exception("The entry key \"" + key + "\" already exist!"));
        }

        synchronized (cache) {
            if(cache.size() + 1 - cacheMaxSize > 0) {
                int x = cacheMaxSize*20/100;
                for(int i=0; i<x; i++) {
                    cache.remove(cache.firstKey());
                }
            }
            
            CacheEntry entry = new CacheEntry(System.currentTimeMillis(), (CEResource) obj.clone());
            cache.put(key, entry);
       //     cacheSize++;
//            cacheSize += entry.getSize();
//            System.out.println("CACHE " + name + " size = " + cache.size());
        }
    }



    public boolean unload(final String key) {
        if(cache.containsKey(key)) {
            synchronized (cache) {
                cache.remove(key);
             //   CacheEntry entry = (CacheEntry) cache.remove(key);
             //   if(entry != null) {
                 //   return true;
                   // cacheSize -= entry.getSize();
            //    }
            }
         //   System.out.println("CACHE:unload size = " + cache.size() + " load " + key);
       //     System.out.println("**************** " + getName() + "  UNLOAD: cacheSize size = " + cacheSize   + "  cacheMaxSize = " + cacheMaxSize + "  objects = " + cache.size() +  " key " + key);
            
            return true;
        }

        return false;
    }


    public void update(final String key, final CEResource obj) throws Exception {
        if(obj == null) {
            return;
        }

        if(!cache.containsKey(key)) {
            throw (new Exception("The entry key \"" + key + "\" doesn't exist!"));
        }
        
        synchronized (cache) {
            CacheEntry entry = new CacheEntry(System.currentTimeMillis(), (CEResource) obj.clone());
            cache.put(key, entry);
        }
    }
    

    public CEResource lookup(final String key) {
        if(key == null || cache == null) {
            return null;
        }

        CacheEntry entry = (CacheEntry) cache.get(key);

        if(entry != null) {
        	return (CEResource) entry.getCEResource().clone();
        }
        
        return null;
    }


    /*
      This method must be used only for tests
    */
    public String cacheStatus() {
    	StringBuffer buff = new StringBuffer("\n");
    	Iterator items = cache.keySet().iterator();
    	while( items.hasNext() ){
    		String key = (String)items.next();
    		buff.append(key).append("\n");
    	}
    	return buff.toString();
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
    /*
     * ContextCache cache = new ContextCache(10); System.out.println("cache max
     * size " + cache.getCacheMaxSize() ); System.out.println("cache size " +
     * cache.getCacheSize() ); Utils.saveObject("/tmp/test.obj", new
     * Integer(1200)); cache.load("stringa 1",
     * Utils.loadObject("/tmp/test.obj")); // cache.load("stringa 2", "GI");
     * System.out.println("cache size 1 " + cache.getCacheSize() );
     * cache.load("stringa 3", "tpt9yt"); cache.lookup("stringa 1");
     * System.out.println("cache size " + cache.lookup("stringa 1") );
     */
    }

}
