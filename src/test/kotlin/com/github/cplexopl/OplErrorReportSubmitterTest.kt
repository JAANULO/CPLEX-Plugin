package com.github.cplexopl

import com.github.cplexopl.error.OplErrorReportSubmitter
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

class OplErrorReportSubmitterTest : BasePlatformTestCase() {

    fun testErrorReportSubmitterActionText() {
        val submitter = OplErrorReportSubmitter()
        assertNotNull("ErrorReportSubmitter nie powinien być null", submitter)
        assertEquals(com.github.cplexopl.OplBundle.message("error.submitter.reportAction"), submitter.reportActionText)
    }
}
