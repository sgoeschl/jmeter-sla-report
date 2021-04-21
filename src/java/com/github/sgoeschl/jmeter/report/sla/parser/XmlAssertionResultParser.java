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
package com.github.sgoeschl.jmeter.report.sla.parser;

import com.github.sgoeschl.jmeter.report.sla.stax.StaxUtil;
import com.github.sgoeschl.jmeter.report.sla.element.AssertionResultElement;
import com.github.sgoeschl.jmeter.report.sla.element.SampleElement;
import com.github.sgoeschl.jmeter.report.sla.stax.ComponentParser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Stack;

/**
 * Parses an "assertionResult" element.
 */
public class XmlAssertionResultParser implements ComponentParser {

    @Override
    public Object startElement(XMLStreamReader streamReader, Stack<Object> elementStack) throws XMLStreamException {

        final AssertionResultElement assertionResultElement = new AssertionResultElement();

        StaxUtil.moveReaderToElement("name", streamReader);
        assertionResultElement.setName(streamReader.getElementText());

        StaxUtil.moveReaderToElement("failure", streamReader);
        assertionResultElement.setFailure(Boolean.parseBoolean(streamReader.getElementText()));

        StaxUtil.moveReaderToElement("error", streamReader);
        assertionResultElement.setError(Boolean.parseBoolean(streamReader.getElementText()));

        if (assertionResultElement.isFailure() || assertionResultElement.isError()) {
            StaxUtil.moveReaderToElement("failureMessage", streamReader);
            assertionResultElement.setFailureMessage(streamReader.getElementText());
            final SampleElement sampleElement = (SampleElement) elementStack.peek();
            sampleElement.getAssertionResultList().add(assertionResultElement);
        }

        return assertionResultElement;
    }

    @Override
    public void endElement(XMLStreamReader streamReader, Stack<Object> elementStack) {
    }
}
