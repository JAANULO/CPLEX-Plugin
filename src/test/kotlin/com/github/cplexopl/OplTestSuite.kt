package com.github.cplexopl

import com.github.cplexopl.settings.OplSettingsTest
import com.github.cplexopl.utils.CplexPathFinderTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    OplParsingTest::class,
    OplFormattingTest::class,
    OplCommenterTest::class,
    OplStructureViewTest::class,
    OplLiveTemplatesTest::class,
    OplReferenceTest::class,
    OplHighlightingTest::class,
    OplConsoleFilterTest::class,
    OplConsoleFilterPerformanceTest::class,
    OplAnnotatorPerformanceTest::class,
    OplRunConfigurationTest::class,
    OplSettingsTest::class,
    OplCompletionTest::class,
    CplexPathFinderTest::class,
    ParserDebug::class
)
class OplTestSuite
