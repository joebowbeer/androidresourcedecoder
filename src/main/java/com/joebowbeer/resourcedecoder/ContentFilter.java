package com.joebowbeer.resourcedecoder;

import java.util.Map;

public class ContentFilter implements ContentHandler {

    private volatile ContentHandler parent;

    public ContentFilter() {
    }

    public ContentFilter(ContentHandler parent) {
        this.parent = parent;
    }

    public ContentHandler getParent() {
        return parent;
    }

    public void setParent(ContentHandler parent) {
        this.parent = parent;
    }

    @Override
    public void onChunkStart(long offset, int type, int headerSize, int totalSize) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onChunkStart(offset, type, headerSize, totalSize);
        }
    }

    @Override
    public void onResourceValue(long offset, ResourceValue value) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onResourceValue(offset, value);
        }
    }

    @Override
    public void onStringPool(StringPool stringPool) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onStringPool(stringPool);
        }
    }

    @Override
    public void onTableStart(int packageCount) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTableStart(packageCount);
        }
    }

    @Override
    public void onTableEnd() {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTableEnd();
        }
    }

    @Override
    public void onTablePackageStart(int id, String name, int typeStrings,
            int lastPublicType, int keyStrings, int lastPublicKey) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTablePackageStart(id, name, typeStrings, lastPublicType,
                    keyStrings, lastPublicKey);
        }
    }

    @Override
    public void onTablePackageEnd() {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTablePackageEnd();
        }
    }

    @Override
    public void onTableTypeSpecStart(int id, int[] configs) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTableTypeSpecStart(id, configs);
        }
    }

    @Override
    public void onTableTypeSpecEnd() {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTableTypeSpecEnd();
        }
    }

    @Override
    public void onTableTypeStart(int id, ResourceConfig config, int entryCount,
            int entryStart, int[] offsets) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTableTypeStart(id, config, entryCount, entryStart, offsets);
        }
    }

    @Override
    public void onTableTypeEnd() {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTableTypeEnd();
        }
    }

    @Override
    public void onTableEntryStart(int id, int flags, int key, int parent,
            int count) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTableEntryStart(id, flags, key, parent, count);
        }
    }

    @Override
    public void onTableEntryMapName(int name) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTableEntryMapName(name);
        }
    }

    @Override
    public void onTableEntryEnd() {
        ContentHandler next = getParent();
        if (next != null) {
            next.onTableEntryEnd();
        }
    }

    @Override
    public void onXmlStart() {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlStart();
        }
    }

    @Override
    public void onXmlEnd() {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlEnd();
        }
    }

    @Override
    public void onXmlResourceMap(Map<Integer, Integer> map) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlResourceMap(map);
        }
    }

    @Override
    public void onXmlNode(int lineNumber, int comment) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlNode(lineNumber, comment);
        }
    }

    @Override
    public void onXmlStartNamespace(int prefixIndex, int uriIndex) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlStartNamespace(prefixIndex, uriIndex);
        }
    }

    @Override
    public void onXmlEndNamespace(int prefixIndex, int uriIndex) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlEndNamespace(prefixIndex, uriIndex);
        }
    }

    @Override
    public void onXmlStartElement(int nsIndex, int nameIndex, int attrIndex,
            int attrSize, int attrCount, int idIndex, int classIndex, int styleIndex) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlStartElement(nsIndex, nameIndex, attrIndex, attrSize,
                    attrCount, idIndex, classIndex, styleIndex);
        }
    }

    @Override
    public void onXmlEndElement(int nsIndex, int nameIndex) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlEndElement(nsIndex, nameIndex);
        }
    }

    @Override
    public void onXmlAttribute(int nsIndex, int nameIndex, int rawIndex) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlAttribute(nsIndex, nameIndex, rawIndex);
        }
    }

    @Override
    public void onXmlCData(int cdataIndex) {
        ContentHandler next = getParent();
        if (next != null) {
            next.onXmlCData(cdataIndex);
        }
    }
}
