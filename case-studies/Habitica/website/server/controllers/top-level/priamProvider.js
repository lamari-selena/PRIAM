import { model as User } from '../../models/user';
import { Task } from '../../models/task';

// PRIAM Provider bridge (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §2). Mounted on
// bare /api (this file lives under controllers/top-level, walked by
// libs/routes.js and mounted at '/' - server/middlewares/appRoutes.js), no
// auth (machine-to-machine, called only by PRIAM).
const WHITELISTS = {
  User: ['username', 'email', 'displayName'],
  Task: ['id', 'text', 'notes'],
  PushDevice: ['regId', 'type'],
};

const USER_FIELD_PATHS = {
  username: 'auth.local.username',
  email: 'auth.local.email',
  displayName: 'profile.name',
};

function toStringValue (value) {
  return value === null || value === undefined ? '' : String(value);
}

function pickAttributes (record, attributes) {
  const allowed = attributes.length > 0 ? attributes : Object.keys(record);
  const out = {};
  allowed.forEach(attr => {
    if (Object.prototype.hasOwnProperty.call(record, attr)) out[attr] = toStringValue(record[attr]);
  });
  return out;
}

async function loadRecords (dataTypeName, idRef) {
  if (dataTypeName === 'User') {
    const user = await User.findById(idRef).select('auth.local.username auth.local.email profile.name').lean().exec();
    if (!user) return [];
    return [{
      username: user.auth && user.auth.local && user.auth.local.username,
      email: user.auth && user.auth.local && user.auth.local.email,
      displayName: user.profile && user.profile.name,
    }];
  }
  if (dataTypeName === 'Task') {
    const tasks = await Task.find({ userId: idRef }).select('text notes').sort({ _id: 1 }).lean().exec();
    return tasks.map(task => ({ id: task._id, text: task.text, notes: task.notes }));
  }
  if (dataTypeName === 'PushDevice') {
    const user = await User.findById(idRef).select('pushDevices').lean().exec();
    if (!user) return [];
    return (user.pushDevices || [])
      .slice()
      .sort((a, b) => String(a.regId).localeCompare(String(b.regId)))
      .map(device => ({ regId: device.regId, type: device.type }));
  }
  return [];
}

function inferDataTypeName (dataName) {
  if (WHITELISTS.Task.includes(dataName)) return 'Task';
  if (WHITELISTS.PushDevice.includes(dataName)) return 'PushDevice';
  if (WHITELISTS.User.includes(dataName)) return 'User';
  return null;
}

const api = {};

// GET {CUSTOM_PROVIDER_URL}/api/dataAccessRight?idRef=...&dataTypeName=...&attributes=a,b,c
// Always answers with a JSON array (§2), one element per row of dataTypeName
// held by idRef (a single element for User, one per record for Task/PushDevice).
api.priamDataAccessRight = {
  method: 'GET',
  url: '/api/dataAccessRight',
  middlewares: [],
  async handler (req, res) {
    const { idRef, dataTypeName } = req.query;
    const attributes = String(req.query.attributes || '').split(',').map(attr => attr.trim()).filter(Boolean);
    if (!idRef || !dataTypeName || !WHITELISTS[dataTypeName]) {
      res.status(200).json([]);
      return;
    }
    const allowed = attributes.filter(attr => WHITELISTS[dataTypeName].includes(attr));
    const records = await loadRecords(dataTypeName, idRef);
    res.status(200).json(records.map(record => pickAttributes(record, allowed)));
  },
};

