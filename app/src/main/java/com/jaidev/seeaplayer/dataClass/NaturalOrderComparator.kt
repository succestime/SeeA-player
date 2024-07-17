package com.jaidev.seeaplayer.dataClass

class NaturalOrderComparator : Comparator<String> {
    override fun compare(o1: String, o2: String): Int {
        return compareNatural(o1.lowercase(), o2.lowercase())
    }

    private fun compareNatural(a: String, b: String): Int {
        var ia = 0
        var ib = 0
        var nza: Int
        var nzb: Int
        val la = a.length
        val lb = b.length
        while (ia < la && ib < lb) {
            nza = zeroCount(a, ia)
            nzb = zeroCount(b, ib)
            ia += nza
            ib += nzb
            if (ia == la && ib == lb) {
                return nza - nzb
            }
            if (ia == la) {
                return -1
            }
            if (ib == lb) {
                return 1
            }
            val ca = a[ia]
            val cb = b[ib]
            var result: Int
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                result = compareRight(a.substring(ia), b.substring(ib))
                if (result != 0) {
                    return result
                }
            }
            if (ca != cb) {
                return ca.compareTo(cb)
            }
            ia++
            ib++
        }
        return la - lb
    }

    private fun compareRight(a: String, b: String): Int {
        var bias = 0
        var ia = 0
        var ib = 0
        val la = a.length
        val lb = b.length
        while (true) {
            val ca = if (ia < la) a[ia] else 0.toChar()
            val cb = if (ib < lb) b[ib] else 0.toChar()
            if (!Character.isDigit(ca) && !Character.isDigit(cb)) {
                return bias
            }
            if (!Character.isDigit(ca)) {
                return -1
            }
            if (!Character.isDigit(cb)) {
                return 1
            }
            if (ca == 0.toChar() && cb == 0.toChar()) {
                return bias
            }
            if (ca < cb) {
                if (bias == 0) {
                    bias = -1
                }
            } else if (ca > cb) {
                if (bias == 0) {
                    bias = 1
                }
            }
            ia++
            ib++
        }
    }

    private fun zeroCount(s: String, i: Int): Int {
        var n = 0
        var index = i
        while (index < s.length && s[index] == '0') {
            n++
            index++
        }
        return n
    }
}
