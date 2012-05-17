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

package org.glite.ce.commonj.configuration.xppm;

public class ConfigurationEvent {
    
    public final static int CREATED_CONFIG = 0;
    
    public final static int UPDATED_CONFIG = 1;
    
    public final static int DELETED_CONFIG = 2;

    private Class<?> category;

    private long timestamp;

    private int type;

    public ConfigurationEvent(Class<?> category, long ts, int type) {

        this.category = category;
        this.timestamp = ts;
        this.type = type;

    }

    public Class<?> getCategory() {
        return category;
    }

    public long getTime() {
        return timestamp;
    }

    public int getType() {
        return type;
    }

}
