"""Runs one ClickHouse query through a running grok_connect_adbc over /query_socket and
checks the Arrow result, the way Datlas consumes it.

Usage: image_smoke_query.py <ws_url> <clickhouse_host> <clickhouse_password>
Requires: websockets, pyarrow
"""
import asyncio
import json
import sys

import pyarrow as pa
import pyarrow.compute as pc
import websockets

ROWS = 250_000


async def run(url, host, password):
    call = {
        "id": "smoke",
        "func": {
            "#type": "DataQuery",
            "query": f"SELECT number AS n, toString(number % 100) AS s FROM system.numbers LIMIT {ROWS}",
            "connection": {
                "id": "smoke",
                "dataSource": "ClickHouseADBC",
                "parameters": {"server": host, "port": 8123},
                "credentials": {"parameters": {"login": "default", "password": password}},
            },
            "params": [],
            "options": {},
        },
        "options": {},
    }
    tables = []
    async with websockets.connect(url, max_size=None) as ws:
        assert await ws.recv() == "CONNECTED"
        await ws.send("QUERY " + json.dumps(call))
        while True:
            msg = await ws.recv()
            if msg.startswith("DATAFRAME PART SIZE:"):
                await ws.send("DATAFRAME PART SIZE RECEIVED")
                tables.append(pa.ipc.open_stream(await ws.recv()).read_all())
                await ws.send("PART OK")
            elif msg.startswith("COMPLETED"):
                await ws.send("COMPLETED_OK")
                assert await ws.recv() == "EOF"
                break
            else:
                raise AssertionError(f"unexpected message: {msg[:500]}")

    rows = sum(t.num_rows for t in tables)
    total = sum(pc.sum(t.column("n")).as_py() or 0 for t in tables)
    distinct = set()
    for t in tables:
        distinct.update(t.column("s").to_pylist())
    print(f"chunks={len(tables)} rows={rows} sum(n)={total} distinct(s)={len(distinct)}")
    assert rows == ROWS, f"expected {ROWS} rows"
    assert total == ROWS * (ROWS - 1) // 2, "sum(n) mismatch"
    assert len(distinct) == 100, "distinct(s) mismatch"


if __name__ == "__main__":
    asyncio.run(run(*sys.argv[1:4]))
