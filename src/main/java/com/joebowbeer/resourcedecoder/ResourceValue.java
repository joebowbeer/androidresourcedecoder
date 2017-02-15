package com.joebowbeer.resourcedecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ResourceValue {

  /* The types of values. */
  // Contains no data.
  public static final int TYPE_NULL = 0x00;

  // The 'data' holds a reference to another resource table entry.
  public static final int TYPE_REFERENCE = 0x01;

  // The 'data' holds an attribute resource identifier.
  public static final int TYPE_ATTRIBUTE = 0x02;

  // The 'data' holds an index into the containing resource table's
  // global value string pool.
  public static final int TYPE_STRING = 0x03;

  // The 'data' holds a single-precision floating point number.
  public static final int TYPE_FLOAT = 0x04;

  // The 'data' holds a complex number encoding a dimension value,
  // such as "100in".
  public static final int TYPE_DIMENSION = 0x05;

  // The 'data' holds a complex number encoding a fraction of a container.
  public static final int TYPE_FRACTION = 0x06;

  /* integer flavors */
  // The 'data' is a raw integer value of the form n..n.
  public static final int TYPE_INT_DEC = 0x10;

  // The 'data' is a raw integer value of the form 0xn..n.
  public static final int TYPE_INT_HEX = 0x11;

  // The 'data' is either 0 or 1, for input "false" or "true" respectively.
  public static final int TYPE_INT_BOOLEAN = 0x12;

  /* integer color flavors */
  // The 'data' is a raw integer value of the form #aarrggbb.
  public static final int TYPE_INT_COLOR_ARGB8 = 0x1c;

  // The 'data' is a raw integer value of the form #rrggbb.
  public static final int TYPE_INT_COLOR_RGB8 = 0x1d;

  // The 'data' is a raw integer value of the form #argb.
  public static final int TYPE_INT_COLOR_ARGB4 = 0x1e;

  // The 'data' is a raw integer value of the form #rgb.
  public static final int TYPE_INT_COLOR_RGB4 = 0x1f;

  public final int type;
  public final byte[] data; // data.length == 4

  public ResourceValue(int type, byte[] data) {
    this.type = type;
    this.data = data;
  }

  @Override
  public String toString() {
    return "[" + typeToString() + " " + String.format("%#x", intValue()) + "]";
  }

  protected String typeToString() {
    switch (type) {
      case TYPE_NULL:
        return "NULL";
      case TYPE_REFERENCE:
        return "REF";
      case TYPE_ATTRIBUTE:
        return "ATTR";
      case TYPE_STRING:
        return "STRING";
      case TYPE_FLOAT:
        return "FLOAT";
      case TYPE_DIMENSION:
        return "DIMEN";
      case TYPE_FRACTION:
        return "FRACTION";
      case TYPE_INT_DEC:
        return "DEC";
      case TYPE_INT_HEX:
        return "HEX";
      case TYPE_INT_BOOLEAN:
        return "BOOL";
      case TYPE_INT_COLOR_ARGB8:
        return "ARGB8";
      case TYPE_INT_COLOR_RGB8:
        return "RGB8";
      case TYPE_INT_COLOR_ARGB4:
        return "ARGB4";
      case TYPE_INT_COLOR_RGB4:
        return "RGB4";
      default:
        return String.format("UNKNOWN(%#x)", type);
    }
  }

  public String format(StringPool pool) {
    switch (type) {
      case TYPE_NULL:
        return ""; // TODO?
      case TYPE_REFERENCE:
        return String.format("@%#010x", intValue());
      case TYPE_ATTRIBUTE:
        return String.format("?%#010x", intValue());
      case TYPE_STRING:
        return pool.getString(intValue()); // TODO?
      case TYPE_FLOAT:
        return String.format("%f", floatValue());
      case TYPE_DIMENSION:
        return String.format("%.2f%s", complexToFloat(), dimensionUnit());
      case TYPE_FRACTION:
        return String.format("%.2f%s", complexToFloat() * 100, fractionUnit());
      case TYPE_INT_DEC:
        return String.valueOf(intValue());
      case TYPE_INT_HEX:
        return String.format("%#x", intValue());
      case TYPE_INT_BOOLEAN:
        return String.valueOf(booleanValue());
      case TYPE_INT_COLOR_ARGB8:
      case TYPE_INT_COLOR_ARGB4:
        return String.format("#%08x", intValue());
      case TYPE_INT_COLOR_RGB8:
      case TYPE_INT_COLOR_RGB4:
        return String.format("#%06x", intValue());
      default:
        return String.format("%#x", intValue());
    }
  }

  protected boolean booleanValue() {
    return (data[0] | data[1] | data[2] | data[3]) != 0;
  }

  protected int intValue() {
    int value = 0;
    for (int i = 4; --i >= 0;) {
      value = (value << 8) + (data[i] & 0xFF);
    }
    return value;
  }

  protected float floatValue() {
    return Float.intBitsToFloat(intValue());
  }

  protected float complexToFloat() {
    int value = intValue();
    return (float) ((value & (COMPLEX_MANTISSA_MASK << COMPLEX_MANTISSA_SHIFT))
        * RADIX_MULTS[(value >> COMPLEX_RADIX_SHIFT) & COMPLEX_RADIX_MASK]);
  }

  protected String dimensionUnit() {
    return DIMENSION_UNIT_STRS[intValue() & COMPLEX_UNIT_MASK];
  }

  protected String fractionUnit() {
    return FRACTION_UNIT_STRS[intValue() & COMPLEX_UNIT_MASK];
  }

  private static final int COMPLEX_MANTISSA_SHIFT = 8;
  private static final int COMPLEX_MANTISSA_MASK = 0xffffff;
  private static final int COMPLEX_RADIX_SHIFT = 4;
  private static final int COMPLEX_RADIX_MASK = 0x3;
  //private static final int COMPLEX_UNIT_SHIFT = 0;
  private static final int COMPLEX_UNIT_MASK = 0xf;
  private static final String[] DIMENSION_UNIT_STRS = {
    "px", "dp", "sp", "pt", "in", "mm"
  };
  private static final String[] FRACTION_UNIT_STRS = {
    "%", "%p"
  };
  private static final double MANTISSA_MULT
      = 1.0 / (1 << COMPLEX_MANTISSA_SHIFT);
  private static final double[] RADIX_MULTS = {
    1.0 * MANTISSA_MULT,
    1.0 / (1 << 7) * MANTISSA_MULT,
    1.0 / (1 << 15) * MANTISSA_MULT,
    1.0 / (1 << 23) * MANTISSA_MULT
  };

  /* Encoders */
  public static byte[] encode(String name, String value) throws IOException {
    int type = typeFromName(name);
    int size = 8;
    ByteArrayOutputStream out = new ByteArrayOutputStream(size);
    writeShort(out, size);
    out.write(0); // res0
    out.write(type);
    writeInt(out, parse(value, type));
    return out.toByteArray();
  }

  public static int typeFromName(String name) {
    if (name.startsWith("R.bool.")) {
      return TYPE_INT_BOOLEAN;
    } else if (name.startsWith("R.color.")) {
      return TYPE_INT_COLOR_ARGB8;
    } else {
      throw new IllegalArgumentException(name);
    }
  }

  private static int parse(String s, int type) {
    switch (type) {
      case TYPE_INT_BOOLEAN: {
        return parseBoolean(s);
      }
      case TYPE_INT_COLOR_ARGB8: {
        return parseColor(s);
      }
      default: {
        throw new IllegalArgumentException(String.valueOf(type));
      }
    }
  }

  private static int parseBoolean(String bool) {
    // TODO: canonical encoding of false is -1?
    return Boolean.parseBoolean(bool) ? -1 : 0;
  }

  private static int parseColor(String color) {
    if (!color.startsWith("#") || color.length() != 9) {
      throw new IllegalArgumentException(color);
    }
    return (int) Long.parseLong(color.substring(1), 16);
  }

  /**
   * LE version of DataOutput.writeInt
   */
  private static void writeInt(OutputStream out, int v) throws IOException {
    out.write(v);
    out.write(v >> 8);
    out.write(v >> 16);
    out.write(v >> 24);
  }

  /**
   * LE version of DataOutput.writeShort
   */
  private static void writeShort(OutputStream out, int v) throws IOException {
    out.write(v);
    out.write(v >> 8);
  }
}
