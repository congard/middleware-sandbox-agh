import pl.edu.agh.distributed.middleware.hw.grpcserver.dht.*
import kotlin.random.Random

private val logger = Logger {}

class DHTService : DHTServiceGrpcKt.DHTServiceCoroutineImplBase() {
    private val devices = listOf(device {
        id = 0
        name = "Relative humidity sensor"
        features += Device.Feature.RELATIVE_HUMIDITY
    }, device {
        id = 1
        name = "Temperature sensor"
        features += Device.Feature.TEMPERATURE
    }, device {
        id = 2
        name = "Relative humidity and Temperature sensor"
        features += Device.Feature.RELATIVE_HUMIDITY
        features += Device.Feature.TEMPERATURE
    })

    override suspend fun getAvailable(request: Empty) = deviceList {
        logger.logFunName {}
        devices += this@DHTService.devices
    }

    override suspend fun getDevice(request: DeviceIdentifier) = deviceReply {
        logger.logFunName {}

        ifDeviceExists(request) {
            device = this
        } orElse {
            error = notFoundError()
        }
    }

    override suspend fun getTemperature(request: DeviceIdentifier) = temperatureReply {
        logger.logFunName {}

        ifDeviceExists(request) {
            ifFeatureSupported(Device.Feature.TEMPERATURE) {
                tempCelsius = temp()
            } orElse {
                error = featureNotSupportedError()
            }
        } orElse {
            error = notFoundError()
        }
    }

    override suspend fun getRelativeHumidity(request: DeviceIdentifier) = relativeHumidityReply {
        logger.logFunName {}

        ifDeviceExists(request) {
            ifFeatureSupported(Device.Feature.RELATIVE_HUMIDITY) {
                relativeHumidity = humidity()
            } orElse {
                error = featureNotSupportedError()
            }
        } orElse {
            error = notFoundError()
        }
    }

    private fun ifDeviceExists(id: DeviceIdentifier, block: Device.() -> Unit) =
        devices.find { it.id == id.id }?.let { it.block(); Success(id) } ?: Failure(id)

    private fun Device.ifFeatureSupported(feature: Device.Feature, block: () -> Unit) =
        if (feature in featuresList) { block(); Success(feature) } else Failure(feature)

    private fun DeviceIdentifier.notFoundError() = error {
        code = Error.Code.DEVICE_NOT_FOUND
        message = "Device with id=${id} cannot be found"
    }

    private fun Device.Feature.featureNotSupportedError() = error {
        code = Error.Code.FEATURE_NOT_SUPPORTED
        message = "Feature ${this@featureNotSupportedError} is not supported"
    }

    private fun Device.temp() =
        Random.nextInt(200, 800) / 10f

    private fun Device.humidity() =
        Random.nextInt(300, 700) / 10f
}