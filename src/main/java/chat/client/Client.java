package chat.client;

import chat.Connection;
import chat.ConsoleHelper;
import chat.Message;
import chat.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected;

    public static void main(String[] args) {
        Client client=new Client();
        client.run();
    }
    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите порт сервера:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите ваше имя:");
        return ConsoleHelper.readString();
    }
    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT,text));
        } catch (IOException e) {
            clientConnected=false;
        }
    }


    public class SocketThread extends Thread{


        @Override
        public void run() {
            String address=getServerAddress();
            int port= getServerPort();
            try {
                Socket socket = new Socket(address, port);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            }
            catch (IOException | ClassNotFoundException e){
                notifyConnectionStatusChanged(false);
            }



        }

        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage("Участник с именем "+userName+" присоеденился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage("Участник с именем "+userName+" покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected=clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }
        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true){
                Message message=connection.receive();
                if (message.getType()==MessageType.NAME_REQUEST){
                    String userName=getUserName();
                    connection.send(new Message(MessageType.USER_NAME,userName));
                }
                else if (message.getType()==MessageType.NAME_ACCEPTED){
                    notifyConnectionStatusChanged(true);
                    return;
                }
                else throw new IOException("Unexpected MessageType");
            }
        }
        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            while (true){
                Message message=connection.receive();
                if (message.getType()==MessageType.TEXT){
                    processIncomingMessage(message.getData());
                }
                else if (message.getType()==MessageType.USER_ADDED){
                    informAboutAddingNewUser(message.getData());
                }
                else if (message.getType()==MessageType.USER_REMOVED){
                    informAboutDeletingNewUser(message.getData());
                }
                else throw new IOException("Unexpected MessageType");
            }
        }


    }



    public void run(){
        SocketThread socketThread=getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this){
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
                return;
            }
            if (clientConnected)
                ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
            else
                ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
        while (clientConnected){
            String text=ConsoleHelper.readString();
            if (text.equals("exit")) break;
            if (shouldSendTextFromConsole()){
                sendTextMessage(text);
            }
        }
    }



}
