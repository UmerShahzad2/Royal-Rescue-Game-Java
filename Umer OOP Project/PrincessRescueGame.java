import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;

public class PrincessRescueGame extends JFrame {
    public PrincessRescueGame() {
        setTitle("Operation: Save The Princess");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        String playerName = JOptionPane.showInputDialog(this, "Enter your name:", "Player Login", JOptionPane.PLAIN_MESSAGE);
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Guest";
        }
        add(new GamePanel(playerName.trim()));
        setVisible(true);
    }
    public static void main(String[] args) {
        new PrincessRescueGame();
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    javax.swing.Timer timer = new javax.swing.Timer(20, this);
    Player player;
    Princess princess;
    ArrayList<Enemy> enemies;
    ArrayList<Bullet> playerBullets;
    ArrayList<Bullet> enemyBullets;
    ArrayList<Cover> covers;
    ArrayList<PowerUp> powerUps;
    boolean up, down, left, right;
    int level;
    int score;
    boolean gameWon;
    boolean gameOver;
    boolean isPaused;
    int spawnCooldown;
    int weaponLevel;
    int timeElapsed;
    int difficultyMultiplier;
    int bossesKilled;
    boolean bossActive;
    int bossIntermissionTimer;
    int nextBossScoreThreshold;
    boolean hasKey;
    String playerName;
    boolean dataSaved;

    public GamePanel(String playerName) {
        this.playerName = playerName;
        setFocusable(true);
        addKeyListener(this);
        covers = new ArrayList<>();
        resetGame();
        timer.start();
    }

    void saveGameData(String status) {
        if (dataSaved) return;
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/princess_rescue_db", "root", "Umer@2006");
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO leaderboards (player_name, score, bosses_killed, status) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, playerName);
            pstmt.setInt(2, score);
            pstmt.setInt(3, bossesKilled);
            pstmt.setString(4, status);
            pstmt.executeUpdate();
            dataSaved = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void resetGame() {
        player = new Player(100, 320);
        princess = new Princess(900, 300);
        enemies = new ArrayList<>();
        playerBullets = new ArrayList<>();
        enemyBullets = new ArrayList<>();
        powerUps = new ArrayList<>();
        covers.clear();
        covers.add(new Cover(250, 120, 50, 80));
        covers.add(new Cover(450, 250, 50, 80));
        covers.add(new Cover(650, 120, 50, 80));
        covers.add(new Cover(450, 450, 50, 80));
        up = false; down = false; left = false; right = false;
        level = 1;
        score = 0;
        gameWon = false;
        gameOver = false;
        isPaused = false;
        spawnCooldown = 0;
        weaponLevel = 1;
        timeElapsed = 0;
        difficultyMultiplier = 0;
        bossesKilled = 0;
        bossActive = false;
        bossIntermissionTimer = 0;
        nextBossScoreThreshold = 1500;
        hasKey = false;
        dataSaved = false;
        spawnLevel();
    }

    void spawnLevel() {
        enemies.clear();
        if (level == 1) {
            enemies.add(new Enemy(800, 100, difficultyMultiplier));
            enemies.add(new Enemy(850, 300, difficultyMultiplier));
            enemies.add(new Enemy(780, 500, difficultyMultiplier));
        } else if (level == 2) {
            enemies.add(new Enemy(800, 100, difficultyMultiplier));
            enemies.add(new Enemy(850, 250, difficultyMultiplier));
            enemies.add(new Enemy(820, 450, difficultyMultiplier));
            enemies.add(new Enemy(900, 550, difficultyMultiplier));
            enemies.add(new Enemy(700, 350, difficultyMultiplier));
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(15, 15, 25));
        g.fillRect(0, 0, getWidth(), getHeight());
        for (Cover c : covers) c.draw(g);
        for (PowerUp p : powerUps) p.draw(g);
        princess.draw(g);
        player.draw(g);
        for (Enemy e : enemies) e.draw(g);
        for (Bullet b : playerBullets) b.draw(g);
        for (Bullet b : enemyBullets) b.draw(g);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g.drawString("PLAYER: " + playerName, 25, 30);
        g.drawString("Health: " + player.health, 25, 55);
        g.drawString("Score: " + score, 25, 80);
        g.drawString("Level: " + level, 25, 105);
        g.drawString("Bosses Defeated: " + bossesKilled + " / 4", 25, 130);
        g.setColor(Color.CYAN);
        g.drawString("WEAPON POWER: LVL " + weaponLevel, 25, 155);
        g.setColor(Color.RED);
        g.drawString("ENEMY INTENSITY: +" + difficultyMultiplier + "%", 25, 180);
        g.setColor(hasKey ? Color.YELLOW : Color.GRAY);
        g.drawString(hasKey ? "KEY ACQUIRED! TOUCH PRINCESS TO WIN!" : "KEY STATUS: LOCKED (DEFEAT ALL 4 BOSSES)", 25, 205);
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Press 'P' to Pause/Resume", 25, 230);
        
        if (bossIntermissionTimer > 0 && !gameOver && !gameWon && !isPaused) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 22));
            g.setColor(Color.YELLOW);
            g.drawString("WARNING: BOSS " + (bossesKilled + 1) + " INCOMING IN " + ((bossIntermissionTimer / 50) + 1) + "s", 320, 40);
        }

        if (gameOver) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 50));
            g.setColor(Color.RED);
            g.drawString("GAME OVER", 350, 250);
            g.setFont(new Font("Segoe UI", Font.BOLD, 22));
            g.setColor(Color.WHITE);
            g.drawString("Press 'R' to Try Again", 390, 310);
        }
        if (gameWon) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 45));
            g.setColor(Color.YELLOW);
            g.drawString("PRINCESS RESCUED! VICTORY!", 200, 250);
            g.setFont(new Font("Segoe UI", Font.BOLD, 22));
            g.setColor(Color.WHITE);
            g.drawString("Press 'R' to Play Again", 390, 310);
        }
        if (isPaused && !gameOver && !gameWon) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 50));
            g.setColor(Color.ORANGE);
            g.drawString("GAME PAUSED", 320, 250);
            g.setFont(new Font("Segoe UI", Font.BOLD, 22));
            g.setColor(Color.WHITE);
            g.drawString("Press 'P' to Resume", 395, 310);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (gameOver || gameWon || isPaused) {
            if (gameOver) saveGameData("FAILED");
            if (gameWon) saveGameData("WON");
            repaint();
            return;
        }

        timeElapsed++;
        if (timeElapsed % 200 == 0) {
            difficultyMultiplier += 3;
        }

        int nextX = player.x;
        int nextY = player.y;
        if (up) nextY -= 6;
        if (down) nextY += 6;
        if (left) nextX -= 6;
        if (right) nextX += 6;

        nextX = Math.max(0, Math.min(940, nextX));
        nextY = Math.max(0, Math.min(620, nextY));

        Rectangle nextBounds = new Rectangle(nextX, nextY, player.w, player.h);
        boolean playerCollided = false;
        for (Cover c : covers) {
            if (nextBounds.intersects(c.getBounds())) {
                playerCollided = true;
                break;
            }
        }
        if (!playerCollided) {
            player.x = nextX;
            player.y = nextY;
        }

        spawnCooldown++;
        int currentSpawnRate = Math.max(35, bossActive ? 45 : (level >= 2) ? 80 - (difficultyMultiplier / 3) : 110 - (difficultyMultiplier / 3));
        if (spawnCooldown > currentSpawnRate) {
            Random r = new Random();
            enemies.add(new Enemy(950, r.nextInt(550) + 50, difficultyMultiplier));
            spawnCooldown = 0;
        }

        if (!bossActive && bossesKilled < 4) {
            if (bossIntermissionTimer > 0) {
                bossIntermissionTimer--;
                if (bossIntermissionTimer == 0) {
                    enemies.add(new BossEnemy(850, 300, difficultyMultiplier, bossesKilled + 1));
                    bossActive = true;
                    level = bossesKilled + 1;
                }
            } else if (score >= nextBossScoreThreshold) {
                bossIntermissionTimer = 500;
            }
        }

        for (Bullet b : playerBullets) b.move();
        for (Bullet b : enemyBullets) b.move();
        for (PowerUp p : powerUps) p.move();

        Random r = new Random();
        for (Enemy enemy : enemies) {
            enemy.follow(player, covers);
            int currentFireRate = enemy.fireRate + (difficultyMultiplier / 20);
            if (r.nextInt(100) < currentFireRate) {
                int dx = player.x - enemy.x;
                int dy = player.y - enemy.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > 0) {
                    double speed = 4.5 + (difficultyMultiplier * 0.03);
                    enemyBullets.add(new Bullet(enemy.x + enemy.w / 2, enemy.y + enemy.h / 2, (dx / distance) * speed, (dy / distance) * speed, false));
                }
            }
        }

        ArrayList<Enemy> dead = new ArrayList<>();
        ArrayList<Bullet> removePB = new ArrayList<>();
        ArrayList<Bullet> removeEB = new ArrayList<>();
        ArrayList<PowerUp> removePU = new ArrayList<>();
        ArrayList<Cover> deadCovers = new ArrayList<>();

        for (PowerUp p : powerUps) {
            if (p.getBounds().intersects(player.getBounds())) {
                if (p.type == 0) {
                    if (weaponLevel < 4) weaponLevel++;
                } else {
                    player.health = Math.min(100, player.health + 35);
                }
                score += 200;
                removePU.add(p);
            }
        }

        for (Bullet b : playerBullets) {
            for (Cover c : covers) {
                if (b.getBounds().intersects(c.getBounds())) {
                    c.hp -= 25;
                    if (c.hp <= 0 && !deadCovers.contains(c)) deadCovers.add(c);
                    removePB.add(b);
                }
            }
            for (Enemy enemy : enemies) {
                if (b.getBounds().intersects(enemy.getBounds())) {
                    if (!removePB.contains(b)) {
                        enemy.health -= 25;
                        removePB.add(b);
                        if (enemy.health <= 0 && !dead.contains(enemy)) {
                            dead.add(enemy);
                            score += 100;
                            if (enemy instanceof BossEnemy) {
                                bossesKilled++;
                                bossActive = false;
                                if (bossesKilled == 1) {
                                    nextBossScoreThreshold = score + 2500;
                                } else if (bossesKilled == 2) {
                                    nextBossScoreThreshold = score + 3500;
                                } else if (bossesKilled == 3) {
                                    nextBossScoreThreshold = score + 4500;
                                }
                                if (bossesKilled >= 4) {
                                    hasKey = true;
                                }
                            }
                            if (r.nextInt(100) < 55) {
                                powerUps.add(new PowerUp(enemy.x, enemy.y, r.nextInt(2)));
                            }
                        }
                    }
                }
            }
        }

        for (Bullet b : enemyBullets) {
            for (Cover c : covers) {
                if (b.getBounds().intersects(c.getBounds())) {
                    c.hp -= 25;
                    if (c.hp <= 0 && !deadCovers.contains(c)) deadCovers.add(c);
                    removeEB.add(b);
                }
            }
            if (b.getBounds().intersects(player.getBounds())) {
                player.health -= 4;
                removeEB.add(b);
            }
        }

        playerBullets.removeAll(removePB);
        enemyBullets.removeAll(removeEB);
        enemies.removeAll(dead);
        powerUps.removeAll(removePU);
        covers.removeAll(deadCovers);

        if (player.health <= 0) {
            gameOver = true;
        }

        if (hasKey) {
            if (player.getBounds().intersects(princess.getBounds())) {
                gameWon = true;
            }
        }

        repaint();
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_P) {
            if (!gameOver && !gameWon) {
                isPaused = !isPaused;
            }
        }
        if (isPaused) return;
        
        if (e.getKeyCode() == KeyEvent.VK_R) {
            if (gameOver || gameWon) {
                resetGame();
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_W) up = true;
        if (e.getKeyCode() == KeyEvent.VK_S) down = true;
        if (e.getKeyCode() == KeyEvent.VK_A) left = true;
        if (e.getKeyCode() == KeyEvent.VK_D) right = true;
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (weaponLevel == 1) {
                playerBullets.add(new Bullet(player.x + 40, player.y + 20, 15, 0, true));
            } else if (weaponLevel == 2) {
                playerBullets.add(new Bullet(player.x + 40, player.y + 10, 15, -1, true));
                playerBullets.add(new Bullet(player.x + 40, player.y + 30, 15, 1, true));
            } else if (weaponLevel == 3) {
                playerBullets.add(new Bullet(player.x + 40, player.y + 20, 15, 0, true));
                playerBullets.add(new Bullet(player.x + 40, player.y + 10, 14, -3, true));
                playerBullets.add(new Bullet(player.x + 40, player.y + 30, 14, 3, true));
            } else if (weaponLevel >= 4) {
                playerBullets.add(new Bullet(player.x + 40, player.y + 20, 15, 0, true));
                playerBullets.add(new Bullet(player.x + 40, player.y + 10, 14, -3, true));
                playerBullets.add(new Bullet(player.x + 40, player.y + 30, 14, 3, true));
                playerBullets.add(new Bullet(player.x + 40, player.y + 5, 13, -5, true));
                playerBullets.add(new Bullet(player.x + 40, player.y + 35, 13, 5, true));
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) up = false;
        if (e.getKeyCode() == KeyEvent.VK_S) down = false;
        if (e.getKeyCode() == KeyEvent.VK_A) left = false;
        if (e.getKeyCode() == KeyEvent.VK_D) right = false;
    }

    public void keyTyped(KeyEvent e) {}
}

