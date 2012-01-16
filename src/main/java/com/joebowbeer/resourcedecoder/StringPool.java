package com.joebowbeer.resourcedecoder;

import java.util.List;

public class StringPool {

    public static final int SORTED_FLAG = 1;
    public static final int UTF8_FLAG = 1 << 8;

    private final List<String> strings;
    private final List<Style> styles;

    public StringPool(List<String> strings, List<Style> styles) {
        this.strings = strings;
        this.styles = styles;
    }

    @Override
    public String toString() {
        return "[StringPool " + strings + " " + styles + "]";
    }

    public String getString(int index) {
        if (index == -1 || index >= strings.size()) {
            return null;
        }
        return strings.get(index);
    }

    // TODO getStyle

    public static class Style {

        /** Special name value indicating the end of an array of spans. */
        public static final int END = 0xFFFFFFFF;

        /** The name of the XML tag that defined the span. */
        public final int name;

        /** The range of characters in the string that this span applies to. */
        public final int firstChar;
        public final int lastChar;

        public Style(int name, int firstChar, int lastChar) {
            this.name = name;
            this.firstChar = firstChar;
            this.lastChar = lastChar;
        }

        @Override
        public String toString() {
            return "[Style " + String.format("%#x", name)
                    + " " + firstChar + " " + lastChar + "]";
        }
    }
}
