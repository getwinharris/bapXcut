package com.getwinharris.bapxcut.commands

import com.getwinharris.bapxcut.models.EditOperation
import com.getwinharris.bapxcut.models.VideoProject
import com.getwinharris.bapxcut.models.id

/**
 * Represents a single, isolated state mutation on a VideoProject.
 * Using a command pattern enables robust, multi-level Undo/Redo tracking.
 */
sealed class EditCommand {
    abstract val description: String
    abstract fun execute(project: VideoProject): VideoProject
}

/**
 * Appends a new operation to the project's operation list.
 * Used for repeatable operations like adding text, image overlays, or audio.
 */
class AddOperationCommand(
    private val operation: EditOperation,
    override val description: String = "Add Effect"
) : EditCommand() {
    override fun execute(project: VideoProject): VideoProject {
        return project.addOperation(operation)
    }
}

/**
 * Removes an operation by its unique ID.
 */
class RemoveOperationCommand(
    private val operationId: String,
    override val description: String = "Remove Effect"
) : EditCommand() {
    override fun execute(project: VideoProject): VideoProject {
        val ops = project.operations.filterNot { it.id == operationId }
        return project.copy(operations = ops, lastModifiedAt = System.currentTimeMillis())
    }
}

/**
 * Replaces a unique timeline modifier (like Trim, Crop, Speed, Reverse).
 * If the operation of the same class doesn't exist, it appends it.
 */
class ReplaceUniqueOperationCommand(
    private val newOperation: EditOperation,
    override val description: String = "Modify Timeline"
) : EditCommand() {
    override fun execute(project: VideoProject): VideoProject {
        val ops = project.operations.toMutableList()
        val index = ops.indexOfFirst { it::class == newOperation::class }
        if (index != -1) {
            ops[index] = newOperation
            val iterator = ops.listIterator(index + 1)
            while (iterator.hasNext()) {
                if (iterator.next()::class == newOperation::class) {
                    iterator.remove()
                }
            }
        } else {
            ops.add(newOperation)
        }
        return project.copy(operations = ops, lastModifiedAt = System.currentTimeMillis())
    }
}

/**
 * Modifies an existing operation by ID. 
 * Allows passing a lambda to modify the specific operation.
 */
class UpdateOperationCommand(
    private val operationId: String,
    override val description: String = "Modify Effect",
    private val updateBlock: (EditOperation) -> EditOperation
) : EditCommand() {
    override fun execute(project: VideoProject): VideoProject {
        val ops = project.operations.map { if (it.id == operationId) updateBlock(it) else it }
        return project.copy(operations = ops, lastModifiedAt = System.currentTimeMillis())
    }
}

/**
 * Replaces all operations of a specific class. Used for effects that 
 * might need total replacement, like clearing and setting new subtitles.
 */
class ReplaceAllOfTypeCommand(
    private val operationClass: Class<out EditOperation>,
    private val newOperations: List<EditOperation>,
    override val description: String = "Replace Effects"
) : EditCommand() {
    override fun execute(project: VideoProject): VideoProject {
        val ops = project.operations.filterNot { it::class.java == operationClass }.toMutableList()
        ops.addAll(newOperations)
        return project.copy(operations = ops, lastModifiedAt = System.currentTimeMillis())
    }
}

/**
 * A highly generic command allowing a complete custom mutation of the operations list.
 */
class MutateListCommand(
    override val description: String = "Modify Project",
    private val mutateBlock: (List<EditOperation>) -> List<EditOperation>
) : EditCommand() {
    override fun execute(project: VideoProject): VideoProject {
        val ops = mutateBlock(project.operations)
        return project.copy(operations = ops, lastModifiedAt = System.currentTimeMillis())
    }
}

/**
 * A highly generic command allowing complete custom mutation of the entire VideoProject.
 */
class MutateProjectCommand(
    override val description: String = "Modify Project",
    private val mutateBlock: (VideoProject) -> VideoProject
) : EditCommand() {
    override fun execute(project: VideoProject): VideoProject {
        return mutateBlock(project).copy(lastModifiedAt = System.currentTimeMillis())
    }
}
