import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.*;
import javax.imageio.ImageIO;
public class DisplayPanel extends JPanel implements MouseListener, KeyListener, ActionListener {

    // ================= GAME DATA =================
    private int spriteHP = 3;
    private int Zomhp;
    private long lastShotTime = 0;
    private double SHOT_DELAY = 200;
    private int BURST_DELAY = 50;
    private int burstCount = 0;
    private long lastBurstTime = 0;
    private double burstDx, burstDy;
    private double burstSx, burstSy;
    private long ZomLastTime = 0;
    private int Zom_Delay = 25;
    private int Dmg = 10;
    private int rocketRadius = 90;
    private Clip gunshotSound;

    private ArrayList<Bullet> bullets = new ArrayList<>();
    private ArrayList<Rocket> rockets = new ArrayList<>();
    private ArrayList<Zombie> zombies = new ArrayList<>();

    // ================= SETTINGS =================
    private boolean mouseHeld = false;
    private int spriteHeight = 40;
    private int spriteWidth = 30;
    private int spriteSpawn = 500;
    private int ZomHeight = 45;
    private int ZomWith = 35;
    private int bulletWidth = 20;
    private int bulletHeight = 20;
    private int ZomSpeed = 1;
    private int speed = 3;
    private int bulletSpeed = 10;
    private double bulletAngle = 0;

    private int score;
    private int wave = 0;
    private int setWaves = 25;
    private String gunType = "Pistol";
    private int spriteX;
    private int spriteY;

    private BufferedImage background;
    private BufferedImage sprite;
    private BufferedImage Zom;
    private BufferedImage bullet;
    private BufferedImage rocket;

    private boolean[] pressedKeys;
    private Timer timer;
    private boolean gameOver;

