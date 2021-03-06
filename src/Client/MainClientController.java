package Client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import jdk.jfr.Event;

import java.beans.EventHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class MainClientController {
    @FXML Label welcomeLabel;

    private String username; // clients username
    private Stage primaryStage; // displayed stage

    private Socket socket = null; // used to store client socket
    private PrintWriter networkOut = null; // used to write to socket
    private BufferedReader networkIn = null; // used to read from socket
    private String winner = null; // stores the rounds winner
    private String[] move = new String[2]; // stores the moves in a round
    private String[] players = new String[2]; // stores the players names in a round
    private Boolean validNames = true; // determines if names are valid
    private Boolean first = true; // determines if it is the first iteration

    //constants\\
    private final double buttonFitWidth = 250;
    private final double buttonFitHeight = 320;
    public static String SERVER_ADDRESS = "localhost"; // server address
    public static int    SERVER_PORT = 16789; // server port

    //-- Private Methods --\\

    /**
     * This function will determine which button action is requested
     * @param actionName - string variable that contains the button command
     */
    private void selectAction(String actionName){ // this is the method that gets fired when any of the action buttons are pressed
        // Conditional to set the clients move
        if (actionName.equalsIgnoreCase("rock") || actionName.equalsIgnoreCase("paper") || actionName.equalsIgnoreCase("scissors")){
            networkOut.println("SETMOVE " + actionName);
            try {
                System.out.println(networkIn.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (actionName.equalsIgnoreCase("results")) { // Conditional if the user wants to view the round results
            networkOut.println("GETSIZE"); // Calls the socket to get the size of the moves Vector
            String line = null;

            int id = -1;
            try {
                line = networkIn.readLine(); // Reads from the socket
                id = (new Integer(line)).intValue(); // Converts the size into an integer
                if (id%2==0){ // conditional if the size of the moves Vector is even (both players made a move)
                    networkOut.println("GETMOVE " + (id-2));
                    move[0] = networkIn.readLine(); // gets the first move
                    networkOut.println("GETMOVE " + (id-1));
                    move[1] = networkIn.readLine(); // Gets the second move

                    System.out.println(move[0] + "\n" + move[1]);

                    int index = move[0].indexOf('[')+1;
                    int index2 = move[0].indexOf(']');
                    players[0] = move[0].substring(index, index2); // Gets first player

                    index = move[0].indexOf(':')+2;
                    move[0] = move[0].substring(index); // Gets first player's move

                    index = move[1].indexOf('[')+1;
                    index2 = move[1].indexOf(']');
                    players[1] = move[1].substring(index, index2); // Gets second player

                    index = move[1].indexOf(':')+2;
                    move[1] = move[1].substring(index); // Gets the second player's move

                    // Conditional to set opponent in local instance
                    if (first) {
                        if (players[0].equalsIgnoreCase(username)){
                            networkOut.println("SETOP " + players[1]);
                            String in = networkIn.readLine();
                        } else if (players[1].equalsIgnoreCase(username)){
                            networkOut.println("SETOP " + players[0]);
                            String in = networkIn.readLine();
                        }
                        first = false;
                    }

                    // Checks if player one wins the round
                    if ((move[0].equalsIgnoreCase("paper") && move[1].equalsIgnoreCase("rock")) ||
                       (move[0].equalsIgnoreCase("rock") && move[1].equalsIgnoreCase("scissors")) ||
                       (move[0].equalsIgnoreCase("scissors") && move[1].equalsIgnoreCase("paper"))) {
                        winner = players[0];
                        System.out.println("The winner of the round is: " + winner);
                        networkOut.println("SETWIN " + players[0]);
                        try {
                            System.out.println(networkIn.readLine());
                            networkOut.println("GETWIN " + players[0]);
                            System.out.println(networkIn.readLine());
                        } catch (IOException e) {
                            System.err.println("Error reading from Handler");
                        }
                    } // Checks if player two wins the round
                    else if (((move[1].equalsIgnoreCase("paper") && move[0].equalsIgnoreCase("rock")) ||
                            (move[1].equalsIgnoreCase("rock") && move[0].equalsIgnoreCase("scissors")) ||
                            (move[1].equalsIgnoreCase("scissors") && move[0].equalsIgnoreCase("paper")))) {
                        winner = players[1];
                        System.out.println("The winner of the round is: " + winner);
                        networkOut.println("SETWIN " + players[1]);
                        try {
                            System.out.println(networkIn.readLine());
                            networkOut.println("GETWIN " + players[1]);
                            System.out.println(networkIn.readLine());
                        } catch (IOException e) {
                            System.err.println("Error reading from Handler");
                        }
                    } else { // If neither then round is a draw
                        System.out.println("Draw!");
                        winner = "Draw";
                    }
                    if (players[0].equalsIgnoreCase(players[1])){ // conditional if a single client enters two moves
                        validNames=false;
                    } else { //save the outcome of the current round and win history to gameLog.txt
                        openGraphics(); // Opens graphics component
                        save(players); // Automatically calls save function
                    }
                } else { // If only one player has selected a move
                    System.err.println("Please wait until the other player has made a move");
                }
            } catch (IOException e) { // Error if cannot read from Handler
                System.err.println("Error reading from Handler");
            }
            if (validNames == false){ // if a boolean that verifies a move is false, then close the program
                System.err.println("You entered the more than one command. Please run the program again properly.");
                System.exit(0);
            }
        } else { // If action is not recognized
            System.out.println("No such action "+actionName);
        }
    }

    //-- Controller Methods --\\

    /**
     * Initializes the data within the MainClientController class
     * @param username - String variable that stores the username
     */
    public void initData(String username){
        this.username = username;

        try{
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT); // sets up socket
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: "+SERVER_ADDRESS);
        } catch (IOException e) {
            System.err.println("IOException while connecting to server: "+SERVER_ADDRESS);
        }
        try {
            networkOut = new PrintWriter(socket.getOutputStream(), true); // sends up writer
            networkIn = new BufferedReader(new InputStreamReader(socket.getInputStream())); // sets up reader
        } catch (IOException e) {
            System.err.println("IOException while opening a read/write connection");
        }

        // Determines if 2 players have already been reached or not
        networkOut.println("GETCLIENTS");
        int id = -1;
        String line = null;
        try {
            line = networkIn.readLine();
            id = (new Integer(line)).intValue();
            if (id >= 2) {
                System.err.println("Too Many Players");
                System.exit(0);
            } else {
                networkOut.println("SETUSR " + username);
                System.out.println(networkIn.readLine());
            }
        } catch (IOException e) {
            System.err.println("IOException while opening a read/write connection"); // More than 2 players will end the program
        }

        // Displays the "Play Game" UI
        welcomeLabel.setText("Welcome to Rock Paper Scissors "+username);
        primaryStage = (Stage) welcomeLabel.getScene().getWindow();
    }

    /**
     * Sets up the Graphics Display stage for the results screen
     */
    public void openGraphics(){
        //method is fired when game is over
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLFiles/Graphics.fxml"));
            primaryStage.setScene(new Scene(loader.load()));

            //getting the buttons to apply images through code
            Label outcome = (Label) primaryStage.getScene().lookup("#outcome");
            ImageView player1 = (ImageView) primaryStage.getScene().lookup("#Player1");
            ImageView player2 = (ImageView) primaryStage.getScene().lookup("#Player2");
            Button newRoundButton = (Button) primaryStage.getScene().lookup("#newRound");
            Button exitButton = (Button) primaryStage.getScene().lookup("#exit");

            //generating images
            Image rock = new Image("/Rock.png");
            Image paper = new Image("/Paper.png");
            Image scissors = new Image("/Scissors.png");

            //Change image based on player choice
            if(move[0].equalsIgnoreCase("rock")){
                player1.setImage(rock);
            } else if(move[0].equalsIgnoreCase("paper")){
                player1.setImage(paper);
            } else {
                player1.setImage(scissors);
            }

            if(move[1].equalsIgnoreCase("rock")){
                player2.setImage(rock);
            } else if(move[1].equalsIgnoreCase("paper")){
                player2.setImage(paper);
            } else {
                player2.setImage(scissors);
            }

            //Making buttons consistent size
            player1.setFitHeight(buttonFitHeight);
            player1.setFitWidth(buttonFitWidth);
            player2.setFitHeight(buttonFitHeight);
            player2.setFitWidth(buttonFitWidth);

            //Set label text dictating the winner
            if(winner == "Draw"){
                outcome.setText("Draw");
            } else {
                outcome.setText(winner + " wins!");
            }

            //Switch back to RPS screen after Graphics screen
            newRoundButton.setOnAction((action -> {
                playGame();
            }));

            //Close both player clients
            exitButton.setOnAction((action -> {
                primaryStage.close();
                networkOut.println("LOGOUT");
                try{
                    System.out.println(networkIn.readLine());
                } catch (IOException e){
                    System.out.println("Error reading from server");
                }
            }));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays Move selector UI for the game
     */
    public void playGame(){
        //method is fired when button is pressed
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLFiles/RockPaperScissors.fxml"));
            primaryStage.setScene(new Scene(loader.load()));

            //getting the buttons to apply images through code
            Button rockButton = (Button) primaryStage.getScene().lookup("#rock");
            Button paperButton = (Button) primaryStage.getScene().lookup("#paper");
            Button scissorsButton = (Button) primaryStage.getScene().lookup("#scissors");
            Button resultsButton = (Button) primaryStage.getScene().lookup("#results");

            //generating image views
            ImageView rockView = new ImageView(new Image("/Rock.png"));
            ImageView paperView = new ImageView(new Image("/Paper.png"));
            ImageView scissorsView = new ImageView(new Image("/Scissors.png"));
            rockView.setFitWidth(buttonFitWidth);
            rockView.setFitHeight(buttonFitHeight);
            paperView.setFitWidth(buttonFitWidth);
            paperView.setFitHeight(buttonFitHeight);
            scissorsView.setFitWidth(buttonFitWidth);
            scissorsView.setFitHeight(buttonFitHeight);

            //adding image view to buttons
            rockButton.setGraphic(rockView);
            paperButton.setGraphic(paperView);
            scissorsButton.setGraphic(scissorsView);

            //connecting to action methods
            rockButton.setOnAction((actionEvent -> {selectAction("rock");}));
            paperButton.setOnAction((actionEvent -> {selectAction("paper");}));
            scissorsButton.setOnAction((actionEvent -> {selectAction("scissors");}));
            resultsButton.setOnAction((action -> {selectAction("results");}));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Save function which is automatically called after every round
     * @param players - Array of players names
     */
    public void save(String[] players){
        String filename = "gameLog.txt";
        try{
            PrintWriter output = new PrintWriter(filename);
            //winner global variable is accessed
            String line;
            if(winner.equalsIgnoreCase("Draw")){
                line = "This round is a draw \n";
            } else {
                line = "The Winner of this Round is: " + winner + " (" + move[0] + " vs. " + move[1] + ")\n";
            }
            //Server needed to get wins for player 1
            networkOut.println("GETWIN " + players[0]);
            String player1Wins =  networkIn.readLine();
            line += players[0] + " has " + player1Wins + " wins.\n";

            //Server needed to get wins for player 1
            networkOut.println("GETWIN " + players[1]);
            String player2Wins =  networkIn.readLine();
            line += players[1] + " has " + player2Wins + " wins.";
            output.println(line);
            output.close();
        }catch(IOException e) {
            System.err.printf("File Error: %s\n", e.getMessage());
        }
    }
}
