-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(sid, name, nickname, email, picture)
VALUES (:sid, :name, :nickname, :email, :picture)

-- :name update-user! :! :n
-- :doc update an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE sid = :sid

-- :name get-user :? :1
-- :doc retrieve a user given the sid.
SELECT * FROM users
WHERE sid = :sid

-- :name get-user-by-email :? :*
-- :doc retrieve a user given email
SELECT * FROM users
WHERE email = :email

-- :name get-users :? :*
-- :doc retrieve all users
SELECT * FROM users

-- :name delete-user! :! :n
-- :doc delete a user given the sid
DELETE FROM users
WHERE sid = :sid

-- :name update-user-district-id! :! :n
-- :doc update user's district it
UPDATE users
SET district_id = :district_id
WHERE sid = :sid

-- :name insert-user-entry! :returning-execute
-- :doc updates user's content data
INSERT INTO entries
(entry)
VALUES (:entry)
RETURNING id

-- :name insert-into-user_entries! :! :n
-- :doc updates user's content data
INSERT INTO user_entries
(user_id, entry_id)
VALUES (:user_id, :entry_id)

-- :name get-user-entries-by-id :? :*
-- :doc retrieve all entry ids given a user id
SELECT entry_id FROM user_entries
WHERE user_id = :id

-- :name get-entry-by-id :? :*
-- :doc retrieve a user with entries by id
SELECT entry FROM entries
WHERE id = :id
