package com.tfg.kaifit_pal.utilities;

public class DataParser {

    private DataParser() {
        throw new IllegalStateException("Utility class");
    }

    public static double parseDoubleUtility(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int parseIntUtility(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}