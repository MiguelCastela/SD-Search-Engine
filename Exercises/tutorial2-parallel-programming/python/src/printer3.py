from pystreamapi import Stream
import time


def print_value(v):
    time.sleep(0.01)  # Simulate a task taking 0.01 seconds
    print(v)


def main():
    array = list(range(100))

    print("Serial execution")
    start = time.time()
    Stream.of(array).for_each(print_value)
    end = time.time()
    print("Serial: ", end - start)

    print("Parallel execution")
    start = time.time()
    Stream.of(array).parallel().for_each(print_value)
    end = time.time()
    print("Parallel: ", end - start)


if __name__ == "__main__":
    main()