CREATE TABLE IF NOT EXISTS profiles (
  id varchar(64) NOT NULL,
  name varchar(255) NOT NULL,
  bind_user varchar(64) NOT NULL,
  skin_up_allow integer NOT NULL DEFAULT 0,
  cape_up_allow integer NOT NULL DEFAULT 0,
  skin_hash varchar(255) DEFAULT NULL,
  cape_hash varchar(255) DEFAULT NULL,
  skin_slim integer DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS users (
  id varchar(64) NOT NULL,
  username varchar(255) NOT NULL,
  password varchar(255) NOT NULL,
  preferred_lang varchar(8) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS admin_users (
  id varchar(64) NOT NULL,
  username varchar(255) NOT NULL,
  password varchar(255) NOT NULL,
  role varchar(50) NOT NULL DEFAULT 'ADMIN',
  enabled integer NOT NULL DEFAULT 1,
  created_at varchar(50) DEFAULT NULL,
  last_login_at varchar(50) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_username ON admin_users (username);