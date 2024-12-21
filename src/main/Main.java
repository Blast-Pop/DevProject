package main;

import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.input.mouse.MouseSettings;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.Item;

import java.awt.*;
import java.io.InputStream;
import java.util.List;
import java.util.Arrays;
import java.util.function.Supplier;

import static org.dreambot.api.wrappers.widgets.Menu.getWidth;


@ScriptManifest(category = Category.WOODCUTTING,
        name = "DevProject",
        author = "ThePoff", version = 1.0,
        description = "Bot"
)

// A Simple Tree Woodcutter in front of the Varrok Exchange. Support Bronze Axe Handling from Inventory. Future Update comming soon.

public class Main extends AbstractScript {

    private long startTime;

    Area OAK_TREE_AREA = new Area(3170,3423,3159, 3410);
    Area TREE_AREA = new Area(3173, 3450, 3147, 3461);
    // BANK AREA
    Area BANK_AREA = new Area(3179, 3448, 3191, 3432);
    // GE AREA
    Area GE_AREA = new Area(3170, 3482, 3159, 3495);


    private boolean doesAnticheat = false;
    private String currentStatus = "";
    private boolean canWoodcutoak = false;
    private int totalLogsCollected = 0;
    private boolean doesBuyingMithrilAxe = false;



    private boolean getallLevelExecuted = false;

    @Override
    public void onStart(){
        log("Starting Timberman");
        InputStream logStream = getClass().getClassLoader().getResourceAsStream("MouseCoordinates.txt");

        if(logStream == null) {
            log("Could not find MouseCoordinates.txt in resources. Stoping script.");
            stop();
            return;
        }
        List<Point> movementData = LogParser.parseLog(logStream);
        if(movementData.isEmpty()) {
            log("No valid movement found. Stopping script.");
            stop();
            return;
        }
        Mouse.setMouseAlgorithm(new WindMouseML(movementData));

        int randomMouseSpeed = Calculations.random(12, 48);
        MouseSettings.setSpeed(randomMouseSpeed);

        log("Mouse algorithm initialized with random speed: " + randomMouseSpeed);
    }


    @Override
    public int onLoop() {
        log("Starting loop..");
        int WoodcuttingSkillLevel = Skills.getRealLevel(Skill.WOODCUTTING);

        //LOOP FOR TREE + BRONZE AXE
        if (WoodcuttingSkillLevel <= 20) {  //  IF LESS OR = 20
            if (!hasBronzeAxe()) {
                currentStatus = "Handling Bronze Axe.";
                log("No Bronze Axe. Getting Bronze Axe.");
                handleBronzeAxe();
            } else if (Inventory.isFull()) {
                currentStatus = "Storing Logs...";
                log("Inventory is full. Storing logs at Bank.");
                storeLogs();

            } else if (!TREE_AREA.contains(Players.getLocal())) {
                currentStatus = "Walking to Tree Area.";
                log("Players is not in the Tree Area. Walking to Tree Area.");
                walkToTreeArea();
            } else {
                log("Chopping Tree...");
                currentStatus = "Chopping Tree.";
                chopTree();

            }
            // LOOP FOR OAK + MITHRIL AXE
        } else if(WoodcuttingSkillLevel > 20) {
            canWoodcutoak = true;
            log("Bot is at level 20 Woodcutting. Starting Oak woodcutting with Mithril Axe Handling.");
            if (!hasMithrilAxe() && doesBuyingMithrilAxe == true) {
                log("Buying Mithril axe at GE.");
                currentStatus = "Buying Mithril Axe.";
                if(!GE_AREA.contains(Players.getLocal())) {
                    Walking.walk(GE_AREA.getRandomTile());
                    buyMithrilAxe();
                }
            }else if(!hasMithrilAxe() && doesBuyingMithrilAxe == false ){
                log("No Mithril Axe in Inventory. Getting Mithril Axe.");
                currentStatus = "Handling Mithril Axe.";
                handleMithrilAxe();
            }else if (Inventory.isFull()) {
                currentStatus = "Storing Oak Logs at Bank...";
                log("Inventory is full. Storing Oak logs at Bank.");
                storeOakLogs();
            } else if (!OAK_TREE_AREA.contains(Players.getLocal())) {
                currentStatus = "Walking to Oak Area.";
                walkToOakTreeArea();
            } else {
                currentStatus = "Chopping Oak Area.";
                log("Chopping Oak Area.");
                chopOakTree();
            }
        }

        return WoodcuttingSkillLevel;
    }


    private boolean hasBronzeAxe() {
        Item equippedAxe = Equipment.getItemInSlot(3); // 3 = main.Main hand
        return (equippedAxe != null && equippedAxe.getName().equals("Bronze axe"))
                || Inventory.contains("Bronze axe");
    }

