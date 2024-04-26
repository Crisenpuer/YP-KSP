import os, json, sys
from twitchio.ext import commands as cmds
from util.logger import log

if os.path.exists('config.json'):
    with open('config.json') as f:
        CONFIG = json.load(f)
else:
    log('Config file not found! You can download example file from here: https://github.com/Crisenpuer/YP-KSP/config-example.json', __name__, 5)
    sys.exit()

def log_command(func):
    async def wrapper(ctx, *args, **kwargs):
        log(f'Executing command: {func.__name__}')
        return await func(ctx, *args, **kwargs)
    return wrapper

class Bot(cmds.bot):
    def __init__(self):
        super().__init__(token=CONFIG['chatbot','token'], prefix=CONFIG['chatbot']['prefix'], nick=CONFIG['chatbot']['nick'])



    @cmds.command(name='commands')
    @log_command
    async def comds(ctx: cmds.Context):
        ctx.send('For all: !commands, !help')
        ctx.send('Follow: ')
        ctx.send('Mod: ')






bot = Bot()
bor