    // ================= CONSTRUCTOR =================
    public DisplayPanel() {
        score = 0;
        gameOver = false;
        spriteX = spriteSpawn;
        spriteY = spriteSpawn;
        pressedKeys = new boolean[128];
        timer = new Timer(10, this);

        try {
            background = ImageIO.read(new File("src/background.png"));
            sprite = ImageIO.read(new File("src/sprite.png"));
            Zom = ImageIO.read(new File("src/Zom.png"));
            bullet = ImageIO.read(new File("src/bullet.png"));
            rocket = ImageIO.read(new File("src/Rocket.png"));
        } catch (IOException e) {
            System.out.println("Image load error: " + e.getMessage());
        }

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("src/gunshot.wav"));
            gunshotSound = AudioSystem.getClip();
            gunshotSound.open(audioInputStream);
        } catch (Exception e) {
            System.out.println("Sound error: " + e.getMessage());
        }

        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();
        timer.start();
    }

    // ================= GAME LOOP =================
    @Override
    public void actionPerformed(ActionEvent e) {
        GunType();
        movesprite();
        moveZombies();
        moveBullets();
        moveRocket();
        separateZombies();

        if (zombies.isEmpty()) {
            wave++;
            spawnWave();
        }

        if (mouseHeld) shoot();
        checkForBulletCollisions();

        if (checkForspriteZomCollision() || wave >= setWaves) {
            spriteHP -= 1;
            if (spriteHP < 1) {
                gameOver = true;
                timer.stop();
            }
        }
        if (gunType.equals("SHOTGUN") && burstCount > 0) {

            long now = System.currentTimeMillis();

            if (now - lastBurstTime >= 50) {

                lastBurstTime = now;

                for (int i = 0; i < 9; i++) {

                    double sx2 = burstDx + (Math.random() - 0.5) * 0.7;
                    double sy2 = burstDy + (Math.random() - 0.5) * 0.7;

                    double len = Math.sqrt(sx2 * sx2 + sy2 * sy2);

                    sx2 /= len;
                    sy2 /= len;

                    bullets.add(new Bullet(burstSx, burstSy, sx2, sy2, false));
                }

                burstCount--;
            }
        }
        repaint();
    }

    // ================= DRAW =================
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 100));
            g.drawString(wave >= setWaves ? "YOU WIN!" : "YOU LOSE", getWidth() / 3, getHeight() / 2);
        } else {
            g.drawImage(sprite, spriteX, spriteY, spriteWidth, spriteHeight, null);
        }

        for (Bullet b : bullets) {
            g.drawImage(bullet, (int) b.x, (int) b.y, bulletWidth, bulletHeight, null);
        }
        for (Rocket r : rockets) {

            Graphics2D g2 = (Graphics2D) g.create();

            double angle = Math.atan2(r.dy, r.dx);

            double centerX = r.x + 15;
            double centerY = r.y + 15;

            g2.rotate(angle, centerX, centerY);

            g2.drawImage(
                    rocket,
                    (int) r.x,
                    (int) r.y,
                    30,
                    20,
                    null
            );

            g2.dispose();
        }
        for (Zombie z : zombies) {
            g.drawImage(Zom, (int) z.x, (int) z.y, ZomWith, ZomHeight, null);
        }
        g.setFont(new Font("Editor", Font.BOLD, 32));
        g.setColor(Color.YELLOW);
        g.drawString("Score: " + score, 50, 30);
        g.drawString("Wave: " + wave, 50, 60);
        g.drawString("Gun: " + gunType, 50, 90);
        g.drawString("Damage: " + Dmg, 50, 120);
        g.drawString("HEALTH: " + spriteHP, 50, 150);
    }

    // ================= INPUT =================
    @Override
    public void mousePressed(MouseEvent e) {
        mouseHeld = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseHeld = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // ================= MOVEMENT =================
    private void movesprite() {
        if (pressedKeys[KeyEvent.VK_A]) spriteX -= speed;
        if (pressedKeys[KeyEvent.VK_D]) spriteX += speed;
        if (pressedKeys[KeyEvent.VK_W]) spriteY -= speed;
        if (pressedKeys[KeyEvent.VK_S]) spriteY += speed;
    }

    private void moveZombies() {

        for (Zombie z : zombies) {

            if (z.x < spriteX)
                z.x += z.speed;
            else
                z.x -= z.speed;

            if (z.y < spriteY)
                z.y += z.speed;
            else
                z.y -= z.speed;
        }
    }

    private void moveBullets() {
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            b.x += b.dx * bulletSpeed;
            b.y += b.dy * bulletSpeed;

            if (b.rocketBullet) {
                double traveled = Math.sqrt((b.x - b.startX) * (b.x - b.startX) + (b.y - b.startY) * (b.y - b.startY));
                if (traveled >= 200) {
                    bullets.remove(i);
                    i--;
                    continue;
                }
            }

            if (out(b.x, b.y)) {
                bullets.remove(i);
                i--;
            }
        }
    }

    private void moveRocket() {
        int rocketSize = 30;

        for (int i = 0; i < rockets.size(); i++) {
            Rocket r = rockets.get(i);

            double prevX = r.x;
            double prevY = r.y;

            r.x += r.dx * (bulletSpeed * 0.6);
            r.y += r.dy * (bulletSpeed * 0.6);

            boolean exploded = false;

            Rectangle rocketRect = new Rectangle((int) r.x, (int) r.y, rocketSize, rocketSize);

            for (Zombie z : zombies) {
                Rectangle zombieRect = new Rectangle((int) z.x, (int) z.y, ZomWith, ZomHeight);

                if (rocketRect.intersects(zombieRect)) {
                    explodeRocket(r);
                    rockets.remove(i);
                    i--;
                    exploded = true;
                    break;
                }
            }

            if (exploded) continue;

            if (out(r.x, r.y)) {
                rockets.remove(i);
                i--;
            }
        }
    }

    // ================= EXPLOSION SYSTEM =================
    private void explodeRocket(Rocket r) {
        for (Zombie z : new ArrayList<>(zombies)) {
            if (distance(z.x, z.y, r.x, r.y) <= rocketRadius) {
                damageZombie(z, Dmg * 5, r.x, r.y);
            }
        }
        spawnExplosionBullets(r.x, r.y);
    }

    private void spawnExplosionBullets(double x, double y) {
        int count = 18;
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            bullets.add(new Bullet(x, y, dx, dy, true));
        }
    }

    // ================= DAMAGE =================
    private void damageZombie(Zombie z, int dmg, double fromX, double fromY) {

        z.hp -= dmg;

        if (z.x < fromX)
            z.x -= 10;
        else
            z.x += 10;

        if (z.y < fromY)
            z.y -= 10;
        else
            z.y += 10;

        if (z.hp <= 0) {
            zombies.remove(z);
            score++;
        }
    }

    private boolean checkForspriteZomCollision() {
        Rectangle spriteRect = new Rectangle(spriteX, spriteY, spriteWidth, spriteHeight);
        for (Zombie z : zombies) {
            Rectangle zomRect = new Rectangle((int) z.x, (int) z.y, ZomWith, ZomHeight);
            if (spriteRect.intersects(zomRect)) {
                zombies.remove(z);
                return true;
            }
        }
        return false;
    }

    private void checkForBulletCollisions() {
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            for (Zombie z : zombies) {
                if (rect(b).intersects(rect(z))) {
                    damageZombie(z, Dmg, b.x, b.y);
                    bullets.remove(i--);
                    break;
                }
            }
        }
    }

    private Rectangle rect(Object o) {
        if (o instanceof Bullet b) return new Rectangle((int) b.x, (int) b.y, bulletWidth, bulletHeight);
        if (o instanceof Zombie z) return new Rectangle((int) z.x, (int) z.y, ZomWith, ZomHeight);
        return new Rectangle();
    }

    // ================= UTIL =================
    private boolean out(double x, double y) {
        return x < 0 || x > getWidth() || y < 0 || y > getHeight();
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    private void playGunshot() {
        if (gunshotSound == null) return;
        gunshotSound.stop();
        gunshotSound.setFramePosition(0);
        gunshotSound.start();
    }

    // ================= ZOMBIE SEPARATION & SPAWNING =================
    private void separateZombies() {
        for (int i = 0; i < zombies.size(); i++) {
            Zombie a = zombies.get(i);
            for (int j = i + 1; j < zombies.size(); j++) {
                Zombie b = zombies.get(j);
                double dx = a.x - b.x;
                double dy = a.y - b.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 0 && dist < 80) {
                    double pushX = dx / dist;
                    double pushY = dy / dist;
                    a.x += pushX;
                    a.y += pushY;
                    b.x -= pushX;
                    b.y -= pushY;
                }
            }
        }
    }

    private void spawnWave() {
        long now = System.currentTimeMillis();

        if (now - ZomLastTime < Zom_Delay)
            return;

        ZomLastTime = now;
        int count = 3 + wave * 2;
        for (int i = 0; i < count; i++) {
            int side = (int) (Math.random() * 4);
            double x, y;
            switch (side) {
                case 0 -> {
                    x = -ZomWith;
                    y = Math.random() * getHeight();
                } // LEFT
                case 1 -> {
                    x = getWidth();
                    y = Math.random() * getHeight();
                } // RIGHT
                case 2 -> {
                    x = Math.random() * getWidth();
                    y = -ZomHeight;
                } // TOP
                default -> {
                    x = Math.random() * getWidth();
                    y = getHeight();
                } // BOTTOM
            }
            zombies.add(new Zombie(x, y));
        }
    }

    // ================= INNER CLASSES =================
    private class Zombie {

        double x;
        double y;

        double speed;
        int hp;

        Zombie(double x, double y) {

            this.x = x;
            this.y = y;

            double roll = Math.random();

            // Fast zombie (20%)
            if (roll < 0.01) {
                speed = 6;
                hp = 30;
            }
            else if (roll < 0.05) {
                speed = 1;
                hp = 250;
            } else if (roll < 0.20) {
                speed = 3.0;
                hp = 5;
            }
            // Tank zombie (30%)
            else if (roll < 0.50) {
                speed = 0.8;
                hp = 50;
            }

            // Normal zombie (50%)
            else {
                speed = 1.5;
                hp = 10;
            }
        }
    }

    private class Bullet {
        double x, y, dx, dy;
        double startX, startY;
        boolean rocketBullet;

        Bullet(double x, double y, double dx, double dy, boolean rocketBullet) {
            this.x = x;
            this.y = y;
            this.startX = x;
            this.startY = y;
            this.dx = dx;
            this.dy = dy;
            this.rocketBullet = rocketBullet;
        }
    }

    private class Rocket {
        double x, y, dx, dy;

        Rocket(double x, double y, double dx, double dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }
    }

    // ================= GUN TYPES =================
    private void GunType() {

        if (pressedKeys[KeyEvent.VK_1]) {
            gunType = "PISTOL";
            SHOT_DELAY = 200;
            Dmg = 10;
            burstCount = 5;
            BURST_DELAY = 0;
        }

        if (pressedKeys[KeyEvent.VK_2]) {
            gunType = "SHOTGUN";
            SHOT_DELAY = 500;
            Dmg = 5;
            burstCount = 3;
            BURST_DELAY = 250;
        }

        if (pressedKeys[KeyEvent.VK_3]) {
            gunType = "AK47";
            double roll = Math.random();
            if (roll < 0.005) {
                SHOT_DELAY = 60;
                Dmg = 250;
            } else if (roll < 0.20) {
                SHOT_DELAY = 70;
                Dmg = 25;
            } else if (roll < 0.40) {
                SHOT_DELAY = 80;
                Dmg = 20;
            } else {
                SHOT_DELAY = 100;
                Dmg = 15;
            }
            burstCount = 1;
            BURST_DELAY = 0;
        }


        if (pressedKeys[KeyEvent.VK_4]) {
            gunType = "MINIGUN";
            SHOT_DELAY = 0.1;
            Dmg = 1;
            burstCount =2;
            BURST_DELAY=0;
        }

        if (pressedKeys[KeyEvent.VK_5]) {
            gunType = "ROCKET LAUNCHER";
            SHOT_DELAY = 100;
            Dmg = 5;
            burstCount =1;
            BURST_DELAY=0;
        }
    }

    private double getSpread() {
        switch (gunType) {
            case "PISTOL":
                return 0.02;
            case "SHOTGUN":
                return 0.6;
            case "AK47":
                return 0.08;
            case "MINIGUN":
                return 0.5;
            case "ROCKET LAUNCHER":
                return 0;
            default:
                return 0;
        }
    }

    private void shoot() {
        long now = System.currentTimeMillis();

        if (now - lastShotTime < SHOT_DELAY)
            return;

        lastShotTime = now;


            Point click = getMousePosition();

            if (click == null)
                return;

            double sx = spriteX + spriteWidth / 2.0;
            double sy = spriteY + spriteHeight / 2.0;

            double dx = click.x - sx;
            double dy = click.y - sy;

            double len = Math.sqrt(dx * dx + dy * dy);

            if (len == 0)
                return;

            dx /= len;
            dy /= len;

            double spread = getSpread();

            dx += (Math.random() - 0.5) * spread;
            dy += (Math.random() - 0.5) * spread;

            len = Math.sqrt(dx * dx + dy * dy);

            dx /= len;
            dy /= len;

            // SHOTGUN
        if (gunType.equals("SHOTGUN")) {

            if (burstCount == 0) {

                playGunshot();

                Point click2 = getMousePosition();
                if (click2 == null) return;

                double sx2 = spriteX + spriteWidth / 2.0;
                double sy2 = spriteY + spriteHeight / 2.0;

                double dx2 = click.x - sx2;
                double dy2 = click.y - sy2;

                double len2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
                if (len == 0) return;

                dx2 /= len2;
                dy2 /= len2;

                burstDx = dx2;
                burstDy = dy2;
                burstSx = sx2;
                burstSy = sy2;

                burstCount = 3; // number of bursts
                lastBurstTime = System.currentTimeMillis();
            }

            return;
        }

            // MINIGUN
            else if (gunType.equals("MINIGUN")) {

                double sx2 = dx + (Math.random() - 0.5) * 0.35;
                double sy2 = dy + (Math.random() - 0.5) * 0.35;

                double len2 = Math.sqrt(sx2 * sx2 + sy2 * sy2);

                sx2 /= len2;
                sy2 /= len2;

                bullets.add(new Bullet(sx, sy, sx2, sy2, false));
            }

            // AK47
            else if (gunType.equals("AK47")) {

                double sx2 = dx + (Math.random() - 0.5) * 0.25;
                double sy2 = dy + (Math.random() - 0.5) * 0.25;

                double len2 = Math.sqrt(sx2 * sx2 + sy2 * sy2);

                sx2 /= len2;
                sy2 /= len2;

                bullets.add(new Bullet(sx, sy, sx2, sy2, false));
            }

            // PISTOL
            else if (gunType.equals("PISTOL")) {
            double roll = Math.random();
            if (roll < 0.1) {
                SHOT_DELAY = 5;
                Dmg = 250;
            } else if (roll < 0.20) {
                SHOT_DELAY = 10;
                Dmg = 25;
            } else if (roll < 0.50) {
                SHOT_DELAY = 15;
                Dmg = 20;
            } else {
                SHOT_DELAY = 200;
                Dmg = 10;
            }
                double sx2 = dx + (Math.random() - 0.5) * 0.1;
                double sy2 = dy + (Math.random() - 0.5) * 0.1;

                double len2 = Math.sqrt(sx2 * sx2 + sy2 * sy2);

                sx2 /= len2;
                sy2 /= len2;

                bullets.add(new Bullet(sx, sy, sx2, sy2, false));
            }

            // ROCKET LAUNCHER
            else if (gunType.equals("ROCKET LAUNCHER")) {

                rockets.add(new Rocket(sx, sy, dx, dy));
                playGunshot();
            }

            // DEFAULT
            else {

                bullets.add(new Bullet(sx, sy, dx, dy, false));
            }

            playGunshot();
        }
    }
