"""PRIAM Provider bridge (playbook §2) - the 4 endpoints PRIAM-Right-service
and PRIAM-Frontend-Provider call to execute an approved GDPR right against
Bank of Anthos's real data. Mounted bare on /api (no /api/priam prefix, no
auth - machine-to-machine, called only by PRIAM through CUSTOM_PROVIDER_URL).

idRef = users.username (playbook §7: chosen deliberately non-numeric, unlike
accountid). dataTypeName is "User" (single row per subject, accounts-db.users)
or "Contact" (several rows per subject, accounts-db.contacts, disambiguated
by the "label" primary key - see Databases/db_insertion_script.sql).

Lives in userservice because it already owns the SQLAlchemy connection to
accounts-db, which holds both tables (contacts.py, in the "contacts"
service, only ever queries its own table). Connections auto-commit here
(no explicit conn.commit()), matching db.py's own existing convention for
this SQLAlchemy 1.4 legacy engine.
"""
from flask import Blueprint, jsonify, request
from sqlalchemy import MetaData, Table, Column, String, Boolean, select

USER_FIELDS = {
    'username', 'accountid', 'firstname', 'lastname', 'birthday',
    'address', 'state', 'zip', 'timezone', 'ssn',
}
# username/accountid excluded: read-only identifiers (data_usage u=0 in the
# SQL annotation) - rectifying them would break the idRef/JWT identity.
USER_WRITABLE = USER_FIELDS - {'username', 'accountid'}
# birthday (DATE NOT NULL, no sensible blank) and ssn (Art. 17(3)(b) legal-
# obligation exception, see processing 3 in the SQL annotation) are
# rectifiable but not erasable.
USER_ERASABLE = USER_WRITABLE - {'birthday', 'ssn'}

CONTACT_FIELDS = {'label', 'account_num', 'routing_num', 'is_external'}
CONTACT_WRITABLE = CONTACT_FIELDS
# Only "label" is erasable: contacts have no surrogate id, so erasing the
# field that identifies the row (playbook §1 point 5) removes the whole
# contact instead of leaving a half-blanked, unaddressable row - same
# treatment as CartItem.quantity in the OnlineBoutique annotation.
CONTACT_ERASABLE = {'label'}


def register_provider_bridge(app, engine, users_table):
    """Registers the 4 bridge routes on the given Flask app."""
    contacts_table = Table(
        'contacts', MetaData(engine),
        Column('username', String), Column('label', String),
        Column('account_num', String), Column('routing_num', String),
        Column('is_external', Boolean),
    )
    bridge = Blueprint('priam_provider', __name__)

    def _resolve_type(data_name, body):
        """dataValue has no dataTypeName - infer it from dataName/primaryKeys
        (playbook §2, §8.2.f)."""
        if data_name in USER_FIELDS:
            return 'User'
        if data_name in CONTACT_FIELDS or body.get('primaryKeys'):
            return 'Contact'
        return None

    @bridge.route('/api/dataAccessRight', methods=['GET'])
    def data_access_right():
        id_ref = request.args.get('idRef')
        data_type_name = request.args.get('dataTypeName')
        attributes = [a for a in request.args.get('attributes', '').split(',') if a]
        with engine.connect() as conn:
            if data_type_name == 'User':
                fields = [f for f in attributes if f in USER_FIELDS] or ['username']
                cols = [users_table.c[f] for f in fields]
                row = conn.execute(
                    select(*cols).where(users_table.c.username == id_ref)
                ).first()
                return jsonify([dict(row._mapping)] if row else [])
            if data_type_name == 'Contact':
                fields = [f for f in attributes if f in CONTACT_FIELDS] or ['label']
                cols = [contacts_table.c[f] for f in fields]
                rows = conn.execute(
                    select(*cols).where(contacts_table.c.username == id_ref)
                    .order_by(contacts_table.c.label)
                ).all()
                return jsonify([dict(r._mapping) for r in rows])
        return jsonify([])

    @bridge.route('/api/rectification', methods=['POST'])
    def rectification():
        body = request.get_json()
        id_ref, data_name, new_value = body['idRef'], body['dataName'], body['newValue']
        data_type_name = body.get('dataTypeName') or _resolve_type(data_name, body)
        with engine.connect() as conn:
            if data_type_name == 'User' and data_name in USER_WRITABLE:
                result = conn.execute(
                    users_table.update().where(users_table.c.username == id_ref)
                    .values(**{data_name: new_value}))
            elif data_type_name == 'Contact' and data_name in CONTACT_WRITABLE:
                label = (body.get('primaryKeys') or {}).get('label')
                result = conn.execute(
                    contacts_table.update().where(
                        (contacts_table.c.username == id_ref) & (contacts_table.c.label == label))
                    .values(**{data_name: new_value}))
            else:
                return 'field not allowed for rectification', 400
            if result.rowcount == 0:
                return 'record not found', 404
        return jsonify({}), 200

    @bridge.route('/api/erasure', methods=['POST'])
    def erasure():
        body = request.get_json()
        id_ref, data_name = body['idRef'], body['dataName']
        data_type_name = body.get('dataTypeName') or _resolve_type(data_name, body)
        with engine.connect() as conn:
            if data_type_name == 'User' and data_name in USER_ERASABLE:
                result = conn.execute(
                    users_table.update().where(users_table.c.username == id_ref)
                    .values(**{data_name: ''}))
            elif data_type_name == 'Contact' and data_name in CONTACT_ERASABLE:
                label = (body.get('primaryKeys') or {}).get('label')
                result = conn.execute(
                    contacts_table.delete().where(
                        (contacts_table.c.username == id_ref) & (contacts_table.c.label == label)))
            else:
                return 'field not allowed for erasure', 400
            if result.rowcount == 0:
                return 'record not found', 404
        return jsonify({}), 200

    @bridge.route('/api/dataValue', methods=['POST'])
    def data_value():
        body = request.get_json()
        id_ref, data_name = body['idRef'], body['dataName']
        data_type_name = _resolve_type(data_name, body)
        with engine.connect() as conn:
            if data_type_name == 'User':
                row = conn.execute(
                    select(users_table.c[data_name]).where(users_table.c.username == id_ref)
                ).first()
            elif data_type_name == 'Contact':
                label = (body.get('primaryKeys') or {}).get('label')
                row = conn.execute(
                    select(contacts_table.c[data_name]).where(
                        (contacts_table.c.username == id_ref) & (contacts_table.c.label == label))
                ).first()
            else:
                return 'unknown field', 400
        return jsonify({'value': row[0] if row else None})

    app.register_blueprint(bridge)
