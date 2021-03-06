package com.cedarsoftware.util.io

import groovy.transform.CompileStatic

import java.lang.reflect.Array

/**
 * This class holds a JSON object in a LinkedHashMap.
 * LinkedHashMap used to keep fields in same order as they are
 * when reflecting them in Groovy.  Instances of this class hold a
 * Map-of-Map representation of a Groovy object, read from the JSON
 * input stream.
 *
 * @param <K> field name in Map-of-Map
 * @param <V> Value
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
 *         limitations under the License.*
 */
@CompileStatic
class JsonObject<K, V> extends LinkedHashMap<K, V>
{
    private boolean isMap = false
    private Object target
    private long id = -1
    private String type
    private int line
    private int column

    String getType()
    {
        return type
    }

    void setType(String type)
    {
        this.type = type
    }

    long getId()
    {
        return id
    }

    void setId(long id)
    {
        this.id = id
    }

    boolean hasId()
    {
        return id != -1
    }

    Object getTarget()
    {
        return target
    }

    Object setTarget(Object target)
    {
        this.target = target
    }

    Class getTargetClass()
    {
        return target.getClass()
    }

    void setColumn(int col)
    {
        column = col
    }

    int getColumn()
    {
        return column
    }

    void setLine(int line)
    {
        this.line = line
    }

    int getLine()
    {
        return line
    }

    boolean isPrimitive()
    {
        if (type == null)
        {
            return false
        }
        switch (type)
        {
            case "boolean":
            case "byte":
            case "char":
            case "double":
            case "float":
            case "int":
            case "long":
            case "short":
                return true
            default:
                return false
        }
    }

    static boolean isPrimitiveWrapper(Class c)
    {
        final String cname = c.getName()
        switch (cname)
        {
            case "java.lang.Boolean":
            case "java.lang.Byte":
            case "java.lang.Character":
            case "java.lang.Double":
            case "java.lang.Float":
            case "java.lang.Integer":
            case "java.lang.Long":
            case "java.lang.Short":
                return true
            default:
                return false
        }
    }

    Object getPrimitiveValue()
    {
        switch(type)
        {
            case "byte":
                Number b = (Number) get("value")
                return b.byteValue()
            case "char":
                String c = (String) get("value")
                return c.charAt(0)
            case "boolean":
            case "double":
            case "long":
                return get("value")
            case "float":
                Number f = (Number) get("value")
                return f.floatValue()
            case "int":
                Number integer = (Number) get("value")
                return integer.intValue()
            case "short":
                Number s = (Number) get("value")
                return s.shortValue()

            default:
                return GroovyJsonReader.error("Invalid primitive type")
        }
    }

    // Map APIs
    boolean isMap()
    {
        return isMap || target instanceof Map
    }

    // Collection APIs
    boolean isCollection()
    {
        if (containsKey("@items") && !containsKey("@keys"))
        {
            return ((target instanceof Collection) || (type != null && !type.contains("[")))
        }

        return target instanceof Collection
    }

    // Array APIs
    boolean isArray()
    {
        if (target == null)
        {
            if (type != null)
            {
                return type.contains("[")
            }
            return containsKey("@items") && !containsKey("@keys")
        }
        return target.getClass().isArray()
    }

    Object[] getArray()
    {
        return (Object[]) get("@items")
    }

    int getLength()
    {
        if (isArray())
        {
            if (target == null)
            {
                Object[] items = (Object[]) get("@items")
                return items == null ? 0 : items.length
            }
            return Array.getLength(target)
        }
        if (isCollection() || isMap())
        {
            Object[] items = (Object[]) get("@items")
            return items == null ? 0 : items.length
        }
        throw new JsonIoException("getLength() called on a non-collection, line " + line + ", col " + column)
    }

    Class getComponentType()
    {
        return target.getClass().getComponentType()
    }

    void moveBytesToMate()
    {
        final byte[] bytes = (byte[]) target
        final Object[] items = getArray()
        final int len = items.length

        for (int i = 0; i < len; i++)
        {
            bytes[i] = ((Number) items[i]).byteValue()
        }
    }

    void moveCharsToMate()
    {
        Object[] items = getArray()
        if (items == null)
        {
             target = null
        }
        else if (items.length == 0)
        {
            target = new char[0]
        }
        else if (items.length == 1)
        {
            String s = (String) items[0]
            target = s.toCharArray()
        }
        else
        {
            throw new JsonIoException("char[] should only have one String in the [], found " + items.length + ", line " + line + ", col " + column)
        }
    }

    V put(K key, V value)
    {
        if (key == null)
        {
            return super.put(null, value)
        }

        if (key.equals("@type"))
        {
            String oldType = type
            type = (String) value
            return (V) oldType
        }
        else if (key.equals("@id"))
        {
            Long oldId = id
            id = (Long) value
            return (V) oldId
        }
        else if (("@items".equals(key) && containsKey("@keys")) || ("@keys".equals(key) && containsKey("@items")))
        {
            isMap = true
        }
        return super.put(key, value)
    }

    void clear()
    {
        super.clear()
        type = null
    }

    void clearArray()
    {
        remove("@items")
    }

    int size()
    {
        if (containsKey("@items"))
        {
            V value = get("@items")
            if (value instanceof Object[])
            {
                return (value as Object[]).length
            }
            else if (value == null)
            {
                return 0
            }
            else
            {
                throw new JsonIoException("JsonObject with @items, but no array [] associated to it, line " + line + ", column " + column)
            }
        }
        else if (containsKey("@ref"))
        {
            return 0
        }

        return super.size()
    }
}
