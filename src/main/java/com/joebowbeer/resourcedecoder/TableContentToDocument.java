package com.joebowbeer.resourcedecoder;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;

import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_FEW;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_L10N;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_L10N_NOT_REQUIRED;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_MANY;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_MAX;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_MIN;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_ONE;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_OTHER;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_TWO;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_TYPE;
import static com.joebowbeer.resourcedecoder.ResourceUtils.ATTR_ZERO;
import static com.joebowbeer.resourcedecoder.ResourceUtils.formatAllowedTypes;
import static com.joebowbeer.resourcedecoder.ResourceUtils.getEntry;
import static com.joebowbeer.resourcedecoder.ResourceUtils.isArrayId;
import static com.joebowbeer.resourcedecoder.ResourceUtils.isComplexEntry;
import static com.joebowbeer.resourcedecoder.ResourceUtils.isPublicEntry;

public class TableContentToDocument extends ContentFilter implements DocumentBuilder {

    private Document document;
    private Element curNode;
    private StringPool pool;
    private StringPool typePool;
    private StringPool keyPool;
    private boolean hasTypePool;
    private boolean hasKeyPool;
    private String restypeName;
    private boolean isComplexEntry;
    private int entryMapName;

    public TableContentToDocument() {
    }

    public TableContentToDocument(ContentHandler parent) {
        super(parent);
    }

    /* DocumentBuilder */

    @Override
    public Document toDocument() {
        return document;
    }

    /* ContentFilter overrides */

    @Override
    public void onTableStart(int packageCount) {
        document = null;
        curNode = new Element("packages");
        super.onTableStart(packageCount);
    }

    @Override
    public void onStringPool(StringPool stringPool) {
        // ignore unless we are inside table content
        if (curNode == null) {
            super.onStringPool(stringPool);
            return;
        }
        if (pool == null) {
            pool = stringPool;
        } else if (hasTypePool && typePool == null) {
            typePool = stringPool;
        } else if (hasKeyPool && keyPool == null) {
            keyPool = stringPool;
        } else {
            throw new IllegalStateException();
        }
        super.onStringPool(stringPool);
    }

    @Override
    public void onTablePackageStart(int id, String name, int typeStrings,
            int lastPublicType, int keyStrings, int lastPublicKey) {
        Element packageNode = new Element("package");
        packageNode.addAttribute(new Attribute("id", String.format("%#x", id)));
        packageNode.addAttribute(new Attribute("name", name));
        hasTypePool = typeStrings != 0;
        hasKeyPool = keyStrings != 0;
        curNode.appendChild(packageNode);
        curNode = packageNode;
        super.onTablePackageStart(id, name, typeStrings,
                lastPublicType, keyStrings, lastPublicKey);
    }

    @Override
    public void onTableTypeSpecStart(int id, int[] configs) {
        Element restypeNode = new Element("resourcetype");
        restypeNode.addAttribute(new Attribute("id", String.format("%#x", id)));
        restypeName = typePool.getString(curNode.getChildCount());
        restypeNode.addAttribute(new Attribute("name", restypeName));
        // ignore fast-lookup configs table
        curNode.appendChild(restypeNode);
        curNode = restypeNode;
        super.onTableTypeSpecStart(id, configs);
    }

    @Override
    public void onTableTypeStart(int id, ResourceConfig config, int entryCount,
            int entryStart, int[] offsets) {
        Element configNode = configElement(config);
        curNode.appendChild(configNode);
        curNode = configNode;
        super.onTableTypeStart(id, config, entryCount, entryStart, offsets);
    }

    @Override
    public void onTableEntryStart(int id, int flags, int key, int parent, int count) {
        String itemName = keyPool.getString(key);
        Element itemNode = new Element("item");
        itemNode.addAttribute(new Attribute("id", String.format("%#x", id)));
        itemNode.addAttribute(new Attribute("name", itemName));
        isComplexEntry = isComplexEntry(flags);
        if (isPublicEntry(flags)) {
            itemNode.addAttribute(new Attribute("ispublic", "true"));
        }
        if (parent != 0) {
            itemNode.addAttribute(new Attribute("parentref", formatName(parent)));
        }
        curNode.appendChild(itemNode);
        curNode = itemNode;
        super.onTableEntryStart(id, flags, key, parent, count);
    }

    @Override
    public void onTableEntryMapName(int name) {
        entryMapName = name;
        super.onTableEntryMapName(name);
    }

    @Override
    public void onResourceValue(long offset, ResourceValue value) {
        // ignore unless we are inside table content
        if (curNode == null) {
            super.onResourceValue(offset, value);
            return;
        }
        if (!isComplexEntry) {
            curNode.addAttribute(new Attribute("value", value.format(pool)));
        } else {
            switch (restypeName) {
                case "attr":
                    curNode.appendChild(attrValueElement(entryMapName, value));
                    break;
                case "array":
                    assert isArrayId(entryMapName);
                    Element elementNode = new Element("element");
                    elementNode.addAttribute(
                            new Attribute("index", String.valueOf(getEntry(entryMapName))));
                    elementNode.addAttribute(new Attribute("value", value.format(pool)));
                    curNode.appendChild(elementNode);
                    break;
                default:
                    Element valueNode = new Element("value");
                    valueNode.addAttribute(new Attribute("name", formatName(entryMapName)));
                    valueNode.addAttribute(new Attribute("value", value.format(pool))); // TODO?
                    curNode.appendChild(valueNode);
                    break;
            }
        }
        super.onResourceValue(offset, value);
    }

