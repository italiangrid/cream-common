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

import java.io.File;
import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.CommonConfigException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * The ConfigurationManager is the engine of the Common Configuration System for
 * any all the Computing Element services. It loads the instantiates the
 * registered configuration handlers and parses the XML configuration file.
 * 
 */
public class ConfigurationManager
    implements Runnable {

    private static Logger logger = Logger.getLogger(ConfigurationManager.class.getName());

    private static String REFRESH_TIME_LABEL = "configuration.manager.refresh.time";

    private static long REFRESH_TIME_DEFAULT = 5;

    private ArrayList<ConfigurationHandler> handlerList;

    private String xmlFilename;

    private long lastTick;

    private long lastModificationTime;

    private long refreshTime;

    private ArrayList<ConfigurationListener> listeners;

    private Thread notifyThread;

    private boolean running;

    private boolean deadlockDetected;

    private ArrayBlockingQueue<ConfigurationEvent> eventQueue;

    /**
     * Instantiates a new configuration manager. The XML configuration file is
     * parsed and the configuration handlers are loaded. The notification
     * manager is disabled.
     * 
     * @param xmlFilename
     *            is the path of the XML configuration file
     * @throws CommonConfigException
     *             if XML file is not correct
     */
    public ConfigurationManager(String xmlFilename) throws CommonConfigException {
        this.xmlFilename = xmlFilename;
        File xmlFile = new File(xmlFilename);

        if (!xmlFile.isFile()) {
            throw new CommonConfigException("Wrong configuration file: " + xmlFilename);
        }

        lastTick = 0;
        lastModificationTime = 0;

        try {
            refreshTime = Long.parseLong(System.getProperty(REFRESH_TIME_LABEL)) * 1000;
            if (refreshTime < 1000) {
                refreshTime = 1000;
            }
        } catch (Throwable th) {
            refreshTime = REFRESH_TIME_DEFAULT * 1000;
        }

        handlerList = new ArrayList<ConfigurationHandler>();

        ServiceLoader<ConfigurationHandler> serviceLoader = ServiceLoader.load(ConfigurationHandler.class);
        for (ConfigurationHandler cHandler : serviceLoader) {
            handlerList.add(cHandler);
            cHandler.disable();
            logger.debug("Loaded handler " + cHandler.getClass().getName());
        }

        eventQueue = new ArrayBlockingQueue<ConfigurationEvent>(1000);

        listeners = new ArrayList<ConfigurationListener>();

        notifyThread = new Thread(this, "ConfigurationNotifier");
        running = false;
        deadlockDetected = false;

    }

    /**
     * Instantiates a new configuration manager. The XML configuration file is
     * parsed and the configuration handlers are loaded. The notification
     * manager is launched with a given refresh rate.
     * 
     * @param xmlFilename
     *            is the path of the XML configuration file
     * @param rate
     *            is the interval in milliseconds between two consecutive
     *            configuration lookups
     * @throws CommonConfigException
     *             if XML file is not correct
     */
    public ConfigurationManager(String xmlFilename, long rate) throws CommonConfigException {

        this(xmlFilename);
        refreshTime = rate / 2;
        running = true;
        notifyThread.start();

    }

    /**
     * Instantiates a new configuration manager. The XML configuration file is
     * parsed and the configuration handlers are loaded. The notification
     * manager is launched with a given refresh rate; a list of listeners is
     * registered at startup
     * 
     * @param xmlFilename
     *            is the path of the XML configuration file
     * @param rate
     *            is the interval in milliseconds between two consecutive
     *            configuration lookups
     * @param listeners
     *            is the array of listeners to be registered at the startup of
     *            the notification manager
     * @throws CommonConfigException
     *             if XML file is not correct
     */
    public ConfigurationManager(String xmlFilename, long rate, ConfigurationListener[] listeners)
        throws CommonConfigException {

        this(xmlFilename);
        refreshTime = rate / 2;

        for (ConfigurationListener lsnr : listeners) {
            this.addListener(lsnr);
        }

        running = true;
        notifyThread.start();

    }

    /**
     * Returns an array of configuration objects registered for a given category
     * The category specify the class type of the returned object.
     * 
     * @param category
     *            is the class type for the registered object
     * @return an array, empty but never null, of configuration objects
     */
    public synchronized Object[] getConfigurationElements(Class<?> category) {

        checkAndUpdate();

        ArrayList<Object> objList = new ArrayList<Object>();

        for (ConfigurationHandler cHandler : handlerList) {

            if (!cHandler.isEnabled())
                continue;

            if (cHandler.getCategory().equals(category)) {
                Object[] objArray = cHandler.getConfigurationElement();
                if (objArray == null) {
                    continue;
                }
                for (Object obj : objArray) {
                    if (category.isInstance(obj)) {
                        objList.add(obj);
                    } else {
                        logger.error("Bad returned category for " + cHandler.getClass().getName());
                    }
                }
            }
        }

        Object[] result = new Object[objList.size()];
        objList.toArray(result);
        return result;
    }

    private void checkAndUpdate() {

        long now = System.currentTimeMillis();
        if ((now - lastTick) < refreshTime) {
            return;
        }

        lastTick = now;
        File xmlFile = new File(xmlFilename);
        long tmpTS = xmlFile.lastModified();

        if (tmpTS > lastModificationTime) {

            Document document = null;
            boolean commitOK = true;

            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                logger.debug("Parsing file " + xmlFile);
                document = builder.parse(xmlFile);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return;
            }

            try {

                for (ConfigurationHandler handler : handlerList) {
                    XPathExpression xpathExpr = handler.getXPath();
                    NodeList pElements = (NodeList) xpathExpr.evaluate(document, XPathConstants.NODESET);
                    if (pElements != null && pElements.getLength() > 0) {

                        if (!handler.isEnabled()) {
                            handler.scheduleForAdd();
                        }

                        if (handler.process(pElements) && handler.isEnabled()) {
                            handler.scheduleForUpdate();
                        }

                    } else {

                        if (handler.isEnabled()) {
                            handler.scheduleForDelete();
                        }

                    }
                }

            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                commitOK = false;
            }

            if (commitOK) {
                for (ConfigurationHandler handler : handlerList) {
                    try {

                        if (handler.isScheduledForAdd() || handler.isScheduledForUpdate()) {
                            handler.commit();
                            logger.debug("Committed " + handler.getCategory().getName());
                        }

                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        handler.disable();
                    }

                    if (handler.isScheduledForAdd()) {

                        handler.enable();
                        enqueueEvent(handler.getCategory(), tmpTS, ConfigurationEvent.CREATED_CONFIG);

                    } else if (handler.isScheduledForUpdate()) {

                        handler.enable();
                        enqueueEvent(handler.getCategory(), tmpTS, ConfigurationEvent.UPDATED_CONFIG);

                    } else if (handler.isScheduledForDelete()) {

                        handler.disable();
                        enqueueEvent(handler.getCategory(), tmpTS, ConfigurationEvent.DELETED_CONFIG);

                    }
                }

                lastModificationTime = tmpTS;
                for (ConfigurationHandler handler : handlerList) {
                    if (!handler.isEnabled()) {
                        continue;
                    }

                    long tmpl = getLatestTriggerTime(handler);
                    if (tmpl > lastModificationTime) {
                        lastModificationTime = tmpl;
                    }
                }

            } else {
                for (ConfigurationHandler handler : handlerList) {
                    try {
                        if (handler.isScheduledForAdd() || handler.isScheduledForUpdate()) {
                            handler.rollback();
                            logger.debug("Rolledback " + handler.getCategory().getName());
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        handler.disable();
                    }

                    if (handler.isScheduledForAdd()) {
                        handler.disable();
                    }

                    if (handler.isScheduledForDelete() || handler.isScheduledForUpdate()) {
                        handler.enable();
                    }
                }
            }

        } else {

            long lastTriggerMod = 0;
            boolean commitOK = true;

            try {

                for (ConfigurationHandler handler : handlerList) {

                    if (!handler.isEnabled()) {
                        continue;
                    }

                    long tmpl = getLatestTriggerTime(handler);
                    if (tmpl > lastTriggerMod) {
                        lastTriggerMod = tmpl;
                    }

                    if (tmpl > lastModificationTime) {
                        if (handler.processTriggers()) {
                            handler.scheduleForUpdate();
                        }
                    }

                }

            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                commitOK = false;
            }

            for (ConfigurationHandler handler : handlerList) {
                if (handler.isScheduledForUpdate()) {
                    try {
                        if (commitOK) {

                            handler.commit();
                            logger.debug("Committed " + handler.getCategory().getName());
                            enqueueEvent(handler.getCategory(), lastTriggerMod, ConfigurationEvent.UPDATED_CONFIG);

                        } else {

                            handler.rollback();

                        }

                        handler.enable();

                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        handler.disable();
                    }
                }
            }

            if (commitOK && lastTriggerMod > lastModificationTime) {
                lastModificationTime = lastTriggerMod;
            }

        }
    }

    private void enqueueEvent(Class<?> cat, long ts, int type) {
        if (running) {
            try {
                ConfigurationEvent event = new ConfigurationEvent(cat, ts, type);
                eventQueue.add(event);
            } catch (IllegalStateException illEx) {
                logger.error("Cannot enqueue event", illEx);
            }
        }
    }

    private long getLatestTriggerTime(ConfigurationHandler handler) {
        long result = 0;

        File[] triggers = handler.getTriggers();
        if (triggers == null) {
            return result;
        }

        for (File trigger : triggers) {

            if (trigger.lastModified() > result) {
                result = trigger.lastModified();
            }

        }

        return result;
    }

    /*
     * Notification manager
     */

    /**
     * Notifies the scheduled configuration events to the registered listeners
     * If no events are scheduled the thread is blocked waiting for
     * configuration changes. The thread may exit if illegal interrupted
     * exception are thrown. The correct way to stop the thread is to call the
     * shutdown method.
     */
    public void run() {

        synchronized (this) {
            checkAndUpdate();
        }

        while (running) {
            try {

                ConfigurationEvent event = eventQueue.poll(refreshTime * 2, TimeUnit.MILLISECONDS);

                if (event != null) {

                    logger.debug("Read event " + event.getCategory().getName() + " " + event.getType());

                    synchronized (listeners) {
                        deadlockDetected = true;
                        for (ConfigurationListener lsnr : listeners) {
                            try {
                                lsnr.notify(event);
                            } catch (Throwable th) {
                                logger.error(th.getMessage(), th);
                            }
                        }
                        deadlockDetected = false;
                    }

                } else {

                    synchronized (this) {
                        checkAndUpdate();
                    }

                }

            } catch (InterruptedException intEx) {
                if (running) {
                    logger.error("Notification dispatcher abruptly terminated");
                    return;
                } else {
                    logger.info("Shutting down notification dispatcher");
                }
            }
        }

        logger.info("ConfigurationNotifier halted");
    }

    /**
     * Stops the notification thread.
     */
    public void shutdown() {
        if (running) {
            running = false;
            notifyThread.interrupt();
        }
    }

    /**
     * Registers a new configuration listener. Attempting to call this method
     * from inside the the notify method of the listener generates an
     * IllegalStateException.
     * 
     * @param lsnr
     *            is the listener to be registered
     */
    public void addListener(ConfigurationListener lsnr) {

        if (deadlockDetected) {
            logger.error("Deadlock detected, cannot add listener");
            throw new IllegalStateException("Deadlock detected");
        }

        synchronized (listeners) {
            listeners.add(lsnr);
        }

    }

    /**
     * Removes a registered configuration listener. Attempting to call this
     * method from inside the the notify method of the listener generates an
     * IllegalStateException.
     * 
     * @param lsnr
     *            is the listener to be removed
     */
    public void removeListener(ConfigurationListener lsnr) {

        if (deadlockDetected) {
            logger.error("Deadlock detected, cannot remove listener");
            throw new IllegalStateException("Deadlock detected");
        }

        synchronized (listeners) {
            listeners.remove(lsnr);
        }

    }

}
