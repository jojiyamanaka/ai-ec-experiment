import { chromium } from 'playwright';

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();

page.on('response', async (res) => {
  const u = res.url();
  if (u.includes('/api/auth/login') || u.includes('/api/cart') || u.includes('/api/cart/items') || u.includes('/api/orders')) {
    console.log(`API ${res.status()} ${u}`);
  }
});

try {
  await page.goto('http://localhost:5173/auth/login', { waitUntil: 'domcontentloaded' });
  await page.locator('input[type="email"]').fill('member01@example.com');
  await page.locator('input[type="password"]').fill('password');
  await page.getByRole('button', { name: 'Login' }).click();

  await page.waitForTimeout(1000);
  const tokenAfterLogin = await page.evaluate(() => localStorage.getItem('authToken'));
  console.log('tokenAfterLogin', tokenAfterLogin ? 'present' : 'missing');

  await page.goto('http://localhost:5173/item', { waitUntil: 'domcontentloaded' });
  await page.locator('a[href^="/item/"]').first().click();
  await page.waitForURL(/\/item\/\d+$/, { timeout: 15000 });
  console.log('detailUrl', page.url());

  await page.getByRole('button', { name: 'カートに追加' }).click();

  const result = await Promise.race([
    page.waitForURL('**/order/cart', { timeout: 10000 }).then(() => 'to_cart'),
    page.locator('text=カートへの追加に失敗しました').waitFor({ timeout: 10000 }).then(() => 'add_error'),
  ]).catch(() => 'timeout');
  console.log('addResult', result, 'url', page.url());

  const state = await page.evaluate(() => ({
    token: localStorage.getItem('authToken'),
    sessionId: localStorage.getItem('sessionId'),
    body: document.body.innerText.slice(0, 300),
  }));
  console.log(JSON.stringify(state, null, 2));
} finally {
  await browser.close();
}
