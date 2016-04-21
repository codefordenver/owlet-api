CREATE TABLE users
(id SERIAL PRIMARY KEY,
 sid VARCHAR(45),
 name VARCHAR(30),
 nickname VARCHAR(30),
 email VARCHAR(60),
 picture VARCHAR(300),
 admin BOOLEAN DEFAULT FALSE,
 district_id varchar(6) DEFAULT '000000');
--;;
CREATE TABLE entries
(id SERIAL PRIMARY KEY,
 entry VARCHAR NULL DEFAULT NULL);
--;;
CREATE TABLE user_entries
(user_id int REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
 entry_id int REFERENCES entries (id) ON UPDATE CASCADE,
 -- explicit primary key
 CONSTRAINT userentries_pk PRIMARY KEY (user_id, entry_id));