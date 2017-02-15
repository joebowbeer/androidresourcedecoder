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
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

  private static final String VERSION_STRING =
      Main.class.getPackage().getSpecificationTitle() + " " +
      Main.class.getPackage().getSpecificationVersion();

  /**
   * Decodes resources.arsc or compiled xml file.
   *
   * @param args command line options
   * @throws java.io.IOException
   */
  public static void main(String[] args) throws IOException {

    // create command line options
    Options options = new Options();
    options.addOption(Option.builder("file")
        .desc("file to modify")
        .argName("file")
        .hasArg()
        .build());
    options.addOption(Option.builder("dump")
        .desc("dump file to stdout")
        .build());
    options.addOption(Option.builder("version")
        .desc("print version and exit")
        .build());
    options.addOption(Option.builder("R")
        .desc("assign value to given resource")
        .argName("resource=value")
        .numberOfArgs(2)
        .valueSeparator()
        .build());
    options.addOption(Option.builder("X")
        .desc("remove xml element matching pattern")
        .argName("pattern")
        .hasArg()
        .build());

    // parse the command line arguments
    String filename;
    Properties resProps;
    Set<String> xmlRemovals;
    boolean dump;
    try {
      CommandLine line = new DefaultParser().parse(options, args);
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
    try (ResourceInputStream in = new ResourceInputStream(
        new BufferedInputStream(new FileInputStream(filename)))) {
      new ResourceDecoder(handler).decode(in);
    }

    /* Dump */
    for (; handler != null; handler = ((ContentFilter) handler).getParent()) {
      if (handler instanceof DocumentBuilder) {
        Document doc = ((DocumentBuilder) handler).toDocument();
        if (doc != null) {
          OutputStream os = System.out;
          Serializer output = (handler instanceof TableContentToDocument)
              ? createSerializer((TableContentToDocument) handler, os)
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
    try (RandomAccessFile file = new RandomAccessFile(filename, "rw")) {
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
    }
    Log.i("Success");
  }

  protected static void removeXmlElements(String filename, Iterable<Chunk> changes)
      throws IOException {
    Log.i("Updating " + filename);
    try (RandomAccessFile file = new RandomAccessFile(filename, "rw")) {
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
    }
    Log.i("Success");
  }

  protected static Serializer createSerializer(TableContentToDocument handler, OutputStream out)
      throws IOException {
    Map<String, String> map = new HashMap<>(handler.getDeclarations()); // clone
    // load android.R.* identifiers
    Properties props = new Properties();
    props.load(Main.class.getResourceAsStream("/android.properties"));
    props.stringPropertyNames().forEach((key) -> {
      map.put(key, props.getProperty(key));
    });
    Set<String> unresolved = new HashSet<>(handler.getReferences()); // clone
    unresolved.removeAll(map.keySet());
    if (!unresolved.isEmpty()) {
      Log.i("Unresolved resource ids: " + unresolved);
    }
    return new TableSerializer(map, out);
  }

  protected static class TableSerializer extends Serializer {

    private final Map<String, String> map;

    public TableSerializer(Map<String, String> map, OutputStream out) {
      super(out);
      this.map = map;
    }

    @Override
    protected void write(Attribute attr) throws IOException {
      super.write(replaceValue(attr));
    }

    protected Attribute replaceValue(Attribute attr) {
      String value = attr.getValue();
      String prefix = "";
      if (value.startsWith("@") || value.startsWith("?") || value.startsWith("$")) {
        prefix = value.substring(0, 1);
        value = value.substring(1);
      }
      if (value.startsWith("0x")) {
        String resolved = map.get(value);
        if (resolved != null) {
          attr = new Attribute(attr);
          if ("$".equals(prefix)) {
            attr.setValue(trimType(resolved));
          } else {
            attr.setValue(prefix + resolved);
          }
        }
      }
      return attr;
    }

    protected static String trimType(String s) {
      int colon = s.indexOf(':') + 1;
      int slash = s.indexOf('/') + 1;
      return s.substring(0, colon) + s.substring(slash);
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
