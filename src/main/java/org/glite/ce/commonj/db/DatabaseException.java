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
 * Authors: Silvano Squizzato (silvano.squizzato@pd.infn.it)
 *
 */

package org.glite.ce.commonj.db;

/**
 * Class representing a generic exception related to the database.
 */
public class DatabaseException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param message The message.
     * @param cause The cause.
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message The message.
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * @param cause The cause.
     */
    public DatabaseException(Throwable cause) {
        super(cause);
    }
    

}
