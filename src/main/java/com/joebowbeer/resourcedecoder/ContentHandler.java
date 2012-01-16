package com.joebowbeer.resourcedecoder;

import java.util.Map;

public interface ContentHandler {

    void onChunkStart(long offset, int type, int headerSize, int totalSize);

    void onResourceValue(long offset, ResourceValue value);

    void onStringPool(StringPool stringPool);

    void onTableStart(int packageCount);

    void onTablePackageStart(int id, String name, int typeStrings,
            int lastPublicType, int keyStrings, int lastPublicKey);

    void onTableTypeSpecStart(int id, int[] configs);

    void onTableTypeStart(int id, ResourceConfig config, int entryCount,
            int entryStart, int[] offsets);

    void onTableEntryStart(int id, int flags, int key, int parent, int count);

    void onTableEntryMapName(int name);

    void onTableEntryEnd();

    void onTableTypeEnd();

    void onTableTypeSpecEnd();

    void onTablePackageEnd();

    void onTableEnd();

    void onXmlStart();

    void onXmlResourceMap(Map<Integer, Integer> map);

    void onXmlNode(int lineNumber, int comment);

    void onXmlStartNamespace(int prefixIndex, int uriIndex);

    void onXmlStartElement(int nsIndex, int nameIndex,
            int attrIndex, int attrSize, int attrCount,
            int idIndex, int classIndex, int styleIndex);

    void onXmlAttribute(int nsIndex, int nameIndex, int rawIndex);

    void onXmlCData(int cdataIndex);

    void onXmlEndElement(int nsIndex, int nameIndex);

    void onXmlEndNamespace(int prefixIndex, int uriIndex);

    void onXmlEnd();
}
