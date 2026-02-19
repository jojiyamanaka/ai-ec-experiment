import { chromium } from 'playwright';

const BASE_URL = 'http://localhost:5173';
const API_BASE = 'http://localhost:3001';
const EMAIL = 'member01@example.com';
const PASSWORD = 'password';
const ORDER_COUNT = 4;

async function loginByApi() {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: EMAIL, password: PASSWORD }),
  });
  const json = await res.json();
  if (!json?.success || !json?.data?.token || !json?.data?.user) {
    throw new Error(`login failed: ${JSON.stringify(json)}`);
  }
  return { token: json.data.token, user: json.data.user };
}

const { token, user } = await loginByApi();

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();

async function injectAuth() {
  await page.evaluate(({ token, user }) => {
    localStorage.setItem('authToken', token);
    localStorage.setItem('authUser', JSON.stringify(user));
    if (!localStorage.getItem('sessionId')) {
      localStorage.setItem('sessionId', `session-pw-${Date.now()}`);
    }
  }, { token, user });
}

page.on('response', (res) => {
  const u = res.url();
  if (u.includes('/api/cart/items') || u.includes('/api/orders') || u.includes('/api/cart')) {
    console.log(`API ${res.status()} ${u}`);
  }
});

async function placeOneOrder(index) {
  await page.goto(`${BASE_URL}/item`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('a[href^="/item/"]', { timeout: 20000 });
  await page.waitForTimeout(500);
  await injectAuth();

  await page.locator('a[href^="/item/"]').first().click();
  await page.waitForURL(/\/item\/\d+$/, { timeout: 15000 });
  await page.waitForTimeout(200);
  await injectAuth();

  await page.getByRole('button', { name: 'カートに追加' }).click();
  await page.waitForURL('**/order/cart', { timeout: 15000 });
  await injectAuth();

  await page.getByRole('button', { name: 'レジに進む' }).click();
  await page.waitForURL('**/order/reg', { timeout: 15000 });
  await injectAuth();

  await page.getByRole('button', { name: '注文を確定する' }).click();
  await page.waitForURL('**/order/complete', { timeout: 15000 });
  await page.getByRole('heading', { name: 'ご注文ありがとうございます' }).waitFor({ timeout: 15000 });

  const orderNumber = (await page.locator('p.mt-2.font-serif.text-2xl.text-zinc-900').first().textContent())?.trim();
  console.log(`ORDER_${index + 1}_DONE orderNumber=${orderNumber || 'N/A'}`);

  await page.getByRole('link', { name: '買い物を続ける' }).click();
  await page.waitForURL('**/item', { timeout: 15000 });
}

try {
  await page.goto(`${BASE_URL}/item`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(2000);
  await injectAuth();

  for (let i = 0; i < ORDER_COUNT; i += 1) {
    await placeOneOrder(i);
  }

  console.log(`SEED_DONE placed=${ORDER_COUNT}`);
} catch (error) {
  console.error('SEED_FAILED', error);
  process.exitCode = 1;
} finally {
  await browser.close();
}
