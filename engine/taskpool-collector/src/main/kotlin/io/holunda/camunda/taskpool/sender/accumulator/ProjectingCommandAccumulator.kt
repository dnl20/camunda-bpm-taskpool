package io.holunda.camunda.taskpool.sender.accumulator

import io.holunda.camunda.taskpool.api.business.WithCorrelations
import io.holunda.camunda.taskpool.api.task.*
import io.holunda.camunda.variable.serializer.serialize
import kotlin.reflect.KClass


/**
 * sorts commands by their order id and project attribute to one command
 */
class ProjectingCommandAccumulator : CommandAccumulator {

  private val sorter: CommandAccumulator = SortingCommandAccumulator()

  override fun invoke(taskCommands: List<EngineTaskCommand>): List<EngineTaskCommand> =
    if (taskCommands.size > 1) {
      // only if there are at least two commands, there is something to accumulate at all
      val sorted = sorter.invoke(taskCommands)
      listOf(collectCommandProperties(sorted.first(), sorted.subList(1, sorted.size)))
    } else {
      // otherwise just return the empty or singleton list
      taskCommands
    }.map(::handlePayloadAndCorrelations)


  @Suppress("UNCHECKED_CAST")
  private fun <T : WithTaskId> collectCommandProperties(command: T, details: List<WithTaskId>): T = projectProperties(
    original = command,
    details = details,
    /**
     * Configuration to change default behavior of replacing the property.
     * For delta-commands (add/delete candidate groups/users), the lists has to be adjusted
     */
    propertyOperationConfig = mutableMapOf<KClass<out Any>, PropertyOperation>().apply {
      // a delete command should remove one element from candidate user
      put(DeleteCandidateUsersCommand::class) { map, key, value ->
        if (key == DeleteCandidateUsersCommand::candidateUsers.name) {
          val candidateUsers = (map[key] as Collection<String>).minus(value as Collection<String>)
          map[key] = candidateUsers
        }
      }
      // a delete command should remove one element from candidate group
      put(DeleteCandidateGroupsCommand::class) { map, key, value ->
        if (key == DeleteCandidateGroupsCommand::candidateGroups.name) {
          val candidateGroups = (map[key] as Collection<String>).minus(value as Collection<String>)
          map[key] = candidateGroups
        }
      }
      // add command should add one element to candidate user
      put(AddCandidateUsersCommand::class) { map, key, value ->
        if (key == AddCandidateUsersCommand::candidateUsers.name) {
          val candidateUsers = (map[key] as Collection<String>).plus(value as Collection<String>)
          map[key] = candidateUsers
        }
      }
      // add command should add one element to candidate group
      put(AddCandidateGroupsCommand::class) { map, key, value ->
        if (key == AddCandidateGroupsCommand::candidateGroups.name) {
          val candidateGroups = (map[key] as Collection<String>).plus(value as Collection<String>)
          map[key] = candidateGroups
        }
      }
    },
    ignoredProperties = listOf(
      WithTaskId::id.name,
      CamundaTaskEventType::eventName.name,
      EngineTaskCommand::order.name,
      // handled separately
      WithPayload::payload.name,
      WithCorrelations::correlations.name
    ),
    projectionErrorDetector = EngineCommandProjectionErrorDetector
  )

  /**
   * Handle payload and correlations and serialize using provided object mapper (e.g. to JSON)
   */
  @Suppress("UNCHECKED_CAST")
  fun <T : WithTaskId> handlePayloadAndCorrelations(command: T): T =
    // handle payload and correlations
    if (command is CreateTaskCommand)
      command.copy(payload = serialize(command.payload)) as T
    else
      command
}

/**
 * If detail is AddCandidateUsersCommand or UpdateAttributeTaskCommand, no error reporting should be done. => False will be returned.
 */
object EngineCommandProjectionErrorDetector: ProjectionErrorDetector {

  override fun shouldReportError(original: Any, detail: Any): Boolean {
    return when(detail) {
      is AddCandidateUsersCommand,
      is UpdateAttributeTaskCommand -> false
      else -> true
    }
  }
}
