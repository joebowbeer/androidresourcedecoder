package com.joebowbeer.resourcedecoder;

import nu.xom.Document;
import nu.xom.Elements;

public class XmlContentToDocument extends XmlContentHandler implements DocumentBuilder {

    private Document document;

    public XmlContentToDocument() {
    }

    public XmlContentToDocument(ContentHandler parent) {
        super(parent);
    }

    /* DocumentBuilder */

    @Override
    public Document toDocument() {
        return document;
    }

    /* XmlContentHandler overrides */

    @Override
    public void onXmlStart() {
        document = null;
        super.onXmlStart();
    }

    @Override
    public void onXmlEnd() {
        Elements elements = curNode.getChildElements();
        for (int i = 0, n = elements.size(); i < n; i++) {
            elements.get(i).detach();
        }
        document = (elements.size() == 1) ? new Document(elements.get(0)) : null;
        super.onXmlEnd();
    }
}
