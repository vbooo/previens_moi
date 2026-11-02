package fr.previensmoi.feature.common.presentation

import fr.previensmoi.domain.model.Proposal
import fr.previensmoi.domain.model.region
import java.util.Locale

/** Hide only standalone country summaries already represented by the labelled region field.
 * A compound criterion, such as delivery in France under a price ceiling, must remain intact. */
fun criteriaForDisplay(plan: Proposal): List<String> {
    val region = plan.draft.region.trim()
    if (region.isEmpty()) return plan.criteria
    fun normalized(value: String) = value.trim().trimEnd('.').lowercase(Locale.ROOT)
    val redundant = listOf(
        "Région spécifiée : $region", "Specified region: $region",
        "Le pays de surveillance est $region", "Le pays de surveillance est la $region",
        "Le pays de surveillance est le $region", "The monitoring country is $region",
    ).map(::normalized).toSet()
    return plan.criteria.filterNot { normalized(it) in redundant }
}
