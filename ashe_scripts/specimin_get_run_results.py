import sys
import os
import re

def print_repo_specifics(file_path: str):
    with open(file_path, 'r') as file:
        lines: list[str] = file.readlines()

    repo_name: str = ""
    branch_name: str = ""
    repo_branch_holder = "", ""
    for line in lines:
        line: str = line.strip()
    
        repo_branch_holder = __extract_case(line)
        if repo_branch_holder != ("", ""):
            repo_name = repo_branch_holder[0]
            branch_name = repo_branch_holder[1]

        if "Minimizing source file..." in line:
            print("'minimization_attempts'  	" + repo_name + " " + branch_name)
        if "BUILD SUCCESSFUL" in line:
            print("'successful_minimization'	"+repo_name + " " + branch_name)
        if "BUILD FAILED" in line:
            print("'failed_minimization'	"+repo_name + " " + branch_name)
        if "Compiling Java files" in line:
                print("'compilation_attempts'   	"+repo_name + " " + branch_name)
        if "Minimized files compiled successfully." in line:
            print("'successful_compilation'	"+repo_name + " " + branch_name)
            print("'full_success'          	"+repo_name + " " + branch_name)
        if "Minimized files failed to compile." in line:
            print("'failed_compilation'           	"+repo_name + " " + branch_name)

def __extract_case(log_line: str):
    """
    Extracts the repository path and branch name from a log line.

    Parameters:
    - log_line (str): A string from the log file containing repository and branch information.

    Returns:
    - tuple: A tuple containing the repository path and the branch name.
    """
    # regex pattern to find the repository path and branch name
    pattern = r'--targetFile "(.+?\.java)" --targetMethod "(.+?)"'
    match = re.search(pattern, log_line)

    if match:
        targetFile = match.group(1).strip()
        targetMethod = match.group(2).strip()
        return targetFile, targetMethod
    else:
        return "", "" 

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python3 specimin_repo_specifics.py <path_to_log_file.log>")
        sys.exit(1)
    log_file_path = sys.argv[1]
    print_repo_specifics(log_file_path)
