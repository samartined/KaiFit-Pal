package com.tfg.kaifit_pal.utilities;

/**
 * Utility class for parsing data.
 * This class cannot be instantiated.
 */
public class DataParser {

    /**
     * Private constructor to prevent instantiation of the utility class.
     * Throws an exception if called.
     */
    private DataParser() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Parses a string into a double.
     * If the string cannot be parsed, returns 0.
     *
     * @param str The string to parse.
     * @return The parsed double, or 0 if the string cannot be parsed.
     */
    public static double parseDoubleUtility(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parses a string into an integer.
     * If the string cannot be parsed, returns 0.
     *
     * @param str The string to parse.
     * @return The parsed integer, or 0 if the string cannot be parsed.
     */
    public static int parseIntUtility(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}