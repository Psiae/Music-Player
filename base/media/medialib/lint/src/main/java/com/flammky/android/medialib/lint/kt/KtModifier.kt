package com.flammky.android.medialib.lint.kt

import org.jetbrains.kotlin.lexer.KtModifierKeywordToken


object KtModifiers {

    val memberFunction = object : KtModifierTokens.MemberFunctions {

        // Inheritance
        override val ABSTRACT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("abstract")
        override val OPEN_KEYWORD = KtModifierKeywordToken.softKeywordModifier("open")
        override val OVERRIDE_KEYWORD = KtModifierKeywordToken.softKeywordModifier("override")
        override val FINAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("final")

        // Visibility
        override val PRIVATE_KEYWORD = KtModifierKeywordToken.softKeywordModifier("private")
        override val PUBLIC_KEYWORD = KtModifierKeywordToken.softKeywordModifier("public")
        override val INTERNAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("internal")
        override val PROTECTED_KEYWORD = KtModifierKeywordToken.softKeywordModifier("protected")
        override val DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD


        //KMM
        override val IMPL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("impl")
        override val EXPECT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("expect")
        override val ACTUAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("actual")

        //Other
        override val INLINE_KEYWORD = KtModifierKeywordToken.softKeywordModifier("inline")
        override val INFIX = KtModifierKeywordToken.softKeywordModifier("infix")
        override val OPERATOR_KEYWORD = KtModifierKeywordToken.softKeywordModifier("operator")
        override val REIFIED_KEYWORD = KtModifierKeywordToken.softKeywordModifier("reified")
        override val SUSPEND = KtModifierKeywordToken.softKeywordModifier("suspend")
    }
}

object KtModifierTokens {

    interface MemberFunctions {

        // inheritance
        val ABSTRACT_KEYWORD: KtModifierKeywordToken
        val OPEN_KEYWORD: KtModifierKeywordToken
        val OVERRIDE_KEYWORD: KtModifierKeywordToken
        val FINAL_KEYWORD: KtModifierKeywordToken

        // visibility
        val DEFAULT_VISIBILITY_KEYWORD: KtModifierKeywordToken
        val PRIVATE_KEYWORD: KtModifierKeywordToken
        val PUBLIC_KEYWORD: KtModifierKeywordToken
        val INTERNAL_KEYWORD: KtModifierKeywordToken
        val PROTECTED_KEYWORD: KtModifierKeywordToken

        // KMM
        val IMPL_KEYWORD: KtModifierKeywordToken
        val EXPECT_KEYWORD: KtModifierKeywordToken
        val ACTUAL_KEYWORD: KtModifierKeywordToken

        // other
        val INLINE_KEYWORD: KtModifierKeywordToken
        val INFIX: KtModifierKeywordToken
        val OPERATOR_KEYWORD: KtModifierKeywordToken
        val REIFIED_KEYWORD: KtModifierKeywordToken
        val SUSPEND: KtModifierKeywordToken
    }
}