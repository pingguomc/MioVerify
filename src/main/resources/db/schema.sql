CREATE TABLE IF NOT EXISTS `profiles` (
  `id` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `bind_user` varchar(64) NOT NULL,
  `skin_up_allow` bit(1) NOT NULL DEFAULT 0,
  `cape_up_allow` bit(1) NOT NULL DEFAULT 0,
  `skin_hash` varchar(255) NULL DEFAULT NULL,
  `cape_hash` varchar(255) NULL DEFAULT NULL,
  `skin_slim` bit(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `users` (
  `id` varchar(64) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NULL DEFAULT NULL,
  `preferred_lang` varchar(8) NULL DEFAULT NULL,
  `auth_type` varchar(16) NOT NULL DEFAULT 'LOCAL',
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `oauth_users` (
  `id` varchar(64) NOT NULL,
  `provider` varchar(32) NOT NULL,
  `provider_user_id` varchar(255) NOT NULL,
  `provider_username` varchar(255) NULL DEFAULT NULL,
  `bind_user` varchar(64) NOT NULL,
  PRIMARY KEY (`id`)
);
