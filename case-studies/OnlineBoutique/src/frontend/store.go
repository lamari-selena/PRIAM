// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"database/sql"
	"errors"
	"os"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	_ "modernc.org/sqlite"

	pb "github.com/GoogleCloudPlatform/microservices-demo/src/frontend/genproto"
)

// db is the durable store for accounts and orders, opened once at startup
// by initStore. See "changes for data persistence.md" for why this exists:
// unlike the rest of this application, checkout data and accounts must
// survive past the HTTP response that created them.
var db *sql.DB

var (
	errEmailTaken         = errors.New("an account with this email address already exists")
	errInvalidCredentials = errors.New("invalid email or password")
)

type account struct {
	ID    string
	Email string
}

type persistedOrder struct {
	OrderID       string
	Email         string
	StreetAddress string
	City          string
	State         string
	ZipCode       int32
	Country       string
	CurrencyCode  string
	PlacedAt      time.Time
	Items         []persistedOrderItem
}

type persistedOrderItem struct {
	ProductID string
	Quantity  int32
	CostUnits int64
	CostNanos int32
}

// initStore opens (creating it if necessary) the SQLite database used to
// persist accounts and orders, and applies the schema. It must be called
// once at startup before any handler touches db.
func initStore() error {
	path := os.Getenv("ACCOUNT_DB_PATH")
	if path == "" {
		// /src is this image's WORKDIR (Dockerfile) and is guaranteed to
		// exist; unlike e.g. /data, no extra directory needs to be created
		// in the distroless base image for the default case to work.
		path = "/src/onlineboutique.db"
	}

	var err error
	db, err = sql.Open("sqlite", path)
	if err != nil {
		return err
	}
	// SQLite allows only one writer at a time; a single shared connection
	// avoids "database is locked" errors under this server's concurrent
	// request handling instead of trying to tune busy-timeouts.
	db.SetMaxOpenConns(1)

	if _, err := db.Exec(`PRAGMA foreign_keys = ON;`); err != nil {
		return err
	}

	const schema = `
CREATE TABLE IF NOT EXISTS users (
	id            TEXT PRIMARY KEY,
	email         TEXT NOT NULL UNIQUE,
	password_hash TEXT NOT NULL,
	created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
	order_id       TEXT PRIMARY KEY,
	user_id        TEXT REFERENCES users(id),
	email          TEXT NOT NULL,
	street_address TEXT NOT NULL,
	city           TEXT NOT NULL,
	state          TEXT NOT NULL,
	zip_code       INTEGER NOT NULL,
	country        TEXT NOT NULL,
	currency_code  TEXT NOT NULL,
	placed_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
	order_id   TEXT NOT NULL REFERENCES orders(order_id),
	product_id TEXT NOT NULL,
	quantity   INTEGER NOT NULL,
	cost_units INTEGER NOT NULL,
	cost_nanos INTEGER NOT NULL,
	PRIMARY KEY (order_id, product_id)
);
`
	_, err = db.Exec(schema)
	return err
}

// createUser hashes password with bcrypt and durably inserts a new account.
func createUser(email, password string) (string, error) {
	var exists int
	if err := db.QueryRow(`SELECT COUNT(1) FROM users WHERE email = ?`, email).Scan(&exists); err != nil {
		return "", err
	}
	if exists > 0 {
		return "", errEmailTaken
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return "", err
	}

	id := uuid.NewString()
	if _, err := db.Exec(`INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)`, id, email, string(hash)); err != nil {
		return "", err
	}
	return id, nil
}

// authenticateUser verifies email/password against the stored bcrypt hash.
func authenticateUser(email, password string) (*account, error) {
	var id, hash string
	err := db.QueryRow(`SELECT id, password_hash FROM users WHERE email = ?`, email).Scan(&id, &hash)
	if err == sql.ErrNoRows {
		return nil, errInvalidCredentials
	}
	if err != nil {
		return nil, err
	}
	if bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)) != nil {
		return nil, errInvalidCredentials
	}
	return &account{ID: id, Email: email}, nil
}

// saveOrder durably persists a completed checkout, unlike the rest of this
// application's checkout PII, which upstream OnlineBoutique never stores
// anywhere past the HTTP response. userID is empty for guest checkouts,
// which are still persisted (so the data genuinely exists) but are not
// linked to any account.
func saveOrder(orderResult *pb.OrderResult, userID, email, streetAddress, city, state, country string, zipCode int32, currencyCode string) error {
	tx, err := db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	var uid interface{}
	if userID != "" {
		uid = userID
	}

	if _, err := tx.Exec(`INSERT INTO orders (order_id, user_id, email, street_address, city, state, zip_code, country, currency_code)
		VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
		orderResult.GetOrderId(), uid, email, streetAddress, city, state, zipCode, country, currencyCode); err != nil {
		return err
	}

	for _, item := range orderResult.GetItems() {
		if _, err := tx.Exec(`INSERT INTO order_items (order_id, product_id, quantity, cost_units, cost_nanos) VALUES (?, ?, ?, ?, ?)`,
			orderResult.GetOrderId(), item.GetItem().GetProductId(), item.GetItem().GetQuantity(), item.GetCost().GetUnits(), item.GetCost().GetNanos()); err != nil {
			return err
		}
	}

	return tx.Commit()
}

// getOrdersForUser returns every order durably linked to userID, most recent first.
func getOrdersForUser(userID string) ([]persistedOrder, error) {
	rows, err := db.Query(`SELECT order_id, email, street_address, city, state, zip_code, country, currency_code, placed_at
		FROM orders WHERE user_id = ? ORDER BY placed_at DESC`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var orders []persistedOrder
	for rows.Next() {
		var o persistedOrder
		var placedAt string
		if err := rows.Scan(&o.OrderID, &o.Email, &o.StreetAddress, &o.City, &o.State, &o.ZipCode, &o.Country, &o.CurrencyCode, &placedAt); err != nil {
			return nil, err
		}
		// SQLite has no native datetime type: CURRENT_TIMESTAMP is stored
		// as plain UTC text ("YYYY-MM-DD HH:MM:SS"), so it must be parsed
		// explicitly rather than relying on driver auto-conversion to
		// time.Time (which only works for genuine time.Time driver values).
		o.PlacedAt, err = time.Parse("2006-01-02 15:04:05", placedAt)
		if err != nil {
			return nil, err
		}
		orders = append(orders, o)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	for i := range orders {
		items, err := getOrderItems(orders[i].OrderID)
		if err != nil {
			return nil, err
		}
		orders[i].Items = items
	}
	return orders, nil
}

func getOrderItems(orderID string) ([]persistedOrderItem, error) {
	rows, err := db.Query(`SELECT product_id, quantity, cost_units, cost_nanos FROM order_items WHERE order_id = ?`, orderID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var items []persistedOrderItem
	for rows.Next() {
		var it persistedOrderItem
		if err := rows.Scan(&it.ProductID, &it.Quantity, &it.CostUnits, &it.CostNanos); err != nil {
			return nil, err
		}
		items = append(items, it)
	}
	return items, rows.Err()
}
