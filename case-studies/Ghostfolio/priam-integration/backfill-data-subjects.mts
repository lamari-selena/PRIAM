// One-off backfill for users created BEFORE PriamService.onUserRegistered()
// was wired into UserService.createUser() (Docs/PRIAM-INTEGRATION-PLAYBOOK.md
// §4bis, last point). Not a permanent application endpoint - reuses
// Ghostfolio's own Prisma client directly, the same pattern prisma/seed.mts
// already uses in this codebase.
//
// Usage (from case-studies/Ghostfolio, with PRIAM_ACTOR_URL/PRIAM_DATA_URL
// pointing at the running PRIAM stack, e.g. via `docker exec ghostfolio` or
// a local `.env` with the Docker-network hostnames replaced by localhost):
//   npx tsx priam-integration/backfill-data-subjects.mts
import { PrismaPg } from '@prisma/adapter-pg';
import { PrismaClient } from '@prisma/client';

const adapter = new PrismaPg({
  connectionString: process.env.DIRECT_URL ?? process.env.DATABASE_URL
});
const prisma = new PrismaClient({ adapter });

const ACTOR_URL = process.env.PRIAM_ACTOR_URL;
const DATA_URL = process.env.PRIAM_DATA_URL;
const DATA_SUBJECT_CATEGORY_ID = 1;
const USER_DATA_IDS = [1, 2, 3, 4];
const ACCOUNT_DATA_IDS = [5, 6, 7, 8];

async function registerDataSubject(idRef: string) {
  // Idempotent upsert-by-idRef on the Actor-service side (playbook §4bis) -
  // safe to replay for a user already registered by the live sign-up hook.
  await fetch(`${ACTOR_URL}/api/DataSubject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idRef, dataSubjectCategoryId: DATA_SUBJECT_CATEGORY_ID })
  });
}

async function reportProcessedData(idRef: string, dataIds: number[]) {
  const idResponse = await fetch(`${ACTOR_URL}/api/DataSubjectId/${encodeURIComponent(idRef)}`);
  if (!idResponse.ok) {
    return;
  }
  const dataSubjectId = await idResponse.json();
  await fetch(`${DATA_URL}/api/processed-data/add?subjectId=${dataSubjectId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dataIds)
  });
}

async function main() {
  if (!ACTOR_URL || !DATA_URL) {
    console.error('PRIAM_ACTOR_URL / PRIAM_DATA_URL not set - aborting.');
    process.exit(1);
  }

  const users = await prisma.user.findMany({
    select: { id: true, _count: { select: { accounts: true } } }
  });

  console.log(`Backfilling ${users.length} existing user(s)...`);

  for (const user of users) {
    // eslint-disable-next-line no-await-in-loop
    await registerDataSubject(user.id);
    // eslint-disable-next-line no-await-in-loop
    await reportProcessedData(user.id, USER_DATA_IDS);

    if (user._count.accounts > 0) {
      // eslint-disable-next-line no-await-in-loop
      await reportProcessedData(user.id, ACCOUNT_DATA_IDS);
    }

    console.log(`  - ${user.id}: registered, reported ${user._count.accounts > 0 ? 'User + Account' : 'User'} data`);
  }

  console.log('Backfill complete.');
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
