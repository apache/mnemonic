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
package org.apache.mnemonic.query.memory;

import org.apache.mnemonic.service.computing.ValueInfo;

/**
 * Queryable interface for memory services
 * It is optional for underlying services
 *
 */
public interface Queryable {

  /**
   * Retrieve existing class names
   *
   * @return an array of class names
   */
  String[] getClassNames();

  /**
   * Retrieve existing entity names according to specified class name
   *
   * @param clsname
   *          specify the class name
   *
   * @return an array of entity names
   */
  String[] getEntityNames(String clsname);

  /**
   * retrieve entity info
   *
   * @param clsname
   *          specify a class name
   *
   * @param etyname
   *          specify a entity name
   *
   * @return an entity info
   */
  EntityInfo getEntityInfo(String clsname, String etyname);

  /**
   * create an entity
   *
   * @param entityinfo
   *          specify an entity info to create
   */
  void createEntity(EntityInfo entityinfo);

  /**
   * destroy an entity
   *
   * @param clsname
   *          specify a class name
   *
   * @param etyname
   *          specify an entity name
   */
  void destroyEntity(String clsname, String etyname);

  /**
   * update entity queryable info for a set of durable objects
   *
   * @param clsname
   *          specify a class name
   *
   * @param etyname
   *          specify an entity name
   *
   * @param updobjs
   *          specify a set of durable objects for update
   */
  void updateQueryableInfo(String clsname, String etyname, ValueInfo updobjs);

  /**
   * delete a set of durable objects
   *
   * @param clsname
   *          specify a class name
   *
   * @param etyname
   *          specify an entity name
   *
   * @param updobjs
   *          specify a set of durable objects to delete
   */
  void deleteQueryableInfo(String clsname, String etyname, ValueInfo updobjs);

  /**
   * do query using a querying string
   *
   * @param querystr
   *          specify a query string
   *
   * @return a result set
   */
  ResultSet query(String querystr);

}
