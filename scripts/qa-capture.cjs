const { chromium } = require('C:\\Users\\焦志龙\\.cache\\codex-runtimes\\codex-primary-runtime\\dependencies\\node\\node_modules\\playwright')
const fs = require('node:fs')
const path = require('node:path')

async function main() {
  const outDir = path.join(process.cwd(), 'design-qa-artifacts')
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: 'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe' })
  const page = await browser.newPage({ viewport: { width: 1440, height: 1024 }, deviceScaleFactor: 1 })
  const errors = []
  page.on('console', (message) => { if (message.type() === 'error') errors.push(`console: ${message.text()}`) })
  page.on('pageerror', (error) => errors.push(`pageerror: ${error.message}`))
  page.on('response', (response) => { if (response.status() >= 400) errors.push(`http ${response.status()}: ${response.url()}`) })

  const capture = async (name, route, wait = 1200) => {
    await page.goto(`http://127.0.0.1:4173${route}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(wait)
    await page.screenshot({ path: path.join(outDir, `${name}.png`), fullPage: false })
  }

  await capture('01-login', '/login', 1600)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL('**/dashboard')
  await page.waitForTimeout(1000)
  await page.screenshot({ path: path.join(outDir, '02-dashboard.png') })
  await page.goto('http://127.0.0.1:4173/projects/prj-001', { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.war-room', { timeout: 20000 })
  await page.waitForTimeout(4000)
  await page.screenshot({ path: path.join(outDir, '03-project-overview.png') })
  await capture('04-requirements', '/projects/prj-001/requirements', 1200)
  await page.getByRole('button', { name: /重新分析/ }).click()
  await page.waitForTimeout(500)
  await capture('05-product-match', '/projects/prj-001/products', 1500)
  await capture('06-retrieval', '/projects/prj-001/retrieval', 1200)
  await page.getByRole('button', { name: /开始检索/ }).click()
  await page.waitForTimeout(500)
  await capture('07-proposal', '/projects/prj-001/proposal', 1300)
  await capture('08-knowledge', '/knowledge', 900)
  await capture('09-model-settings', '/settings/models', 900)

  fs.writeFileSync(path.join(outDir, 'browser-errors.json'), JSON.stringify(errors, null, 2))
  console.log(JSON.stringify({ screenshots: 9, errors }, null, 2))
  await browser.close()
}

main().catch((error) => { console.error(error); process.exitCode = 1 })
