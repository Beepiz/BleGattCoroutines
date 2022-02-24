#!/usr/bin/env kotlin

@file:DependsOn("it.krzeminski:github-actions-kotlin-dsl:0.6.1")

import it.krzeminski.githubactions.actions.actions.CheckoutV2
import it.krzeminski.githubactions.domain.RunnerType.UbuntuLatest
import it.krzeminski.githubactions.domain.triggers.Push
import it.krzeminski.githubactions.domain.triggers.WorkflowDispatch
import it.krzeminski.githubactions.dsl.workflow
import it.krzeminski.githubactions.yaml.writeToFile
import java.nio.file.Paths

val workflow = workflow(
    name = "Test workflow",
    on = listOf(WorkflowDispatch()),
    sourceFile = Paths.get(".github/workflows/run-gradle-help.main.kts"),
    targetFile = Paths.get(".github/workflows/run-gradle-help.yml")
) {
    job(name = "test_job", runsOn = UbuntuLatest) {
        uses(name = "Check out", action = CheckoutV2())
        run(name = "Run gradle help", command = "./gradlew help")
    }
}

workflow.writeToFile()
