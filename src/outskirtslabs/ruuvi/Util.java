package outskirtslabs.ruuvi;

class Util {
    // replace this function with org.apache.commons.lang3.ArrayUtils
    public static byte[] toPrimitive(Byte[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return new byte[0];
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].byteValue();
        }
        return result;
    }


    /**
     * Converts a hex sequence to raw bytes
     *
     * @param hex the hex string to parse
     * @return a byte-array containing the byte-values of the hex string
     */
    public static byte[] hexToBytes(String hex) {
        String s = hex.replaceAll(" ", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len - 1 /*-1 because we'll read two at a time*/; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Use to read line from hcidump and confirm if Mac address should be present
     *
     * @param line a space separated string of hex, first six decimals are
     *             assumed to be part of the MAC address, rest of the line is discarded
     * @return true if Mac address should be found, false if Mac address should not be present
     */
    public static boolean hasMacAddress(String line) {
        return line != null
                && line.startsWith("> ")
                && line.trim().length() > 37; //
    }

    /**
     * Gets a MAC address from a space-separated hex string
     *
     * @param line a space separated string of hex, this string is checked by  {@link #hasMacAddress(String)}
     * @return the MAC address, without spaces
     */
    public static String getMacFromLine(String line) {
        if (!hasMacAddress(line)) {
            return null;
        }

        String[] terms = line.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 13; i >= 8; i--) {
            sb.append(terms[i]);
        }
        return sb.toString();
    }
}
