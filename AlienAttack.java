import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.*;

public class AlienAttack extends JPanel implements ActionListener, KeyListener {
    int tileSize = 32;
    int rows = 16;
    int columns = 16;

    int boardWidth = tileSize * columns;
    int boardHeight = tileSize * rows; 

    Image shipImg;
    Image alienCyanImg;
    Image alienMagentaImg;
    Image alienYellowImg;
    Image alienGreenImg;
    ArrayList<Image> alienImgArray;
    
    Clip shootSound;
    Clip explosionSound;
    Clip alienDeathSound;
    Clip backgroundMusic;
    Clip gameOverSound;
    Clip levelUpSound;
    Clip victorySound;
    
    Font arcadeFont;
    Font smallArcadeFont;
    Font tinyArcadeFont;
    
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image img;
        boolean alive = true;
        boolean used = false;
        int velX = 0;
        int velY = 0;
        int directionChangeTimer = 0;

        Block(int x, int y, int width, int height, Image img) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.img = img;
        }
    }
    
    boolean inMenu = true;
    boolean gameOver = false;
    boolean victory = false;
    boolean levelTransition = false;
    int currentLevel = 1;
    int maxLevel = 5;
    int health = 3;
    int maxHealth = 3;
    int score = 0;
    
    boolean gameOverCooldown = false;
    int gameOverCooldownTimer = 0;
    final int GAME_OVER_COOLDOWN = 90;
    
    int alienVelocityX = 1;
    
    int shipWidth = tileSize*2;
    int shipHeight = tileSize;
    int shipX = tileSize * columns/2 - tileSize;
    int shipY = tileSize * rows - tileSize*2;
    int shipVelocityX = tileSize;
    Block ship;
    ArrayList<Block> alienArray;
    ArrayList<Block> alienBulletArray;
    int alienWidth = tileSize*2;
    int alienHeight = tileSize;
    int alienX = tileSize;
    int alienY = tileSize;

    int alienRows = 2;
    int alienColumns = 3;
    int alienCount = 0;
    int alienShootProbability = 2;
    
    ArrayList<Block> bulletArray;
    int bulletWidth = tileSize/8;
    int bulletHeight = tileSize/2;
    int bulletVelocityY = -10;
    int alienBulletVelocityY = 5;

    Timer gameLoop;
    int transitionTimer = 0;
    final int TRANSITION_DURATION = 120;
    Random random = new Random();

    AlienAttack() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);

        shipImg = new ImageIcon(getClass().getResource("./ship.png")).getImage();
        alienCyanImg = new ImageIcon(getClass().getResource("./alien-cyan.png")).getImage();
        alienMagentaImg = new ImageIcon(getClass().getResource("./alien-magenta.png")).getImage();
        alienYellowImg = new ImageIcon(getClass().getResource("./alien-yellow.png")).getImage();
        alienGreenImg = new ImageIcon(getClass().getResource("./alien-green.png")).getImage();

        alienImgArray = new ArrayList<Image>();
        alienImgArray.add(alienCyanImg);
        alienImgArray.add(alienMagentaImg);
        alienImgArray.add(alienYellowImg);
        alienImgArray.add(alienGreenImg);

        ship = new Block(shipX, shipY, shipWidth, shipHeight, shipImg);
        alienArray = new ArrayList<Block>();
        bulletArray = new ArrayList<Block>();
        alienBulletArray = new ArrayList<Block>();

        try {
            arcadeFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("./arcade.ttf")).deriveFont(36f);
            smallArcadeFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("./arcade.ttf")).deriveFont(24f);
            tinyArcadeFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("./arcade.ttf")).deriveFont(18f);
        } catch (Exception e) {
            arcadeFont = new Font("Arial", Font.BOLD, 36);
            smallArcadeFont = new Font("Arial", Font.BOLD, 24);
            tinyArcadeFont = new Font("Arial", Font.BOLD, 18);
        }

        loadSounds();

        gameLoop = new Timer(1000/60, this);
        createAliens();
        gameLoop.start();
    }
    
    private void loadSounds() {
        try {
            shootSound = loadClip("./shoot.wav");
            explosionSound = loadClip("./explosion.wav");
            alienDeathSound = loadClip("./aliendeath.wav");
            backgroundMusic = loadClip("./background.wav");
            gameOverSound = loadClip("./gameover.wav");
            levelUpSound = loadClip("./levelup.wav");
            victorySound = loadClip("./victory.wav");
            
            if (backgroundMusic != null) {
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Exception e) {
            System.out.println("Error loading sounds: " + e.getMessage());
        }
    }
    
    private Clip loadClip(String soundFile) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResource(soundFile));
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            return clip;
        } catch (Exception e) {
            System.out.println("Could not load sound: " + soundFile);
            return null;
        }
    }
    
    private void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    private void stopSound(Clip clip) {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }
    
    public void draw(Graphics g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, boardWidth, boardHeight);
        
        if (inMenu) {
            drawMenu(g);
            return;
        }
        
        if (levelTransition) {
            g.setFont(smallArcadeFont);
            g.setColor(Color.CYAN);
            String levelText = "LEVEL " + currentLevel;
            FontMetrics fm = g.getFontMetrics();
            int x = (boardWidth - fm.stringWidth(levelText)) / 2;
            int y = boardHeight / 2;
            g.drawString(levelText, x, y);
            return;
        }
        
        if (gameOver) {
            drawGameOverScreen(g);
            return;
        }
        
        if (victory) {
            drawVictoryScreen(g);
            return;
        }
        
        drawGameElements(g);
    }
    
    private void drawMenu(Graphics g) {
        g.setFont(arcadeFont);
        g.setColor(Color.CYAN);
        
        String title = "ALIEN ATTACK";
        FontMetrics fm = g.getFontMetrics();
        int x = (boardWidth - fm.stringWidth(title)) / 2;
        int y = boardHeight / 3;
        g.drawString(title, x, y);
        
        g.setFont(smallArcadeFont);
        g.setColor(Color.YELLOW);
        String startText = "PRESS SPACE TO START";
        fm = g.getFontMetrics();
        x = (boardWidth - fm.stringWidth(startText)) / 2;
        y = boardHeight / 2 + 30;
        g.drawString(startText, x, y);
        
        g.setFont(tinyArcadeFont);
        g.setColor(Color.WHITE);
        String controls1 = "CONTROLS:";
        String controls2 = "LEFT/RIGHT ARROWS - MOVE";
        String controls3 = "SPACE - SHOOT";
        
        fm = g.getFontMetrics();
        x = (boardWidth - fm.stringWidth(controls1)) / 2;
        y = boardHeight / 2 + 80;
        g.drawString(controls1, x, y);
        
        x = (boardWidth - fm.stringWidth(controls2)) / 2;
        y = boardHeight / 2 + 105;
        g.drawString(controls2, x, y);
        
        x = (boardWidth - fm.stringWidth(controls3)) / 2;
        y = boardHeight / 2 + 130;
        g.drawString(controls3, x, y);
    }
    
    private void drawGameOverScreen(Graphics g) {
        g.setFont(arcadeFont);
        
        g.setColor(Color.RED);
        String gameOverText = "GAME OVER";
        FontMetrics fm = g.getFontMetrics();
        int x = (boardWidth - fm.stringWidth(gameOverText)) / 2;
        int y = boardHeight / 3;
        g.drawString(gameOverText, x, y);
        
        g.setColor(Color.YELLOW);
        String scoreText = "SCORE: " + score;
        x = (boardWidth - fm.stringWidth(scoreText)) / 2;
        y = boardHeight / 2;
        g.drawString(scoreText, x, y);
        
        g.setFont(smallArcadeFont);
        g.setColor(Color.CYAN);
        String levelText = "LEVEL REACHED: " + currentLevel;
        fm = g.getFontMetrics();
        x = (boardWidth - fm.stringWidth(levelText)) / 2;
        y = boardHeight / 2 + 50;
        g.drawString(levelText, x, y);
        
        g.setFont(tinyArcadeFont);
        if (gameOverCooldown) {
            g.setColor(Color.ORANGE);
            String cooldownText = "READY IN: " + ((GAME_OVER_COOLDOWN - gameOverCooldownTimer) / 60 + 1);
            fm = g.getFontMetrics();
            x = (boardWidth - fm.stringWidth(cooldownText)) / 2;
            y = boardHeight / 2 + 100;
            g.drawString(cooldownText, x, y);
        } else {
            g.setColor(Color.GREEN);
            String restartText = "PRESS SPACE TO PLAY AGAIN";
            fm = g.getFontMetrics();
            x = (boardWidth - fm.stringWidth(restartText)) / 2;
            y = boardHeight / 2 + 100;
            g.drawString(restartText, x, y);
        }
    }
    
    private void drawVictoryScreen(Graphics g) {
        g.setFont(arcadeFont);
        
        g.setColor(Color.GREEN);
        String victoryText = "VICTORY!";
        FontMetrics fm = g.getFontMetrics();
        int x = (boardWidth - fm.stringWidth(victoryText)) / 2;
        int y = boardHeight / 3;
        g.drawString(victoryText, x, y);
        
        g.setFont(smallArcadeFont);
        g.setColor(Color.YELLOW);
        String scoreText = "FINAL SCORE: " + score;
        fm = g.getFontMetrics();
        x = (boardWidth - fm.stringWidth(scoreText)) / 2;
        y = boardHeight / 2;
        g.drawString(scoreText, x, y);
        
        g.setFont(smallArcadeFont);
        g.setColor(Color.CYAN);
        String levelText = "COMPLETED ALL " + maxLevel + " LEVELS!";
        fm = g.getFontMetrics();
        x = (boardWidth - fm.stringWidth(levelText)) / 2;
        y = boardHeight / 2;
        g.drawString(levelText, x, y);
        
        g.setFont(tinyArcadeFont);
        g.setColor(Color.GREEN);
        String restartText = "PRESS SPACE TO PLAY AGAIN";
        fm = g.getFontMetrics();
        x = (boardWidth - fm.stringWidth(restartText)) / 2;
        y = boardHeight / 2 + 100;
        g.drawString(restartText, x, y);
    }
    
    private void drawGameElements(Graphics g) {
        g.drawImage(ship.img, ship.x, ship.y, ship.width, ship.height, null);

        for (int i = 0; i < alienArray.size(); i++) {
            Block alien = alienArray.get(i);
            if (alien.alive) {
                g.drawImage(alien.img, alien.x, alien.y, alien.width, alien.height, null);
            }
        }

        g.setColor(Color.white);
        for (int i = 0; i < bulletArray.size(); i++) {
            Block bullet = bulletArray.get(i);
            if (!bullet.used) {
                g.drawRect(bullet.x, bullet.y, bullet.width, bullet.height);
            }
        }
        
        g.setColor(Color.red);
        for (int i = 0; i < alienBulletArray.size(); i++) {
            Block bullet = alienBulletArray.get(i);
            if (!bullet.used) {
                g.fillRect(bullet.x, bullet.y, bullet.width, bullet.height);
            }
        }
        
        g.setColor(Color.green);
        for (int i = 0; i < health; i++) {
            g.fillRect(10 + i * 25, 50, 20, 20);
        }
        
        g.setColor(Color.cyan);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Level: " + currentLevel + "/" + maxLevel, boardWidth - 150, 35);
        
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        g.drawString(String.valueOf((int) score), 10, 35);
    }

    public void move() {
        if (inMenu || levelTransition || gameOver || victory) {
            if (levelTransition) {
                transitionTimer++;
                if (transitionTimer >= TRANSITION_DURATION) {
                    levelTransition = false;
                    transitionTimer = 0;
                    createAliens();
                }
            }
            
            if (gameOver && gameOverCooldown) {
                gameOverCooldownTimer++;
                if (gameOverCooldownTimer >= GAME_OVER_COOLDOWN) {
                    gameOverCooldown = false;
                    gameOverCooldownTimer = 0;
                }
            }
            return;
        }
        
        for (int i = 0; i < alienArray.size(); i++) {
            Block alien = alienArray.get(i);
            if (alien.alive) {
                if (currentLevel <= 2) {
                    alien.x += alienVelocityX;
                    if (alien.x + alien.width >= boardWidth || alien.x <= 0) {
                        alienVelocityX *= -1;
                        alien.x += alienVelocityX * 2;
                        for (int j = 0; j < alienArray.size(); j++) {
                            alienArray.get(j).y += alienHeight;
                        }
                        break;
                    }
                } else {
                    alien.directionChangeTimer++;
                    if (alien.directionChangeTimer >= random.nextInt(30) + 30) {
                        alien.directionChangeTimer = 0;
                        int direction = random.nextInt(8);
                        switch (direction) {
                            case 0: alien.velX = 2; alien.velY = 0; break;
                            case 1: alien.velX = -2; alien.velY = 0; break;
                            case 2: alien.velX = 0; alien.velY = 1; break;
                            case 3: alien.velX = 0; alien.velY = -1; break;
                            case 4: alien.velX = 1; alien.velY = 1; break;
                            case 5: alien.velX = -1; alien.velY = 1; break;
                            case 6: alien.velX = 1; alien.velY = -1; break;
                            case 7: alien.velX = -1; alien.velY = -1; break;
                        }
                    }
                    
                    alien.x += alien.velX;
                    alien.y += alien.velY;
                    
                    if (alien.x <= 0) {
                        alien.x = 0;
                        alien.velX = Math.abs(alien.velX);
                    } else if (alien.x + alien.width >= boardWidth) {
                        alien.x = boardWidth - alien.width;
                        alien.velX = -Math.abs(alien.velX);
                    }
                    
                    if (alien.y <= 0) {
                        alien.y = 0;
                        alien.velY = Math.abs(alien.velY);
                    } else if (alien.y + alien.height >= boardHeight - tileSize * 2) {
                        alien.y = boardHeight - alien.height - tileSize * 2;
                        alien.velY = -Math.abs(alien.velY);
                    }
                }

                if (alien.y >= ship.y) {
                    alien.alive = false;
                    alienCount--;
                    loseHealth();
                }
                
                if (random.nextInt(1000) < alienShootProbability) {
                    Block alienBullet = new Block(
                        alien.x + alienWidth/2 - bulletWidth/2, 
                        alien.y + alienHeight, 
                        bulletWidth, 
                        bulletHeight, 
                        null
                    );
                    alienBulletArray.add(alienBullet);
                }
            }
        }

        for (int i = 0; i < bulletArray.size(); i++) {
            Block bullet = bulletArray.get(i);
            bullet.y += bulletVelocityY;
            for (int j = 0; j < alienArray.size(); j++) {
                Block alien = alienArray.get(j);
                if (!bullet.used && alien.alive && detectCollision(bullet, alien)) {
                    bullet.used = true;
                    alien.alive = false;
                    alienCount--;
                    score += 100;
                    playSound(alienDeathSound);
                }
            }
        }

        for (int i = 0; i < alienBulletArray.size(); i++) {
            Block bullet = alienBulletArray.get(i);
            bullet.y += alienBulletVelocityY;
            if (!bullet.used && detectCollision(bullet, ship)) {
                bullet.used = true;
                loseHealth();
                playSound(explosionSound);
            }
        }

        while (bulletArray.size() > 0 && (bulletArray.get(0).used || bulletArray.get(0).y < 0)) {
            bulletArray.remove(0);
        }
        
        while (alienBulletArray.size() > 0 && (alienBulletArray.get(0).used || alienBulletArray.get(0).y > boardHeight)) {
            alienBulletArray.remove(0);
        }

        if (alienCount == 0) {
            score += alienColumns * alienRows * 100;
            levelUp();
        }
    }
    
    public void levelUp() {
        if (currentLevel < maxLevel) {
            currentLevel++;
            levelTransition = true;
            transitionTimer = 0;
            playSound(levelUpSound);
            
            switch(currentLevel) {
                case 2:
                    alienColumns = 4;
                    alienRows = 3;
                    alienShootProbability = 3;
                    break;
                case 3:
                    alienColumns = 4;
                    alienRows = 3;
                    alienShootProbability = 4;
                    for (Block alien : alienArray) {
                        alien.velX = random.nextBoolean() ? 2 : -2;
                        alien.velY = 0;
                    }
                    break;
                case 4:
                    alienColumns = 5;
                    alienRows = 4;
                    alienShootProbability = 5;
                    break;
                case 5:
                    alienColumns = 6;
                    alienRows = 4;
                    alienShootProbability = 6;
                    break;
            }
            
            bulletArray.clear();
            alienBulletArray.clear();
        } else {
            victory = true;
            playSound(victorySound);
            stopSound(backgroundMusic);
        }
    }
    
    public void loseHealth() {
        health--;
        if (health <= 0) {
            gameOver = true;
            gameOverCooldown = true;
            playSound(gameOverSound);
            stopSound(backgroundMusic);
        }
    }

    public void createAliens() {
        alienArray.clear();
        for (int c = 0; c < alienColumns; c++) {
            for (int r = 0; r < alienRows; r++) {
                int randomImgIndex = random.nextInt(alienImgArray.size());
                Block alien = new Block(
                    alienX + c*alienWidth, 
                    alienY + r*alienHeight, 
                    alienWidth, 
                    alienHeight,
                    alienImgArray.get(randomImgIndex)
                );
                if (currentLevel >= 3) {
                    alien.velX = random.nextBoolean() ? 2 : -2;
                    alien.velY = random.nextInt(3) - 1;
                }
                alienArray.add(alien);
            }
        }
        alienCount = alienArray.size();
    }
    

    public boolean detectCollision(Block a, Block b) {
        return  a.x < b.x + b.width && 
                a.x + a.width > b.x &&  
                a.y < b.y + b.height && 
                a.y + a.height > b.y; 
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (inMenu) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                inMenu = false;
                if (backgroundMusic != null) {
                    backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                }
            }
        }
        else if (gameOver || victory) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE && (!gameOver || !gameOverCooldown)) {
                resetGame();
            }
        }
        else if (!levelTransition) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT  && ship.x - shipVelocityX >= 0) {
                ship.x -= shipVelocityX;
            }
            else if (e.getKeyCode() == KeyEvent.VK_RIGHT  && ship.x + shipVelocityX + ship.width <= boardWidth) {
                ship.x += shipVelocityX; 
            }
            else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                Block bullet = new Block(ship.x + shipWidth*15/32, ship.y, bulletWidth, bulletHeight, null);
                bulletArray.add(bullet);
                playSound(shootSound);
            }
        }
    }
    
    private void resetGame() {
        ship.x = shipX;
        bulletArray.clear();
        alienBulletArray.clear();
        alienArray.clear();
        gameOver = false;
        victory = false;
        inMenu = false;
        score = 0;
        health = maxHealth;
        currentLevel = 1;
        levelTransition = false;
        transitionTimer = 0;
        alienColumns = 3;
        alienRows = 2;
        alienVelocityX = 1;
        alienShootProbability = 2;
        gameOverCooldown = false;
        gameOverCooldownTimer = 0;
        
        if (backgroundMusic != null) {
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
        
        createAliens();
    }
}