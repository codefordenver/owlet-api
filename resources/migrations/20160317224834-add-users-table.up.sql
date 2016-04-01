CREATE TABLE users
(id VARCHAR(45) PRIMARY KEY,
 name VARCHAR(30),
 nickname VARCHAR(30),
 email VARCHAR(60),
 picture VARCHAR(300),
 admin BOOLEAN DEFAULT FALSE,
 district_id varchar(6) DEFAULT '000000');