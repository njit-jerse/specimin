#!/sbin/python
from collections import defaultdict
import re
import os
import sys

def deinterleave(file_path: str):
    thread_logs = defaultdict(list)
    
    with open(file_path, "r") as infile:
        for line in infile:
            #log format is like this:
            #13:40:26.262 [ForkJoinPool-1-worker-9]
            #\[ForkJoinPool-1-worker-\([0-9]\+\)]
            pattern = re.compile(r"\[ForkJoinPool-1-worker-([0-9]+)]")
            #pattern = re.compile(r"ForkJoinPool")
            with open(file_path, "r") as infile:
                for line in infile:
                    match = pattern.search(line)
                    if match:
                        thread = match.group(1)
                        thread_logs[thread].append(line.strip())
                break

    with open(output_file, "w") as outfile:
        for thread_id, messages in thread_logs.items():
            outfile.write(f"Logs for {thread_id}:\n")
            for message in messages:
                outfile.write(f"{thread_id}: {message}\n")
            outfile.write("\n")


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python3 specimin_repo_specifics.py <path_to_log_file.log>")
        sys.exit(1)
    log_file_path = sys.argv[1]
    directory: str = os.path.dirname(log_file_path)
    output_file = os.path.join(directory, 'deinterleaved-log.txt')
    deinterleave(log_file_path)
    print(f"De-interleaved log written to: {output_file}")
