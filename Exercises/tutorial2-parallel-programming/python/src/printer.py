from pystreamapi import Stream
import time


def print_value(v):
    print(v)


def main():
    array = list(range(1000000))

    print("Serial execution")
    start = time.time()
    Stream.of(array)
    end = time.time()
    print("Serial: ", end - start)

    #Stream.of(array).for_each(print_value)

    print("Parallel execution")
    start = time.time()
    Stream.of(array).parallel()
    end = time.time()
    print("Parallel: ", end - start)
    #Stream.of(array).parallel().for_each(print_value)


if __name__ == "__main__":
    main()
