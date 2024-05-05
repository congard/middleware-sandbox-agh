from __future__ import print_function

import grpc
import logging

from typing import Callable
from InputLoop import InputLoop

import sys
sys.path.append("gen")

import dht_client
import bulb_client


def print_help():
    print(f"""SmartHome Client
    
    help                    print this message
    connect <ip>:<port>     connect to the specified server
    disconnect              disconnect (closes channel)
    exit                    exit
    dht                     enter DHT devices configuration mode
    bulb                    enter SmartBulb devices configuration mode
    """)


def print_not_connected_error():
    print("There is no active connection")


channel: grpc.Channel = None


def is_connected() -> bool:
    return channel is not None


def connect(target: str):
    disconnect()

    global channel
    print(f"Connecting to {target}")

    channel = grpc.insecure_channel(target)


def disconnect():
    global channel

    if channel is not None:
        channel.close()
        channel = None
        print("Disconnected")


class MainInputLoop(InputLoop):
    def __init__(self):
        super().__init__("smarthome")

    def on_loop(self) -> bool:
        match self.args[0]:
            case "help":
                print_help()
            case "connect":
                if self.argc == 1:
                    target = "localhost:50051"
                else:
                    target = self.args[1]
                connect(target)
            case "disconnect":
                self.if_connected_then(lambda: disconnect())
            case "exit":
                disconnect()
                return False
            case "dht":
                self.if_connected_then(lambda: dht_client.run(channel))
            case "bulb":
                self.if_connected_then(lambda: bulb_client.run(channel))
            case _:
                self.print_invalid_command_error()
        return True

    @staticmethod
    def if_connected_then(block: Callable[[], None]):
        if is_connected():
            block()
        else:
            print_not_connected_error()


if __name__ == "__main__":
    logging.basicConfig()
    MainInputLoop().loop()
