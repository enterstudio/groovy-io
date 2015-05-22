package com.cedarsoftware.util.io

import groovy.transform.CompileStatic

import java.lang.reflect.Field

/**
 * The MapResolver converts the raw Maps created from the GroovyJsonParser to higher
 * quality Maps representing the implied object graph.  It does this by replace
 * @ref values with the Map with an @id key with the same value.
 *
 * This approach 'rewires' the original object graph.  During the resolution process,
 * if 'peer' classes can be found for given Maps (for example, an @type entry is
 * available which indicates the class that would have been associated to the Map,
 * then the associated class is consulted to help 'improve' the quality of the primitive
 * values within the map fields.  For example, if the peer class indicated that a field
 * was of type 'short', and the Map had a long value (JSON only returns long's for integer
 * types), then the long would be converted to a short.
 *
 * The final Map representation is a very high-quality graph that represents the original
 * JSON graph.  It can be passed as input to JsonWriter, and the JsonWriter will write
 * out the equivalent JSON to what was originally read.  This technique allows groovy-io to
 * be used on a machine that does not have any of the Groovy classes from the original graph,
 * read it in a JSON graph (any JSON graph), return the equivalent maps, allow mutations of
 * those maps, and finally this graph can be written out.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
@CompileStatic
class MapResolver extends Resolver
{
    protected MapResolver(Map<Long, JsonObject> objsRead, Map<String, Object> args)
    {
        super(objsRead, args)
    }

    protected void traverseFields(final Deque<JsonObject<String, Object>> stack, final JsonObject<String, Object> jsonObj)
    {
        final Object target = jsonObj.target
        for (Map.Entry<String, Object> e : jsonObj.entrySet())
        {
            final String fieldName = e.key

            if (fieldName.charAt(0) == '@')
            {   // Skip our own meta fields
                continue
            }

            final Field field = (target != null) ? MetaUtils.getField(target.getClass(), fieldName) : null
            final Object rhs = e.value

            if (rhs == null)
            {
                jsonObj[fieldName] = null
            }
            else if (rhs == JsonParser.EMPTY_OBJECT)
            {
                jsonObj[fieldName] = new JsonObject()
            }
            else if (rhs.getClass().isArray())
            {    // LHS of assignment is an [] field or RHS is an array and LHS is Object (Map)
                JsonObject<String, Object> jsonArray = new JsonObject<>()
                jsonArray['@items'] = rhs
                stack.addFirst(jsonArray)
                jsonObj[fieldName] = jsonArray
            }
            else if (rhs instanceof JsonObject)
            {
                JsonObject<String, Object> jObj = (JsonObject) rhs
                if (field != null && JsonObject.isPrimitiveWrapper(field.type))
                {
                    jObj.value = MetaUtils.newPrimitiveWrapper(field.type, jObj.value)
                    continue
                }
                Long ref = (Long) jObj['@ref']

                if (ref != null)
                {    // Correct field references
                    JsonObject refObject = getReferencedObj(ref)
                    jsonObj[fieldName] = refObject    // Update Map-of-Maps reference
                }
                else
                {
                    stack.addFirst(jObj)
                }
            }
            else if (field != null)
            {   // The code below is 'upgrading' the RHS values in the passed in JsonObject Map
                // by using the @type class name (when specified and exists), to coerce the vanilla
                // JSON values into the proper types defined by the class listed in @type.  This is
                // a cool feature of json-io, that even when reading a map-of-maps JSON file, it will
                // improve the final types of values in the maps RHS, to be of the field type that
                // was optionally specified in @type.
                final Class fieldType = field.type
                if (MetaUtils.isPrimitive(fieldType))
                {
                    jsonObj[fieldName] = MetaUtils.newPrimitiveWrapper(fieldType, rhs)
                }
                else if (BigDecimal.class == fieldType)
                {
                    jsonObj[fieldName] = Readers.bigDecimalFrom(rhs)
                }
                else if (BigInteger.class == fieldType)
                {
                    jsonObj[fieldName] = Readers.bigIntegerFrom(rhs)
                }
                else if (rhs instanceof String)
                {
                    if (fieldType != String.class && fieldType != StringBuilder.class && fieldType != StringBuffer.class)
                    {
                        if ("".equals(((String)rhs).trim()))
                        {   // Allow "" to null out a non-String field on the inbound JSON
                            jsonObj[fieldName] = null
                        }
                    }
                }
            }
        }
        jsonObj.target = null  // don't waste space (used for typed return, not for Map return)
    }
    /**
     * Process java.util.Collection and it's derivatives.  Collections are written specially
     * so that the serialization does not expose the Collection's internal structure, for
     * example a TreeSet.  All entries are processed, except unresolved references, which
     * are filled in later.  For an indexable collection, the unresolved references are set
     * back into the proper element location.  For non-indexable collections (Sets), the
     * unresolved references are added via .add().
     * @param stack   a Stack (Deque) used to support graph traversal.
     * @param jsonObj a Map-of-Map representation of the JSON input stream.
     */
    protected void traverseCollection(final Deque<JsonObject<String, Object>> stack, final JsonObject<String, Object> jsonObj)
    {
        final Object[] items = jsonObj.getArray()
        if (items == null || items.length == 0)
        {
            return
        }

        int idx = 0
        final List copy = new ArrayList(items.length)

        for (Object element : items)
        {
            if (element == JsonParser.EMPTY_OBJECT)
            {
                copy.add(new JsonObject())
                continue
            }

            copy.add(element)

            if (element instanceof Object[])
            {   // array element inside Collection
                JsonObject<String, Object> jsonObject = new JsonObject<>()
                jsonObject['@items'] = element
                stack.addFirst(jsonObject)
            }
            else if (element instanceof JsonObject)
            {
                JsonObject<String, Object> jsonObject = (JsonObject<String, Object>) element
                Long ref = (Long) jsonObject['@ref']

                if (ref != null)
                {    // connect reference
                    JsonObject refObject = getReferencedObj(ref)
                    copy[idx] = refObject
                }
                else
                {
                    stack.addFirst(jsonObject)
                }
            }
            idx++
        }
        jsonObj.target = null  // don't waste space (used for typed return, not generic Map return)

        for (int i=0; i < items.length; i++)
        {
            items[i] = copy[i]
        }
    }

    protected void traverseArray(Deque<JsonObject<String, Object>> stack, JsonObject<String, Object> jsonObj)
    {
        traverseCollection(stack, jsonObj)
    }
}
