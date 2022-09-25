package com.flammky.android.medialib.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.lang.jvm.JvmModifier
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

object ImplSuffix {

    val ISSUE = Issue.create(
        "ImplSuffix",
        briefDescription = "Avoid Impl Suffix",
        explanation = """
          `Impl Suffix` should not be used when extending abstract class or implementing interface.
          If possible just create concrete class altogether without involving `abstract` or `interface` modifier even for clarity reasons.
          Otherwise use `Default` prefix if the class is open or otherwise `Real` prefix
        """,
        category = Category.USABILITY,
        priority = 6,
        severity = Severity.WARNING,
        implementation = Implementation(LintDetector::class.java, Scope.JAVA_FILE_SCOPE)
    )

    class LintDetector : Detector(), SourceCodeScanner {

        override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UClass::class.java)
        override fun createUastHandler(context: JavaContext): UElementHandler = UastHandler(context)

        class UastHandler(val context: JavaContext) : UElementHandler() {

            override fun visitClass(node: UClass) {
                val className = node.name ?: return

                if (className.endsWith("Impl")) {


                    val noSuffix = className.removeSuffix("Impl")
                    val interfaces = node.interfaces

                    if (interfaces.any { it.name is String && noSuffix == it.name }) {
                        return report(context, node)
                    }

                    val superClass = node.javaPsi.superClass ?: return

                    if (superClass.hasModifier(JvmModifier.ABSTRACT) && noSuffix == superClass.name) {
                        return report(context, node)
                    }
                }
            }

            private fun report(context: JavaContext, node: UClass) {
                context.report(
                    issue = ISSUE,
                    location = context.getNameLocation(node),
                    message = ISSUE.getExplanation(TextFormat.TEXT)
                )
            }

            // TODO
            private fun quickFixData(context: JavaContext, node: UClass) {
                LintFix.create()
            }
        }
    }
}