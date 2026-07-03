import subprocess
import signal
import os

def execute(command_args):
    """
    Executes an external command and returns its output.
    
    :param command_args: A list of strings (e.g., ["ls", "-l", "/tmp"])
    :return: The return code of the command and its standard output as a string.
    """
    try:
        # check=True raises an exception if the command fails
        # capture_output=True grabs stdout and stderr
        # text=True ensures the result is a string, not bytes
        result = subprocess.run(
            command_args, 
            capture_output=True, 
            text=True, 
            check=True
        )
        return result.returncode, result.stdout
    
    except subprocess.CalledProcessError as e:
        output = (e.stdout or "") + (("\n" + e.stderr) if e.stderr else "")
        return e.returncode, output
    except FileNotFoundError:
        return -1, "Error: The specified command was not found."

# Example Usage:
# output = execute(["echo", "Hello World"])
# print(output)

def start(command_args):
    """
    Starts an external command in the background.
    
    :param command_args: List of strings, e.g., ["python", "script.py"]
    :return: A subprocess.Popen object (the handle for the process).
    """
    try:
        # We use Popen to start the process without blocking
        process = subprocess.Popen(
            command_args,
            stdout=subprocess.PIPE,  # Optional: capture output
            stderr=subprocess.PIPE,  # Optional: capture errors
            text=True
        )
        #print(f"Process started with PID: {process.pid}")
        return process
    except Exception as e:
        print(f"Failed to start process: {e}")
        return None

def stop(process):
    """
    Terminates the background process.
    
    :param process: The subprocess.Popen object returned by start().
    """
    if process is None:
        print("No process to stop.")
        return

    # Check if the process is still running
    if process.poll() is None:
        #print(f"Terminating process {process.pid}...")
        process.terminate()  # Sends SIGTERM (polite request to stop)
        
        try:
            # Wait up to 5 seconds for it to shut down gracefully
            process.wait(timeout=5)
            # print("Process stopped gracefully.")
        except subprocess.TimeoutExpired:
            # If it doesn't stop, kill it forcefully
            print("Process did not stop; forcing kill...")
            process.kill()  # Sends SIGKILL (immediate termination)
    else:
        print("Process had already finished.")