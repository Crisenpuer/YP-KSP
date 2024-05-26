import json, psutil, os, subprocess, logging
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')
log = logging.getLogger(__name__)


with open('config.json') as f:
    CFG = json.load(f)

CFG_KSP = CFG["ksp"]

def kill():
    global KSPEXEC
    for process in psutil.process_iter(['pid', 'name']):
        try:
            if process.info['name'] == CFG_KSP["exec"]:
                psutil.Process(process.info['pid']).kill()
        except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess) as e:
            null = None

def start():
    subprocess.Popen([f"{CFG_KSP["path"]}{CFG_KSP["exec"]}"], shell=True)