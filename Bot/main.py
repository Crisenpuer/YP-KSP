import os, json, sys, logging
from twitchio.ext import commands as cmds
import util.KSP as game

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')
log = logging.getLogger(__name__)

if os.path.exists('config.json'):
    with open('config.json') as f:
        CFG = json.load(f)
else:
    log.critical('Config file not found! You can download example file from here: https://github.com/Crisenpuer/YP-KSP/config-example.json', __name__, 5)
    sys.exit()

CFG_CHATBOT = CFG["chatbot"]
CFG_KSP = CFG["ksp"]

def wrap_command(func):
    async def wrapper(ctx:cmds.Context, *args, **kwargs):
        log.info('%s is executing command: %s', ctx.author.display_name, func.__name__)
        try:
            return await func(ctx, *args, **kwargs)
        except Exception as e:
            log.error('Error occured while executing command: %s', e)
    return wrapper

bot = cmds.Bot(token=CFG_CHATBOT["token"], prefix=CFG_CHATBOT["prefix"], initial_channels=CFG_CHATBOT["init-channels"])

@bot.command(name='restartbot')
@wrap_command
async def cmd_restartbot(ctx: cmds.Context) -> None:
    log.info(f"Restarting the bot (request by {ctx.author.name})")
    await ctx.send("Restarting bot...")
    if sys.platform.startswith('win'):
        os.system('cls')
    else:
        os.system('clear') 
    os.execl(sys.executable, sys.executable, *sys.argv)
    

@bot.command(name="commands")
@wrap_command
async def cmd_comands(ctx: cmds.Context) -> None:
    cmd_lst = CFG_CHATBOT["commands-list"]["alls"]
    if ctx.author.is_mod:
        cmd_lst.extend(CFG_CHATBOT["commands-list"]["mods"])

    await ctx.send(', '.join(cmd_lst))
    
@bot.command(name='botconsole')
@wrap_command
async def cmd_botconsole(ctx:cmds.Context) -> None:
    await ctx.send(f"https://twitch.tv/{CFG_CHATBOT["nick"]}")

@bot.command(name='startksp')
@wrap_command
async def cmd_startksp(ctx:cmds.Context) -> None:
    if ctx.author.is_mod:
        await ctx.send("Trying to start KSP...")
        try:
            game.start()
        except:
            ctx.send("Could not start KSP")
    else:
        ctx.send("You don't have permission to use that command")

@bot.command(name='killksp')
@wrap_command
async def cmd_startksp(ctx:cmds.Context) -> None:
    if ctx.author.is_mod:
        await ctx.send("Trying to kill KSP...")
        try:
            game.kill()
        except:
            ctx.send("Could not kill KSP")
    else:
        ctx.send("You don't have permission to use that command")

log.info("Starting the bot...")
bot.run()