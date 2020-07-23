/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.data.source.remote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Result.Error
import com.example.android.architecture.blueprints.todoapp.data.Result.Success
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.util.httpClientAndroid
import com.google.gson.annotations.SerializedName
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Implementation of the data source that adds a latency simulating network.
 */
@KtorExperimentalAPI
object TasksRemoteDataSource : TasksDataSource {

    private const val SERVICE_LATENCY_IN_MILLIS = 2000L

    private var TASKS_SERVICE_DATA = LinkedHashMap<String, Task>(2)

    init {
        addTask("Build tower in Pisa", "Ground looks good, no foundation work required.")
        addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!")
    }

    private const val SERVER_URL = "https://ktor-jib-57lqbht3qa-de.a.run.app"

    private val observableTasks = MutableLiveData<Result<List<Task>>>()

    override suspend fun refreshTasks() {
        observableTasks.value = getTasks()
    }

    override suspend fun refreshTask(taskId: String) {
        refreshTasks()
    }

    override fun observeTasks(): LiveData<Result<List<Task>>> {
        return observableTasks
    }

    override fun observeTask(taskId: String): LiveData<Result<Task>> {
        return observableTasks.map { tasks ->
            when (tasks) {
                is Result.Loading -> Result.Loading
                is Error -> Error(tasks.exception)
                is Success -> {
                    val task = tasks.data.firstOrNull() { it.id == taskId }
                        ?: return@map Error(Exception("Not found"))
                    Success(task)
                }
            }
        }
    }

    override suspend fun getTasks(): Result<List<Task>> {
        val taskResponse =
                httpClientAndroid.get<TaskListResponse>("$SERVER_URL/api/tasks")

        val tasks = taskResponse.data.map {
            Task(title = it.title, description = it.description, isCompleted = it.completed, id = it.id)
        }

        return Success(tasks)
    }

    override suspend fun getTask(taskId: String): Result<Task> {
        // Simulate network by delaying the execution.
        delay(SERVICE_LATENCY_IN_MILLIS)
        TASKS_SERVICE_DATA[taskId]?.let {
            return Success(it)
        }
        return Error(Exception("Task not found"))
    }

    private fun addTask(title: String, description: String) {
        val newTask = Task(title, description)
        TASKS_SERVICE_DATA[newTask.id] = newTask
    }

    override suspend fun saveTask(task: Task) {

        try {
            httpClientAndroid.post<Task>("$SERVER_URL/api/tasks") {
                contentType(ContentType.Application.Json)
                body = TaskRequest(title = task.title, description = task.description)
            }
        } catch (e: NoTransformationFoundException) {
            Timber.e("Error while saveTask:${e.message}")
        }
    }

    override suspend fun completeTask(task: Task) {

        httpClientAndroid.patch<Unit>("$SERVER_URL/api/tasks/${task.id}/complete")

    }

    override suspend fun completeTask(taskId: String) {

        httpClientAndroid.patch<Unit>("$SERVER_URL/api/tasks/$taskId/complete")

    }

    override suspend fun activateTask(task: Task) {
        // Not required for the remote data source
    }

    override suspend fun activateTask(taskId: String) {
        // Not required for the remote data source
    }

    override suspend fun clearCompletedTasks() {
        TASKS_SERVICE_DATA = TASKS_SERVICE_DATA.filterValues {
            !it.isCompleted
        } as LinkedHashMap<String, Task>
    }

    override suspend fun deleteAllTasks() {
        TASKS_SERVICE_DATA.clear()
    }

    override suspend fun deleteTask(taskId: String) {

        httpClientAndroid.delete<Unit>("$SERVER_URL/api/tasks/$taskId/delete")

    }
}

data class TaskRequest(
        val title: String,
        val description: String
)

data class TaskListResponse(
        @SerializedName("data") val data: List<TaskResponse>
)

data class TaskResponse(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("description") val description: String,
        @SerializedName("completed") val completed: Boolean
)