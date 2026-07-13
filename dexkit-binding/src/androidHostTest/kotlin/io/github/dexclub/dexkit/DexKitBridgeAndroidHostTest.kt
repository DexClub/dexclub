package io.github.dexclub.dexkit

import io.github.dexclub.dexkit.query.MatchType
import io.github.dexclub.dexkit.query.OpCodeMatchType
import io.github.dexclub.dexkit.query.RetentionPolicyType
import io.github.dexclub.dexkit.query.StringMatchType
import io.github.dexclub.dexkit.query.TargetElementType
import io.github.dexclub.dexkit.query.UsingType
import org.luckypray.dexkit.query.enums.MatchType as NativeMatchType
import org.luckypray.dexkit.query.enums.OpCodeMatchType as NativeOpCodeMatchType
import org.luckypray.dexkit.query.enums.RetentionPolicyType as NativeRetentionPolicyType
import org.luckypray.dexkit.query.enums.StringMatchType as NativeStringMatchType
import org.luckypray.dexkit.query.enums.TargetElementType as NativeTargetElementType
import org.luckypray.dexkit.query.enums.UsingType as NativeUsingType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DexKitBridgeAndroidHostTest {
    @Test
    fun androidBridgeRejectsEmptyInputsBeforeCreatingNativeDelegate() {
        assertFailsWith<IllegalArgumentException> { DexKitBridge(emptyList()) }
        assertFailsWith<IllegalArgumentException> { DexKitBridge("") }
        assertFailsWith<IllegalArgumentException> { DexKitBridge(emptyArray()) }
    }

    @Test
    fun sharedEnumMappersTargetAndroidCoreTypes() {
        assertEquals(NativeStringMatchType.Equals, StringMatchType.Equals.toNative())
        assertEquals(NativeMatchType.Contains, MatchType.Contains.toNative())
        assertEquals(NativeOpCodeMatchType.StartsWith, OpCodeMatchType.StartsWith.toNative())
        assertEquals(NativeUsingType.Write, UsingType.Write.toNative())
        assertEquals(NativeRetentionPolicyType.Runtime, RetentionPolicyType.Runtime.toNative())
        assertEquals(NativeTargetElementType.TypeUse, TargetElementType.TypeUse.toNative())
    }
}
