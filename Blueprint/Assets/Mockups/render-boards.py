import argparse
import asyncio
from pathlib import Path
from playwright.async_api import async_playwright


async def render_all(boards_dir: Path, output_dir: Path, width: int, height: int, wait_ms: int) -> None:
    boards = sorted(boards_dir.glob('*.html'))
    if not boards:
        print(f'No .html boards found in {boards_dir}')
        return

    output_dir.mkdir(parents=True, exist_ok=True)

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(
            viewport={'width': width, 'height': height},
            device_scale_factor=1,
        )
        page = await context.new_page()

        for board in boards:
            out = output_dir / f'{board.stem}.png'
            url = board.resolve().as_uri()

            await page.goto(url, wait_until='load')
            if wait_ms > 0:
                await page.wait_for_timeout(wait_ms)
            await page.screenshot(path=str(out), full_page=False)
            print(f'OK  {board.name} -> {out}')

        await context.close()
        await browser.close()


def main() -> None:
    parser = argparse.ArgumentParser(description='Render HTML boards to PNG using Playwright (Python).')
    parser.add_argument('--boards-dir', default='./Boards')
    parser.add_argument('--output-dir', default='./Exports')
    parser.add_argument('--width', type=int, default=500)
    parser.add_argument('--height', type=int, default=990)
    parser.add_argument('--wait-ms', type=int, default=1800)
    args = parser.parse_args()

    boards_dir = Path(args.boards_dir).resolve()
    output_dir = Path(args.output_dir).resolve()

    if not boards_dir.exists():
        raise SystemExit(f'Boards directory not found: {boards_dir}')

    asyncio.run(render_all(boards_dir, output_dir, args.width, args.height, args.wait_ms))


if __name__ == '__main__':
    main()
