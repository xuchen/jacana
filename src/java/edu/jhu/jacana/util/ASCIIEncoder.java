// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jacana.util;

/**
   @author Benjamin Van Durme

   see: http://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters

   A not-very-efficient tool for encoding byte sequences to/from printable
   ASCII.
 */
public class ASCIIEncoder {
  // When MIN = 32, MAX = 126 then:
  // ' ', '"', '$', '%', '/', '\'
  // public final static byte[] defaultReserved = new byte[] {32,34,36,37,47,92};
  public final static byte[] defaultReserved = new byte[] {};
  static final int[] defaultEncoder = buildEncoder(defaultReserved);
  static final byte[][] defaultDecoder = buildDecoder(defaultEncoder);

  static final byte MIN = 65;
  static final byte MAX = 90;


  private static byte nextValidByte (byte x, byte[] reserved) {
    x = (byte) (x + 1);
    for (int i = 0; i < reserved.length; i++)
      if (x == reserved[i])
		    x = (byte) (x + 1);
    return x;
  }
  /**
     Returns an array of length 256, mapping bytes to integers such that the
     first and second byte of the short encode printable characters, excepting
     those in the reserved. The pair of first and second are unique for each
     byte value.
  */
  public static int[] buildEncoder(byte[] reserved) {
    java.util.Arrays.sort(reserved);
    int[] encoder = new int[256];
    byte first = nextValidByte((byte)(MIN-1), reserved);
    byte second = nextValidByte((byte)(MIN-1), reserved);
    int zero = 0;

    for (int i = 0; i < 256; i++) {
	    encoder[i] = (((first | zero) << 8) + second);
	    second = nextValidByte(second,reserved);
	    if (second == MAX + 1) {
        second = nextValidByte((byte)(MIN-1),reserved);
        first = nextValidByte(first,reserved);
	    }
    }
    return encoder;
  }

  public static byte[][] buildDecoder (int[] encoder) {
    // grossly inefficient, but allows for direct access
    byte[][] decoder = new byte[(MAX-MIN)+1][(MAX-MIN)+1];
    int code;
    byte first, second;
    for (int i = 0; i < encoder.length; i++) {
	    code = encoder[i];
	    first = (byte) (code >> 8);
	    second = (byte) code;
	    decoder[first-MIN][second-MIN] = (byte) (i - 128);
    }
    return decoder;
  }
  public static byte[] decode (byte[] encoded) {
    return decode (encoded, defaultDecoder);
  }
  public static byte[] decode (byte[] encoded, byte[][] decoder) {
    byte[] raw = new byte[encoded.length/2];
    for (int i = 0; i < raw.length; i++)
	    raw[i] = decoder[encoded[i*2]-MIN][encoded[i*2+1]-MIN];
    return raw;
  }

  public static byte[] encode (byte[] raw) {
    return encode(raw, defaultEncoder);
  }
  public static byte[] encode (byte[] raw, int[] encoder) {
    byte[] encoded = new byte[raw.length * 2];
    int ei;
    int code;
    for (int i = 0; i < raw.length; i++) {
	    ei = i*2;
	    code = encoder[raw[i]+128];
	    encoded[ei] = (byte) (code >> 8);
	    encoded[ei+1] = (byte) code; // takes just the lower 8 bits
    }
    return encoded;
  }


  public static void main (String[] args) {
    byte[] original = new byte[256];
    for (int i = 0; i < 256; i++)
      original[i] = (byte) (i - 128);

    byte[] encoded = encode(original);
    byte[] decoded = decode(encoded);
    for (int i = 0; i < 256; i++)
	    if (original[i] != decoded[i])
        System.out.println(original[i] + " != " + decoded[i]);

    String test = "This is a \"test\"";
    System.out.println(test);
    byte[] testEncoded = encode(test.getBytes());
    System.out.println(new String(testEncoded));
    byte[] testDecoded = decode(testEncoded);
    System.out.println(new String(testDecoded));

  }
}