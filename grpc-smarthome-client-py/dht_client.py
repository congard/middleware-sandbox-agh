import grpc
import gen.dht_pb2 as dht_pb2
import gen.dht_pb2_grpc as dht_pb2_grpc

from typing import Any
from InputLoop import InputLoop


def print_help():
    print("""DHT Client - Digital Humidity Temperature Sensor
    help                    print this message
    exit                    exit
    list                    print available devices list
    device <id>             print properties of the specified device
    temp <id>               read temperature from the specified device
    humidity <id>           read relative humidity from the specified device
    """)


channel: grpc.Channel = None
stub: dht_pb2_grpc.DHTServiceStub = None


def list_devices():
    response = stub.GetAvailable(dht_pb2.Empty())
    print(response)


def mk_dev_id(dev_id: int) -> dht_pb2.DeviceIdentifier:
    return dht_pb2.DeviceIdentifier(id=dev_id)


def is_error(response: Any) -> bool:
    return response.HasField("error")


def print_error(err: dht_pb2.Error):
    print(f"Error code: {err.code}")
    print(f"Message: {err.message}")


def device_properties(dev_id: int):
    response = stub.GetDevice(mk_dev_id(dev_id))

    if is_error(response):
        print_error(response.error)
    else:
        dev: dht_pb2.Device = response.device
        print(dev)


def temp(dev_id: int):
    response = stub.GetTemperature(mk_dev_id(dev_id))

    if is_error(response):
        print_error(response.error)
    else:
        print(f"{round(response.tempCelsius, 2)} °C")


def humidity(dev_id: int):
    response = stub.GetRelativeHumidity(mk_dev_id(dev_id))

    if is_error(response):
        print_error(response.error)
    else:
        print(f"{round(response.relativeHumidity, 2)} %")


class DHTInputLoop(InputLoop):
    def __init__(self):
        super().__init__("dht")

    def on_loop(self) -> bool:
        match self.args[0]:
            case "help":
                print_help()
            case "exit":
                return False
            case "list":
                list_devices()
            case "device":
                self.verify_argc(2, lambda: device_properties(int(self.args[1])))
            case "temp":
                self.verify_argc(2, lambda: temp(int(self.args[1])))
            case "humidity":
                self.verify_argc(2, lambda: humidity(int(self.args[1])))
            case _:
                self.print_invalid_command_error()
        return True


def run(in_channel: grpc.Channel):
    global channel, stub
    channel = in_channel
    stub = dht_pb2_grpc.DHTServiceStub(channel)

    DHTInputLoop().loop()
