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

import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;

public class LogUtils {

    private static boolean initialized = false;

    public static void setup() {
        if (!initialized) {

            Properties props = new Properties();
            props.put("log4j.rootLogger", "debug, fileout");
            props.put("log4j.logger.org.glite.ce.commonj.configuration.xppm.ConfigurationManager",
                    "debug, regexcollector");
            props.put("log4j.appender.regexcollector",
                    "org.glite.ce.commonj.configuration.xppm.test.RegexCollectorAppender");
            props.put("log4j.appender.fileout", "org.apache.log4j.RollingFileAppender");
            props.put("log4j.appender.fileout.File", System.getProperty("log4j.output.file", "/tmp/testsuite.log"));
            props.put("log4j.appender.fileout.MaxFileSize", "10000KB");
            props.put("log4j.appender.fileout.MaxBackupIndex", "100");
            props.put("log4j.appender.fileout.layout", "org.apache.log4j.PatternLayout");
            String formatStr = "%d{dd MMM yyyy HH:mm:ss,SSS} %-5p %c (%F:%L) - (%t) %m%n";
            props.put("log4j.appender.fileout.layout.ConversionPattern", formatStr);

            PropertyConfigurator.configure(props);

            initialized = true;
        }
    }

    public static void startCollect(String loggerName, String regex) {
        RegexCollectorAppender.addPattern(loggerName, regex);
    }

    public static LoggingEvent[] getLoggerSnapshot() {
        RegexCollectorAppender.resetPatternTable();
        return RegexCollectorAppender.getCollectedEvents();
    }
}
