/*
 * Anarres C Preprocessor
 * Copyright (c) 2007-2015, Shevek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package foundry.veil.lib.anarres.cpp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;

/*
 * NOTE: This File was edited by the Veil Team based on this commit: https://github.com/shevek/jcpp/commit/5e50e75ec33f5b4567cabfd60b6baca39524a8b7
 *
 * - Updated formatting to more closely follow project standards
 * - Removed all file/IO
 * - Fixed minor errors
 */

public class NumericValue extends Number {

    public static final int F_UNSIGNED = 1;
    public static final int F_INT = 2;
    public static final int F_LONG = 4;
    public static final int F_LONGLONG = 8;
    public static final int F_FLOAT = 16;
    public static final int F_DOUBLE = 32;

    public static final int FF_SIZE = F_INT | F_LONG | F_LONGLONG | F_FLOAT | F_DOUBLE;

    private final int base;
    private final String integer;
    private String fraction;
    private int expbase = 0;
    private String exponent;
    private int flags;

    public NumericValue( int base, @NotNull String integer) {
        this.base = base;
        this.integer = integer;
    }

    
    public int getBase() {
        return base;
    }

    @NotNull
    public String getIntegerPart() {
        return integer;
    }

    @Nullable
    public String getFractionalPart() {
        return fraction;
    }

    /* pp */ void setFractionalPart(@NotNull String fraction) {
        this.fraction = fraction;
    }

    
    public int getExponentBase() {
        return expbase;
    }

    @Nullable
    public String getExponent() {
        return exponent;
    }

    /* pp */ void setExponent( int expbase, @NotNull String exponent) {
        this.expbase = expbase;
        this.exponent = exponent;
    }

    public int getFlags() {
        return flags;
    }

    /* pp */ void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * So, it turns out that parsing arbitrary bases into arbitrary
     * precision numbers is nontrivial, and this routine gets it wrong
     * in many important cases.
     */
    @NotNull
    public BigDecimal toBigDecimal() {
        int scale = 0;
        String text = this.getIntegerPart();
        String t_fraction = this.getFractionalPart();
        if (t_fraction != null) {
            text += this.getFractionalPart();
            // XXX Wrong for anything but base 10.
            scale += t_fraction.length();
        }
        String t_exponent = this.getExponent();
        if (t_exponent != null) {
            scale -= Integer.parseInt(t_exponent);
        }
        BigInteger unscaled = new BigInteger(text, this.getBase());
        return new BigDecimal(unscaled, scale);
    }

    // We could construct a heuristic for when an 'int' is large enough.
    // private static final int S_MAXLEN_LONG = String.valueOf(Long.MAX_VALUE).length();
    // private static final int S_MAXLEN_INT = String.valueOf(Integer.MAX_VALUE).length();

    @NotNull
    public Number toJavaLangNumber() {
        int flags = this.getFlags();
        if ((flags & F_DOUBLE) != 0) {
            return this.doubleValue();
        } else if ((flags & F_FLOAT) != 0) {
            return this.floatValue();
        } else if ((flags & (F_LONG | F_LONGLONG)) != 0) {
            return this.longValue();
        } else if ((flags & F_INT) != 0) {
            return this.intValue();
        } else if (this.getFractionalPart() != null) {
            return this.doubleValue();    // .1 is a double in Java.
        } else if (this.getExponent() != null) {
            return this.doubleValue();
        } else {
            // This is an attempt to avoid overflowing on over-long integers.
            // However, now we just overflow on over-long longs.
            // We should really use BigInteger.
            long value = this.longValue();
            if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                return (int) value;
            }
            return value;
        }
    }

    private int exponentValue() {
        return Integer.parseInt(exponent, 10);
    }

    @Override
    public int intValue() {
        // String.isEmpty() is since 1.6
        int v = integer.length() == 0 ? 0 : Integer.parseInt(integer, base);
        if (expbase == 2) {
            v = v << this.exponentValue();
        } else if (expbase != 0) {
            v = (int) (v * Math.pow(expbase, this.exponentValue()));
        }
        return v;
    }

    @Override
    public long longValue() {
        // String.isEmpty() is since 1.6
        long v = integer.length() == 0 ? 0 : Long.parseLong(integer, base);
        if (expbase == 2) {
            v = v << this.exponentValue();
        } else if (expbase != 0) {
            v = (long) (v * Math.pow(expbase, this.exponentValue()));
        }
        return v;
    }

    @Override
    public float floatValue() {
        if (this.getBase() != 10) {
            return this.longValue();
        }
        return Float.parseFloat(this.toString());
    }

    @Override
    public double doubleValue() {
        if (this.getBase() != 10) {
            return this.longValue();
        }
        return Double.parseDouble(this.toString());
    }

    private boolean appendFlags(StringBuilder buf, String suffix, int flag) {
        if ((this.getFlags() & flag) != flag) {
            return false;
        }
        buf.append(suffix);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        switch (base) {
            case 8:
                buf.append('0');
                break;
            case 10:
                break;
            case 16:
                buf.append("0x");
                break;
            case 2:
                buf.append('b');
                break;
            default:
                buf.append("[base-").append(base).append("]");
                break;
        }
        buf.append(this.getIntegerPart());
        if (this.getFractionalPart() != null) {
            buf.append('.').append(this.getFractionalPart());
        }
        if (this.getExponent() != null) {
            buf.append(base > 10 ? 'p' : 'e');
            buf.append(this.getExponent());
        }
        /*
         if (appendFlags(buf, "ui", F_UNSIGNED | F_INT));
         else if (appendFlags(buf, "ul", F_UNSIGNED | F_LONG));
         else if (appendFlags(buf, "ull", F_UNSIGNED | F_LONGLONG));
         else if (appendFlags(buf, "i", F_INT));
         else if (appendFlags(buf, "l", F_LONG));
         else if (appendFlags(buf, "ll", F_LONGLONG));
         else if (appendFlags(buf, "f", F_FLOAT));
         else if (appendFlags(buf, "d", F_DOUBLE));
         */
        return buf.toString();
    }
}
