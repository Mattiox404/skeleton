package it.unimib.sd2025;

import java.net.*;
import java.io.*;

/**
 * Classe principale in cui parte il database.
 */
public class Main {
    /**
     * Porta di ascolto.
     */
    public static final int PORT = 3030;
    
    /**
     * Istanza singleton del database.
     */
    private static final KeyValueDatabase database = new KeyValueDatabase();

    /**
     * Avvia il database e l'ascolto di nuove connessioni.
     */
    public static void startServer() throws IOException {
        // Carica dati iniziali
        database.loadInitialData();
        
        var server = new ServerSocket(PORT);
        System.out.println("Database listening at localhost:" + PORT);
        System.out.println("Initial database size: " + database.size() + " entries");

        try {
            while (true) {
                Socket clientSocket = server.accept();
                new Handler(clientSocket, database).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            server.close();
        }
    }

    /**
     * Handler di una connessione del client.
     * Implementa protocollo testuale simile a Redis.
     */
    private static class Handler extends Thread {
        private final Socket client;
        private final KeyValueDatabase database;
        private PrintWriter out;
        private BufferedReader in;

        public Handler(Socket client, KeyValueDatabase database) {
            this.client = client;
            this.database = database;
        }

        @Override
        public void run() {
            try {
                this.out = new PrintWriter(client.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String response = processCommand(inputLine.trim());
                    out.println(response);
                    
                    // Se il comando è QUIT, termina la connessione
                    if ("QUIT".equalsIgnoreCase(inputLine.trim())) {
                        break;
                    }
                }

            } catch (IOException e) {
                System.err.println("Client handler error: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        /**
         * Processa un comando ricevuto dal client.
         * Protocollo testuale: COMANDO [arg1] [arg2] ...
         */
        private String processCommand(String command) {
            if (command.isEmpty()) {
                return "ERR empty command";
            }

            String[] parts = command.split("\\s+");
            String cmd = parts[0].toUpperCase();

            try {
                switch (cmd) {
                    case "SET":
                        return handleSet(parts);
                    case "GET":
                        return handleGet(parts);
                    case "DEL":
                    case "DELETE":
                        return handleDelete(parts);
                    case "EXISTS":
                        return handleExists(parts);
                    case "KEYS":
                        return handleKeys(parts);
                    case "INCR":
                        return handleIncrement(parts);
                    case "SIZE":
                        return handleSize();
                    case "FLUSH":
                        return handleFlush();
                    case "PING":
                        return "PONG";
                    case "QUIT":
                        return "BYE";
                    default:
                        return "ERR unknown command: " + cmd;
                }
            } catch (Exception e) {
                return "ERR " + e.getMessage();
            }
        }

        private String handleSet(String[] parts) {
            if (parts.length < 3) {
                return "ERR SET requires key and value";
            }
            
            String key = parts[1];
            // Concatena tutti gli argomenti dal terzo in poi come valore
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                if (i > 2) valueBuilder.append(" ");
                valueBuilder.append(parts[i]);
            }
            String value = valueBuilder.toString();
            
            boolean success = database.set(key, value);
            return success ? "OK" : "ERR invalid key";
        }

        private String handleGet(String[] parts) {
            if (parts.length != 2) {
                return "ERR GET requires exactly one key";
            }
            
            String value = database.get(parts[1]);
            return value != null ? "OK " + value : "NIL";
        }

        private String handleDelete(String[] parts) {
            if (parts.length != 2) {
                return "ERR DELETE requires exactly one key";
            }
            
            boolean deleted = database.delete(parts[1]);
            return deleted ? "OK" : "NIL";
        }

        private String handleExists(String[] parts) {
            if (parts.length != 2) {
                return "ERR EXISTS requires exactly one key";
            }
            
            boolean exists = database.exists(parts[1]);
            return exists ? "OK 1" : "OK 0";
        }

        private String handleKeys(String[] parts) {
            String pattern = parts.length > 1 ? parts[1] : "*";
            String keys = database.getKeys(pattern);
            return "OK " + keys;
        }

        private String handleIncrement(String[] parts) {
            if (parts.length != 2) {
                return "ERR INCR requires exactly one key";
            }
            
            String newValue = database.increment(parts[1]);
            return newValue != null ? "OK " + newValue : "ERR value is not a number";
        }

        private String handleSize() {
            return "OK " + database.size();
        }

        private String handleFlush() {
            database.flush();
            return "OK";
        }

        private void closeConnection() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (client != null) client.close();
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Metodo principale di avvio del database.
     */
    public static void main(String[] args) throws IOException {
        startServer();
    }
}