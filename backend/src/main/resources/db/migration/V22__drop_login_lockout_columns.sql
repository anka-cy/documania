-- Le blocage de compte après échecs de connexion n'a jamais été activé :
-- la protection effective est la limitation de débit par adresse IP
-- (RateLimitService). Les colonnes héritées sont supprimées.
ALTER TABLE `users`
    DROP COLUMN `failed_login_attempts`,
    DROP COLUMN `locked_until`;
