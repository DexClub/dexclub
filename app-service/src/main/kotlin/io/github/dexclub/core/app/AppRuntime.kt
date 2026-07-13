package io.github.dexclub.core.app

import io.github.dexclub.core.api.shared.Services
import io.github.dexclub.core.app.session.DexContextRegistry
import io.github.dexclub.core.app.session.TargetSessionRuntime
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.session.WorkspaceRuntime

class AppRuntime internal constructor(
    val services: Services,
    val workspaceRuntime: WorkspaceRuntime,
    val appUseCases: AppUseCases,
) : AutoCloseable {
    constructor(services: Services) : this(
        services = services,
        workspaceRuntime = WorkspaceRuntime(services.workspace),
        appUseCases = AppUseCases(services),
    )

    override fun close() {
        services.closeDexService()
    }
}

fun createAppRuntime(
    services: Services = createDefaultAppServices(),
): AppRuntime = AppRuntime(services)

class SessionAppRuntime internal constructor(
    val services: Services,
    val sessionService: TargetSessionService,
    val appUseCases: AppUseCases,
    val sessionRuntime: TargetSessionRuntime,
) : AutoCloseable {
    constructor(
        services: Services,
        sessionService: TargetSessionService = TargetSessionService(),
    ) : this(
        services = services,
        sessionService = sessionService,
        appUseCases = AppUseCases(
            services = services,
            sessionService = sessionService,
        ),
        sessionRuntime = TargetSessionRuntime(
            workspaceService = services.workspace,
            sessionService = sessionService,
            dexContextRegistry = DexContextRegistry(services.dex::releaseDexContext),
        ),
    )

    override fun close() {
        sessionRuntime.close()
        services.closeDexService()
    }
}

fun createSessionAppRuntime(
    services: Services = createDefaultAppServices(),
    sessionService: TargetSessionService = TargetSessionService(),
): SessionAppRuntime = SessionAppRuntime(
    services = services,
    sessionService = sessionService,
)

private fun Services.closeDexService() {
    (dex as? AutoCloseable)?.close()
}
