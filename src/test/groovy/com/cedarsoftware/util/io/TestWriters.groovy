package com.cedarsoftware.util.io

import org.junit.Test

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License")
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class TestWriters
{
    @Test
    void testUnusedAPIs()
    {
        Writers.TimeZoneWriter tzw = new Writers.TimeZoneWriter()
        tzw.writePrimitiveForm("", new StringWriter())

        Writers.CalendarWriter cw = new Writers.CalendarWriter()
        cw.writePrimitiveForm("", new StringWriter())

        Writers.TimestampWriter tsw = new Writers.TimestampWriter()
        tsw.writePrimitiveForm("", new StringWriter())

        Writers.LocaleWriter lw = new Writers.LocaleWriter()
        lw.writePrimitiveForm("", new StringWriter())

        Writers.JsonStringWriter jsw = new Writers.JsonStringWriter()
        jsw.write("", false, new StringWriter())
    }
}
