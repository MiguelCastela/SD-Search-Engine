from pystreamapi import Stream
from time import time as millis
import random


# def monte_carlo_pi():
#     # pi = 4 * (number of points in the circle) / (number of points in the square)
#     inside = 0
#     total = 1000000
#     for _ in range(total):
#         x = random.random()
#         y = random.random()
#         if x*x + y*y < 1:
#             inside += 1
#     return 4 * inside / total

def monte_carlo_pi(x):
    x = random.random()
    y = random.random()
    if(x*x + y*y < 1):
        return True
    else: 
        return False

def main():
    iterations = 1000000
    
    start_time = millis()
    print("Serial Monte Carlo Pi execution")
    ct = Stream.iterate(None, monte_carlo_pi).limit(iterations).filter(lambda x: x).count()
    serial_pi_estimate = 4 * ct / iterations
    end_time = millis()
    print(f"Serial Monte Carlo Pi: {serial_pi_estimate}")
    print(f"Serial Monte Carlo Pi execution time: {end_time - start_time} ms")    
    
    start_time = millis()
    print("Parallel Monte Carlo Pi parallel execution")
    #ct = Stream.parallel_of(range(iterations)).map(monte_carlo_pi).filter(lambda x: x).count()
    #ct = Stream.parallel_of(range(iterations)).map(lambda x: monte_carlo_pi(x)).filter(lambda x: x).count()
    
    paralel_stream = Stream.parallel_of(range(iterations))
    mapped_stream = paralel_stream.map(lambda x: monte_carlo_pi(x))
    filtered_stream = mapped_stream.filter(lambda x: x == True)
    ct = filtered_stream.count()
    
    parallel_pi_estimate = 4 * ct / iterations
    end_time = millis()
    print(f"Parallel Monte Carlo Pi: {parallel_pi_estimate}")
    print(f"Parallel Monte Carlo Pi execution time: {end_time - start_time} ms")


if __name__ == "__main__":
    main()
