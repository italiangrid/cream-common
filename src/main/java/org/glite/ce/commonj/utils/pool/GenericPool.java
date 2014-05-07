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
 * Authors: L. Zangrando <zangrando@pd.infn.it>
 *
 */

package org.glite.ce.commonj.utils.pool;

import java.util.ArrayList;

public abstract class GenericPool<T> {
    private ArrayList<T> allocatedItems;
    private ArrayList<T> unAllocatedItems;
    private int minPoolSize = 1;
    private int maxPoolSize = 50;
    private boolean shutdown = false;

    public GenericPool() throws GenericPoolException {
        // initialize pool
        initialize();
    }
    
    
    /**
     * Creates the pool.
     * 
     * @param minIdle
     *            minimum number of objects residing in the pool
     */
    public GenericPool(final int minPoolSize, final int maxPoolSize) throws GenericPoolException {
        if (minPoolSize < 0) {
            throw new GenericPoolException("wrong value for minPoolSize");
        }
        
        if (maxPoolSize < 0) {
            throw new GenericPoolException("wrong value for maxPoolSize");
        }
        
        this.minPoolSize= minPoolSize;
        this.maxPoolSize= maxPoolSize;
        
        // initialize pool
        initialize();
    }

    
    private void initialize() {
        allocatedItems = new ArrayList<T>();
        unAllocatedItems = new ArrayList<T>();

        for (int i = 0; i < minPoolSize; i++) {
            unAllocatedItems.add(allocate());
        }
    }
    

    public boolean isShutdown() {
        return shutdown;
    }
    
    
    /**
     * Gets the next free object from the pool. If the pool doesn't contain any
     * objects, a new object will be allocated and given to the caller of this
     * method back.
     * 
     * @return T borrowed object
     */
    public synchronized T get() throws GenericPoolException {
        if (shutdown) {
            throw new GenericPoolException("the pool is shutdown");
        }
        
        T item = null;
        
        if (unAllocatedItems.size() == 0 && allocatedItems.size() < maxPoolSize) {
            item = allocate();
            System.out.println("allocated new item");
        } else {
            item = unAllocatedItems.remove(0);
        }

        allocatedItems.add(item);

        return item;
    }

    /**
     * Returns object back to the pool.
     * 
     * @param object
     *            object to be returned
     */
    public synchronized void release(T item) throws GenericPoolException {
        if (shutdown) {
            throw new GenericPoolException("the pool is shutdown");
        }
        
        if (item == null) {
            return;
        }
        
        if (allocatedItems.remove(item)) {
            unAllocatedItems.add(item);
        }
    }

    /**
     * Shutdown this pool.
     */
    public void shutdown() {
        shutdown = true;
        
        for (T item : allocatedItems) {
            if (item instanceof Destroyable) {
                ((Destroyable)item).destroy();
            }
        }
        allocatedItems.clear();
        
        for (T item : unAllocatedItems) {
            if (item instanceof Destroyable) {
                ((Destroyable)item).destroy();
            }
        }
        unAllocatedItems.clear();
    }

    public void printStatus() {
        System.out.println("allocatedItems=" + allocatedItems.size() + " unAllocatedItems=" + unAllocatedItems.size());
    }
    
    
    /**
     * Creates a new object.
     * 
     * @return T new object
     */
    protected abstract T allocate();

}
