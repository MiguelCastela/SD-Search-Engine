from pystreamapi import Stream
import time


def print_value(v):
    print(v)


def main():
    array = list(range(10000000))

    print("Serial execution")
    start = time.time()
    Stream.of(sum(array))
    end = time.time()
    print("Serial: ", end - start)

    #Stream.of(array).for_each(print_value)

    print("Parallel execution")
    start = time.time()
    Stream.of(sum(array)).parallel()
    end = time.time()
    print("Parallel: ", end - start)
    #Stream.of(array).paralalel().for_each(print_value)


if __name__ == "__main__":
    main()
