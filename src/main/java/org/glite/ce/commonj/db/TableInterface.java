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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * Interface for a generic to access a database.
 *
 */
public interface TableInterface  {
    
    /**
     * Returns from the named sequence.
     * @param sequenceName The name of the sequence.
     * @connection The connection.
     * @return The id from the named sequence.
     * @throws SQLException
     */
    public long executeSequenceId(String sequenceName, Connection connection) throws SQLException;
    
    /**
     * Returns the prepared statement for the given query.
     * @return The prepared statement for the given query.
     * @param query The query to be executed.
     * @connection The connection.
     * @throws SQLException
     */
    public PreparedStatement getPreparedStatement(Query query, Connection connection) throws SQLException;
}
