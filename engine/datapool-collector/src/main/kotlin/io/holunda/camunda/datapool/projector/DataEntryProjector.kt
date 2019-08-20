package io.holunda.camunda.datapool.projector

import io.holunda.camunda.taskpool.api.business.EntryType
import org.springframework.stereotype.Component

@Component
class DataEntryProjector(private val suppliers: List<DataEntryProjectionSupplier>) {
  fun getProjection(entryType: EntryType) = suppliers.find { it.entryType == entryType }
}
