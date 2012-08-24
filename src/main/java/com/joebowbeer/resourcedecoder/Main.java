package com.joebowbeer.resourcedecoder;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Serializer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

    private static String VERSION_STRING =
            Main.class.getPackage().getSpecificationTitle() + " " +
            Main.class.getPackage().getSpecificationVersion();

    /**
     * Decodes resources.arsc or compiled xml file.
     * @param args command line options
     */
    public static void main(String[] args) throws IOException {

        // create command line options
        Options options = new Options();
        options.addOption(OptionBuilder
                .withArgName("file").hasArg()
                .withDescription("file to modify")
                .create("file"));
        options.addOption(OptionBuilder
                .withDescription("dump file to stdout")
                .create("dump"));
        options.addOption(OptionBuilder
                .withDescription("print version and exit")
                .create("version"));
        options.addOption(OptionBuilder
                .withArgName("resource=value")
                .hasArgs(2)
                .withValueSeparator()
                .withDescription("assign value to given resource")
                .create("R"));
        options.addOption(OptionBuilder
                .withArgName("pattern")
                .hasArgs(1)
                .withDescription("remove xml element matching pattern")
                .create("X"));

        // parse the command line arguments
        String filename;
        Properties resProps;
        Set<String> xmlRemovals;
        boolean dump;
        try {
            CommandLine line = new GnuParser().parse(options, args);
            filename = line.getOptionValue("file");
            resProps = line.getOptionProperties("R");
            xmlRemovals = line.getOptionProperties("X").stringPropertyNames();
            dump = line.hasOption("dump");
            if (line.hasOption("version")) {
                Log.i(VERSION_STRING);
                System.exit(0);
                return;
            }
            boolean fileCmd = dump || !resProps.isEmpty() || !xmlRemovals.isEmpty();
            if (filename == null || !fileCmd) {
                throw new ParseException("Missing option");
            }
        } catch (ParseException ex) {
            Log.i(VERSION_STRING);
            Log.e("Command parsing failed: " + ex.getMessage());
            new HelpFormatter().printHelp("java -jar resourcedecoder.jar [options]", options);
            System.exit(1);
            return;
        }

        /* Create chain of content handlers */

        ContentHandler handler = new ContentFilter();

        TableAttributeMatcher resMatcher = null;
        if (!resProps.isEmpty()) {
            Set<String> patterns = resProps.stringPropertyNames();
            handler = resMatcher = new TableAttributeMatcher(patterns, handler);
        }

        XmlElementMatcher xmlMatcher = null;
        if (!xmlRemovals.isEmpty()) {
            handler = xmlMatcher = new XmlElementMatcher(xmlRemovals, handler);
        }

        if (dump) {
            handler = new TableContentToDocument(new XmlContentToDocument(handler));
        }

        /* Decode */

        Log.i("Reading " + filename);
        ResourceInputStream in = new ResourceInputStream(
            new BufferedInputStream(new FileInputStream(filename)));
        try {
            new ResourceDecoder(handler).decode(in);
        } finally {
            in.close();
        }

        /* Dump */

        for (; handler != null; handler = ((ContentFilter) handler).getParent()) {
            if (handler instanceof DocumentBuilder) {
                Document doc = ((DocumentBuilder) handler).toDocument();
                if (doc != null) {
                    OutputStream os = System.out;
                    Serializer output = (handler instanceof TableContentToDocument)
                            ? new TableSerializer((TableContentToDocument) handler, os)
                            : new Serializer(os);
                    output.setIndent(4);
                    output.setMaxLength(72);
                    output.write(doc);
                    output.flush();
                }
            }
        }

        /* Apply edits */

        if (resMatcher != null) {
            Map<String, List<Long>> matches = resMatcher.getMatches();
            if (!resMatcher.getPatterns().equals(matches.keySet())) {
                Log.e("Some resources were not matched " + resMatcher.getPatterns());
                System.exit(2);
                return;
            }
            Log.i("Found: " + matches);
            editResourceValues(filename, resProps, matches);
        }

        if (xmlMatcher != null) {
            Set<Chunk> changes = xmlMatcher.getChanges();
            if (changes.isEmpty()) {
                Log.i("No match for elements " + xmlMatcher.getSelectors());
            } else {
                Log.i("Removing elements " + xmlMatcher.getSelectors());
                removeXmlElements(filename, changes);
            }
        }
    }

    protected static void editResourceValues(String filename, Properties props,
            Map<String, List<Long>> matches) throws IOException {
        Log.i("Updating " + filename);
        RandomAccessFile file = new RandomAccessFile(filename, "rw");
        try {
            for (Entry<String, List<Long>> entry : matches.entrySet()) {
                String key = entry.getKey();
                // TODO update multiple configurations?
                long pos = entry.getValue().get(0);
                file.seek(pos);
                // validate size and type of new value
                byte[] orig = new byte[4]; // TODO?
                file.readFully(orig);
                String value = props.getProperty(key);
                byte[] data = ResourceValue.encode(key, value);
                for (int i = 0; i < 4; i++) {
                    if (orig[i] != data[i]) {
                        throw new IllegalStateException();
                    }
                }
                // write new value bytes
                file.write(data, 4, 4); // TODO?
                Log.i(pos + ": " + key + "=" + value);
            }
        } finally {
            file.close();
        }
        Log.i("Success");
    }

    protected static void removeXmlElements(String filename, Iterable<Chunk> changes)
            throws IOException {
        Log.i("Updating " + filename);
        RandomAccessFile file = new RandomAccessFile(filename, "rw");
        try {
            for (Chunk chunk : changes) {
                Log.i(chunk);
                // validate chunk
                file.seek(chunk.offset);
                int type = unsignedShortLE(file.readShort());
                int headerSize = unsignedShortLE(file.readShort());
                if (type != chunk.type || headerSize != chunk.headerSize) {
                    throw new IllegalStateException();
                }
                // write new totalSize (LE)
                file.writeInt(intLE(chunk.totalSize));
            }
        } finally {
            file.close();
        }
        Log.i("Success");
    }

    protected static class TableSerializer extends Serializer {

        private final Map<Integer, String> ids;
        private final Set<Integer> refs; // TODO!

        public TableSerializer(TableContentToDocument handler, OutputStream out) {
            super(out);
            ids = new HashMap(handler.getIdentifiers());
            refs = new HashSet(handler.getReferences());
            // prune identifiers
            ids.keySet().retainAll(refs); // referenced identifiers
            refs.removeAll(ids.keySet()); // android references
        }

        @Override
        protected void write(Attribute attr) throws IOException {
            super.write(replaceName(replaceValue(attr)));
        }

        protected Attribute replaceName(Attribute attr) {
            String name = attr.getLocalName();
            if (name.startsWith("0x") && name.length() == 10) {
                try {
                    String resolved = ids.get(Integer.decode(name));
                    if (resolved != null) {
                        attr = new Attribute(attr);
                        attr.setLocalName(resolved);
                    }
                } catch (NumberFormatException ex) {
                    // fall through
                }
            }
            return attr;
        }

        protected Attribute replaceValue(Attribute attr) {
            String value = attr.getValue();
            String prefix = "";
            if (value.startsWith("@") || value.startsWith("?")) {
                prefix = value.substring(0, 1);
                value = value.substring(1);
            }
            if (value.startsWith("0x") && value.length() == 10) {
                try {
                    String resolved = ids.get(Integer.decode(value));
                    if (resolved != null) {
                        attr = new Attribute(attr);
                        attr.setValue(prefix + resolved);
                    }
                } catch (NumberFormatException ex) {
                    // fall through
                }
            }
            return attr;
        }
    }

    /* little-endian conversion */

    private static int unsignedShortLE(short value) {
        return Short.reverseBytes(value) & 0xFFFF;
    }

    private static int intLE(int value) {
        return Integer.reverseBytes(value);
    }
}
