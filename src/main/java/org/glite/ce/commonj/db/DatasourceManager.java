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

package org.glite.ce.commonj.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * Class to manage <code>JDBC</code> datasources. Connection pooling is used.
 * <code>ConnectionManager</code> can register a valid data source
 * configuration.
 */
public class DatasourceManager {
    /** The logger */
    private static final Logger logger = Logger.getLogger(DatasourceManager.class);

    /** The datasource cache */
    private static final HashMap<String, DataSource> datasourceCache = new HashMap<String, DataSource>(0);

    public static void destroy() {
        datasourceCache.clear();
    }
    
    public static boolean addDataSource(String dataSourceName, DataSource dataSource) {
        boolean isPutDatasource = false;
        if ((dataSourceName == null) || (dataSource == null)) {
            logger.warn("dataSourceName and/or dataSource is/are null: (" + dataSourceName + ", " + dataSource + ")");        
        } else {          
            if (!datasourceCache.containsKey(dataSourceName)) {
                datasourceCache.put(dataSourceName, dataSource);
                isPutDatasource = true;
            }
        }
        return isPutDatasource;
    }

    /**
     * Returns a connection from the connection pool associated to a given
     * datasource name.
     * 
     * @param datasourceName
     *            The datasource name.
     * @return a connection from the connection pool associated to a given
     *         datasource name.
     * @throws SQLException
     * @throws DatabaseException
     */
    public static Connection getConnection(String dataSourceName) throws DatabaseException {
        if (dataSourceName == null) {
            throw new DatabaseException("datasourceName not specified!");
        }

        if (!datasourceCache.containsKey(dataSourceName)) {
            throw new DatabaseException("datasourceName " + dataSourceName + " not found!");
        }

        DataSource dataSource = datasourceCache.get(dataSourceName);
        logger.debug("getConnection " + dataSourceName);
        
        Connection connection = null;

        try {
            connection = dataSource.getConnection();

            if (connection != null) {
                logger.debug("Connection got from datasource named: " + dataSourceName);
                connection.setAutoCommit(false);
            } else {
                throw new DatabaseException("problem in opening connection to target database [" + dataSourceName + "]");
            }
        } catch (Throwable e) {
            logger.error(e.getMessage());
            throw new DatabaseException("getConnection error: " + e.getMessage());
        }

        return connection;
    }    
}
