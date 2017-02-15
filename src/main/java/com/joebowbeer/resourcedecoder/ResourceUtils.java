package com.joebowbeer.resourcedecoder;

public class ResourceUtils {

  private ResourceUtils() {
    // do not instantiate tool class
  }

  public static final int ENTRY_FLAG_COMPLEX = 0x0001;

  public static final int ENTRY_FLAG_PUBLIC = 0x0002;

  public static boolean isComplexEntry(int flags) {
    return (flags & ENTRY_FLAG_COMPLEX) != 0;
  }

  public static boolean isPublicEntry(int flags) {
    return (flags & ENTRY_FLAG_PUBLIC) != 0;
  }

  // Methods for building/splitting resource identifiers.
  public static int makeId(int pack, int type, int entry) {
    return ((pack + 1) << 24) | (((type + 1) & 0xFF) << 16) | (entry & 0xFFFF);
  }

  public static int getPackage(int id) {
    return (id >> 24) - 1;
  }

  public static int getType(int id) {
    return ((id >> 16) & 0xFF) - 1;
  }

  public static int getEntry(int id) {
    return id & 0xFFFF;
  }

  public static boolean isInternalId(int resid) {
    return (resid & 0xFFFF0000) != 0 && (resid & 0xFF0000) == 0;
  }

  public static int makeInternal(int entry) {
    return 0x01000000 | (entry & 0xFFFF);
  }

  public static int makeArray(int entry) {
    return 0x02000000 | (entry & 0xFFFF);
  }

  public static boolean isArrayId(int resid) {
    return (resid & 0xFFFF0000) == 0x02000000;
  }

  // Special values for 'name' when defining attribute resources.
  // This entry holds the attribute's type code.
  public static final int ATTR_TYPE = 0x01000000;

  // For integral attributes, this is the minimum value it can hold.
  public static final int ATTR_MIN = 0x01000001;

  // For integral attributes, this is the maximum value it can hold.
  public static final int ATTR_MAX = 0x01000002;

  // Localization of this resource can be encouraged or required with
  // an aapt flag if this is set
  public static final int ATTR_L10N = 0x01000003;

  // for plural support, see android.content.res.PluralRules#attrForQuantity(int)
  public static final int ATTR_OTHER = 0x01000004;
  public static final int ATTR_ZERO = 0x01000005;
  public static final int ATTR_ONE = 0x01000006;
  public static final int ATTR_TWO = 0x01000007;
  public static final int ATTR_FEW = 0x01000008;
  public static final int ATTR_MANY = 0x01000009;

  // Bit mask of allowed types, for use with ATTR_TYPE.
  // No type has been defined for this attribute, use generic
  // type handling.  The low 16 bits are for types that can be
  // handled generically; the upper 16 require additional information
  // in the bag so cannot be handled generically for TYPE_ANY.
  public static final int ATTR_TYPE_ANY = 0x0000FFFF;

  // Attribute holds a references to another resource.
  public static final int ATTR_TYPE_REFERENCE = 1;

  // Attribute holds a generic string.
  public static final int ATTR_TYPE_STRING = 1 << 1;

  // Attribute holds an integer value.  ATTR_MIN and ATTR_MIN can
  // optionally specify a constrained range of possible integer values.
  public static final int ATTR_TYPE_INTEGER = 1 << 2;

  // Attribute holds a boolean integer.
  public static final int ATTR_TYPE_BOOLEAN = 1 << 3;

  // Attribute holds a color value.
  public static final int ATTR_TYPE_COLOR = 1 << 4;

  // Attribute holds a floating point value.
  public static final int ATTR_TYPE_FLOAT = 1 << 5;

  // Attribute holds a dimension value, such as "20px".
  public static final int ATTR_TYPE_DIMENSION = 1 << 6;

  // Attribute holds a fraction value, such as "20%".
  public static final int ATTR_TYPE_FRACTION = 1 << 7;

  // Attribute holds an enumeration.  The enumeration values are
  // supplied as additional entries in the map.
  public static final int ATTR_TYPE_ENUM = 1 << 16;

  // Attribute holds a bitmaks of flags.  The flag bit values are
  // supplied as additional entries in the map.
  public static final int ATTR_TYPE_FLAGS = 1 << 17;

  // Enum of localization modes, for use with ATTR_L10N.
  public static final int ATTR_L10N_NOT_REQUIRED = 0;
  public static final int ATTR_L10N_SUGGESTED = 1;

  private enum AllowedType {
    REFERENCE(ATTR_TYPE_REFERENCE, "reference"),
    STRING(ATTR_TYPE_STRING, "string"),
    INTEGER(ATTR_TYPE_INTEGER, "integer"),
    BOOLEAN(ATTR_TYPE_BOOLEAN, "boolean"),
    COLOR(ATTR_TYPE_COLOR, "color"),
    FLOAT(ATTR_TYPE_FLOAT, "float"),
    DIMENSION(ATTR_TYPE_DIMENSION, "dimension"),
    FRACTION(ATTR_TYPE_FRACTION, "fraction");

    public final int value;
    public final String name;

    AllowedType(int value, String name) {
      this.value = value;
      this.name = name;
    }
  }

  public static String formatAllowedTypes(int type) {
    switch (type) {
      case ATTR_TYPE_ANY:
        return "any";
      case ATTR_TYPE_ENUM:
        return "enum";
      case ATTR_TYPE_FLAGS:
        return "flags";
      default: {
        StringBuilder sb = new StringBuilder();
        for (AllowedType at : AllowedType.values()) {
          if ((type & at.value) != 0) {
            if (sb.length() != 0) {
              sb.append('|');
            }
            sb.append(at.name);
          }
        }
        return sb.toString();
      }
    }
  }

  public static String formatQuantity(int quantityId) {
    switch (quantityId) {
      case ATTR_OTHER:
        return "other";
      case ATTR_ZERO:
        return "zero";
      case ATTR_ONE:
        return "one";
      case ATTR_TWO:
        return "two";
      case ATTR_FEW:
        return "few";
      case ATTR_MANY:
        return "many";
      default:
        throw new IllegalArgumentException(String.valueOf(quantityId));
    }
  }
}
