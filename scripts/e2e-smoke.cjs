const { chromium } = require('C:\\Users\\焦志龙\\.cache\\codex-runtimes\\codex-primary-runtime\\dependencies\\node\\node_modules\\playwright')
const fs = require('node:fs')
const path = require('node:path')

const webBase = process.env.GIS_AGENT_WEB_BASE || 'http://127.0.0.1:5174'

async function main() {
  const outputDir = path.join(process.cwd(), 'test-artifacts')
  fs.mkdirSync(outputDir, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: 'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe' })
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
  const errors = []
  const checks = []
  page.on('console', message => { if (message.type() === 'error') errors.push(`console: ${message.text()}`) })
  page.on('pageerror', error => errors.push(`pageerror: ${error.message}`))
  page.on('response', response => { if (response.status() >= 400) errors.push(`http ${response.status()}: ${response.url()}`) })

  const visit = async (route, selector) => {
    await page.goto(`${webBase}${route}`, { waitUntil: 'domcontentloaded' })
    if (selector) await page.waitForSelector(selector, { timeout: 30000 })
    await page.waitForTimeout(400)
    checks.push(route)
  }

  await visit('/login', '.login-card')
  await page.locator('input[autocomplete="current-password"]').fill('admin123')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL('**/dashboard', { timeout: 30000 })
  await page.waitForSelector('.dashboard-grid')
  checks.push('/dashboard')

  await visit('/projects', '.project-table')
  const firstProjectLink = await page.locator('.table-row').first().getAttribute('href')
  if (!firstProjectLink) throw new Error('No project link found')
  const projectId = firstProjectLink.split('/')[2]

  await page.getByRole('button', { name: /新建项目/ }).click()
  await page.waitForSelector('.modal-backdrop')
  await page.locator('.modal-backdrop').click({ position: { x: 5, y: 5 } })
  if (!(await page.locator('.create-dialog').isVisible())) throw new Error('Project dialog closed after backdrop click')
  await page.locator('.create-dialog header button').click()
  checks.push('project-dialog-backdrop-lock')

  await visit(`/projects/${projectId}`, '.war-room')
  const layerButton = page.getByTitle('隐藏业务图层')
  await layerButton.click()
  await page.getByTitle('显示业务图层').waitFor()
  await page.getByTitle('显示业务图层').click()
  await page.getByTitle('复位三维场景').click()
  checks.push('project-scene-controls')
  await visit(`/projects/${projectId}/requirements`, '.analysis-grid')
  await page.getByRole('button', { name: /运行需求分析Agent/ }).click()
  await page.waitForFunction(() => document.body.textContent.includes('需求要点') && document.querySelectorAll('.demand-points li').length > 0, null, { timeout: 30000 })
  checks.push('requirement-analysis-run')

  await visit(`/projects/${projectId}/products`, '.match-layout')
  await page.waitForFunction(() => document.querySelectorAll('.product-row').length >= 1, null, { timeout: 30000 })
  await page.getByRole('button', { name: 'GIS平台', exact: true }).click()
  if (!(await page.getByRole('button', { name: 'GIS平台', exact: true }).evaluate(node => node.classList.contains('active')))) throw new Error('Product category filter did not activate')
  checks.push('product-match-run')
  checks.push('product-category-filter')

  await visit(`/projects/${projectId}/retrieval`, '.retrieval-layout')
  await page.getByRole('button', { name: /开始检索/ }).click()
  await page.waitForFunction(() => document.querySelectorAll('.evidence article').length >= 1, null, { timeout: 60000 })
  checks.push('ragflow-retrieval-run')

  await visit(`/projects/${projectId}/proposal`, '.proposal-layout')
  checks.push('solution-result-view')
  await visit('/knowledge', '.kb-layout')
  await page.getByRole('button', { name: /同步 RAGFlow/ }).click()
  await page.waitForFunction(() => document.body.textContent.includes('已同步'), null, { timeout: 30000 })
  checks.push('knowledge-sync')

  await visit('/generations', '.record-table')
  await visit('/settings/models', '.model-grid')
  await page.getByRole('button', { name: /测试连接/ }).first().click()
  await page.waitForTimeout(800)
  checks.push('model-connection-test')

  await visit('/settings/users', '.user-page')
  await page.getByRole('button', { name: /新增用户/ }).click()
  await page.waitForSelector('.dialog-mask')
  await page.locator('.dialog-mask').click({ position: { x: 5, y: 5 } })
  if (!(await page.locator('.dialog').isVisible())) throw new Error('User dialog closed after backdrop click')
  await page.locator('.dialog header button').click()
  checks.push('user-dialog-backdrop-lock')

  await page.screenshot({ path: path.join(outputDir, 'full-function-smoke.png'), fullPage: false })
  const result = { projectId, checks, errors }
  fs.writeFileSync(path.join(outputDir, 'e2e-smoke-result.json'), JSON.stringify(result, null, 2))
  console.log(JSON.stringify(result, null, 2))
  await browser.close()
  if (errors.length) process.exitCode = 2
}

main().catch(error => { console.error(error); process.exitCode = 1 })
