package com.github.fengdai

data class RClass(
        val typeName: String,
        val fieldNames: List<String>
) {
    val kind = R.of(typeName)

    override fun toString(): String {
        return fieldNames.fold("[$kind] $typeName\n") { string, fieldName ->
            string + "    $fieldName\n"
        }
    }

    enum class R {
        anim,
        animator,
        array,
        attr,
        bool,
        color,
        dimen,
        drawable,
        id,
        integer,
        interpolator,
        layout,
        raw,
        string,
        style,
        styleable;

        companion object {
            internal fun of(type: String): R {
                return valueOf(type.substring(type.indexOf("$") + 1, type.length))
            }
        }
    }

    companion object {
        val NAMES = R.values().map { "R$${it.name}" }
    }
}
