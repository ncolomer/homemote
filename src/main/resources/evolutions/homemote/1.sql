# EmrScheduler tables

# --- !Ups

CREATE TABLE IF NOT EXISTS "group" (
  name VARCHAR PRIMARY KEY,
  tags VARCHAR[] NOT NULL,
  groups VARCHAR[] NOT NULL
);

CREATE TABLE IF NOT EXISTS "measure" (
  id BIGSERIAL PRIMARY KEY,
  origin VARCHAR NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  name VARCHAR NOT NULL,
  value DECIMAL NOT NULL
);

CREATE TABLE IF NOT EXISTS "node" (
  unique_id VARCHAR PRIMARY KEY,
  network_id SMALLINT NOT NULL UNIQUE,
  first_seen TIMESTAMP NOT NULL,
  last_seen TIMESTAMP NOT NULL,
  firmware_name VARCHAR NOT NULL,
  firmware_version VARCHAR NOT NULL,
  battery_voltage DECIMAL,
  battery_timestamp TIMESTAMP,
  tags VARCHAR[] NOT NULL
);

CREATE TABLE IF NOT EXISTS "state" (
  origin VARCHAR NOT NULL,
  updated TIMESTAMP NOT NULL,
  key VARCHAR NOT NULL,
  value VARCHAR NOT NULL,
  PRIMARY KEY (origin, key)
);

# --- !Downs

DROP TABLE "state";
DROP TABLE "node";
DROP TABLE "measure";
DROP TABLE "group";
