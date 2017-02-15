package com.joebowbeer.resourcedecoder;

public class Chunk {

  public final long offset;
  public final int type;
  public final int headerSize;
  public final int totalSize;

  public Chunk(long offset, int type, int headerSize, int totalSize) {
    this.offset = offset;
    this.type = type;
    this.headerSize = headerSize;
    this.totalSize = totalSize;
  }

  @Override
  public String toString() {
    return "[Chunk " + offset + " " + ResourceDecoder.typeToString(type) + " "
        + headerSize + " " + totalSize + "]";
  }
}
