package com.joebowbeer.resourcedecoder;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.joebowbeer.resourcedecoder.ResourceUtils.isComplexEntry;
import com.joebowbeer.resourcedecoder.StringPool.Style;

/**
 * Decodes compiled resources or xml.
 * <p>
 * From <a href="http://android.git.kernel.org/?p=platform/frameworks/base.git">
 * platform/frameworks/base.git</a>. See:
 * <ul>
 * <li>include/utils/ResourceTypes.h</li>
 * <li>libs/utils/ResourceTypes.cpp</li>
 * <li>tools/aapt/XMLNode.cpp</li>
 * </ul>
 * <p>
 * Also see <a href="http://code.google.com/p/adqmisc/">
 * code.google.com/p/adqmisc (arse)</a>
 */
public class ResourceDecoder {

    /* Resource chunk type identifiers */

    public static final int TYPE_NONE = -1;

    public static final int TYPE_NULL = 0x0000;
    public static final int TYPE_STRING_POOL = 0x0001;
    public static final int TYPE_TABLE = 0x0002;
    public static final int TYPE_XML = 0x0003;

    /* Chunk types inside XML */

    public static final int TYPE_XML_START_NAMESPACE = 0x0100;
    public static final int TYPE_XML_END_NAMESPACE = 0x0101;
    public static final int TYPE_XML_START_ELEMENT = 0x0102;
    public static final int TYPE_XML_END_ELEMENT = 0x0103;
    public static final int TYPE_XML_CDATA = 0x0104;

    /*
     * This contains an array mapping strings in the string pool
     * back to resource identifiers. It is optional.
     */
    public static final int TYPE_XML_RESOURCE_MAP = 0x0180;

    /* Chunk types inside TABLE */

    public static final int TYPE_TABLE_PACKAGE = 0x0200;
    public static final int TYPE_TABLE_TYPE = 0x0201;
    public static final int TYPE_TABLE_TYPE_SPEC = 0x0202;

    //private static final int XML_FIRST_CHUNK_TYPE = 0x0100;
    //private static final int XML_LAST_CHUNK_TYPE = 0x017f;

    /** Chunk header length. */
    private static final int HSIZE = 8;

    private final ContentHandler contentHandler;

    private boolean tableTypeSpecStarted;

