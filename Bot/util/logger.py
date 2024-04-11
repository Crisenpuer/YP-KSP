from datetime import datetime
from colorama import init, Fore, Back

init()

def now():
    return datetime.now().strftime("%H:%M:%S")
def today():
    return datetime.now().strftime("%Y-%m-%d")

def log(msg:str, exec:str="DEBUG", type:int=1):
    if type > 5 or type < 1:
        raise ValueError(f'\'type\' is out of range ({type}) where min is 1 and max is 5')

    now_str = f"{today()} {now()}"
    logparts = [f'{Fore.LIGHTBLACK_EX}{now_str}{Fore.RESET}']
    if type == 1:
        logparts.append(f' {Fore.LIGHTBLUE_EX}INFO{Fore.RESET}   ')
    elif type == 2:
        logparts.append(f' {Fore.GREEN}SUCCESS{Fore.RESET}')
    elif type == 3:
        logparts.append(f' {Fore.RED}FAILURE{Fore.RESET}')
    elif type == 4:
        logparts.append(f' {Back.YELLOW}{Fore.RED}WARNING{Fore.RESET}{Back.RESET}')
    elif type == 5:
        logparts.append(f' {Back.RED}{Fore.WHITE}ERROR{Fore.RESET}{Back.RESET}  ')
    logparts.append(f'{Fore.MAGENTA}{exec}{Fore.RESET}')
    logparts.append(f' {msg}')
    print("".join(logparts))