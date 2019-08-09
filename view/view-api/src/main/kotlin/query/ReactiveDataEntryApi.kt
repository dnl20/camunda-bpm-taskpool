package io.holunda.camunda.taskpool.view.query

import io.holunda.camunda.taskpool.view.query.data.DataEntriesForUserQuery
import io.holunda.camunda.taskpool.view.query.data.DataEntriesQueryResult
import io.holunda.camunda.taskpool.view.query.data.DataEntryForIdentityQuery
import java.util.concurrent.CompletableFuture

/**
 * Defines an API interface for Data Entry Queries.
 */
interface ReactiveDataEntryApi {

  fun query(query: DataEntryForIdentityQuery): CompletableFuture<DataEntriesQueryResult>

  fun query(query: DataEntriesForUserQuery): CompletableFuture<DataEntriesQueryResult>
}