    public ResourceDecoder(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    protected ContentHandler getHandler() {
        return contentHandler;
    }

    public int decode(ResourceInputStream in) throws IOException {
        int type;
        try {
            type = in.readUnsignedShort();
        } catch (EOFException ex) {
            return TYPE_NONE;
        }
        int headerSize = in.readUnsignedShort();
        int totalSize = in.readInt();
        getHandler().onChunkStart(in.getResourceOffset() - HSIZE,
                type, headerSize, totalSize);
        in = new ResourceInputStream(in, totalSize - HSIZE);
        switch (type) {
            case TYPE_NULL:
                decodeNull(headerSize, totalSize, in);
                break;
            case TYPE_STRING_POOL:
                decodeStringPool(headerSize, totalSize, in);
                break;
            case TYPE_TABLE:
                decodeTable(headerSize, totalSize, in);
                break;
            case TYPE_XML:
                decodeXml(headerSize, totalSize, in);
                break;
            case TYPE_XML_RESOURCE_MAP:
                decodeXmlResourceMap(headerSize, totalSize, in);
                break;
            case TYPE_XML_CDATA:
                decodeXmlCData(headerSize, totalSize, in);
                break;
            case TYPE_XML_END_ELEMENT:
                decodeXmlEndElement(headerSize, totalSize, in);
                break;
            case TYPE_XML_END_NAMESPACE:
                decodeXmlEndNamespace(headerSize, totalSize, in);
                break;
            case TYPE_XML_START_ELEMENT:
                decodeXmlStartElement(headerSize, totalSize, in);
                break;
            case TYPE_XML_START_NAMESPACE:
                decodeXmlStartNamespace(headerSize, totalSize, in);
                break;
            case TYPE_TABLE_PACKAGE:
                decodeTablePackage(headerSize, totalSize, in);
                break;
            case TYPE_TABLE_TYPE:
                decodeTableType(headerSize, totalSize, in);
                break;
            case TYPE_TABLE_TYPE_SPEC:
                decodeTableTypeSpec(headerSize, totalSize, in);
                break;
            default:
                decodeUnknownType(type, headerSize, totalSize, in);
        }
        return type;
    }

    protected void decodeUnknownType(int type, int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        Log.i(in.getResourceOffset() - HSIZE + ": " + typeToString(type));
        in.skipFully(totalSize - HSIZE);
    }

    protected void decodeNull(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
    }

    protected void decodeStringPool(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        int stringCount = in.readInt();
        int styleCount = in.readInt();
        int flags = in.readInt();
        boolean utf8 = (flags & StringPool.UTF8_FLAG) != 0;
        int stringsStart = in.readInt();
        int stylesStart = in.readInt();
        in.skipFully(headerSize - (HSIZE + 20));
        int[] stringOffset = new int[stringCount];
        for (int i = 0; i < stringCount; i++) {
            stringOffset[i] = in.readInt();
        }
        int dataRead = stringCount * 4;
        int[] styleOffset = new int[styleCount];
        for (int i = 0; i < styleCount; i++) {
            styleOffset[i] = in.readInt();
        }
        dataRead += styleCount * 4;
        List<String> strings = new ArrayList<String>(stringCount);
        for (int i = 0; i < stringCount; i++) {
            if (stringOffset[i] != headerSize + dataRead - stringsStart) {
                // Have we seen this offset before?
                int j = i;
                while (--j >= 0 && stringOffset[j] != stringOffset[i]) { }
                if (j < 0) {
                    throw new IllegalStateException();
                }
                strings.add(strings.get(j));
            } else {
                dataRead += (utf8 ? decodeStringUtf8(in, strings) : decodeString(in, strings));
            }
        }
        assert (styleCount == 0) == (stylesStart == 0);
        List<Style> styles = new ArrayList<Style>(styleCount);
        if (styleCount != 0) {
            // skip to start
            int npad = stylesStart - headerSize - dataRead;
            if (npad != 0) {
                in.skipFully(npad);
                dataRead += npad;
            }
            for (int i = 0; i < styleCount; i++) {
                dataRead += decodeStyle(in, styles);
            }
        }
        in.skipFully(totalSize - headerSize - dataRead);
        getHandler().onStringPool(new StringPool(strings, styles));
    }

    protected int decodeStringUtf8(ResourceInputStream in, List<String> list)
            throws IOException {
        int nchars = in.readUnsignedByte();
        int dataRead = 1;
        if ((nchars & 0x80) != 0) { // TODO: only 2 bytes?
            nchars = ((nchars & 0x7F) << 8) | in.readUnsignedByte();
            dataRead++;
        }
        int nbytes = in.readUnsignedByte();
        dataRead++;
        if ((nbytes & 0x80) != 0) { // TODO: only 2 bytes?
            nbytes = ((nbytes & 0x7F) << 8) | in.readUnsignedByte();
            dataRead++;
        }
        byte[] data = new byte[nbytes];
        in.readFully(data);
        dataRead += data.length;
        int z1 = in.readUnsignedByte();
        dataRead++;
        assert z1 == 0; // check null termination
        String s = new String(data, "UTF-8");
        assert nchars == s.length();
        list.add(s);
        return dataRead;
    }

    protected int decodeString(ResourceInputStream in, List<String> list)
            throws IOException {
        int nchars = in.readUnsignedShort();
        int dataRead = 2;
        if ((nchars & 0x8000) != 0) {
            nchars = ((nchars & 0x7FFF) << 16) | in.readUnsignedShort();
            dataRead += 2;
        }
        byte[] data = new byte[nchars * 2];
        in.readFully(data);
        dataRead += data.length;
        int z1 = in.readUnsignedByte();
        int z2 = in.readUnsignedByte();
        assert (z1 | z2) == 0; // check null termination
        dataRead += 2;
        list.add(new String(data, "UTF-16LE"));
        return dataRead;
    }

    protected int decodeStyle(ResourceInputStream in, List<Style> list)
            throws IOException {
        int name = in.readInt();
        int dataRead = 4;
        int firstChar = 0;
        int lastChar = 0;
        if (name != Style.END) {
            firstChar = in.readInt();
            lastChar = in.readInt();
            dataRead += 8;
        }
        list.add(new Style(name, firstChar, lastChar));
        return dataRead;
    }

    protected void decodeTable(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        int packageCount = in.readInt();
        in.skipFully(headerSize - (HSIZE + 4));
        getHandler().onTableStart(packageCount);
        while (decode(in) != TYPE_NONE) { }
        getHandler().onTableEnd();
    }

    protected void decodeTablePackage(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        int id = in.readInt();
        byte[] data = new byte[256];
        in.readFully(data);
        String name = new String(data, "UTF-16LE").replace("\0", ""); // 0-terminated
        int typeStrings = in.readInt();
        int lastPublicType = in.readInt();
        int keyStrings = in.readInt();
        int lastPublicKey = in.readInt();
        in.skipFully(headerSize - (HSIZE + 276));
        assert typeStrings == 0 || keyStrings == 0 || typeStrings < keyStrings;
        getHandler().onTablePackageStart(id, name, typeStrings,
                lastPublicType, keyStrings, lastPublicKey);
        while (decode(in) != TYPE_NONE) { }
        if (tableTypeSpecStarted) {
            getHandler().onTableTypeSpecEnd();
            tableTypeSpecStarted = false;
        }
        getHandler().onTablePackageEnd();
    }

    protected void decodeTableTypeSpec(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        if (tableTypeSpecStarted) {
            getHandler().onTableTypeSpecEnd();
            tableTypeSpecStarted = false;
        }
        int id = in.readUnsignedByte();
        assert id > 0;
        int res0 = in.readUnsignedByte();
        int res1 = in.readUnsignedShort();
        assert (res0 | res1) == 0;
        int entryCount = in.readInt();
        in.skipFully(headerSize - (HSIZE + 8));
        int[] configs = new int[entryCount];
        for (int i = 0; i < entryCount; i++) {
            configs[i] = in.readInt();
        }
        in.skipFully(totalSize - headerSize - entryCount * 4);
        getHandler().onTableTypeSpecStart(id, configs);
        tableTypeSpecStarted = true;
    }

    private static final int NO_ENTRY = 0xFFFFFFFF;

    protected void decodeTableType(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        int id = in.readUnsignedByte();
        assert id > 0;
        int res0 = in.readUnsignedByte();
        int res1 = in.readUnsignedShort();
        assert (res0 | res1) == 0;
        int entryCount = in.readInt();
        int entryStart = in.readInt();

        // configuration
        int configSize = in.readInt();
        int mcc = in.readUnsignedShort();
        int mnc = in.readUnsignedShort();
        byte[] data = new byte[2];
        in.readFully(data);
        String language = new String(data, "US-ASCII").replace("\0", "");
        in.readFully(data);
        String country = new String(data, "US-ASCII").replace("\0", "");
        int orientation = in.readUnsignedByte();
        int touchscreen = in.readUnsignedByte();
        int density = in.readUnsignedShort();
        int keyboard = in.readUnsignedByte();
        int navigation = in.readUnsignedByte();
        int inputFlags = in.readUnsignedByte();
        int inputPad0 = in.readUnsignedByte();
        int screenWidth = in.readUnsignedShort();
        int screenHeight = in.readUnsignedShort();
        int sdkVersion = in.readUnsignedShort();
        int minorVersion = in.readUnsignedShort();
        int screenLayout = in.readUnsignedByte();
        int uiMode = in.readUnsignedByte();
        int screenConfigPad1 = in.readUnsignedByte();
        int screenConfigPad2 = in.readUnsignedByte();
        ResourceConfig config = new ResourceConfig(mcc, mnc, language, country,
                orientation, touchscreen, density,
                keyboard, navigation, inputFlags, screenWidth, screenHeight,
                sdkVersion, minorVersion, screenLayout, uiMode);

        in.skipFully(headerSize - (HSIZE + 12 + 32));

        int[] offsets = new int[entryCount];
        for (int i = 0; i < entryCount; i++) {
            offsets[i] = in.readInt();
        }
        int dataRead = entryCount * 4;
        getHandler().onTableTypeStart(id, config, entryCount, entryStart, offsets);
        for (int i = 0; i < entryCount; i++) {
            if (offsets[i] != NO_ENTRY) {
                dataRead += decodeTableEntry(in, i);
            }
        }
        in.skipFully(totalSize - headerSize - dataRead);
        getHandler().onTableTypeEnd();
    }

    protected int decodeTableEntry(ResourceInputStream in, int index)
            throws IOException {
        int size = in.readUnsignedShort();
        int flags = in.readUnsignedShort();
        int key = in.readInt();
        int dataRead = 8;
        if (!isComplexEntry(flags)) {
            getHandler().onTableEntryStart(index, flags, key, 0, 0);
            dataRead += decodeResourceValue(in);
        } else {
            int parent = in.readInt();
            int count = in.readInt();
            dataRead += 8;
            getHandler().onTableEntryStart(index, flags, key, parent, count);
            for (int i = 0; i < count; i++) {
                int name = in.readInt();
                dataRead += 4;
                getHandler().onTableEntryMapName(name);
                dataRead += decodeResourceValue(in);
            }
        }
        getHandler().onTableEntryEnd();
        return dataRead;
    }

    protected int decodeResourceValue(ResourceInputStream in)
            throws IOException {
        long offset = in.getResourceOffset();
        int size = in.readUnsignedShort();
        assert size == 8;
        int res0 = in.readUnsignedByte();
        assert res0 == 0; // res0 is always zero
        int type = in.readUnsignedByte();
        byte[] data = new byte[4];
        in.readFully(data);
        in.skipFully(size - 8);
        getHandler().onResourceValue(offset, new ResourceValue(type, data));
        return size;
    }

    protected void decodeXml(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        in.skipFully(headerSize - HSIZE);
        getHandler().onXmlStart();
        while (decode(in) != TYPE_NONE) { }
        getHandler().onXmlEnd();
    }

    protected void decodeXmlResourceMap(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        in.skipFully(headerSize - HSIZE);
        int entryCount = (totalSize - headerSize) / 4;
        Map<Integer, Integer> map = new HashMap<Integer, Integer>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            map.put(in.readInt(), i);
        }
        in.skipFully(totalSize - headerSize - entryCount * 4);
        getHandler().onXmlResourceMap(map);
    }

