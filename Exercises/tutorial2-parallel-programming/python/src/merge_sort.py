import concurrent.futures
from pystreamapi import Stream
import time

def merge(left, right):
    result = []
    i = j = 0
    while i < len(left) and j < len(right):
        if left[i] < right[j]:
            result.append(left[i])
            i += 1
        else:
            result.append(right[j])
            j += 1
    result.extend(left[i:])
    result.extend(right[j:])
    return result

def merge_sort_serial(lst):
    if len(lst) <= 1:
        return lst
    mid = len(lst) // 2
    left = merge_sort_serial(lst[:mid])
    right = merge_sort_serial(lst[mid:])
    return merge(left, right)

def merge_sort_parallel(lst, executor):
    if len(lst) <= 1:
        return lst
    mid = len(lst) // 2
    
    # Submit left and right halves to be sorted concurrently
    future_left = executor.submit(merge_sort_serial, lst[:mid])
    future_right = executor.submit(merge_sort_serial, lst[mid:])
    
    left = future_left.result()
    right = future_right.result()
    
    return merge(left, right)

def sort_and_remove_stopwords(words, stop_words, parallel=False, num_workers=2):
    filtered_words = Stream.of(words).filter(lambda word: word.lower() not in stop_words).to_list()
    
    if parallel:
        with concurrent.futures.ThreadPoolExecutor(max_workers=num_workers) as executor:
            return merge_sort_parallel(filtered_words, executor)
    else:
        return merge_sort_serial(filtered_words)

if __name__ == "__main__":
    words = ["apple", "banana", "the", "is", "grape", "orange", "an", "cherry", "and", "mango"]
    stop_words = {"the", "is", "an", "and"}

    print("Serial Merge Sort:")
    start_time = time.time()
    print(sort_and_remove_stopwords(words, stop_words, parallel=False))
    print(f"Time taken: {time.time() - start_time} seconds")

    print("\nParallel Merge Sort (Multithreading):")
    start_time = time.time()
    print(sort_and_remove_stopwords(words, stop_words, parallel=True, num_workers=4))
    print(f"Time taken: {time.time() - start_time} seconds")
