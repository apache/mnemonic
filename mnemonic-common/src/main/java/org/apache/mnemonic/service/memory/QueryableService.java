/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.mnemonic.service.memory;

import org.apache.mnemonic.query.memory.EntityInfo;
import org.apache.mnemonic.query.memory.ResultSet;
import org.apache.mnemonic.service.computing.ValueInfo;

/**
 * An interface for interacting with a Queryable Service that manages memory pools.
 */
public interface QueryableService {

    /**
     * Retrieve existing class names in the specified memory pool.
     *
     * @param id the identifier of the backed memory pool
     * @return an array of class names
     */
    String[] getClassNames(long id);

    /**
     * Retrieve existing entity names according to the specified class name in the given memory pool.
     *
     * @param id      the identifier of the backed memory pool
     * @param clsname specify the class name
     * @return an array of entity names
     */
    String[] getEntityNames(long id, String clsname);

    /**
     * Retrieve entity info for a specific class and entity name in the given memory pool.
     *
     * @param id      the identifier of the backed memory pool
     * @param clsname specify a class name
     * @param etyname specify an entity name
     * @return an entity info
     */
    EntityInfo getEntityInfo(long id, String clsname, String etyname);

    /**
     * Create an entity in the specified memory pool based on the given entity info.
     *
     * @param id          the identifier of the backed memory pool
     * @param entityinfo specify an entity info to create
     */
    void createEntity(long id, EntityInfo entityinfo);

    /**
     * Destroy an entity in the specified memory pool based on class and entity names.
     *
     * @param id      the identifier of the backed memory pool
     * @param clsname specify a class name
     * @param etyname specify an entity name
     */
    void destroyEntity(long id, String clsname, String etyname);

    /**
     * Update queryable info for a set of durable objects associated with a specific entity.
     *
     * @param id      the identifier of the backed memory pool
     * @param clsname specify a class name
     * @param etyname specify an entity name
     * @param updobjs specify a set of durable objects for update
     */
    void updateQueryableInfo(long id, String clsname, String etyname, ValueInfo updobjs);

    /**
     * Delete a set of durable objects associated with a specific entity.
     *
     * @param id      the identifier of the backed memory pool
     * @param clsname specify a class name
     * @param etyname specify an entity name
     * @param updobjs specify a set of durable objects to delete
     */
    void deleteQueryableInfo(long id, String clsname, String etyname, ValueInfo updobjs);

    /**
     * Perform a query using a querying string in the specified memory pool.
     *
     * @param id       the identifier of the backed memory pool
     * @param querystr specify a query string
     * @return a result set
     */
    ResultSet query(long id, String querystr);
}
