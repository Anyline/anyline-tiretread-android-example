package io.anyline.tiretread.demo.common

class StringUtils {
    companion object {
        /**
         * Convert first letter to upper case.
         *
         * @param str for making the first letter upper case.
         * @return corrected name with upper case.
         */
        fun capitalize(str: String?): String? {
            if (str == null || str.isEmpty()) {
                return ""
            }
            val first = str[0]
            return if (Character.isUpperCase(first)) {
                str
            } else {
                first.uppercaseChar().toString() + str.substring(1)
            }
        }
    }
}