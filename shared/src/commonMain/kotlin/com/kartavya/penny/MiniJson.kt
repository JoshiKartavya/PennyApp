package com.kartavya.penny

class MiniJson(private val json: String) {
    private var index = 0

    fun parse(): Any? {
        skipWhitespace()
        if (index >= json.length) return null
        return when (val char = json[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> {
                if (char == '-' || char.isDigit()) {
                    parseNumber()
                } else {
                    index++
                    null
                }
            }
        }
    }

    private fun skipWhitespace() {
        while (index < json.length && (json[index] == ' ' || json[index] == '\n' || json[index] == '\r' || json[index] == '\t')) {
            index++
        }
    }

    private fun parseObject(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        index++ // skip '{'
        while (true) {
            skipWhitespace()
            if (index >= json.length || json[index] == '}') {
                if (index < json.length) index++ // skip '}'
                break
            }
            val key = parseString()
            skipWhitespace()
            if (index < json.length && json[index] == ':') {
                index++ // skip ':'
            }
            val value = parse()
            map[key] = value
            skipWhitespace()
            if (index < json.length && json[index] == ',') {
                index++ // skip ','
            } else if (index >= json.length || json[index] == '}') {
                if (index < json.length) index++ // skip '}'
                break
            }
        }
        return map
    }

    private fun parseArray(): List<Any?> {
        val list = mutableListOf<Any?>()
        index++ // skip '['
        while (true) {
            skipWhitespace()
            if (index >= json.length || json[index] == ']') {
                if (index < json.length) index++ // skip ']'
                break
            }
            val value = parse()
            list.add(value)
            skipWhitespace()
            if (index < json.length && json[index] == ',') {
                index++ // skip ','
            } else if (index >= json.length || json[index] == ']') {
                if (index < json.length) index++ // skip ']'
                break
            }
        }
        return list
    }

    private fun parseString(): String {
        index++ // skip starting '"'
        val sb = StringBuilder()
        while (index < json.length) {
            val char = json[index]
            if (char == '"') {
                index++ // skip ending '"'
                break
            } else if (char == '\\') {
                index++
                if (index < json.length) {
                    val esc = json[index]
                    when (esc) {
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        '\\' -> sb.append('\\')
                        '"' -> sb.append('"')
                        '/' -> sb.append('/')
                        else -> sb.append(esc)
                    }
                    index++
                }
            } else {
                sb.append(char)
                index++
            }
        }
        return sb.toString()
    }

    private fun parseBoolean(): Boolean {
        return if (json.startsWith("true", index)) {
            index += 4
            true
        } else {
            index += 5
            false
        }
    }

    private fun parseNull(): Any? {
        index += 4 // skip 'null'
        return null
    }

    private fun parseNumber(): Number {
        val start = index
        if (json[index] == '-') index++
        while (index < json.length && (json[index].isDigit() || json[index] == '.' || json[index] == 'e' || json[index] == 'E' || json[index] == '+' || json[index] == '-')) {
            index++
        }
        val numStr = json.substring(start, index)
        return if (numStr.contains('.')) {
            numStr.toDoubleOrNull() ?: 0.0
        } else {
            numStr.toLongOrNull() ?: numStr.toIntOrNull() ?: 0
        }
    }
}
