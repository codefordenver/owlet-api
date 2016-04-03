-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, name, nickname, email, picture)
VALUES (:id, :name, :nickname, :email, :picture)

-- :name update-user! :! :n
-- :doc update an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieve a user given the id.
SELECT * FROM users
WHERE id = :id

-- :name get-user-by-email :? :*
-- :doc retrieve a user given email
SELECT * FROM users
WHERE email = :email

-- :name get-users :? :*
-- :doc retrieve all users
SELECT * FROM users

-- :name delete-user! :! :n
-- :doc delete a user given the id
DELETE FROM users
WHERE id = :id

-- :name update-user-district-id! :! :n
-- :doc update user's district it
UPDATE users
SET district_id = :district_id
WHERE id = :id