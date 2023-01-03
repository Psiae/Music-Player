package com.flammky.android.medialib.lint

import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class IssueRegistry : com.android.tools.lint.client.api.IssueRegistry() {
	override val issues: List<Issue> = listOf(
		UnsafeBySuspend.ISSUE,
	)
	override val api: Int = CURRENT_API
}
