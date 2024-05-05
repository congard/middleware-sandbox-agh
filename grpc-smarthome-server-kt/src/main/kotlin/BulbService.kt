import pl.edu.agh.distributed.middleware.hw.grpcserver.bulb.*

private val logger = Logger {}

class BulbService : BulbServiceGrpcKt.BulbServiceCoroutineImplBase() {
    private val bulbs = listOf(BulbDetails(
        id = 0,
        name = "Simple bulb"
    ), BulbDetails(
        id = 1,
        name = "Bulb with color control",
        features = listOf(Bulb.Feature.DYNAMIC_COLOR),
        colors = listOf("#ffffff", "#ff0000", "#00ff00", "#0000fff"),
        currColor = "#ffffff"
    ), BulbDetails(
        id = 2,
        name = "Full featured bulb",
        features = Bulb.Feature.entries - Bulb.Feature.UNRECOGNIZED,
        colors = listOf("#ffffff", "#ff0000", "#00ff00", "#0000ff", "#ffff00", "#ff00ff"),
        brightnessLevels = (1..5).toList(),
        currColor = "#ffffff",
        currBrightnessLevel = 1
    ))

    override suspend fun getAvailable(request: Empty) = bulbList {
        bulbs += this@BulbService.bulbs.map { it.toBulb() }
    }.logFunName {}

    override suspend fun getBulb(request: Identifier) = bulbReply {
        ifBulbExists(request) {
            bulb = toBulb()
        } orElse {
            error = notFoundError()
        }
    }.logFunName {}

    override suspend fun getColor(request: Identifier) = colorReply {
        ifBulbExists(request) {
            ifFeatureSupported(Bulb.Feature.DYNAMIC_COLOR) {
                color = currColor!!
            } orElse {
                error = featureNotSupportedError()
            }
        } orElse {
            error = notFoundError()
        }
    }.logFunName {}

    override suspend fun getBrightness(request: Identifier) = brightnessReply {
        ifBulbExists(request) {
            ifFeatureSupported(Bulb.Feature.DYNAMIC_BRIGHTNESS) {
                level = currBrightnessLevel!!
            } orElse {
                error = featureNotSupportedError()
            }
        } orElse {
            error = notFoundError()
        }
    }.logFunName {}

    override suspend fun getSchedule(request: Identifier) = scheduleReply {
        ifBulbExists(request) {
            ifFeatureSupported(Bulb.Feature.SCHEDULE) {
                entries += scheduleEntries
            } orElse {
                error = featureNotSupportedError()
            }
        } orElse {
            error = notFoundError()
        }
    }.logFunName {}

    override suspend fun getState(request: Identifier) = stateReply {
        ifBulbExists(request) {
            this@stateReply.state = state
        } orElse {
            error = notFoundError()
        }
    }.logFunName {}

    override suspend fun turnOn(request: Identifier) =
        changeState(request, Bulb.State.ON).logFunName {}

    override suspend fun turnOff(request: Identifier) =
        changeState(request, Bulb.State.OFF).logFunName {}

    override suspend fun setColor(
        request: ColorChangeRequest
    ) = autoStatusReply(request.id.toId(), Bulb.Feature.DYNAMIC_COLOR, color = request.color) { bulb ->
        bulb.currColor = request.color
    }.logFunName {}

    override suspend fun setBrightness(
        request: BrightnessChangeRequest
    ) = autoStatusReply(request.id.toId(), Bulb.Feature.DYNAMIC_BRIGHTNESS, level = request.level) { bulb ->
        bulb.currBrightnessLevel = request.level
    }.logFunName {}

    override suspend fun addScheduleEntry(
        request: ScheduleEntry
    ) = autoStatusReply(
        request.id.toId(), Bulb.Feature.SCHEDULE,
        color = if (request.hasColor()) request.color else null,
        level = if (request.hasBrightnessLevel()) request.brightnessLevel else null
    ) { bulb ->
        bulb.scheduleEntries += scheduleEntry {
            id = bulb.scheduleEntries.size.toLong()
            delay = request.delay

            if (request.hasState())
                state = request.state

            if (request.hasColor())
                color = request.color

            if (request.hasBrightnessLevel())
                brightnessLevel = request.brightnessLevel
        }
    }.logFunName {}

