import { chromium } from 'playwright';

const SCREEN_DIR = '/work/screenshots';
const fs = await import('fs');
fs.mkdirSync(SCREEN_DIR, { recursive: true });

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
page.on('console', msg => console.log('[console]', msg.type(), msg.text()));
page.on('pageerror', err => console.log('[pageerror]', err.message));

async function shot (name) {
  await page.screenshot({ path: `${SCREEN_DIR}/${name}.png`, fullPage: true });
  console.log('screenshot:', name, page.url());
}

// 1. Log into Habitica as the fresh, consent-undecided test subject
await page.goto('http://localhost:5173/login', { waitUntil: 'networkidle' });
await shot('1_habitica_login_page');
await page.fill('#usernameInput', 'priam-browser-test@example.com');
await page.fill('#passwordInput', 'BrowserTest123!');
await page.getByRole('button', { name: 'Login' }).click();

// Give the app-shell a moment to mount, fetch the user, and let the router's
// afterEach hook fire the PRIAM consent redirect (§4bis/§8.7 pattern).
await page.waitForTimeout(4000);
await shot('2_after_login_redirect');
console.log('URL after login+redirect wait:', page.url());

// 2. If redirected to PRIAM-Frontend's consent page, we will hit Keycloak's
// login form first (angular-oauth2-oidc APP_INITIALIZER). Log in with the
// SAME credentials (auto-provisioned by provision_keycloak_user()).
if (page.url().includes('8080') || page.url().includes('keycloak')) {
  await shot('3_keycloak_login_form');
  const userField = page.locator('#username, input[name="username"]').first();
  await userField.fill('priam-browser-test@example.com');
  await page.locator('#password, input[name="password"]').first().fill('BrowserTest123!');
  await page.locator('#kc-login, button[type="submit"], input[type="submit"]').first().click();
  await page.waitForTimeout(3000);
  await shot('4_after_keycloak_login');
}

console.log('Final URL:', page.url());
await shot('5_priam_consent_page');

// 3. Try to find and toggle the OPTIONAL "Push Notifications" processing.
const bodyText = await page.textContent('body').catch(() => '');
console.log('Consent page body snippet:', (bodyText || '').slice(0, 2000));

await browser.close();
