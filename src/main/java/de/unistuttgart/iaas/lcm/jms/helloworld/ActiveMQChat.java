package de.unistuttgart.iaas.lcm.jms.helloworld;

import org.apache.activemq.command.ProducerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("ALL")
public class ActiveMQChat {

    final static Logger logger = LoggerFactory.getLogger(HelloWorldActiveMQ.class);

    private static ConnectionFactory conFactory;

    private static Topic topic;
    private static Connection connection;
    private static Session session;

    private static String user;

    public static void main(String[] args) throws Exception{
        System.out.println("Please login:");
        Scanner scan = new Scanner(System.in);
        user = scan.nextLine();
        init();

        System.out.println("Hello " + user + ", write a message:");
        subscribe(topic);
        //Publish
        connection.start();
        MessageProducer producer = session.createProducer(topic);
        while (true) {
            String chatMsg= scan.nextLine();
            String[] parts = chatMsg.split(" ");
            if("join".equals(parts[0])) {
                String topicname = parts[1];
                if ("default".equals(topicname)){
                    producer = session.createProducer(topic);
                } else {
                    Topic chosenTopic = session.createTopic(topicname);
                    producer = session.createProducer(chosenTopic);
                    System.out.println("Joining topic " + chosenTopic);
                    subscribe(chosenTopic);
                }
            } else if ("talk-to".equals(parts[0])){
                String partner = parts[1];
                String topicname = "";
                topicname += (user.compareTo(partner) > 0)? partner : user;
                topicname += (user.compareTo(partner) > 0)? user : partner;
                Topic privateTopic = session.createTopic(topicname);
                producer = session.createProducer(privateTopic);
                System.out.println("Joined private Topic " + topicname);
                subscribe(privateTopic);
            }
            else {
                TextMessage msg = session.createTextMessage(chatMsg);
                msg.setStringProperty("userName", user);
                producer.send(msg);
            }
        }
    }

    private static void subscribe(Topic topic){

        try {
        MessageConsumer consumer = session.createConsumer(topic);
        final List<String> blacklist = new ArrayList<String>();
        blacklist.add("Seedrix");
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message msg) {
                    try {
                        TextMessage message = (TextMessage) msg;
                        String username = message.getStringProperty("userName");
                        if (!blacklist.contains(username))
                            System.out.println(username + ": " + message.getText());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private static void init() {
        try {
            Context jndi = new InitialContext();
            conFactory = (ConnectionFactory) jndi.lookup("HelloWorldFactory");
            connection = conFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = (Topic) jndi.lookup("Chat");

        } catch (NamingException e) {
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
