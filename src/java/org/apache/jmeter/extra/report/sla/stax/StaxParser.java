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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: sgoeschl
 * Date: 01.04.11
 * Time: 15:13
 * To change this template use File | Settings | File Templates.
 */
public class StaxParser {

    private Stack<Object> elementStack;
    private Map delegates;

    public StaxParser() {
        delegates = new HashMap();
        elementStack = new Stack<Object>();
    }

    public void parseElement(XMLStreamReader staxXmlReader) throws XMLStreamException {

        Object element;
        String localName;

        for (int event = staxXmlReader.next(); event != XMLStreamConstants.END_DOCUMENT; event = staxXmlReader.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                localName = staxXmlReader.getLocalName();
                // If a Component Parser is registered that can handle
                // this localName delegate to parser...
                if (delegates.containsKey(localName)) {
                    ComponentParser parser = (ComponentParser) delegates.get(localName);
                    element = parser.startElement(staxXmlReader, elementStack);
                    elementStack.push(element);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                localName = staxXmlReader.getLocalName();
                // If a Component Parser is registered that can handle
                // this localName delegate to parser...
                if (delegates.containsKey(localName)) {
                    ComponentParser parser = (ComponentParser) delegates.get(localName);
                    parser.endElement(staxXmlReader, elementStack);
                    elementStack.pop();
                }
            }

        } //rof
    }

    public void registerParser(String name, ComponentParser parser) {
        delegates.put(name, parser);
    }

}