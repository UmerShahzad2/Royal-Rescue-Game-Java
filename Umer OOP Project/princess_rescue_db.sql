CREATE DATABASE IF NOT EXISTS princess_rescue_db;

USE princess_rescue_db;

CREATE TABLE IF NOT EXISTS leaderboards (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(50),
    score INT,
    bosses_killed INT,
    status VARCHAR(20),
    play_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);