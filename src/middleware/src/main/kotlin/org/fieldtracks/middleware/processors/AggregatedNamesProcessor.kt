package org.fieldtracks.middleware.processors

import org.fieldtracks.middleware.*

class AggregatedNamesProcessor(): BatchProcessor<AggregatedNames, StoneName> {

    var currentTable: AggregatedNames? = AggregatedNames(names = ArrayList())

    override fun processBatch(prevResult: AggregatedNames?, messages: List<StoneName>): AggregatedNames? {
        if(currentTable!!.names.isEmpty() && prevResult != null && prevResult.names.isNotEmpty()) {
            currentTable = prevResult
        }
        val nameMap = currentTable!!.names.associateBy {it.id}.toMutableMap()
        messages.forEach { nameMap[it.id] = it }
        currentTable = AggregatedNames(ArrayList(nameMap.values))
        return currentTable
    }

}