    private boolean hasMithrilAxe() {
        Item equippedAxe = Equipment.getItemInSlot(3); // 3 = main.Main hand
        return (equippedAxe != null && equippedAxe.getName().equals("Mithril axe"))
                || Inventory.contains("Mithril axe");
    }

    private void handleBronzeAxe() {
        if (!BANK_AREA.contains(Players.getLocal())) {
            Walking.walk(BANK_AREA.getRandomTile());
            sleepUntil(() -> BANK_AREA.contains(Players.getLocal()), 5000);
        } else if (!Bank.isOpen()) {
            Bank.open();
            sleepUntil(Bank::isOpen, 3000);
        } else if (!Inventory.contains("Bronze axe")) {
            Bank.withdraw("Bronze axe", 1);
            sleepUntil(() -> Inventory.contains("Bronze axe"), 3000);
        } else {
            Bank.close();
            sleepUntil(() -> !Bank.isOpen(), 2000);
        }
        if (Inventory.contains("Bronze axe") && Skills.getRealLevel(Skill.ATTACK) <= 21) {
            Inventory.interact("Wield");

        }
    }
    /*
    !Bank.contains("Mithril axe")) {
        log("No Mithril Axe. Attempting to buy at GE.");
        currentStatus = "Buying Mithril Axe.";
        buyMithrilAxe();
    */


    private void handleMithrilAxe() {
        log("Handling Mithril Axe...");
        if (!BANK_AREA.contains(Players.getLocal())) {
            Walking.walk(BANK_AREA.getRandomTile());
            sleepUntil(() -> BANK_AREA.contains(Players.getLocal()), 5000);
        } else if (!Bank.isOpen()) {
            Bank.open();
            sleepUntil(Bank::isOpen, 3000);
        } else if (!Bank.contains("Mithril axe")) {
            log("Mithril axe not found in bank. Attempting to buy.");
            currentStatus = "Buying Mithril Axe.";
            doesBuyingMithrilAxe = true;
            buyMithrilAxe();
            return;
        } else if (!Inventory.contains("Mithril axe")) {
            Bank.withdraw("Mithril axe", 1);
            sleepUntil(() -> Inventory.contains("Mithril axe"), 3000);
        } else {
            Bank.close();
            sleepUntil(() -> !Bank.isOpen(), 2000);
        }

        if (Inventory.contains("Mithril axe") && Skills.getRealLevel(Skill.ATTACK) >= 21) {
            log("Equipping Mithril axe...");
            Item mithrilAxe = Inventory.get("Mithril axe");
            if (mithrilAxe != null && mithrilAxe.hasAction("Wield")) {
                mithrilAxe.interact("Wield");
                sleepUntil(() -> Equipment.contains("Mithril axe"), 2000);
            }
        }
    }


    private void walkToTreeArea() {
        int randomTimeout = Calculations.random(288, 1121);
        Walking.walk(TREE_AREA);
        sleepUntil(() -> TREE_AREA.contains(Players.getLocal()),randomTimeout);
    }

    private void walkToOakTreeArea() {
        int randomTimeout = Calculations.random(299, 1145);
        Walking.walk(OAK_TREE_AREA);
        sleepUntil(() -> OAK_TREE_AREA.contains(Players.getLocal()),randomTimeout);
    }

    private void chopTree() {
        GameObject tree = GameObjects.closest("Tree");
        if (tree != null && tree.interact("Chop down")) {
            sleep(700);
            assert tree != null;
            if (tree != null) {
                log("Facing Tree...");
                Camera.keyboardRotateToEntity(tree);
                int randomPitch = Calculations.random(152, 353);
                Camera.keyboardRotateToPitch(randomPitch);

            }
            sleepUntil(() -> !tree.exists() || Inventory.isFull(), 10000);
        }
    }

    private void chopOakTree() {
        GameObject tree = GameObjects.closest("Oak tree");
        if (tree != null && tree.interact("Chop down")) {
            sleep(700);
            assert tree != null;
            if (tree != null) {
                log("Facing Tree...");
                Camera.keyboardRotateToEntity(tree);
                int randomPitch = Calculations.random(152, 353);
                Camera.keyboardRotateToPitch(randomPitch);

            }
            sleepUntil(() -> !tree.exists() || Inventory.isFull(), 10000);
        }
    }

    private void storeOakLogs() {
        int randomTimeout = Calculations.random(229, 1544);
        if (!BANK_AREA.contains(Players.getLocal())) {
            Walking.walk(BANK_AREA.getRandomTile());
            sleepUntil(() -> BANK_AREA.contains(Players.getLocal()), randomTimeout);
        } else if (!Bank.isOpen()) {
            Bank.open();
            sleepUntil(Bank::isOpen, randomTimeout);
        } else {
            Bank.depositAll("Oak logs");
            sleepUntil(() -> !Inventory.contains("Oak logs"), randomTimeout);
            Bank.close();

            sleepUntil(() -> !Bank.isOpen(), randomTimeout);
        }
    }

