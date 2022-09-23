package com.flammky.android.medialib.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

object UnsafeBySuspend {

    val ANNOTATION = "com.flammky.android.medialib.errorprone.UnsafeBySuspend"

    val ISSUE = Issue.create(
        id = "UnsafeBySuspend",
        briefDescription = "This function is error prone when called from a `suspend` function",
        explanation = """
				The annotated function is error prone when called from a `suspend` function.
				Please refer to the documentation
			""",
        category = Category.USABILITY,
        priority = 6,
        severity = Severity.INFORMATIONAL,
        implementation = Implementation(LintDetector::class.java, Scope.JAVA_FILE_SCOPE)
    )

    class LintDetector : Detector(), SourceCodeScanner {

        override fun getApplicableUastTypes(): List<Class<out UElement>> {
            return listOf(UMethod::class.java)
        }

        override fun createUastHandler(context: JavaContext): UElementHandler {
            return SuspendVisitor(context)
        }

        class SuspendVisitor(val jContext: JavaContext) : UElementHandler() {

            override fun visitMethod(node: UMethod) {
                if (node.annotations.any { it.hasQualifiedName(ANNOTATION) }) {
                    reportUnsafeBySuspend(node)
                }
            }

            private fun reportUnsafeBySuspend(node: UMethod) {
                jContext.report(
                    issue = ISSUE,
                    scopeClass = node,
                    location = jContext.getNameLocation(node),
                    message = """
					This function should not be called from a `suspend` function.
					Please refer to the documentation
				"""
                )
            }
        }
    }
}


