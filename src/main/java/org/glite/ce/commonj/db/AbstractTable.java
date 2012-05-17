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
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;


/**
 * Class representing a generic Data Access Object accessing the database.
 *
 */
public class AbstractTable implements TableInterface {
    
    /** The logger */
    private final static Logger logger = Logger.getLogger(AbstractTable.class);
    
    /** The sequence id query */
    protected Query sequenceIdQuery = null;
    
    /** The insert query */
    protected Query insertQuery = null;
    
    /** The delete query */
    protected Query deleteQuery = null;
    
    /** The update query */
    protected Query updateQuery = null;
    
    /** The select query */
    protected Query selectQuery = null;
 
    /**
     * Returns the query for retrieving the id from a sequence.
     * @param sequenceName The sequence name.
     * @return The query for retrieving the id from a sequence.
     */
    protected Query getSequenceIdQuery(String sequenceName) {
        if (sequenceIdQuery == null) {
            sequenceIdQuery = new Query();
            sequenceIdQuery.setName(Query.SEQUENCE_ID_STATEMENT);
            sequenceIdQuery.setStatement("select nextVal('" + sequenceName + "')");
        }
        logger.debug("sequenceIdQuery = " + sequenceIdQuery.getStatement());
        return sequenceIdQuery;
    }
    
    /**
     * @see org.glite.ce.commonj.db.TableInterface#getPreparedStatement(org.glite.ce.common.db.Query, java.sql.Connection)
     */
    public PreparedStatement getPreparedStatement(Query query, Connection connection) throws SQLException, IllegalArgumentException {
        if (query == null) {
            throw new IllegalArgumentException("query not specified!");
        }
        if (query.getName() == null) {
            throw new IllegalArgumentException("query name not specified!");
        }
        if (query.getStatement() == null) {
            throw new IllegalArgumentException("query statement not specified!");
        }
        if (connection == null) {
            throw new IllegalArgumentException("connection not specified!");
        }

        PreparedStatement statement = connection.prepareStatement(query.getStatement(), PreparedStatement.RETURN_GENERATED_KEYS);
        
        if(statement == null) {
            throw new SQLException("cannot create a PrepareStatement for the query \"" + query.getStatement() + "\"");
        }
        
        return statement;
    }

    /**
     * @see org.glite.ce.commonj.db.TableInterface#executeSequenceId(java.lang.String, java.sql.Connection)
     */
    public long executeSequenceId(String sequenceName, Connection connection) throws SQLException, IllegalArgumentException {
        if (sequenceName == null) {
            throw new IllegalArgumentException("sequenceName not specified!");
        }
        if (connection == null) {
            throw new IllegalArgumentException("connection not specified!");
        }
        
        long id = -1;
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        
        try {
            // Get the id from the sequence named sequenceName
            pstmt = getPreparedStatement(getSequenceIdQuery(sequenceName), connection);
            rset = pstmt.executeQuery();
            if (rset != null) {
                rset.next();
                id = rset.getLong(1);
            }
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException sqle1) {
                    logger.error(sqle1);
                }
            }
            if (rset != null) {
                try {
                    rset.close();
                } catch (SQLException sqle2) {
                    logger.error(sqle2);
                }
            }
        }
        return id;
    }
}
