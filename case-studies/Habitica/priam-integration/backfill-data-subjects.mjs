// One-off backfill for Habitica users that existed before register_data_subject()
// (server/libs/priam.js) was wired into sign-up (Docs/PRIAM-INTEGRATION-PLAYBOOK.md
// §4bis, last point). Safe to re-run (idempotent: PRIAM's DataSubject upsert is
// idempotent by idRef, and reportProcessedData just increments a reference count).
//
// Usage (from case-studies/Habitica/, with the Habitica+PRIAM stacks running):
//   NODE_DB_URI=mongodb://localhost:27017/habitrpg \
//   PRIAM_ACTOR_URL=http://localhost:8082 \
//   PRIAM_DATA_URL=http://localhost:8081 \
//   node priam-integration/backfill-data-subjects.mjs
//
// Reuses the app's own database directly (the same NODE_DB_URI Habitica's
// server connects to) via the mongoose driver already a project dependency -
// not a separate database-to-database access, and not a permanent endpoint.
import mongoose from 'mongoose';

const NODE_DB_URI = process.env.NODE_DB_URI || 'mongodb://localhost:27017/habitrpg';
const ACTOR_URL = process.env.PRIAM_ACTOR_URL || 'http://localhost:8082';
const DATA_URL = process.env.PRIAM_DATA_URL || 'http://localhost:8081';

const DATA_SUBJECT_CATEGORY_ID = 1;
const USER_DATA_IDS = [1, 2, 3];
const TASK_DATA_IDS = [4, 5, 6];
const PUSH_DEVICE_DATA_IDS = [7, 8];

async function registerDataSubject (idRef) {
  await fetch(`${ACTOR_URL}/api/DataSubject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idRef, dataSubjectCategoryId: DATA_SUBJECT_CATEGORY_ID }),
  });
}

async function reportProcessedData (idRef, dataIds) {
  const idResponse = await fetch(`${ACTOR_URL}/api/DataSubjectId/${encodeURIComponent(idRef)}`);
  if (!idResponse.ok) return;
  const dataSubjectId = await idResponse.json();
  await fetch(`${DATA_URL}/api/processed-data/add?subjectId=${dataSubjectId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dataIds),
  });
}

async function main () {
  await mongoose.connect(NODE_DB_URI);
  const { db } = mongoose.connection;

  const users = await db.collection('users').find({}, {
    projection: { _id: 1, pushDevices: 1 },
  }).toArray();

  let processed = 0;
  for (const user of users) {
    const idRef = user._id;
    // eslint-disable-next-line no-await-in-loop
    await registerDataSubject(idRef);
    // eslint-disable-next-line no-await-in-loop
    await reportProcessedData(idRef, USER_DATA_IDS);

    // eslint-disable-next-line no-await-in-loop
    const taskCount = await db.collection('tasks').countDocuments({ userId: idRef });
    for (let i = 0; i < taskCount; i += 1) {
      // eslint-disable-next-line no-await-in-loop
      await reportProcessedData(idRef, TASK_DATA_IDS);
    }

    const pushDeviceCount = (user.pushDevices || []).length;
    for (let i = 0; i < pushDeviceCount; i += 1) {
      // eslint-disable-next-line no-await-in-loop
      await reportProcessedData(idRef, PUSH_DEVICE_DATA_IDS);
    }

    processed += 1;
    console.log(`Backfilled ${idRef} (${taskCount} tasks, ${pushDeviceCount} push devices)`);
  }

  console.log(`Done: ${processed} user(s) backfilled.`);
  await mongoose.disconnect();
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