class GameObject {
    int x, y, w, h, health = 100;
    Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }
}

class Player extends GameObject {
    Image img;
    Player(int x, int y) {
        this.x = x;
        this.y = y;
        w = 60;
        h = 60;
        img = new ImageIcon("prince.png").getImage();
    }
    void draw(Graphics g) {
        if (img != null && img.getWidth(null) > 0) {
            g.drawImage(img, x, y, w, h, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillOval(x, y, w, h);
            g.setColor(Color.RED);
            g.drawOval(x, y, w, h);
            g.drawLine(x + 20, y + 20, x + 55, y + 20);
        }
    }
}

class Enemy extends GameObject {
    int fireRate = 2;
    double speedBase = 1.8;
    Image img;
    Enemy(int x, int y, int mult) {
        this.x = x;
        this.y = y;
        w = 50;
        h = 50;
        this.health = 80 + (mult * 1);
        this.speedBase = 1.8 + (mult * 0.01);
        img = new ImageIcon("enemy.png").getImage();
    }
    void follow(Player p, ArrayList<Cover> covers) {
        int nextX = x;
        int nextY = y;
        if (x > p.x) nextX -= (int)speedBase;
        else nextX += (int)speedBase;
        if (y > p.y) nextY -= (int)speedBase;
        else nextY += (int)speedBase;
        Rectangle nextBounds = new Rectangle(nextX, nextY, w, h);
        boolean collided = false;
        for (Cover c : covers) {
            if (nextBounds.intersects(c.getBounds())) {
                collided = true;
                break;
            }
        }
        if (!collided) {
            x = nextX;
            y = nextY;
        } else {
            if (y > p.y) y -= (int)speedBase;
            else y += (int)speedBase;
        }
    }
    void draw(Graphics g) {
        if (img != null && img.getWidth(null) > 0) {
            g.drawImage(img, x, y, w, h, null);
        } else {
            g.setColor(Color.RED);
            g.fillRect(x, y, w, h);
        }
    }
}

class BossEnemy extends Enemy {
    int bossNum;
    BossEnemy(int x, int y, int mult, int bossNum) {
        super(x, y, mult);
        this.bossNum = bossNum;
        health = (500 * bossNum) + (mult * 3);
        fireRate = 6 + (bossNum * 2);
        w = 60 + (bossNum * 10);
        h = 60 + (bossNum * 10);
        speedBase = 1.0 + (bossNum * 0.2);
    }
    void draw(Graphics g) {
        if (img != null && img.getWidth(null) > 0) {
            g.drawImage(img, x, y, w, h, null);
        } else {
            if (bossNum == 1) g.setColor(Color.MAGENTA);
            else if (bossNum == 2) g.setColor(Color.ORANGE);
            else if (bossNum == 3) g.setColor(new Color(128, 0, 128));
            else g.setColor(new Color(139, 0, 0));
            g.fillRect(x, y, w, h);
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g.drawString("BOSS B" + bossNum, x + 5, y + 20);
    }
}

class Princess extends GameObject {
    Image img;
    Princess(int x, int y) {
        this.x = x;
        this.y = y;
        w = 60;
        h = 80;
        img = new ImageIcon("princess.png").getImage();
    }
    void draw(Graphics g) {
        if (img != null && img.getWidth(null) > 0) {
            g.drawImage(img, x, y, w, h, null);
        } else {
            g.setColor(Color.PINK);
            g.fillOval(x, y, w, h);
        }
    }
}

class Cover {
    int x, y, w, h;
    int hp = 500;
    Cover(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }
    void draw(Graphics g) {
        g.setColor(Color.GRAY);
        g.fillRect(x, y, w, h);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("HP: " + hp, x, y - 5);
    }
}

class Bullet {
    int x, y;
    double dx, dy;
    boolean player;
    Bullet(int x, int y, double dx, double dy, boolean player) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.player = player;
    }
    void move() {
        x += dx;
        y += dy;
    }
    Rectangle getBounds() {
        return new Rectangle(x, y, 8, 8);
    }
    void draw(Graphics g) {
        g.setColor(player ? Color.YELLOW : Color.ORANGE);
        g.fillOval(x, y, 8, 8);
    }
}

class PowerUp {
    int x, y, w = 20, h = 20;
    int type;
    PowerUp(int x, int y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
    void move() {
        x -= 2;
    }
    Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }
    void draw(Graphics g) {
        if (type == 0) {
            g.setColor(Color.GREEN);
            g.fillOval(x, y, w, h);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("P", x + 6, y + 15);
        } else {
            g.setColor(Color.RED);
            g.fillOval(x, y, w, h);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("H", x + 6, y + 15);
        }
    }
}