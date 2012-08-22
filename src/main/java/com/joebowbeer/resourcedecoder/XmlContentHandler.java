package com.joebowbeer.resourcedecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Text;

/** Base class for handling XML content. */
public class XmlContentHandler extends ContentFilter {

    protected Element curNode;
    protected StringPool pool;
    protected final List<List<String>> namespaces = new ArrayList<>();
    protected String attrNamespace;
    protected String attrName;

    public XmlContentHandler() {
    }

    public XmlContentHandler(ContentHandler parent) {
        super(parent);
    }

    protected boolean insideXml() {
        return curNode != null;
    }

    /* ContentFilter overrides */

    @Override
    public void onXmlStart() {
        curNode = new Element("tempRoot");
        super.onXmlStart();
    }

    @Override
    public void onStringPool(StringPool stringPool) {
        // ignore unless we are inside xml content
        if (insideXml()) {
            pool = stringPool;
        }
        super.onStringPool(stringPool);
    }

    @Override
    public void onXmlResourceMap(Map<Integer, Integer> map) {
        // ignore
        super.onXmlResourceMap(map);
    }

    @Override
    public void onXmlNode(int lineNumber, int comment) {
        // ignore
        super.onXmlNode(lineNumber, comment);
    }

    @Override
    public void onXmlStartNamespace(int prefixIndex, int uriIndex) {
        List<String> pair = Arrays.asList(
                pool.getString(prefixIndex),
                pool.getString(uriIndex));
        namespaces.add(0, pair);
        super.onXmlStartNamespace(prefixIndex, uriIndex);
    }

    @Override
    public void onXmlStartElement(int nsIndex, int nameIndex, int attrIndex,
        int attrSize, int attrCount, int idIndex, int classIndex, int styleIndex) {
        String namespace = pool.getString(nsIndex);
        String name = pool.getString(nameIndex);
        Element child = (namespace != null)
                ? new Element(addNsPrefix(name, namespace), namespace)
                : new Element(name);
        curNode.appendChild(child);
        curNode = child;
        super.onXmlStartElement(nsIndex, nameIndex, attrIndex, attrSize,
                attrCount, idIndex, classIndex, styleIndex);
    }

    @Override
    public void onXmlAttribute(int nsIndex, int nameIndex, int rawIndex) {
        attrNamespace = pool.getString(nsIndex);
        attrName = pool.getString(nameIndex);
        super.onXmlAttribute(nsIndex, nameIndex, rawIndex);
    }

    @Override
    public void onResourceValue(long offset, ResourceValue value) {
        // ignore unless we are inside xml content
        if (!insideXml()) {
            super.onResourceValue(offset, value);
            return;
        }
        // TODO lazy lookup string-typed attributes here?
        String attrValue = value.format(pool);
        if (attrNamespace != null) {
            String name = addNsPrefix(attrName, attrNamespace);
            curNode.addAttribute(new Attribute(name, attrNamespace, attrValue));
        } else {
            curNode.addAttribute(new Attribute(attrName, attrValue));
        }
        attrNamespace = null;
        attrName = null;
        super.onResourceValue(offset, value);
    }

    @Override
    public void onXmlCData(int cdataIndex) {
        curNode.appendChild(new Text(pool.getString(cdataIndex)));
        super.onXmlCData(cdataIndex);
    }

    @Override
    public void onXmlEndElement(int nsIndex, int nameIndex) {
        curNode = (Element) curNode.getParent();
        super.onXmlEndElement(nsIndex, nameIndex);
    }

    @Override
    public void onXmlEndNamespace(int prefixIndex, int uriIndex) {
        List<String> pair = Arrays.asList(
                pool.getString(prefixIndex),
                pool.getString(uriIndex));
        namespaces.remove(namespaces.lastIndexOf(pair));
        super.onXmlEndNamespace(prefixIndex, uriIndex);
    }

    @Override
    public void onXmlEnd() {
        curNode = null;
        pool = null;
        namespaces.clear();
        super.onXmlEnd();
    }

    protected String addNsPrefix(String name, String namespace) {
        for (List<String> pair : namespaces) {
            if (namespace.equals(pair.get(1))) {
                return pair.get(0) + ":" + name;
            }
        }
        return name;
    }
}
