package com.hdn.theme.testmoduleads

data class ConfigIdRemote (
    val banner: List<String> = listOf(),
    val bannerCollapse: List<String> = listOf(""),
    val interstitial: List<String> = listOf(""),
    val rewarded: List<String> = listOf(""),
    val native: List<String> = listOf(""),
    val appOpen: List<String> = listOf(""),

    val interSplash: List<String> = listOf(""),
    val nativeLang1: List<String> = listOf(""),
    val nativeLang2: List<String> = listOf(""),
    val nativeLang3: List<String> = listOf(""),
    val nativeCollapse: List<String> = listOf(""),
    val nativeSplash: List<String> = listOf(""),
    val nativeFullSplash: List<String> = listOf(""),

    val nativeDialogFull: List<String> = listOf(""),
    val nativeDialogFull2: List<String> = listOf(""),

    val nativePermission: List<String> = listOf(""),
    val nativeObFull: List<String> = listOf(""),
    val nativeOb1: List<String> = listOf(""),
    val nativeOb2: List<String> = listOf(""),
    val nativeOb3: List<String> = listOf(""),
)
