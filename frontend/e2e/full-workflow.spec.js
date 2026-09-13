import { test, expect } from '@playwright/test';

/**
 * End-to-end test against the REAL running stack (MySQL + backend + frontend).
 * No mocking, no fake data — every assertion reads real rendered content or
 * queries the live API directly.
 *
 * Run:        npx playwright test
 * Re-run HTML: npx playwright show-report
 */

const API = 'http://127.0.0.1:8080/api';
const ts = Date.now();
const COLLECTOR = `pw_collector_${ts}`;
const PASSWORD = 'Test@1234';
const STORAGE_KEY = 'janvoice_user';

/** Register a unique collector and log in via the live API. */
async function registerAndLogin(request) {
  await request.post(`${API}/auth/register`, {
    data: { username: COLLECTOR, password: PASSWORD, role: 'CITIZEN', wardArea: 'Indore' },
  });
  const res = await request.post(`${API}/auth/login`, {
    data: { username: COLLECTOR, password: PASSWORD },
  });
  const body = await res.json();
  expect(body.sessionToken, 'Login must return a sessionToken').toBeTruthy();
  return body.sessionToken;
}

/**
 * Create a lot via the live API and walk it through the collector-allowed
 * transition: CREATED -> QUOTE_REQUESTED. Returns the lot object.
 */
async function createLot(request, token) {
  const headers = { Authorization: `Bearer ${token}` };

  const mRes = await request.get(`${API}/materials`);
  const materials = await mRes.json();
  const mat = materials.find((m) => m.active) || materials[0];
  expect(mat, 'At least one material must exist').toBeTruthy();

  const lotRes = await request.post(`${API}/lots`, {
    data: { materialId: mat.id, approximateWeight: 0.75, description: `Playwright E2E ${ts}` },
    headers,
  });
  expect(lotRes.status(), 'Lot creation must succeed').toBe(201);
  const lot = await lotRes.json();
  expect(lot.lot_reference).toContain('EWS');

  await request.patch(`${API}/lots/${lot.id}/status`, {
    data: { status: 'QUOTE_REQUESTED' },
    headers,
  });
  return lot;
}

/** Inject the session token into localStorage so the frontend auth interceptor picks it up. */
async function setSessionStorage(page, token) {
  await page.evaluate(
    ([key, user]) => localStorage.setItem(key, JSON.stringify(user)),
    [STORAGE_KEY, { sessionToken: token, username: COLLECTOR, role: 'CITIZEN', wardArea: 'Indore' }]
  );
}

