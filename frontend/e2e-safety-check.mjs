import { chromium } from '@playwright/test';

/**
 * Live validation of the Safety Guide page against the running dev server.
 * Run:  node e2e-safety-check.mjs
 */
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1380, height: 900 } });

await page.goto('http://127.0.0.1:5173/');
await page.evaluate(() => localStorage.setItem('janvoice_user', JSON.stringify({
  sessionToken: 'validation-token', username: 'validation_user', role: 'CITIZEN', wardArea: 'Indore',
})));
await page.reload();
await page.waitForLoadState('networkidle');

// 1. Nav entry renders in English
const nav = page.locator('button.nav').filter({ hasText: /safety guide/i });
if (!(await nav.count())) throw new Error('FAIL: Safety Guide nav entry not found');
await nav.first().click();
await page.waitForTimeout(400);
await expectVisible(page, 'h1:has-text("Handle e-waste safely")');
await expectVisible(page, 'text=Never burn cables or boards to recover copper');
await expectVisible(page, 'text=Do not break CRT screens');
await expectVisible(page, 'text=Store devices separately in a dry, cool container');
console.log('PASS: English Safety Guide renders (nav, title, dos, donts)');

// 2. Spoken summary + CTA
await expectVisible(page, 'button.outline.tiny:has-text("Listen")');
await expectVisible(page, 'button.primary:has-text("Create a new lot")');
console.log('PASS: Listen button and Create-lot CTA render');

// 3. Hindi toggle re-renders bilingual copy
await page.locator('button.lang-btn').click();
await page.waitForTimeout(300);
await expectVisible(page, 'h1:has-text("ई-वेस्ट को सुरक्षित रखें")');
await expectVisible(page, 'text=तार या बोर्ड जलाकर तांबा न निकालें');
console.log('PASS: Hindi Safety Guide renders');

await page.screenshot({ path: 'test-results/safety-guide-hi.png', fullPage: true });
await page.locator('button.lang-btn').click();
await page.waitForTimeout(300);
await page.screenshot({ path: 'test-results/safety-guide-en.png', fullPage: true });

// 4. Page does not crash the app and topbar title updates
await expectVisible(page, 'h1:has-text("Handle e-waste safely")');
console.log('PASS: App stable after toggling back to English');

async function expectVisible(page, selector) {
  try {
    await page.locator(selector).first().waitFor({ state: 'visible', timeout: 6000 });
  } catch {
    throw new Error('FAIL: expected visible -> ' + selector);
  }
}

await browser.close();
console.log('ALL SAFETY PAGE CHECKS PASSED');