    override suspend fun removeScheduleEntry(
        request: RemoveScheduleEntryRequest
    ) = autoStatusReply(request.bulbId.toId(), Bulb.Feature.SCHEDULE) { bulb ->
        if (!bulb.scheduleEntries.removeIf { it.id == request.entryId }) {
            error = bulb.scheduleEntryNotFoundError(request.entryId)
        }
    }.logFunName {}

    private fun changeState(id: Identifier, newState: Bulb.State) = autoStatusReply(id) { bulb ->
        if (bulb.state != newState) {
            bulb.state = newState
        } else {
            error = bulb.noEffectError()
        }
    }

    private fun autoStatusReply(
        id: Identifier,
        block: StatusReplyKt.Dsl.(BulbDetails) -> Unit
    ) = statusReply {
        ifBulbExists(id) {
            this@statusReply.block(this)
        } orElse {
            error = notFoundError()
        }
    }

    private fun autoStatusReply(
        id: Identifier,
        feature: Bulb.Feature,
        color: String? = null,
        level: Int? = null,
        block: StatusReplyKt.Dsl.(BulbDetails) -> Unit,
    ) = autoStatusReply(id) { bulb ->
        bulb.ifFeatureSupported(feature) {
            if (color != null && !bulb.isColorSupported(color)) {
                error = bulb.notSupportedColorError()
            } else if (level != null && !bulb.isBrightnessLevelSupported(level)) {
                error = bulb.notSupportedBrightnessError()
            } else {
                block(bulb)
            }
        } orElse {
            error = featureNotSupportedError()
        }
    }

    private fun ifBulbExists(id: Identifier, block: BulbDetails.() -> Unit) =
        bulbs.find { it.id == id.id }?.let { it.block(); Success(id) } ?: Failure(id)

    private fun BulbDetails.ifFeatureSupported(feature: Bulb.Feature, block: () -> Unit) =
        if (feature in features) { block(); Success(feature) } else Failure(feature)

    private fun Identifier.notFoundError() = error {
        code = Error.Code.DEVICE_NOT_FOUND
        message = "Bulb with id=${id} cannot be found"
    }

    private fun BulbDetails.scheduleEntryNotFoundError(entryId: Long) = error {
        code = Error.Code.DEVICE_NOT_FOUND
        message = "Schedule entry (id=$entryId) of bulb (id=${this@scheduleEntryNotFoundError.id}) cannot be found"
    }

    private fun Bulb.Feature.featureNotSupportedError() = error {
        code = Error.Code.FEATURE_NOT_SUPPORTED
        message = "Feature ${this@featureNotSupportedError} is not supported"
    }

    private fun BulbDetails.noEffectError() = error {
        code = Error.Code.ACTION_HAS_NO_EFFECT
        message = "Action has no effect (bulb id=$id)"
    }

    private fun BulbDetails.notSupportedColorError() = error {
        code = Error.Code.COLOR_NOT_SUPPORTED
        message = "The provided color is not supported by the bulb with id=$id"
    }

    private fun BulbDetails.notSupportedBrightnessError() = error {
        code = Error.Code.BRIGHTNESS_LEVEL_NOT_SUPPORTED
        message = "The provided brightness level is not supported by the bulb with id=$id"
    }

    private fun Long.toId() = identifier { id = this@toId }

    private fun <T> T.logFunName(dummyFun: DummyFun<Unit>) = run {
        logger.logFunName(dummyFun)
        this
    }

    private data class BulbDetails(
        val id: Long,
        val name: String,
        val features: List<Bulb.Feature> = listOf(),
        val colors: List<String> = listOf(),
        val brightnessLevels: List<Int> = listOf(),

        var state: Bulb.State = Bulb.State.OFF,

        var currColor: String? = null,
        var currBrightnessLevel: Int? = null,
        var scheduleEntries: ArrayList<ScheduleEntry> = ArrayList()
    ) {
        fun toBulb() = bulb {
            id = this@BulbDetails.id
            name = this@BulbDetails.name
            features += this@BulbDetails.features
            colors += this@BulbDetails.colors
            brightnessLevels += this@BulbDetails.brightnessLevels
            state = this@BulbDetails.state
            this@BulbDetails.currColor?.also { currColor = it }
            this@BulbDetails.currBrightnessLevel?.also { currBrightnessLevel = it }
            scheduleEntries += this@BulbDetails.scheduleEntries
        }

        fun isColorSupported(color: String) =
            color in colors

        fun isBrightnessLevelSupported(level: Int) =
            level in brightnessLevels
    }
}