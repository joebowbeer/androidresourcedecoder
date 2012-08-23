package com.joebowbeer.resourcedecoder;

import java.util.HashMap;
import java.util.Map;

import static com.joebowbeer.resourcedecoder.ResourceUtils.isComplexEntry;
import static com.joebowbeer.resourcedecoder.ResourceUtils.makeId;

/**
 * ContentFilter that maps resource ids to their names.
 */
public class TableResourceMapper extends ContentFilter {

    private final Map<Integer, String> references = new HashMap<>();

    private boolean tableStarted;
    private StringPool pool;
    private boolean hasTypePool;
    private boolean hasKeyPool;
    private StringPool typePool;
    private StringPool keyPool;
    private int packageId;
    private String packageName;
    private int typeSpecIndex;
    private String restypeName;
    private ResourceConfig itemConfig;
    private String itemName;
    private boolean isComplexEntry;

    public TableResourceMapper() {
    }

    public TableResourceMapper(ContentHandler parent) {
        super(parent);
    }

    public Map<Integer, String> getReferences() {
        return references;
    }

    /* ContentFilter overrides */

    @Override
    public void onTableStart(int packageCount) {
        tableStarted = true;
        super.onTableStart(packageCount);
    }

    @Override
    public void onStringPool(StringPool stringPool) {
        // ignore unless we are inside table content
        if (!tableStarted) {
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
        packageId = id;
        packageName = name;
        hasTypePool = typeStrings != 0;
        hasKeyPool = keyStrings != 0;
        super.onTablePackageStart(id, name, typeStrings, lastPublicType,
                keyStrings, lastPublicKey);
    }

    @Override
    public void onTableTypeSpecStart(int id, int[] configs) {
        restypeName = typePool.getString(typeSpecIndex++);
        super.onTableTypeSpecStart(id, configs);
    }

    @Override
    public void onTableTypeStart(int id, ResourceConfig config, int entryCount,
            int entryStart, int[] offsets) {
        itemConfig = config;
        super.onTableTypeStart(id, config, entryCount, entryStart, offsets);
    }

    @Override
    public void onTableEntryStart(int id, int flags, int key, int parent,
            int count) {
        itemName = keyPool.getString(key);
        isComplexEntry = isComplexEntry(flags);
        int resId = makeId(packageId - 1, typeSpecIndex - 1, id);
        references.put(resId, restypeName + "/" + itemName);
        super.onTableEntryStart(id, flags, key, parent, count);
    }

    @Override
    public void onTableEntryEnd() {
        itemName = null;
        super.onTableEntryEnd();
    }

    @Override
    public void onTableTypeEnd() {
        itemConfig = null;
        super.onTableTypeEnd();
    }

    @Override
    public void onTableTypeSpecEnd() {
        restypeName = null;
        super.onTableTypeSpecEnd();
    }

    @Override
    public void onTablePackageEnd() {
        packageId = 0;
        packageName = null;
        typeSpecIndex = 0;
        typePool = null;
        keyPool = null;
        super.onTablePackageEnd();
    }

    @Override
    public void onTableEnd() {
        tableStarted = false;
        pool = null;
        super.onTableEnd();
    }
}
