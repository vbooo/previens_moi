package fr.previensmoi.feature.common.presentation

import fr.previensmoi.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanContentTest {
    private fun plan(region: String, vararg criteria: String) = Proposal(
        "p", 1, RequestDraft(answers = mapOf("region" to region)), Long.MAX_VALUE,
        criteria = criteria.toList(),
    )
    @Test fun `country summary is represented once without discarding compound criteria`() {
        val meaningful = "Promotions disponibles en France sous 400 euros"
        assertEquals(listOf(meaningful), criteriaForDisplay(plan("France",
            "Le pays de surveillance est la France.", meaningful, "Région spécifiée : France")))
        assertEquals(listOf("Prices in France below 400 euros"), criteriaForDisplay(plan("France",
            "Specified region: France", "The monitoring country is France", "Prices in France below 400 euros")))
    }
    @Test fun `different or missing region never removes a criterion`() {
        val text = "Le pays de surveillance est la France"
        assertEquals(listOf(text), criteriaForDisplay(plan("Belgique", text)))
        assertEquals(listOf(text), criteriaForDisplay(plan("", text)))
    }
}
