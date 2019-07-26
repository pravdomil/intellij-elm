package org.frawa.elmtest.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentDescriptorReusePolicy

class ElmTestProgramRunner : GenericProgramRunner<RunnerSettings>() {

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val result = state.execute(environment.executor, this)
        return RunContentBuilder(result!!, environment).showRunContent(environment.contentToReuse).makeReusable()
    }

    @Suppress("UnstableApiUsage")
    private fun RunContentDescriptor.makeReusable(): RunContentDescriptor {
        reusePolicy = object : RunContentDescriptorReusePolicy() {
            override fun canBeReusedBy(newDescriptor: RunContentDescriptor) = true
        }
        return this
    }

    override fun getRunnerId() = "ELM_TEST_PROGRAM_RUNNER"

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
            DefaultRunExecutor.EXECUTOR_ID == executorId && profile is ElmTestRunConfiguration
}
