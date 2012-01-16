package com.joebowbeer.resourcedecoder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * XmlContentHandler that locates elements matched by the given set of pattern
 * strings. Produces a set of {@link Chunk} instances representing the edits
 * that are needed to remove the matched elements. Each edit extends a chunk's
 * totalSize, causing the XML parser to skip past one or more subsequent elements.
 * 
 * @see XmlElementSelector
 */
public class XmlElementMatcher extends XmlContentHandler {

    private final Set<XmlElementSelector> selectors = new HashSet<XmlElementSelector>();

    private final Deque<Chunk> chunks = new ArrayDeque<Chunk>();

    private final Set<Chunk> changes = new HashSet<Chunk>();

    private Deque<Long> startElementOffsetStack = new ArrayDeque<Long>();

    public XmlElementMatcher(Iterable<String> patterns) {
        addSelectors(patterns);
    }

    public XmlElementMatcher(Iterable<String> patterns, ContentHandler parent) {
        super(parent);
        addSelectors(patterns);
    }

    public Set<XmlElementSelector> getSelectors() {
        return selectors;
    }

    public Set<Chunk> getChanges() {
        return changes;
    }

    private void addSelectors(Iterable<String> patterns) {
        for (String pattern : patterns) {
            selectors.add(new XmlElementSelector(pattern));
        }
    }

    /* XmlContentHandler overrides */

    @Override
    public void onChunkStart(long offset, int type, int headerSize, int totalSize) {
        // ignore unless we are inside xml content
        if (insideXml()) {
            chunks.add(new Chunk(offset, type, headerSize, totalSize));
        }
        super.onChunkStart(offset, type, headerSize, totalSize);
    }

    @Override
    public void onXmlStartElement(int nsIndex, int nameIndex, int attrIndex,
            int attrSize, int attrCount, int idIndex, int classIndex, int styleIndex) {
        startElementOffsetStack.add(chunks.getLast().offset);
        super.onXmlStartElement(nsIndex, nameIndex, attrIndex, attrSize,
                attrCount, idIndex, classIndex, styleIndex);
    }

    @Override
    public void onXmlEndElement(int nsIndex, int nameIndex) {
        long startElementOffset = startElementOffsetStack.removeLast();
        for (XmlElementSelector selector : selectors) {
            if (selector.matches(curNode)) {
                // remove XmlEndElement chunk
                Chunk chunk = chunks.removeLast();
                changes.remove(chunk);
                long nextOffset = chunk.offset + chunk.totalSize;
                // roll back to chunk that precedes XmlStartElement
                do {
                    chunk = chunks.removeLast();
                    changes.remove(chunk);
                } while (chunk.offset >= startElementOffset);
                assert startElementOffset == chunk.offset + chunk.totalSize;
                // extend chunk to end of matched element
                long newSize = nextOffset - chunk.offset;
                assert newSize == (int) newSize;
                chunk = new Chunk(chunk.offset, chunk.type, chunk.headerSize, (int) newSize);
                chunks.add(chunk);
                changes.add(chunk);
                break; // matched
            }
        }
        super.onXmlEndElement(nsIndex, nameIndex);
    }

    @Override
    public void onXmlEnd() {
        chunks.clear();
        super.onXmlEnd();
    }
}
