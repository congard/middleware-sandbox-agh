import grpc
import gen.bulb_pb2 as bulb_pb2
import gen.bulb_pb2_grpc as bulb_pb2_grpc

from typing import Callable, Any
from InputLoop import InputLoop


def print_help():
    print("""Smart Bulb
    help                    print this message
    exit                    exit
    list                    print available bulbs list
    bulb <id>               print properties of the specified bulb
    color <id>              print color of the specified bulb
    color <id> <color>      change color
    brightness <id>         print brightness of the specified bulb
    brightness <id> <level> change brightness level
    schedule <id>           print schedule of the specified bulb
    rm-entry <id> <eid>     remove schedule entry of id <eid>
    add-entry <id> <args>   add schedule entry, args format:
                            id=<bulb id>
                            delay=<seconds>
                            [optional] state=<on|off>
                            [optional] color=<string>
                            [optional] brightnessLevel=<int>
    state <id>              print the state of the specified bulb
    on <id>                 turn the bulb on
    off <id>                turn the bulb off
    """)


channel: grpc.Channel = None
stub: bulb_pb2_grpc.BulbServiceStub = None


def list_devices():
    response = stub.GetAvailable(bulb_pb2.Empty())
    print(response)


def mk_dev_id(dev_id: int) -> bulb_pb2.Identifier:
    return bulb_pb2.Identifier(id=dev_id)


def is_error(response: Any) -> bool:
    return response.HasField("error")


def print_error(err: bulb_pb2.Error):
    print(f"Error code: {err.code}")
    print(f"Message: {err.message}")


def handle_response(response: Any, success: Callable[[Any], None] = lambda _: print("Done")):
    if is_error(response):
        print_error(response.error)
    else:
        success(response)


def bulb_properties(dev_id: int):
    handle_response(stub.GetBulb(mk_dev_id(dev_id)), lambda r: print(r.bulb))


def print_color(dev_id: int):
    handle_response(stub.GetColor(mk_dev_id(dev_id)), lambda r: print(r.color))


def print_brightness(dev_id: int):
    handle_response(stub.GetBrightness(mk_dev_id(dev_id)), lambda r: print(r.level))


def set_color(dev_id: int, color: str):
    handle_response(stub.SetColor(bulb_pb2.ColorChangeRequest(id=dev_id, color=color)))


def set_brightness(dev_id: int, level: int):
    handle_response(stub.SetBrightness(bulb_pb2.BrightnessChangeRequest(id=dev_id, level=level)))


def print_schedule(dev_id: int):
    handle_response(stub.GetSchedule(mk_dev_id(dev_id)), lambda r: print(r.entries))


def add_entry(args: list[str]):
    valid_keys = ["id", "delay", "state", "color", "brightnessLevel"]

    # parse params
    params: dict[str, str] = {}

    for arg in args:
        kw = arg.split("=")

        if len(kw) != 2:
            print(f"Invalid argument '{arg}'")
            return

        k, w = kw

        if k not in valid_keys:
            print(f"Invalid key '{k}'")
            return

        if k != "color":
            w = int(w)

        params[k] = w

    if "id" not in params:
        print("Error: id is obligatory")
        return

    if "delay" not in params:
        print("Error: delay is obligatory")
        return

    handle_response(stub.AddScheduleEntry(bulb_pb2.ScheduleEntry(**params)))


def remove_entry(dev_id: int, entry_id: int):
    handle_response(stub.RemoveScheduleEntry(bulb_pb2.RemoveScheduleEntryRequest(bulbId=dev_id, entryId=entry_id)))


def print_state(dev_id: int):
    def printable_state(r: Any):
        match r.state:
            case bulb_pb2.Bulb.State.ON:
                print("On")
            case bulb_pb2.Bulb.State.OFF:
                print("Off")
            case _:
                print("unknown")

    handle_response(stub.GetState(mk_dev_id(dev_id)), printable_state)


def turn_on(dev_id: int):
    handle_response(stub.TurnOn(mk_dev_id(dev_id)))


def turn_off(dev_id: int):
    handle_response(stub.TurnOff(mk_dev_id(dev_id)))


class BulbInputLoop(InputLoop):
    def __init__(self):
        super().__init__("bulb")

    def on_loop(self) -> bool:
        match self.args[0]:
            case "help":
                print_help()
            case "exit":
                return False
            case "list":
                list_devices()
            case "bulb":
                self.verify_argc(2, lambda: bulb_properties(int(self.args[1])))
            case "color":
                match self.argc:
                    case 2:
                        print_color(int(self.args[1]))
                    case 3:
                        set_color(int(self.args[1]), self.args[2])
                    case _:
                        self.print_unexpected_argc(2, 3)
            case "brightness":
                match self.argc:
                    case 2:
                        print_brightness(int(self.args[1]))
                    case 3:
                        set_brightness(int(self.args[1]), int(self.args[2]))
                    case _:
                        self.print_unexpected_argc(2, 3)
            case "schedule":
                self.verify_argc(2, lambda: print_schedule(int(self.args[1])))
            case "rm-entry":
                self.verify_argc(3, lambda: remove_entry(int(self.args[1]), int(self.args[2])))
            case "add-entry":
                add_entry(self.args[1:])
            case "state":
                self.verify_argc(2, lambda: print_state(int(self.args[1])))
            case "on":
                self.verify_argc(2, lambda: turn_on(int(self.args[1])))
            case "off":
                self.verify_argc(2, lambda: turn_off(int(self.args[1])))
            case _:
                self.print_invalid_command_error()
        return True


def run(in_channel: grpc.Channel):
    global channel, stub
    channel = in_channel
    stub = bulb_pb2_grpc.BulbServiceStub(channel)

    BulbInputLoop().loop()
