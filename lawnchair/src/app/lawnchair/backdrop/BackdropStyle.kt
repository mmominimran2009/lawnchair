package app.lawnchair.backdrop

enum class BackdropStyle(val prefValue: String) {
    NONE("none"),
    BLUR("blur"),
    LIQUID_GLASS("liquid_glass"),
    ;

    companion object {
        val DEFAULT = NONE

        fun fromPrefValue(value: String): BackdropStyle =
            entries.firstOrNull { it.prefValue == value } ?: DEFAULT
    }
}

