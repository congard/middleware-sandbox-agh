from abc import abstractmethod
from typing import Callable


class InputLoop:
    def __init__(self, name: str):
        self.name = name
        self.args: list[str] = []
        self.argc: int = -1

    def loop(self):
        while True:
            print(f"{self.name} > ", end="")

            self.args = input().split(" ")
            self.argc = len(self.args)

            if self.argc == 0 or len(self.args[0]) == 0:
                continue

            if not self.on_loop():
                break

    @abstractmethod
    def on_loop(self) -> bool:
        pass

    def verify_argc(self, expected: int, block: Callable[[], None]):
        if self.argc != expected:
            self.print_unexpected_argc(expected)
        else:
            block()

    def print_unexpected_argc(self, *expected: int):
        print(f"Unexpected argument count.\nExpected: {' or '.join(map(str, expected))}, got: {self.argc}")

    def print_invalid_command_error(self):
        print(f"Unknown args '{' '.join(self.args)}'")
