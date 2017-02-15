package com.joebowbeer.resourcedecoder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Represents an XML element selector defined by the given pattern string. The pattern syntax is
 * based loosely on the CSS selector syntax. The pattern
 * <b>action[android:name=com.swype.example]</b> matches an <b>action</b>
 * element whose <b>android:name</b> attribute is equal to
 * <b>com.swype.example</b>. Multiple attributes can be selected by appending additional
 * <b>[name=value]</b> components.
 *
 * @see XmlElementMatcher
 */
public class XmlElementSelector {

  public final String elementName;

  public final Map<String, String> attributes = new HashMap<>();

  public XmlElementSelector(String pattern) {
    Iterator<String> iter = Arrays.asList(pattern.split("\\[|\\]")).iterator();
    elementName = iter.next();
    while (iter.hasNext()) {
      String attr = iter.next();
      if (!attr.isEmpty()) {
        String[] pair = attr.split("=");
        attributes.put(pair[0], pair[1]);
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    XmlElementSelector other = (XmlElementSelector) obj;
    return elementName.equals(other.elementName) && attributes.equals(other.attributes);
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 61 * hash + elementName.hashCode();
    hash = 61 * hash + attributes.hashCode();
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(elementName);
    attributes.entrySet().forEach((e) -> {
      sb.append('[').append(e.getKey()).append('=').append(e.getValue()).append(']');
    });
    return sb.toString();
  }

  public boolean matches(Element node) {
    if (!elementName.equals(node.getQualifiedName())) {
      return false;
    }
    Map<String, String> map = new HashMap<>(attributes);
    for (int i = 0, n = node.getAttributeCount(); i < n; i++) {
      Attribute attr = node.getAttribute(i);
      String name = attr.getQualifiedName();
      String value = map.remove(name);
      if (value != null && !value.equals(attr.getValue())) {
        return false; // attribute values don't match
      }
    }
    // map is empty if all selected attributes have been matched
    return map.isEmpty();
  }
}