    private void storeLogs() {
        int randomTimeout = Calculations.random(258, 1322);
        if (!BANK_AREA.contains(Players.getLocal())) {
            Walking.walk(BANK_AREA.getRandomTile());
            sleepUntil(() -> BANK_AREA.contains(Players.getLocal()), randomTimeout);
        } else if (!Bank.isOpen()) {
            Bank.open();
            sleepUntil(Bank::isOpen, randomTimeout);
        } else {
            Bank.depositAll("Logs");
            sleepUntil(() -> !Inventory.contains("Logs"), randomTimeout);
            Bank.close();

            sleepUntil(() -> !Bank.isOpen(), randomTimeout);
        }
    }

    private void sleepUntil(Supplier<Boolean> condition, int timeout) {
        int waited = 0;
        int sleepInterval = 50;
        while (!condition.get() && waited < timeout) {
            sleep(sleepInterval);
            waited += sleepInterval;
        }
    }

    private void buyMithrilAxe(){
        int GPvalue = 0;
        if (!GE_AREA.contains(Players.getLocal())) {
            Walking.walk(GE_AREA.getRandomTile());
            sleepUntil(() -> GE_AREA.contains(Players.getLocal()), Calculations.random(152, 353));
        } else {
            Bank.open();
            GPvalue = Bank.count("Coins");
            Bank.close();
        }
        if (GPvalue >= 126) {
            log("Buying Mithril Axe...");
            GrandExchange.open();
            int randomTimer = 0;
            randomTimer = Calculations.random(8569, 15258);
            if(GrandExchange.buyItem("Mithril axe", 1, 126)){
                if (Sleep.sleepUntil(GrandExchange::isReadyToCollect, randomTimer )) {
                    GrandExchange.collect();
                }

            }

        }
    }


    private boolean AntiCheatCheck(){
        int random1 = 0;
        int random2 = 0;
        random1 = Calculations.random(1, 20);
        random2 = Calculations.random(1, 20);
        if (random1 == random2){
            doesAnticheat = true;
        }
        return doesAnticheat;
    }



    @Override
    public void onPaint(Graphics g) {
        String ThePoffDiscordLink = "https://discord.gg/xpQAbGSGEu";
        // Temps écoulé
        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        int hours = (int) (elapsedTime / 3600);
        int minutes = (int) ((elapsedTime % 3600) / 60);
        int seconds = (int) (elapsedTime % 60);
        String formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        // Récupération des niveaux
        int AttackLevel = Skills.getRealLevel(Skill.ATTACK);
        int Woodcutting = Skills.getRealLevel(Skill.WOODCUTTING);
        boolean canWoodcutOak = Woodcutting >= 20;

        // Suivi des logs et calcul des GP
        int gpPerLog = 84; // Valeur par log en GP
        int totalLogs = totalLogsCollected;   // Méthode pour récupérer le nombre de logs collectés
        int totalGP = totalLogs * gpPerLog; // Calcul des GP accumulés

        // Récupère les dimensions de la fenêtre du client
        int width = getWidth();

        // Police pour l'affichage
        Font font = new Font("Arial", Font.BOLD, 24);
        g.setFont(font);

        // Liste des informations à afficher
        String[] lines = {
                "Bot Status: " + currentStatus,
                "Time elapsed: " + formattedTime,
                "Woodcutting: " + Woodcutting,
                "Can Woodcut Oak: " + canWoodcutOak,
                "ThePoff Discord: " + ThePoffDiscordLink
        };

        // Calcul des dimensions du rectangle
        int padding = 25; // Espace autour du texte
        int lineHeight = g.getFontMetrics().getHeight(); // Hauteur de chaque ligne
        int rectWidth = Arrays.stream(lines)
                .mapToInt(g.getFontMetrics()::stringWidth)
                .max()
                .orElse(0) + 2 * padding; // Largeur ajustée pour le texte le plus large
        int rectHeight = lines.length * lineHeight + 2 * padding; // Hauteur ajustée pour le nombre de lignes

        // Position du rectangle
        int rectX = Math.max(width - rectWidth - padding, padding); // À droite avec un espace
        int rectY = padding;

        // Fond noir semi-transparent
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(rectX, rectY, rectWidth, rectHeight);

        // Texte blanc
        g.setColor(Color.WHITE);
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], rectX + padding, rectY + padding + (i + 1) * lineHeight - 5);
        }
    }






}
