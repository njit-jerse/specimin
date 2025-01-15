#!/sbin/python
from collections import defaultdict
import re
import os
import sys

def deinterleave(infile):
    thread_logs = defaultdict(list)

    #log format is like this:
    #13:40:26.262 [ForkJoinPool-1-worker-9]
    #\[ForkJoinPool-1-worker-\([0-9]\+\)]
    pattern = re.compile(r"\[ForkJoinPool-[0-9]+-worker-([0-9]+)]")
    multiThreadEndPattern = re.compile(r"\[main]")
    
    for line in infile:
        match = pattern.search(line)
        multiThreadEndMatch = multiThreadEndPattern.search(line)
        if match:
            thread = match.group(1)
        if multiThreadEndMatch:
            break
        print(line)
        thread_logs[thread].append(line.strip())

    with open(output_file, "a") as outfile:
        for thread_id, messages in thread_logs.items():
            outfile.write(f"Logs for {thread_id}:\n")
            for message in messages:
                outfile.write(f"{message}\n")
            outfile.write("\n")
            outfile.write(line)
        
#continues switching between reading the "main" thread and de-interleaving the
#multithreaded section until there are no more lines
def parseLog(file_path: str):
    with open(file_path, "r") as infile:
        moreLines = True
        while moreLines:
            moreLines = parseMainThread(infile)
            deinterleave(infile)


def parseMainThread(infile) -> bool:
    mainEndPattern = re.compile(r"Dry run with [0-9]+ threads")
    logBuffer:list[str] = []
    #if there isn't another run in the log file, then this will run until the
    #end of file.
    moreLines = False
    for line in infile:
        match = mainEndPattern.search(line)
        logBuffer.append(line)
        if match:
            moreLines = True
            break
    with open(output_file, "a") as outfile:
                for line in logBuffer:
                    outfile.write(line)
    return moreLines




if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python3 specimin_repo_specifics.py <path_to_log_file.log>")
        sys.exit(1)
    log_file_path = sys.argv[1]
    directory: str = os.path.dirname(log_file_path)
    output_file = os.path.join(directory, 'deinterleaved-log.txt')
    parseLog(log_file_path)
    print(f"De-interleaved log written to: {output_file}")
