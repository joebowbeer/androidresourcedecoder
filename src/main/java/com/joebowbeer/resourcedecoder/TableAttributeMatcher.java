package com.joebowbeer.resourcedecoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.joebowbeer.resourcedecoder.ResourceUtils.isComplexEntry;

/**
 * ContentFilter that locates ResourceValue instances matched by one of a given set of pattern
 * strings. Patterns are of the form <b>name=value</b>, for example:
 * <b>R.color.background=#ff000000</b>.
 */
public class TableAttributeMatcher extends ContentFilter {

  private final Set<String> patterns;

  private final Map<String, List<Long>> matches = new HashMap<>();

  private boolean tableStarted;
  private StringPool pool;
  private boolean hasTypePool;
  private boolean hasKeyPool;
  private StringPool typePool;
  private StringPool keyPool;
  private String packageName;
  private int typeSpecIndex;
  private String restypeName;
  private ResourceConfig itemConfig;
  private String itemName;
  private boolean isComplexEntry;

  public TableAttributeMatcher(Set<String> patterns) {
    this.patterns = patterns;
  }

  public TableAttributeMatcher(Set<String> patterns, ContentHandler parent) {
    super(parent);
    this.patterns = patterns;
  }

  public Set<String> getPatterns() {
    return patterns;
  }

  public Map<String, List<Long>> getMatches() {
    return matches;
  }

  /* ContentFilter overrides */
  @Override
  public void onTableStart(int packageCount) {
    tableStarted = true;
    matches.clear();
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
    super.onTableEntryStart(id, flags, key, parent, count);
  }

  @Override
  public void onResourceValue(long offset, ResourceValue value) {
    assert offset == (int) offset;
    if (tableStarted && !isComplexEntry) {
      String name = "R." + restypeName + "." + itemName;
      if (patterns.contains(name)) {
        List<Long> list = matches.get(name);
        if (list == null) {
          list = new ArrayList<>();
          matches.put(name, list);
        }
        list.add(offset);
      }
    }
    super.onResourceValue(offset, value);
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
    typeSpecIndex = 0;
    typePool = null;
    keyPool = null;
    packageName = null;
    super.onTablePackageEnd();
  }

  @Override
  public void onTableEnd() {
    tableStarted = false;
    pool = null;
    super.onTableEnd();
  }
}
