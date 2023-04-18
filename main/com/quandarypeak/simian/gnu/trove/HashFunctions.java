package com.quandarypeak.simian.gnu.trove;

/**
 * Provides various hash functions.
 */
public final class HashFunctions {
    /**
     * Returns a hashcode for the specified value.
     *
     * @return a hash code value for the specified value.
     */
    public static int hash(final long value) {
        return (int) (value ^ value >> 32);

        /*
         * The hashcode is computed as
         * <blockquote><pre>
         * 31^5*(d[0]*31^(n-1) + d[1]*31^(n-2) + ... + d[n-1])
         * </pre></blockquote>
         * using <code>int</code> arithmetic, where <code>d[i]</code> is the
         * <i>i</i>th digit of the value, counting from the right, <code>n</code> is the number of decimal digits of the specified value,
         * and <code>^</code> indicates exponentiation.
         * (The hash value of the value zero is zero.)

         value &= 0x7FFFFFFFFFFFFFFFL; // make it >=0 (0x7FFFFFFFFFFFFFFFL==Long.MAX_VALUE)
         int hashCode = 0;
         do hashCode = 31*hashCode + (int) (value%10);
         while ((value /= 10) > 0);

         return 28629151*hashCode; // spread even further; h*31^5
        */
    }
}