    protected void decodeXmlNodeHeader(int headerSize, ResourceInputStream in)
            throws IOException {
        int lineNumber = in.readInt();
        int comment = in.readInt();
        in.skipFully(headerSize - (HSIZE + 8));
        getHandler().onXmlNode(lineNumber, comment);
    }

    protected void decodeXmlCData(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        decodeXmlNodeHeader(headerSize, in);
        int cdataIndex = in.readInt();
        int dataRead = 4;
        getHandler().onXmlCData(cdataIndex);
        dataRead += decodeResourceValue(in);
        in.skipFully(totalSize - headerSize - dataRead);
    }

    protected void decodeXmlStartElement(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        decodeXmlNodeHeader(headerSize, in);
        int nsIndex = in.readInt();
        int nameIndex = in.readInt();
        int attrIndex = in.readUnsignedShort();
        int attrSize = in.readUnsignedShort();
        int attrCount = in.readUnsignedShort();
        int idIndex = in.readUnsignedShort();
        int classIndex = in.readUnsignedShort();
        int styleIndex = in.readUnsignedShort();
        int dataRead = 20;
        getHandler().onXmlStartElement(nsIndex, nameIndex, attrIndex,
                attrSize, attrCount, idIndex, classIndex, styleIndex);
        for (int i = 0; i < attrCount; i++) {
            dataRead += decodeXmlAttribute(in);
        }
        in.skipFully(totalSize - headerSize - dataRead);
    }

