# Concurrent-Image-Processor
Demonstrates concurrency in both data parallel and task parallel versions
Author: Alan Tam
Date: MAy 15, 2016

This program demonstrate concurrency by processing images following the regex: image_.*?.jpg. 

There are two versions: task parallel and data parallel.
The task parallel version will take a number of threads where each thread will process different images concurrently.
Usage: java ConcurrentImageProcessorTaskParallelTaskParallel <# threads> <filter name> <path>

The data parallel version will take a number of threads and all threads will process the same image concurrently.
Usage: java ConcurrentImageProcessorDataParallelTaskParallel <# threads> <filter name> <path>

hw5_432 - holds the 2 classes containing the main
-ConcurrentImageProcessorTaskParallelTaskParallel
-ConcurrentImageProcessorDataParallelTaskParallel

WeirdFilter - holds the weird filter classes which does a funky color scheme based on surrounding pixels
-DataParallelWeirdFilter
-RGB
-WeirdFilter
