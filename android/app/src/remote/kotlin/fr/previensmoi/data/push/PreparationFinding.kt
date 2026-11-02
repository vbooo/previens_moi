package fr.previensmoi.data.push

import fr.previensmoi.domain.model.Finding
import fr.previensmoi.domain.model.Watch
import fr.previensmoi.domain.model.WatchStatus

/** A ready notice may represent a first result, including a once-only watch already completed. */
internal fun preparationFinding(watch: Watch, findings: List<Finding>): Finding? {
    if (watch.status !in setOf(WatchStatus.Active, WatchStatus.Completed)) return null
    return findings.filter {
        it.watchId == watch.id && it.planId == watch.plan.id &&
            it.planVersion == watch.plan.version && it.noveltyBasis == "initial"
    }.maxByOrNull { it.observedAt }
}
