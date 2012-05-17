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
 * Class representing a query.
 *
 */
public class Query {
    
    /** Label for sequenceId prepared statement */
    public static final String SEQUENCE_ID_STATEMENT = "SEQUENCE_ID_STATEMENT";
    
    /** Label for insert prepared statement */
    public static final String INSERT_STATEMENT = "INSERT_STATEMENT";
    
    /** Label for delete prepared statement */
    public static final String DELETE_STATEMENT = "DELETE_STATEMENT";
    
    /** Label for delete prepared statement */
    public static final String UPDATE_STATEMENT = "UPDATE_STATEMENT";
    
    /** Label for select prepared statement */
    public static final String SELECT_STATEMENT = "SELECT_STATEMENT";
    
    /** Query name */
    private String name = null;
    
    /** SQL statement */
    private String statement = null;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the statement
     */
    public String getStatement() {
        return statement;
    }

    /**
     * @param statement the statement to set
     */
    public void setStatement(String statement) {
        this.statement = statement;
    }
}
