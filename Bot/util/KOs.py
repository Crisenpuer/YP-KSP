import telnetlib3, time
import logging

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')
log = logging.getLogger(__name__)


async def connect(ip:str, port:int) -> tuple[any, any]:
    reader, writer = await telnetlib3.open_connection(ip, port)
    return reader, writer

async def select_cpu(writer, cpu_idx:int) -> None:
    writer.write(b"^D")
    writer.write(b"^D")
    time.sleep(0.1)

    writer.write(cpu_idx.encode())

async def send_cmd(writer, cmd:str, ctx_author_name: str) -> None:
    if cmd.endswith('.'):
        writer.write(f"{cmd.encode()} // {ctx_author_name}")
    else:
        writer.write(f"{cmd.encode()}. // {ctx_author_name}")