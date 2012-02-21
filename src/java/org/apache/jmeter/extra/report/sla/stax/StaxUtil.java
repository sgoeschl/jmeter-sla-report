/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.jmeter.extra.report.sla.stax;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class StaxUtil {

    public static void moveReaderToElement(String target, XMLStreamReader reader) throws XMLStreamException {

        // If current element is equal to target

        for (int event = reader.next(); event != XMLStreamConstants.END_DOCUMENT; event = reader.next()) {
            if ((event == XMLStreamConstants.START_ELEMENT) && (reader.getLocalName().equals(target))) {
                return;
            }
        }
    }

    public static void writeElement(XMLStreamWriter writer, String elementName, String value) throws XMLStreamException {
        writer.writeStartElement(elementName);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }

}
