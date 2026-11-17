package com.hdn.adsmodule.model

/**
 * Margin quanh banner (đơn vị dp). Adaptive size co theo [start] + [end].
 * Ví dụ: BannerMargin(start = 12, top = 8, end = 12), BannerMargin.symmetric(horizontal = 16).
 */
data class BannerMargin(
    val start: Int = 0,
    val top: Int = 0,
    val end: Int = 0,
    val bottom: Int = 0,
) {
    val isEmpty: Boolean get() = start == 0 && top == 0 && end == 0 && bottom == 0

    companion object {
        @JvmField
        val NONE = BannerMargin()

        @JvmStatic
        fun all(value: Int) = BannerMargin(value, value, value, value)

        @JvmStatic
        @JvmOverloads
        fun symmetric(horizontal: Int = 0, vertical: Int = 0) =
            BannerMargin(start = horizontal, top = vertical, end = horizontal, bottom = vertical)
    }
}
