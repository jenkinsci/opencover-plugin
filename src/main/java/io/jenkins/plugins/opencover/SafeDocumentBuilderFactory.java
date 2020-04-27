package io.jenkins.plugins.opencover;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class SafeDocumentBuilderFactory {
    public static DocumentBuilderFactory newInstance() throws ParserConfigurationException {

        DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();

        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        String FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
        factory.setFeature(FEATURE, true);

        FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
        factory.setFeature(FEATURE, false);

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setNamespaceAware(true);

        return factory;
    }
}