    protected int decodeXmlAttribute(ResourceInputStream in) throws IOException {
        int nsIndex = in.readInt();
        int nameIndex = in.readInt();
        int rawIndex = in.readInt();
        int dataRead = 12;
        getHandler().onXmlAttribute(nsIndex, nameIndex, rawIndex);
        dataRead += decodeResourceValue(in);
        return dataRead;
    }

    protected void decodeXmlEndElement(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        decodeXmlNodeHeader(headerSize, in);
        int nsIndex = in.readInt();
        int nameIndex = in.readInt();
        int dataRead = 8;
        in.skipFully(totalSize - headerSize - dataRead);
        getHandler().onXmlEndElement(nsIndex, nameIndex);
    }

    protected void decodeXmlStartNamespace(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        decodeXmlNodeHeader(headerSize, in);
        int prefixIndex = in.readInt();
        int uriIndex = in.readInt();
        int dataRead = 8;
        in.skipFully(totalSize - headerSize - dataRead);
        getHandler().onXmlStartNamespace(prefixIndex, uriIndex);
    }

    protected void decodeXmlEndNamespace(int headerSize, int totalSize,
            ResourceInputStream in) throws IOException {
        decodeXmlNodeHeader(headerSize, in);
        int prefixIndex = in.readInt();
        int uriIndex = in.readInt();
        int dataRead = 8;
        in.skipFully(totalSize - headerSize - dataRead);
        getHandler().onXmlEndNamespace(prefixIndex, uriIndex);
    }

    protected static String typeToString(int type) {
        switch (type) {
            case TYPE_NULL:
                return "NULL";
            case TYPE_STRING_POOL:
                return "STRING_POOL";
            case TYPE_TABLE:
                return "TABLE";
            case TYPE_XML:
                return "XML";
            case TYPE_XML_RESOURCE_MAP:
                return "XML_RESOURCE_MAP";
            case TYPE_XML_CDATA:
                return "XML_CDATA";
            case TYPE_XML_END_ELEMENT:
                return "XML_END_ELEMENT";
            case TYPE_XML_END_NAMESPACE:
                return "XML_END_NAMESPACE";
            case TYPE_XML_START_ELEMENT:
                return "XML_START_ELEMENT";
            case TYPE_XML_START_NAMESPACE:
                return "XML_START_NAMESPACE";
            case TYPE_TABLE_PACKAGE:
                return "TABLE_PACKAGE";
            case TYPE_TABLE_TYPE:
                return "TABLE_TYPE";
            case TYPE_TABLE_TYPE_SPEC:
                return "TABLE_TYPE_SPEC";
            default:
                return String.format("UNKNOWN(%#x)", type);
        }
    }
}
