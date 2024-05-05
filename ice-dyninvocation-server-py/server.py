# source: https://doc.zeroc.com/ice/3.7/release-notes/using-the-python-distribution

import Ice
import sys

import DynamicInvocation
from typing import Sequence


def log(s: str):
    print(s)


class ConcreteCalculator(DynamicInvocation.Calculator):
    def muld(self, p1: float, p2: float, current=None) -> float:
        log(f"muld({p1}, {p2})")
        return p1 * p2

    def mmuld(self, d: list[float], current=None) -> float:
        log(f"mmuld({', '.join(map(str, d))})")

        self.check_argc(d)

        res = 1.0

        for i in d:
            res *= i

        return res

    def stat(self, d: list[float], current=None) -> DynamicInvocation.Stats:
        log(f"stat({', '.join(map(str, d))})")

        self.check_argc(d)

        n = len(d)
        d = sorted(d)
        med = d[n // 2] if n % 2 != 0 else (d[n // 2 - 1] + d[n // 2]) * 0.5
        avg = sum(d) / n

        return DynamicInvocation.Stats(d[0], d[-1], avg, med)

    @staticmethod
    def to_complex(c: DynamicInvocation.Complex) -> complex:
        return complex(c.re, c.im)

    @staticmethod
    def check_argc(c: Sequence, expected_at_least: int = 2):
        if len(c) < expected_at_least:
            raise DynamicInvocation.TooFewArgumentException()

    def mulc(self, c1, c2, context=None) -> DynamicInvocation.Complex:
        log(f"mulc({c1}, {c2})")
        c1 = self.to_complex(c1)
        c2 = self.to_complex(c2)
        c3 = c1 * c2
        return DynamicInvocation.Complex(c3.real, c3.imag)

    def mmulc(self, numbers: list[DynamicInvocation.Complex], context=None) -> DynamicInvocation.Complex:
        log(f"mmulc({', '.join(map(str, numbers))})")

        self.check_argc(numbers)

        c = self.to_complex(numbers[0])

        for i in range(1, len(numbers)):
            c *= self.to_complex(numbers[i])

        return DynamicInvocation.Complex(c.real, c.imag)


if __name__ == "__main__":
    with Ice.initialize(sys.argv) as communicator:
        adapter = communicator.createObjectAdapterWithEndpoints("Calculator", "default -p 10000")

        obj = ConcreteCalculator()
        adapter.add(obj, communicator.stringToIdentity("Calculator"))
        adapter.activate()

        log("Server started")

        communicator.waitForShutdown()
