# Python sample for Simian parser testing - original work, public domain
# Covers: imports, constants, type hints, functions, classes, decorators,
#         exception handling, comprehensions, context managers, lambdas,
#         operators, loops, closures, unpacking, assertions

import os
import sys
from typing import List, Optional, Dict, Tuple

MAX_RETRIES: int = 3
TIMEOUT: float = 30.0


def greet(name: str, greeting: str = "Hello") -> str:
    """Return a personalised greeting string."""
    return greeting + ", " + name + "!"


def paginate(total: int, page_size: int) -> int:
    # Floor division - exercises the // operator
    return total // page_size


def safe_divide(a: float, b: float) -> Optional[float]:
    try:
        result = a / b
    except ZeroDivisionError:
        return None
    except TypeError as e:
        raise RuntimeError("Invalid input") from e
    else:
        return result
    finally:
        pass


def process_items(items: List[int]) -> Dict[str, list]:
    evens = [x for x in items if x % 2 == 0]
    odds = [x for x in items if x % 2 != 0]
    squares = {x: x * x for x in items}
    unique = {x for x in items}
    gen = (x ** 2 for x in items)
    return {"evens": evens, "odds": odds}


def read_file(path: str) -> str:
    with open(path, "r") as f:
        return f.read()


def loop_examples(items: list) -> None:
    for i, item in enumerate(items):
        if item is None:
            continue
        if i > 10:
            break
    n = len(items)
    while n > 0:
        n -= 1


def logical_check(value, items: list) -> bool:
    return value in items and value is not None


def make_adder(n: int):
    def adder(x: int) -> int:
        return x + n
    return adder


double = lambda x: x * 2


def clamp(value: int, lo: int, hi: int) -> int:
    return lo if value < lo else (hi if value > hi else value)


def unpack_example() -> Tuple[int, int]:
    a, b = 10, 20
    a, b = b, a
    x, *rest = [1, 2, 3, 4, 5]
    return a, b


class Animal:
    species_count: int = 0

    def __init__(self, name: str, age: int) -> None:
        self.name = name
        self.age = age
        Animal.species_count += 1

    def __repr__(self) -> str:
        return f"Animal({self.name!r})"

    @property
    def is_adult(self) -> bool:
        return self.age >= 2

    @staticmethod
    def kingdom() -> str:
        return "Animalia"

    @classmethod
    def from_dict(cls, data: dict) -> "Animal":
        return cls(data["name"], data["age"])


class Dog(Animal):
    def __init__(self, name: str, age: int, breed: str) -> None:
        super().__init__(name, age)
        self.breed = breed

    def speak(self) -> str:
        return "Woof!"


def assert_positive(value: int) -> bool:
    assert value > 0, "must be positive"
    return True


def global_example() -> None:
    global MAX_RETRIES
    MAX_RETRIES = 5


def closure_example():
    count = 0

    def increment() -> int:
        nonlocal count
        count += 1
        return count

    return increment
