/**
 * Copyright 2010-present Facebook. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.TagFu.facebook.model;

/**
 * Provides a strongly-typed representation of a Place as defined by the Graph API. Note that this interface is intended
 * to be used with GraphObject.Factory and not implemented directly.
 */
public interface GraphPlace extends GraphObject {

    /**
     * Returns the category of the place.
     * 
     * @return the category of the place
     */
    String getCategory();

    /**
     * Returns the ID of the place.
     * 
     * @return the ID of the place
     */
    String getId();

    /**
     * Returns the location of the place.
     * 
     * @return the location of the place
     */
    GraphLocation getLocation();

    /**
     * Returns the name of the place.
     * 
     * @return the name of the place
     */
    String getName();

    /**
     * Sets the category of the place.
     * 
     * @param category the category of the place
     */
    void setCategory(String category);

    /**
     * Sets the ID of the place.
     * 
     * @param id the ID of the place
     */
    void setId(String id);

    /**
     * Sets the location of the place.
     * 
     * @param location the location of the place
     */
    void setLocation(GraphLocation location);

    /**
     * Sets the name of the place.
     * 
     * @param name the name of the place
     */
    void setName(String name);
}