    @Override
    public void onTableEntryEnd() {
        entryMapName = 0;
        curNode = (Element) curNode.getParent();
        super.onTableEntryEnd();
    }

    @Override
    public void onTableTypeEnd() {
        curNode = (Element) curNode.getParent();
        super.onTableTypeEnd();
    }

    @Override
    public void onTableTypeSpecEnd() {
        restypeName = null;
        curNode = (Element) curNode.getParent();
        super.onTableTypeSpecEnd();
    }

    @Override
    public void onTablePackageEnd() {
        typePool = null;
        keyPool = null;
        curNode = (Element) curNode.getParent();
        super.onTablePackageEnd();
    }

    @Override
    public void onTableEnd() {
        document = new Document(curNode);
        curNode = null;
        pool = null;
        super.onTableEnd();
    }

    protected Element configElement(ResourceConfig config) {
        Element node = new Element("configuration");
        if (config.mcc != 0) {
            node.addAttribute(new Attribute("mcc", String.valueOf(config.mcc)));
        }
        if (config.mnc != 0) {
            node.addAttribute(new Attribute("mnc", String.valueOf(config.mnc)));
        }
        if (!config.language.isEmpty()) {
            node.addAttribute(new Attribute("language", config.language));
        }
        if (!config.country.isEmpty()) {
            node.addAttribute(new Attribute("country", config.country));
        }
        if (config.orientation != 0) {
            node.addAttribute(new Attribute("orientation", config.orientationQualifier()));
        }
        if (config.touchscreen != 0) {
            node.addAttribute(new Attribute("touchscreen",
                    String.valueOf(config.touchscreen)));
        }
        if (config.density != 0) {
            node.addAttribute(new Attribute("density",
                    String.valueOf(config.density)));
        }
        if (config.keyboard != 0) {
            node.addAttribute(new Attribute("keyboard",
                    String.valueOf(config.keyboard)));
        }
        if (config.navigation != 0) {
            node.addAttribute(new Attribute("navigation",
                    String.valueOf(config.navigation)));
        }
        if (config.inputFlags != 0) {
            node.addAttribute(new Attribute("inputflags",
                    String.format("%#x", config.inputFlags)));
        }
        if (config.screenWidth != 0) {
            node.addAttribute(new Attribute("screenwidth",
                    String.valueOf(config.screenWidth)));
        }
        if (config.screenHeight != 0) {
            node.addAttribute(new Attribute("screenheight",
                    String.valueOf(config.screenHeight)));
        }
        if (config.sdkVersion != 0) {
            node.addAttribute(new Attribute("sdkversion", String.valueOf(config.sdkVersion)));
        }
        if (config.screenLayout != 0) {
            node.addAttribute(new Attribute("screenLayout", String.valueOf(config.screenLayout)));
        }
        if (config.uiMode != 0) {
            node.addAttribute(new Attribute("uiMode", String.valueOf(config.uiMode)));
        }
        return node;
    }

    protected Element attrValueElement(int name, ResourceValue value) {
        Element node = new Element("value");
        switch (name) {
            case ATTR_TYPE:
                node.addAttribute(new Attribute("allowedtypes",
                        formatAllowedTypes(value.intValue())));
                break;
            case ATTR_MIN:
                node.addAttribute(new Attribute("minvalue", value.format(null)));
                break;
            case ATTR_MAX:
                node.addAttribute(new Attribute("maxvalue", value.format(null)));
                break;
            case ATTR_L10N:
                node.addAttribute(new Attribute("localisation",
                        (value.intValue() == ATTR_L10N_NOT_REQUIRED)
                            ? "notrequired" : "suggested"));
                break;
            case ATTR_OTHER:
                node.addAttribute(new Attribute("quantity", value.format(null)));
                break;
            case ATTR_ZERO:
                node.addAttribute(new Attribute("quantity", "zero"));
                break;
            case ATTR_ONE:
                node.addAttribute(new Attribute("quantity", "one"));
                break;
            case ATTR_TWO:
                node.addAttribute(new Attribute("quantity", "two"));
                break;
            case ATTR_FEW:
                node.addAttribute(new Attribute("quantity", "few"));
                break;
            case ATTR_MANY:
                node.addAttribute(new Attribute("quantity", "many"));
                break;
            default:
                node.addAttribute(new Attribute("name", formatName(name)));
                node.addAttribute(new Attribute("value", value.format(pool))); // TODO?
        }
        return node;
    }

    private String formatName(int name) {
        return String.format("@%#08x", name); // TODO: android.R.attr
    }
}
