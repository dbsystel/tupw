/*
 * Copyright (c) 2018, DB Systel GmbH
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Frank Schwab, DB Systel GmbH
 *
 * Changes: 
 *     2018-08-16: V1.0.0: Created. fhs
 *     2018-08-20: V1.1.0: Expand valuation of 2 to 4 byte compressed numbers. fhs
 *     2018-12-11: V1.1.1: Clarify exceptions and comments. fhs
 */
package dbsnumberlib;

import java.util.Arrays;

/**
 * Converts integers from and to an unsigned packed byte array
 * 
 * @author FrankSchwab
 * @version 1.1.1
 */
public class PackedUnsignedInteger {

   private final static int START_TWO_BYTE_VALUE = 0x40;
   private final static int START_THREE_BYTE_VALUE = 0x4000;
   private final static int START_FOUR_BYTE_VALUE = 0x400000;
   private final static int START_TOO_LARGE_VALUE = 0x40000000;
   
   /**
    * Convert an integer into a packed decimal byte array
    * 
    * Valid integers range from 0 to 1,077,936,127.
    * All other numbers throw an {@code IllegalArgumentException}
    * 
    * @param aNumber Integer to convert
    * @return Packed decimal byte array with integer as value 
    * @throws IllegalArgumentException if {@code aNumber} has not a value between 0 and 1,077,936,127 (inclusive)
    */
   public static byte[] fromInteger(final int aNumber) throws IllegalArgumentException {
      byte[] result;
      int intermediateNumber;
      
      if (aNumber >= 0)
         if (aNumber <= 0x3f) {
            result = new byte[1];
            result[0] = (byte) (aNumber & 0x3f);
         } else if (aNumber < (START_THREE_BYTE_VALUE + START_TWO_BYTE_VALUE)) {
            result = new byte[2];
            intermediateNumber = aNumber - START_TWO_BYTE_VALUE;

            result[1] = (byte) (intermediateNumber & 0xff);

            intermediateNumber >>>= 8;
            result[0] = (byte) (0x40 | (intermediateNumber & 0xff));
         } else if (aNumber < (START_FOUR_BYTE_VALUE + START_THREE_BYTE_VALUE)) {
            result = new byte[3];
            intermediateNumber = aNumber - START_THREE_BYTE_VALUE;

            result[2] = (byte) (intermediateNumber & 0xff);

            intermediateNumber >>>= 8;
            result[1] = (byte) (intermediateNumber & 0xff);

            intermediateNumber >>>= 8;
            result[0] = (byte) (0x80 | (intermediateNumber & 0xff));
         } else if (aNumber < (START_TOO_LARGE_VALUE + START_FOUR_BYTE_VALUE)) {
            result = new byte[4];
            intermediateNumber = aNumber - START_FOUR_BYTE_VALUE;

            result[3] = (byte) (intermediateNumber & 0xff);

            intermediateNumber >>>= 8;
            result[2] = (byte) (intermediateNumber & 0xff);

            intermediateNumber >>>= 8;
            result[1] = (byte) (intermediateNumber & 0xff);

            intermediateNumber >>>= 8;
            result[0] = (byte) (0xc0 | (intermediateNumber & 0xff));
         } else
            throw new IllegalArgumentException("Integer too large for packed integer");
      else
         throw new IllegalArgumentException("Integer must not be negative");

      return result;
   }

   /**
    * Get expected length of packed decimal byte array from first byte
    * 
    * @param firstByteOfPackedNumber First byte of packed decimal integer
    * @return Expected length (1 to 4)
    */
   public static int getExpectedLength(final byte firstByteOfPackedNumber) {
      return ((firstByteOfPackedNumber >>> 6) & 0x03) + 1;
   }

   /**
    * Convert a packed decimal byte array into an integer
    * 
    * @param packedNumber Packed decimal byte array
    * @return Converted integer (value between 0 and 1,077,936,127)
    * @throws IllegalArgumentException if the actual length of the packed number does not match the expected length
    */
   public static int toInteger(final byte[] packedNumber) throws IllegalArgumentException {
      int result = 0;

      final int expectedLength = getExpectedLength(packedNumber[0]);

      if (expectedLength == packedNumber.length)
         switch (expectedLength) {
            case 1:
               result = (packedNumber[0] & 0x3f);
               break;

            case 2:
               result = (((packedNumber[0] & 0x3f) << 8) |
                          (packedNumber[1] & 0xff)) +
                        START_TWO_BYTE_VALUE;
               break;

            case 3:
               result = (((packedNumber[0] & 0x3f) << 16) |
                         ((packedNumber[1] & 0xff) << 8) |
                          (packedNumber[2] & 0xff)) +
                        START_THREE_BYTE_VALUE;
               break;

            case 4:
               result = (((packedNumber[0] & 0x3f) << 24) |
                         ((packedNumber[1] & 0xff) << 16) |
                         ((packedNumber[2] & 0xff) << 8) |
                          (packedNumber[3] & 0xff)) +
                        START_FOUR_BYTE_VALUE;
               break;

            // There is no "else" case as all possible values of "expectedLength" are covered
         }
      else
         throw new IllegalArgumentException("Actual length of packed integer array does not match expected length");

      return result;
   }

   /**
    * Convert a packed decimal byte array in a larger array to an integer
    * 
    * @param arrayWithPackedNumber Array where the packed decimal byte array resides
    * @param startIndex Start index of decimal byte array
    * @return Converted integer
    * @throws IllegalArgumentException if the array is not long enough
    */
   public static int toIntegerFromArray(final byte[] arrayWithPackedNumber, final int startIndex) throws IllegalArgumentException {
      final int expectedLength = getExpectedLength(arrayWithPackedNumber[startIndex]);

      if ((startIndex + expectedLength) <= arrayWithPackedNumber.length)
         return toInteger(Arrays.copyOfRange(arrayWithPackedNumber, startIndex, startIndex + expectedLength));
      else
         throw new IllegalArgumentException("Array too short for packed integer");
   }

}
