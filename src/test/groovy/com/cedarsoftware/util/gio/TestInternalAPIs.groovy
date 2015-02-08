package com.cedarsoftware.util.gio

import org.junit.Assert
import org.junit.Test

import static org.junit.Assert.assertNotNull

/**
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
class TestInternalAPIs
{
    static class DerivedWriter extends JsonWriter
    {
        public DerivedWriter(OutputStream out) throws IOException
        {
            super(out)
        }
    }

    @Test
    void testDistanceToInterface() throws Exception
    {
        Assert.assertEquals(1, JsonWriter.getDistanceToInterface(Serializable.class, LinkedList.class))
        Assert.assertEquals(3, JsonWriter.getDistanceToInterface(Iterable.class, LinkedList.class))
        Assert.assertEquals(2, JsonWriter.getDistanceToInterface(Serializable.class, BigInteger.class))
    }

    @Test
    void testCleanString()
    {
        String s = JsonReader.removeLeadingAndTrailingQuotes('"Foo"')
        assert "Foo" == s
        s = JsonReader.removeLeadingAndTrailingQuotes("Foo")
        assert "Foo" == s
        s = JsonReader.removeLeadingAndTrailingQuotes('"Foo')
        assert "Foo" == s
        s = JsonReader.removeLeadingAndTrailingQuotes('Foo"')
        assert "Foo" == s
        s = JsonReader.removeLeadingAndTrailingQuotes('""Foo""')
        assert "Foo" == s
    }

    @Test
    void testProtectedAPIs() throws Exception
    {
        ByteArrayOutputStream bao = new ByteArrayOutputStream()
        DerivedWriter writer = new DerivedWriter(bao)
        Map ref = writer.objectsReferenced
        Map vis = writer.objectsVisited
        Map args = DerivedWriter.args
        assertNotNull(ref)
        assertNotNull(vis)
        assertNotNull(args)
    }

    @Test
    void testNoAnalysisForCustomWriter() throws Exception
    {
        JsonWriter.addWriter(Dog.class, new JsonWriter.JsonClassWriter() {
            public void writePrimitiveForm(Object o, Writer out)  throws IOException
            { }

            public void write(Object o, boolean showType, Writer out)  throws IOException
            { }

            public boolean hasPrimitiveForm()
            {
                false
            }
        })
    }
}
