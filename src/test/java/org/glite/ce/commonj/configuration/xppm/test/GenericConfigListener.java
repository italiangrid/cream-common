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

package org.glite.ce.commonj.configuration.xppm.test;

import java.util.ArrayList;
import java.util.Iterator;

import org.glite.ce.commonj.configuration.xppm.ConfigurationListener;
import org.glite.ce.commonj.configuration.xppm.ConfigurationEvent;
import org.apache.log4j.Logger;

public class GenericConfigListener
    implements ConfigurationListener, Iterable<ConfigurationEvent> {

    private static Logger logger = Logger.getLogger(GenericConfigListener.class.getName());

    private ArrayList<ConfigurationEvent> eventList;

    public GenericConfigListener() {
        eventList = new ArrayList<ConfigurationEvent>();
    }

    public void notify(ConfigurationEvent event) {
        logger.info("Received event for " + event.getCategory().getName());
        eventList.add(event);
    }

    public void reset() {
        eventList.clear();
    }

    public int size() {
        return eventList.size();
    }

    public Iterator<ConfigurationEvent> iterator() {
        return eventList.iterator();
    }

    public ConfigurationEvent getLastEvent() {

        if (eventList.size() == 0) {
            return null;
        }

        return eventList.get(eventList.size() - 1);
    }

}
