package com.github.cplexopl.error

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.util.Consumer
import java.awt.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class OplErrorReportSubmitter : ErrorReportSubmitter() {

    override fun getReportActionText(): String = com.github.cplexopl.OplBundle.message("error.submitter.reportAction")

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        val event = events.firstOrNull()
        val throwableText = event?.throwableText ?: "No stacktrace available"
        val message = event?.message ?: "Unhandled Plugin Exception"

        val title = URLEncoder.encode("[Bug]: $message", StandardCharsets.UTF_8.name())
        val bodyText = """
            ### Description
            ${additionalInfo ?: "No additional information provided."}

            ### Stacktrace
            ```
            $throwableText
            ```
        """.trimIndent()

        val truncatedBody = if (bodyText.length > 4000) bodyText.substring(0, 4000) + "\n... (truncated)" else bodyText
        val body = URLEncoder.encode(truncatedBody, StandardCharsets.UTF_8.name())
        val url = "https://github.com/JAANULO/CPLEX-Plugin/issues/new?title=$title&body=$body"

        BrowserUtil.browse(url)
        consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
        return true
    }
}
