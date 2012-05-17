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

package org.glite.ce.commonj.utils;

/**
 * This class is a simple semaphore used to synchronize access to shared resources.
 *
 * @author Luigi Zangrando (zangrando@pd.infn.it)
 * 
 */
public class BooleanLock extends Object {
    private boolean value;

    /**
     * Creates a new BooleanLock object with initial value false.
     */
    public BooleanLock() {
        this(false);
    }

    /**
     * Creates a new BooleanLock object with specified initial value
     *
     * @param The boolean initial value
     */
    public BooleanLock(boolean initialValue) {
        value = initialValue;
    }

    /**
     * Set a new value to BooleanLock.
     * When changed all objects synchronized will be notified.
     *
     * @param newValue The boolean new value.
     */
    public synchronized void setValue(boolean newValue) {
        if (newValue != value) {
            value = newValue;
            notifyAll();
        }
    }

    /**
     * Set BooleanLock value to true if it is set to false before the timeout.
     *
     * @param msTimeout The timeout expressed in milliseconds.
     * If 0L wait indefinitely.
     *
     * @return A boolean representing the final outcome.
     * This will be true if the BooleanLock value has been successfully set to true before the timeout, false otherwise.
     *
     * @throws InterruptedException 
     */
    public synchronized boolean waitToSetTrue(long msTimeout) throws InterruptedException {
        boolean success = waitUntilFalse(msTimeout);

        if (success) {
            setValue(true);
        }

        return success;
    }

    /**
     * Set BooleanLock value to false if it is set to true before the timeout.
     *
     * @param msTimeout The timeout expressed in milliseconds.
     * If 0L wait indefinitely.
     *
     * @return A boolean representing the final outcome.
     * This will be true if the BooleanLock value has been successfully set to false before the timeout, false otherwise.
     *
     * @throws InterruptedException 
     */
    public synchronized boolean waitToSetFalse(long msTimeout) throws InterruptedException {
        boolean success = waitUntilTrue(msTimeout);

        if (success) {
            setValue(false);
        }

        return success;
    }

    /**
     * Check if BooleanLock value is true
     *
     * @return The boolean the response
     */
    public synchronized boolean isTrue() {
        return value;
    }


    /**
     * Check if BooleanLock value is false
     *
     * @return The boolean representing the response
     */
    public synchronized boolean isFalse() {
        return !value;
    }

    /**
     * Check if the value of the BooleanLock is true 
     * or wait for a maximum amount of time specified by msTimeout that the value is set to true.
     *
     * @param msTimeout The maximum amount of time to wait before return a response.
     * This is expressed in milliseconds.
     * If 0L wait indefinitely.
     *
     * @return A boolean representing the response of the operation.
     *
     * @throws InterruptedException
     */
    public synchronized boolean waitUntilTrue(long msTimeout) throws InterruptedException {
        return waitUntilStateIs(true, msTimeout);
    }

    /**
     * Check if the value of the BooleanLock is false 
     * or wait for a maximum amount of time specified by msTimeout that the value is set to false.
     *
     * @param msTimeout The maximum amount of time to wait before return a response.
     * This is expressed in milliseconds.
     * If 0L wait indefinitely.
     *
     * @return A boolean representing the response of the operation.
     *
     * @throws InterruptedException
     */
    public synchronized boolean waitUntilFalse(long msTimeout) throws InterruptedException {
        return waitUntilStateIs(false, msTimeout);
    }

    /**
     * Check if the value of the BooleanLock equals the specified state 
     * or wait for a maximum amount of time specified by msTimeout that the value is set to the specified state.
     *
     * @param msTimeout The maximum amount of time to wait before return a response.
     * This is expressed in milliseconds.
     * If 0L wait indefinitely.
     *
     * @return A boolean representing the response of the operation.
     *
     * @throws InterruptedException
     */
    public synchronized boolean waitUntilStateIs(boolean state, long msTimeout) throws InterruptedException {
        if (msTimeout == 0L) {
            while (value != state) {
                wait(); // wait indefinitely until notified
            }

            // condition has finally been met
            return true;
        }

        // only wait for the specified amount of time
        long endTime = System.currentTimeMillis() + msTimeout;
        long msRemaining = msTimeout;

        while ((value != state) && (msRemaining > 0L)) {
            wait(msRemaining);
            msRemaining = endTime - System.currentTimeMillis();
        }

        // May have timed out, or may have met value, 
        // calculate return value.
        return (value == state);
    }
}
