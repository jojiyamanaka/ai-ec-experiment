import { chromium } from 'playwright';

const pageUrl = 'http://localhost:5173/auth/login';

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();

try {
  await page.goto(pageUrl, { waitUntil: 'domcontentloaded' });
  await page.locator('input[type="email"]').fill('member01@example.com');
  await page.locator('input[type="password"]').fill('password');
  await page.getByRole('button', { name: 'Login' }).click();

  await page.waitForTimeout(1500);
  await page.goto('http://localhost:5173/item', { waitUntil: 'domcontentloaded' });

  const state = await page.evaluate(async () => {
    const token = localStorage.getItem('authToken');
    const authUser = localStorage.getItem('authUser');

    let cartStatus = -1;
    let cartBody = null;
    if (token) {
      const res = await fetch('http://localhost:3001/api/cart', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-Session-Id': 'session-playwright-debug',
        },
      });
      cartStatus = res.status;
      cartBody = await res.text();
    }

    return { token, authUser, cartStatus, cartBody, url: location.href, html: document.body.innerText.slice(0, 400) };
  });

  console.log(JSON.stringify(state, null, 2));
} finally {
  await browser.close();
}