// POST {CUSTOM_PROVIDER_URL}/api/rectification  body: {idRef, dataTypeName, dataName, newValue, primaryKeys}
api.priamRectification = {
  method: 'POST',
  url: '/api/rectification',
  middlewares: [],
  async handler (req, res) {
    const {
      idRef, dataTypeName, dataName, newValue, primaryKeys,
    } = req.body;
    if (!WHITELISTS[dataTypeName] || !WHITELISTS[dataTypeName].includes(dataName)) {
      res.status(400).json({ error: 'Unknown or non-rectifiable field' });
      return;
    }
    if (dataTypeName === 'User') {
      await User.updateOne({ _id: idRef }, { $set: { [USER_FIELD_PATHS[dataName]]: newValue } }).exec();
    } else if (dataTypeName === 'Task' && (dataName === 'text' || dataName === 'notes')) {
      const taskId = primaryKeys && primaryKeys.id;
      if (!taskId) {
        res.status(404).json({ error: 'Record not found' });
        return;
      }
      const result = await Task.updateOne({ _id: taskId, userId: idRef }, { $set: { [dataName]: newValue } }).exec();
      if (result.matchedCount === 0) {
        res.status(404).json({ error: 'Record not found' });
        return;
      }
    } else {
      res.status(400).json({ error: 'Field is not rectifiable' });
      return;
    }
    res.status(200).json({ success: true });
  },
};

// POST {CUSTOM_PROVIDER_URL}/api/erasure  body: {idRef, dataTypeName, dataName, primaryKeys}
api.priamErasure = {
  method: 'POST',
  url: '/api/erasure',
  middlewares: [],
  async handler (req, res) {
    const {
      idRef, dataTypeName, dataName, primaryKeys,
    } = req.body;
    if (!WHITELISTS[dataTypeName] || !WHITELISTS[dataTypeName].includes(dataName)) {
      res.status(400).json({ error: 'Unknown or non-erasable field' });
      return;
    }
    if (dataTypeName === 'User') {
      await User.updateOne({ _id: idRef }, { $set: { [USER_FIELD_PATHS[dataName]]: '' } }).exec();
    } else if (dataTypeName === 'Task' && (dataName === 'text' || dataName === 'notes')) {
      const taskId = primaryKeys && primaryKeys.id;
      if (!taskId) {
        res.status(404).json({ error: 'Record not found' });
        return;
      }
      const result = await Task.updateOne({ _id: taskId, userId: idRef }, { $set: { [dataName]: '' } }).exec();
      if (result.matchedCount === 0) {
        res.status(404).json({ error: 'Record not found' });
        return;
      }
    } else if (dataTypeName === 'PushDevice') {
      // No surrogate id on a push device subdocument - erasing either field
      // removes the whole device, same pattern as a composite-key row with
      // no separate id (playbook §1 point 5, BankOfAnthos Contact.label).
      const regId = primaryKeys && primaryKeys.regId;
      if (!regId) {
        res.status(404).json({ error: 'Record not found' });
        return;
      }
      await User.updateOne({ _id: idRef }, { $pull: { pushDevices: { regId } } }).exec();
    } else {
      res.status(400).json({ error: 'Field is not erasable' });
      return;
    }
    res.status(200).json({ success: true });
  },
};

// POST {CUSTOM_PROVIDER_URL}/api/dataValue  body: {idRef, dataName, primaryKeys}
// No dataTypeName (§2/§8.2.f) - inferred from dataName's whitelist.
api.priamDataValue = {
  method: 'POST',
  url: '/api/dataValue',
  middlewares: [],
  async handler (req, res) {
    const { idRef, dataName, primaryKeys } = req.body;
    const dataTypeName = inferDataTypeName(dataName);
    if (!dataTypeName) {
      res.status(404).json({ error: 'Unknown field' });
      return;
    }
    const records = await loadRecords(dataTypeName, idRef);
    let record = records[0];
    if (dataTypeName === 'Task' && primaryKeys && primaryKeys.id) {
      record = records.find(row => String(row.id) === String(primaryKeys.id));
    } else if (dataTypeName === 'PushDevice' && primaryKeys && primaryKeys.regId) {
      record = records.find(row => String(row.regId) === String(primaryKeys.regId));
    }
    if (!record) {
      res.status(404).json({ error: 'Record not found' });
      return;
    }
    res.status(200).json({ value: toStringValue(record[dataName]) });
  },
};

export default api;
