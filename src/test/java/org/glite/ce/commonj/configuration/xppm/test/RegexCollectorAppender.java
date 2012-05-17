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
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class RegexCollectorAppender
    extends AppenderSkeleton {

    private static HashMap<String, Pattern> patterns = new HashMap<String, Pattern>();

    private static ArrayList<LoggingEvent> collected = new ArrayList<LoggingEvent>();

    public static void addPattern(String loggerName, String regex) {
        synchronized (patterns) {
            patterns.put(loggerName, Pattern.compile(regex));
        }
    }

    public static void resetPatternTable() {
        synchronized (patterns) {
            patterns.clear();
        }
    }

    public static LoggingEvent[] getCollectedEvents() {
        synchronized (collected) {
            LoggingEvent[] result = new LoggingEvent[collected.size()];
            collected.toArray(result);
            return result;
        }
    }

    public static void clean() {
        synchronized (collected) {
            collected.clear();
        }
    }

    public RegexCollectorAppender() {
        super();
    }

    public void append(LoggingEvent event) {

        Pattern tmpp = null;
        synchronized (patterns) {
            tmpp = patterns.get(event.getLoggerName());
        }

        if (tmpp != null) {
            Matcher match = tmpp.matcher(event.getMessage().toString());
            if (match.find()) {
                synchronized (collected) {
                    collected.add(event);
                }
            }
        }

    }

    public boolean requiresLayout() {
        return false;
    }

    public void close() {
    }
}