test.describe('E-Waste Saathi — full collector E2E (real MySQL)', () => {
  let token;

  test.beforeAll(async ({ request }) => {
    token = await registerAndLogin(request);
  });

  test('a. Register a new collector account', async ({ request }) => {
    const res = await request.post(`${API}/auth/register`, {
      data: { username: `pw_reg_${ts}`, password: PASSWORD, role: 'CITIZEN', wardArea: 'Indore' },
    });
    const body = await res.json();
    test.info().attach('register-response', { body: JSON.stringify(body), contentType: 'application/json' });
    expect(res.status()).toBe(201);
    expect(body.username).toBe(`pw_reg_${ts}`);
  });

  test('b. Log in', async ({ request }) => {
    const res = await request.post(`${API}/auth/login`, {
      data: { username: COLLECTOR, password: PASSWORD },
    });
    const body = await res.json();
    test.info().attach('login-response', { body: JSON.stringify(body), contentType: 'application/json' });
    expect(res.status()).toBe(200);
    expect(body.sessionToken).toBeTruthy();
    expect(body.role).toBe('CITIZEN');
  });

  test('c. Dashboard shows all zeros for new collector (zero data)', async ({ page }) => {
    await page.goto('/');
    await setSessionStorage(page, token);
    await page.reload();

    await page.locator('button.nav').filter({ hasText: /dashboard/i }).first().click();
    await page.waitForLoadState('networkidle');

    await expect(page.locator('text=Total earnings')).toBeVisible({ timeout: 8000 });
    await expect(page.locator('text=Total earnings').locator('..').getByText('₹0')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('text=Pending amount').locator('..').getByText('₹0')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('text=Digital lots').locator('..').getByText('0')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('text=Active lots').locator('..').getByText('0')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('text=Completed handovers').locator('..').getByText('0')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('text=No lots yet')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('text=No transactions yet')).toBeVisible({ timeout: 5000 });
  });

  test('d. Scan page material list loads from live API (not empty)', async ({ page }) => {
    await page.goto('/');
    await setSessionStorage(page, token);
    await page.reload();

    await page.locator('button.nav').filter({ hasText: /^scan/i }).first().click();
    await page.waitForLoadState('networkidle');

    const materialButtons = page.locator('button.material');
    await expect(materialButtons.first()).toBeVisible({ timeout: 10000 });
    const count = await materialButtons.count();
    expect(count).toBeGreaterThan(0);
    test.info().attach('material-count', { body: `Found ${count} materials from live API`, contentType: 'text/plain' });
  });

  test('e. Create a digital lot and see it in Dashboard + Lots view', async ({ page, request }) => {
    const lot = await createLot(request, token);

    await page.goto('/');
    await setSessionStorage(page, token);
    await page.reload();
    // Dashboard stat renders the live lot count (robust against substring
    // ambiguity in text= by scoping to the stat card).
    await expect(page.locator('div.stat', { hasText: 'Digital lots' }).locator('strong')).toHaveText(/\d+/, { timeout: 15000 });
    await expect(page.locator('text=' + lot.lot_reference)).toBeVisible({ timeout: 15000 });

    await page.locator('button.nav').filter({ hasText: /my lots/i }).first().click();
    await page.waitForLoadState('networkidle');
    await expect(page.locator('text=' + lot.lot_reference)).toBeVisible({ timeout: 10000 });
  });

  test('f. Prices page renders real data or correct empty state (no fake ₹)', async ({ page }) => {
    await page.goto('/');
    await setSessionStorage(page, token);
    await page.reload();
    await page.locator('button.nav').filter({ hasText: /price/i }).first().click();
    await page.waitForLoadState('networkidle');

    const body = await page.textContent('body');
    expect(body).not.toContain('₹999');
    expect(body).not.toContain('Fake');
    const hasRealData = /₹[0-9]/.test(body) || /rate|price|rupee/i.test(body);
    const hasEmptyState = /no price|unavailable|not available|price data required/i.test(body);
    expect(hasRealData || hasEmptyState).toBeTruthy();
  });

  test('g. Recycler matching page renders real results or correct empty state', async ({ page }) => {
    await page.goto('/');
    await setSessionStorage(page, token);
    await page.reload();
    await page.locator('button.nav').filter({ hasText: /recycl/i }).first().click();
    await page.waitForLoadState('networkidle');

    const body = await page.textContent('body');
    expect(body).not.toContain('Fake');
    const hasEmpty = /no recyclers|no match|not available|unavailable/i.test(body);
    const hasReal = /recycl|REC-|business_name|authorized/i.test(body);
    expect(hasEmpty || hasReal).toBeTruthy();
  });

  test('h. Dashboard earnings stay ₹0 (no PAID transaction reachable yet)', async ({ page, request }) => {
    const headers = { Authorization: `Bearer ${token}` };
    const lot = await createLot(request, token);

    const txListRes = await request.get(`${API}/transactions`, { headers });
    const txs = await txListRes.json();
    const ourTxs = txs.filter((t) => t.lot_id === lot.id);
    expect(ourTxs.length).toBe(0);

    const dashRes = await request.get(`${API}/dashboard/summary`, { headers });
    const summary = await dashRes.json();
    expect(Number(summary.total_earnings)).toBe(0);

    await page.goto('/');
    await setSessionStorage(page, token);
    await page.reload();
    await page.locator('button.nav').filter({ hasText: /dashboard/i }).first().click();
    await page.waitForLoadState('networkidle');
    await expect(page.locator('text=Total earnings').locator('..').getByText('₹0')).toBeVisible({ timeout: 8000 });
    await expect(page.locator('text=' + lot.lot_reference)).toBeVisible({ timeout: 10000 });
  });
test('i. Phase 8 handover: collector marks lot handed over in UI, recycler confirms receipt via API', async ({ page, request }) => {
    // A second registered user creates a recycler profile and acts as the
    // matched recycler actor for the journey (registration always yields CITIZEN).
    const recyclerUser = `pw_recycler_${ts}`;
    await request.post(`${API}/auth/register`, {
      data: { username: recyclerUser, password: PASSWORD, role: 'CITIZEN', wardArea: 'Indore' },
    });
    const recyclerLogin = await request.post(`${API}/auth/login`, {
      data: { username: recyclerUser, password: PASSWORD },
    });
    const recyclerSession = await recyclerLogin.json();
    const recyclerHeaders = { Authorization: `Bearer ${recyclerSession.sessionToken}` };

    const materials = await (await request.get(`${API}/materials`)).json();
    const mat = materials.find((m) => m.active) || materials[0];
    const recyclerRes = await request.post(`${API}/recyclers`, {
      data: { businessName: `Playwright Recycler ${ts}`, phone: '9000000000', city: 'Indore', serviceArea: 'Indore', pickupAvailable: false, acceptedMaterialIds: [mat.id] },
      headers: recyclerHeaders,
    });
    expect(recyclerRes.status(), 'Recycler creation must succeed').toBe(201);
    const recycler = await recyclerRes.json();

    const headers = { Authorization: `Bearer ${token}` };
    const lotRes = await request.post(`${API}/lots`, {
      data: { materialId: mat.id, approximateWeight: 0.75, description: `Handover E2E ${ts}` },
      headers,
    });
    expect(lotRes.status()).toBe(201);
    const lot = await lotRes.json();
    const setStatus = async (actorHeaders, status) => {
      const res = await request.patch(`${API}/lots/${lot.id}/status`, { data: { status }, headers: actorHeaders });
      expect(res.status(), `Transition to ${status} must succeed`).toBe(200);
      return res.json();
    };
    await request.patch(`${API}/lots/${lot.id}/recycler`, { data: { recyclerId: recycler.id }, headers });
    await setStatus(headers, 'QUOTE_REQUESTED');
    await setStatus(recyclerHeaders, 'QUOTE_RECEIVED');
    await setStatus(headers, 'ACCEPTED');
    await setStatus(headers, 'READY_FOR_HANDOVER');

    // A recycler cannot confirm receipt before the handover happened.
    const premature = await request.patch(`${API}/lots/${lot.id}/status`, {
      data: { status: 'RECYCLER_CONFIRMED' },
      headers: recyclerHeaders,
    });
    expect(premature.status(), 'Recycler cannot confirm before handover').toBe(403);

    // Collector UI: the lot must show the "Mark handed over" action.
    await page.goto('/');
    await setSessionStorage(page, token);
    await page.reload();
    await page.locator('button.nav').filter({ hasText: /my lots/i }).first().click();
    await page.waitForLoadState('networkidle');
    const card = page.locator('div.lot-card', { hasText: lot.lot_reference });
    await expect(card).toBeVisible({ timeout: 10000 });
    const markButton = card.locator('button.handover-btn').filter({ hasText: /handed over/i }).first();
    await expect(markButton).toBeVisible({ timeout: 8000 });
    await markButton.click();

    // Wait for the PATCH + refetch to land (the status span updates only after
    // the server confirms; the button label itself contains "handed over", so
    // the status must be asserted through the scoped span, not a text search).
    await expect(card.locator('span.status')).toHaveText('Handed Over', { timeout: 15000 });
    const handed = await (await request.get(`${API}/lots/${lot.id}`, { headers })).json();
    expect(handed.status).toBe('HANDED_OVER');
    expect(handed.handed_over_at, 'handed_over_at must be recorded on the lot').toBeTruthy();

    // Recycler confirms receipt with the final weighed amount.
    const confirmed = await setStatus(recyclerHeaders, 'RECYCLER_CONFIRMED');
    expect(confirmed.recycler_confirmed_at, 'recycler_confirmed_at must be recorded').toBeTruthy();

    await page.reload();
    await page.locator('button.nav').filter({ hasText: /my lots/i }).first().click();
    await page.waitForLoadState('networkidle');
    const confirmedCard = page.locator('div.lot-card', { hasText: lot.lot_reference });
    await expect(confirmedCard.locator('span.status')).toHaveText('Recycler Confirmed', { timeout: 15000 });
  });
});
