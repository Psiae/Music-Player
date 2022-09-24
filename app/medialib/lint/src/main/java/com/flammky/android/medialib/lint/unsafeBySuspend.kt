package com.flammky.android.medialib.lint

import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinUParameter

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

        override fun applicableAnnotations(): List<String> {
            return listOf(ANNOTATION)
        }

        override fun visitAnnotationUsage(
            context: JavaContext,
            element: UElement,
            annotationInfo: AnnotationInfo,
            usageInfo: AnnotationUsageInfo
        ) {

            element.getParentOfType<UMethod>()?.let { function ->
                if (function.uAnnotations.any { it.qualifiedName == ANNOTATION }) return
            }

            // TODO: Find KtModifierListOwner that has `suspend` modifier available

            val lambda = element.getParentOfType<ULambdaExpression>()
            if (lambda is ULambdaExpression
                && lambda.parameters
                    .any { param -> param is KotlinUParameter
                            && param.type.equalsToText("kotlinx.coroutines.CoroutineScope")
                    }
            ) {
                context.report(
                    issue = ISSUE,
                    scope = usageInfo.usage,
                    location = context.getNameLocation(usageInfo.usage),
                    message = """
					        This function should not be called from a `suspend` function.
					        Please refer to the documentation
				        """
                )
            }
        }
    }
}


