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

import java.util.HashMap;

public class CacheLockBin extends HashMap{

    private int count;
    private int reserved;
    private int lock;
    private int[] opCount;
    private int[] opLock;
    private int[] opReserved;

    public CacheLockBin(){
        super();
        count = 0;
        reserved = 0;
        lock = 0;
        opLock = new int[CacheLock.NUM_OF_OPERATION];
        opCount = new int[CacheLock.NUM_OF_OPERATION];
        opReserved = new int[CacheLock.NUM_OF_OPERATION];

        for(int k=0; k<CacheLock.NUM_OF_OPERATION; k++){
            opLock[k] = 0;
            opCount[k] = 0;
            opReserved[k] = 0;
        }
    }

    public void incrCount(int op){
        opCount[op]++;
        count++;
    }

    public void decrCount(int op){
        opCount[op]--;
        count--;
    }

    public int getCount(int op){
        return opCount[op];
    }

    public int getCount(){
        return count;
    }

    public int getOpLock(int op){
        return opLock[op];
    }

    public int[] getOpLock(){
        return opLock;
    }

    public void addOpLock(int op){
        opLock[op]++;
        lock++;
    }

    public void removeOpLock(int op){
        opLock[op]--;
        lock--;
    }

    public boolean isFree(){
        return lock==0;
    }

    public void incrReserved(int op){
        opReserved[op]++;
        reserved++;
    }

    public void decrReserved(int op){
        opReserved[op]--;
        reserved--;
    }

    public int getReserved(int op){
        return opReserved[op];
    }

    public int getReserved(){
        return reserved;
    }

}
