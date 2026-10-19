package org.cloudburstmc.server.configuration.codec;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Checked numeric conversions for persisted configuration values.
 */
@UtilityClass
public class ConfigurationNumbers {

    public static int integer(Object value, String path) {
        try {
            return decimal(value, path).intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(path + " must be a whole number in the int range: " + value, e);
        }
    }

    public static long longValue(Object value, String path) {
        try {
            return decimal(value, path).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(path + " must be a whole number in the long range: " + value, e);
        }
    }

    public static float floatValue(Object value, String path) {
        BigDecimal number = decimal(value, path);
        float result = ((Number) value).floatValue();

        if (!Float.isFinite(result) || result == 0 && number.signum() != 0) {
            throw new IllegalArgumentException(path + " is outside the finite float range: " + value);
        }

        return result;
    }

    public static double doubleValue(Object value, String path) {
        BigDecimal number = decimal(value, path);
        double result = ((Number) value).doubleValue();

        if (!Double.isFinite(result) || result == 0 && number.signum() != 0) {
            throw new IllegalArgumentException(path + " is outside the finite double range: " + value);
        }

        return result;
    }

    private static BigDecimal decimal(Object value, String path) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(path + " must be a number: " + value);
        }

        if (number instanceof BigDecimal decimal) {
            return decimal;
        }

        if (number instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }

        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(path + " must be a finite number: " + value, e);
        }
    }
}
