import javax.swing.*;
import java.awt.*;
import javax.swing.border.TitledBorder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class PasswordApp {

    // --- UI COLOR PALETTE (Cyber Dark Theme) ---
    private static final Color BG_DARK = new Color(30, 33, 36);      // Deep charcoal
    private static final Color BG_LIGHT = new Color(40, 43, 48);     // Lighter gray for panels
    private static final Color FG_WHITE = new Color(220, 221, 222);  // Off-white text
    private static final Color ACCENT_CYAN = new Color(114, 137, 218); // Soft blurple/cyan
    
    // Status Colors
    private static final Color COLOR_WEAK = new Color(240, 71, 71);   // Neon Red
    private static final Color COLOR_MEDIUM = new Color(250, 166, 26); // Neon Orange
    private static final Color COLOR_STRONG = new Color(67, 181, 129); // Neon Green

    // Global variables to support the live brute force thread
    private static String currentPoolChars = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static boolean isSimulating = false;

    // Generates a cryptographically secure random Salt
    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : salt) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // SHA-256 Hashing method taking both password and salt
    private static String getSHA256(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = salt + password;
            byte[] encodedHash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "Error generating hash";
        }
    }

    // Core logic extracted into a reusable method
    private static void processPassword(String password, boolean shouldSave, JTextArea resultArea, JProgressBar strengthMeter, JLabel fileLocationLabel, JFrame frame) {
        resultArea.setText(" > Initializing secure handshake...\n > Sending data to Python analyzer...\n");
        fileLocationLabel.setText(" "); 

        try {
            ProcessBuilder pb = new ProcessBuilder("python", "analyzer.py", password);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String strength = "Weak";
            
            StringBuilder fullOutput = new StringBuilder();
            
            // Temporary storage for hash outputs to display them cleanly at the top
            String md5Hash = "N/A";
            String sha1Hash = "N/A";
            String sha256Hash = "N/A";

            // Temporary storage for dynamic analysis metrics
            String crackTime = "N/A";
            String warnings = "None";
            String suggestion = "None";

            StringBuilder analysisBlock = new StringBuilder();
            analysisBlock.append("==================================================\n");
            analysisBlock.append("          BRUTE FORCE ATTACK SIMULATION           \n");
            analysisBlock.append("==================================================\n");

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MD5:")) {
                    md5Hash = line.substring(4).trim();
                } else if (line.startsWith("SHA1:")) {
                    sha1Hash = line.substring(5).trim();
                } else if (line.startsWith("SHA256:")) {
                    sha256Hash = line.substring(7).trim();
                } else if (line.startsWith("POOL_CHARS:")) {
                    currentPoolChars = line.substring(11).trim();
                } else if (line.startsWith("STRENGTH:")) {
                    strength = line.substring(9).trim();
                } else if (line.startsWith("CRACK_TIME:")) {
                    crackTime = line.substring(11).trim();
                } else if (line.startsWith("WARNINGS:")) {
                    warnings = line.substring(9).trim();
                } else if (line.startsWith("SUGGESTION:")) {
                    suggestion = line.substring(11).trim();
                } else { 
                    analysisBlock.append(" > ").append(line).append("\n");
                }
            }

            // --- FEATURE: MULTI-HASH GENERATOR DISPLAY ---
            fullOutput.append("==================================================\n");
            fullOutput.append("          CRYPTOGRAPHIC MULTI-HASH SHIELD         \n");
            fullOutput.append("==================================================\n");
            fullOutput.append("MD5 Hash     [DEPRECATED] : ").append(md5Hash).append("\n");
            fullOutput.append("SHA-1 Hash   [VULNERABLE] : ").append(sha1Hash).append("\n");
            fullOutput.append("SHA-256 Hash [SECURE]     : ").append(sha256Hash).append("\n\n");

            // Append the defensive salt
            String randomSalt = generateSalt();
            String saltedHash = getSHA256(password, randomSalt);
            fullOutput.append("Generated Local Salt      : ").append(randomSalt).append("\n");
            fullOutput.append("Salted Local SHA-256      : ").append(saltedHash).append("\n\n");

            // Append the simulated attack metrics
            fullOutput.append(analysisBlock);
            
            // Append the parsed variables cleanly at the bottom of the analysis
            fullOutput.append("Estimated Crack Time            : ").append(crackTime).append("\n");
            fullOutput.append("System Warnings                 : ").append(warnings).append("\n");
            fullOutput.append("Safe Suggestion Password        : ").append(suggestion).append("\n");
            fullOutput.append("==================================================\n");

            resultArea.setText(fullOutput.toString());

            // Update the visual GUI progress bar & Colors based on strength
            if (strength.equals("Weak")) {
                strengthMeter.setValue(30);
                strengthMeter.setForeground(COLOR_WEAK);
                strengthMeter.setString("CRITICAL: WEAK VULNERABILITY");
            } else if (strength.equals("Medium")) {
                strengthMeter.setValue(65);
                strengthMeter.setForeground(COLOR_MEDIUM);
                strengthMeter.setString("WARNING: MEDIUM STRENGTH");
            } else {
                strengthMeter.setValue(100);
                strengthMeter.setForeground(COLOR_STRONG);
                strengthMeter.setString("SECURE: STRONG SHIELD");
            }

            // HANDLE AUTO-SAVE AUTOMATICALLY BASED ON BUTTON PRESSED
            if (shouldSave) {
                File directory = new File("SAVED_PASSWORD");
                if (!directory.exists()) {
                    directory.mkdir();
                }

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String fileName = "SAVED_PASSWORD/analysis_" + timeStamp + ".txt";

                try (FileWriter writer = new FileWriter(fileName)) {
                    writer.write("User Password Used: " + password + "\n\n");
                    writer.write(fullOutput.toString());
                    
                    fileLocationLabel.setText("✔ Log saved successfully to: " + fileName);
                    fileLocationLabel.setForeground(COLOR_STRONG); 
                } catch (Exception fileEx) {
                    fileLocationLabel.setText("✖ Error saving log file: " + fileEx.getMessage());
                    fileLocationLabel.setForeground(COLOR_WEAK);
                }
            } else {
                fileLocationLabel.setText("ℹ Privacy Mode Active: No logs written to disk.");
                fileLocationLabel.setForeground(ACCENT_CYAN); 
            }

        } catch (Exception ex) {
            resultArea.setText("CRITICAL ERROR: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        // Set up Frame
        JFrame frame = new JFrame("Sentinel | Password Analyzer & Hasher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 620); 
        frame.setLayout(new BorderLayout(15, 15));
        frame.getContentPane().setBackground(BG_DARK);

        // --- 1. INPUT PANEL (TOP) ---
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBackground(BG_LIGHT);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_CYAN),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel("ENTER TARGET PASSWORD");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(ACCENT_CYAN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setMaximumSize(new Dimension(450, 35));
        passwordField.setBackground(BG_DARK);
        passwordField.setForeground(FG_WHITE);
        passwordField.setCaretColor(FG_WHITE);
        passwordField.setBorder(BorderFactory.createLineBorder(new Color(66, 69, 73), 1));
        passwordField.setFont(new Font("Monospaced", Font.PLAIN, 16));

        JCheckBox showPasswordCheckbox = new JCheckBox("Show Password");
        showPasswordCheckbox.setBackground(BG_LIGHT);
        showPasswordCheckbox.setForeground(FG_WHITE);
        showPasswordCheckbox.setFocusPainted(false);
        showPasswordCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        showPasswordCheckbox.addActionListener(e -> {
            if (showPasswordCheckbox.isSelected()) {
                passwordField.setEchoChar((char) 0); 
            } else {
                passwordField.setEchoChar('•'); 
            }
        });

        // Visual Strength Meter
        JProgressBar strengthMeter = new JProgressBar(0, 100);
        strengthMeter.setStringPainted(true);
        strengthMeter.setString("Awaiting Input...");
        strengthMeter.setFont(new Font("SansSerif", Font.BOLD, 11));
        strengthMeter.setMaximumSize(new Dimension(450, 25));
        strengthMeter.setBackground(BG_DARK);
        strengthMeter.setBorder(BorderFactory.createLineBorder(new Color(66, 69, 73), 1));

        inputPanel.add(titleLabel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        inputPanel.add(passwordField);
        inputPanel.add(showPasswordCheckbox);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        inputPanel.add(strengthMeter);

        // --- 2. RESULTS AREA (CENTER) ---
        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setBackground(BG_DARK);
        resultArea.setForeground(FG_WHITE);
        resultArea.setCaretColor(FG_WHITE);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBackground(BG_DARK);
        
        // Custom Styled Titled Border
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(66, 69, 73), 1),
                " TERMINAL OUTPUT "
        );
        titledBorder.setTitleColor(ACCENT_CYAN);
        titledBorder.setTitleFont(new Font("SansSerif", Font.BOLD, 11));
        scrollPane.setBorder(titledBorder);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BG_DARK);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // --- 3. CONTROL PANEL (BOTTOM) ---
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBackground(BG_LIGHT);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(66, 69, 73)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // Dynamic Feedback Label (Placed on top of buttons now)
        JLabel fileLocationLabel = new JLabel(" ", SwingConstants.CENTER);
        fileLocationLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        fileLocationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(BG_LIGHT);

        JButton analyzeAndSaveButton = createStyledButton("ANALYZE & SAVE", COLOR_STRONG);
        JButton analyzeNoSaveButton = createStyledButton("ANALYZE (DON'T SAVE)", ACCENT_CYAN);
        
        // --- FEATURE: BRUTE FORCE SIMULATOR BUTTON ---
        JButton bruteForceButton = createStyledButton("SIMULATE ATTACK", COLOR_MEDIUM);
        
        JButton quitButton = createStyledButton("QUIT", COLOR_WEAK);
        
        buttonPanel.add(analyzeAndSaveButton);
        buttonPanel.add(analyzeNoSaveButton);
        buttonPanel.add(bruteForceButton); 
        buttonPanel.add(quitButton);

        bottomPanel.add(fileLocationLabel); 
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        bottomPanel.add(buttonPanel);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // --- 4. LOGIC ---
        
        analyzeAndSaveButton.addActionListener(e -> {
            String password = new String(passwordField.getPassword());
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter a password first!", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (isSimulating) return; 
            processPassword(password, true, resultArea, strengthMeter, fileLocationLabel, frame);
        });

        analyzeNoSaveButton.addActionListener(e -> {
            String password = new String(passwordField.getPassword());
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter a password first!", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (isSimulating) return; 
            processPassword(password, false, resultArea, strengthMeter, fileLocationLabel, frame);
        });

        // --- FEATURE: THE LIVE VISUAL BRUTE-FORCE SIMULATOR ---
        bruteForceButton.addActionListener(e -> {
            String password = new String(passwordField.getPassword());
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Enter a password to simulate an attack!", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (isSimulating) return;
            isSimulating = true;
            
            resultArea.setText("==================================================\n");
            resultArea.append("       INITIALIZING DICTIONARY HYBRID ATTACK      \n");
            resultArea.append("==================================================\n");

            // Run on a separate background thread so the GUI doesn't freeze
            new Thread(() -> {
                Random random = new Random();
                long startTime = System.currentTimeMillis();
                
                // Simulate 40 rapid guesses
                for (int i = 0; i < 40; i++) {
                    StringBuilder guess = new StringBuilder();
                    int lengthToSimulate = Math.max(password.length(), 4);
                    for (int j = 0; j < lengthToSimulate; j++) {
                        guess.append(currentPoolChars.charAt(random.nextInt(currentPoolChars.length())));
                    }
                    resultArea.append(" [ATTEMPT] Trying payload: " + guess.toString() + " -> FAILED\n");
                    resultArea.setCaretPosition(resultArea.getDocument().getLength());
                    
                    try { 
                        Thread.sleep(60); 
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } 
                }

                long endTime = System.currentTimeMillis();
                resultArea.append("\n==================================================\n");
                resultArea.append(" [SUCCESS] Payload Matched: " + password + "\n");
                resultArea.append(" [METRICS] Target acquired in " + (endTime - startTime) + "ms\n");
                resultArea.append("==================================================\n");
                
                isSimulating = false;
            }).start();
        });

        // Quit Button Logic with Custom Dark Theme Pop-up
        quitButton.addActionListener(e -> {
            Color oldBg = UIManager.getColor("OptionPane.background");
            Color oldPanelBg = UIManager.getColor("Panel.background");
            Color oldFg = UIManager.getColor("OptionPane.messageForeground");

            UIManager.put("OptionPane.background", BG_DARK);
            UIManager.put("Panel.background", BG_DARK);
            UIManager.put("OptionPane.messageForeground", FG_WHITE);
            UIManager.put("Button.background", BG_LIGHT);
            UIManager.put("Button.foreground", FG_WHITE);
            UIManager.put("Button.focus", new Color(0, 0, 0, 0)); 

            String message = " > CRITICAL: Terminate secure session?\n > Unsaved volatile data will be purged.";
            
            int confirm = JOptionPane.showConfirmDialog(
                    frame, 
                    message, 
                    " SYSTEM TERMINATION ", 
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            UIManager.put("OptionPane.background", oldBg);
            UIManager.put("Panel.background", oldPanelBg);
            UIManager.put("OptionPane.messageForeground", oldFg);

            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JButton createStyledButton(String text, Color hoverColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setBackground(BG_DARK);
        button.setForeground(FG_WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(hoverColor, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
                button.setForeground(BG_DARK);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BG_DARK);
                button.setForeground(FG_WHITE);
            }
        });
        
        return button;
    }
}

