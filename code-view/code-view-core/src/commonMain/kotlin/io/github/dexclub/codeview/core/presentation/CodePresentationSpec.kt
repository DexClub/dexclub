package io.github.dexclub.codeview.core.presentation

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class CodePresentationSpec(
    public val largeFileThresholdBytes: Int = 512 * 1024,
    public val hugeFileThresholdBytes: Int = 2 * 1024 * 1024,
    public val longLineThresholdChars: Int = 4096,
    public val extremeLineCountThreshold: Int = 100_000,
) {
    init {
        require(largeFileThresholdBytes > 0) {
            "largeFileThresholdBytes 必须大于 0: $largeFileThresholdBytes"
        }
        require(hugeFileThresholdBytes >= largeFileThresholdBytes) {
            "hugeFileThresholdBytes 不能小于 largeFileThresholdBytes: huge=$hugeFileThresholdBytes, large=$largeFileThresholdBytes"
        }
        require(longLineThresholdChars > 0) {
            "longLineThresholdChars 必须大于 0: $longLineThresholdChars"
        }
        require(extremeLineCountThreshold > 0) {
            "extremeLineCountThreshold 必须大于 0: $extremeLineCountThreshold"
        }
    }
}
