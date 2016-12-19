package gossip;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Notification;
import javax.management.NotificationListener;


public class Client implements NotificationListener {

    private ArrayList<Member> memberList;

    private ArrayList<Member> deadList;

    private ArrayList<Member> oldVersionMembersList;

    // time interval for sending gossip msg
    // used in MembershipGossiper
    private int t_gossip; //in ms

    //time until declaring a member dead
    //used in TimeoutTimer
    public int t_cleanup; //in ms

    private Random random;

    private DatagramSocket server;

    private String myAddress;

    private Member me;



    /**
     * Setup the client's lists, gossiping parameters, and parse the startup config file.
     *
     * @throws SocketException
     * @throws InterruptedException
     * @throws UnknownHostException
     */
    public Client() throws SocketException, InterruptedException, UnknownHostException {

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println(">>> I'm OUT <<<");
            }
        }));

        memberList = new ArrayList<Member>();

        deadList = new ArrayList<Member>();

        oldVersionMembersList = new ArrayList<Member>();


        //time interval for sending gossip msg
        // 1 second TODO: make configurable
        t_gossip = 1000;
        // 10 seconds TODO: make configurable
        //time until declaring a member dead
        t_cleanup = 10000;

        random = new Random();

        int port = 0;


        String myIpAddress = InetAddress.getLocalHost().getHostAddress();
        this.myAddress = myIpAddress + ":" + port;
        //Check my IP
        //System.out.println(">>>>" + myAddress);


        ArrayList<String> startupHostsList = parseStartupMembers();

        // loop over the initial hosts, and find ourselves
        for (String host : startupHostsList) {

            Member member = new Member(host, 0, this, t_cleanup);
            File_info file_info;

            if (host.contains(myIpAddress)) {
                // save our own Member class so we can increment our heartbeat later
                me = member;
                port = Integer.parseInt(host.split(":")[1]);
                this.myAddress = myIpAddress + ":" + port;

                //////////////////////////////////////////////////////
                //create a file info for the only i know at this time
                //add it to peer list
                file_info = new File_info(parseFileVersion());
                me.setF_i(file_info);
                //////////////////////////////////////////////////////

                System.out.println("ME >> " + me);

            }
            memberList.add(member);
        }


        System.out.println("---------------------");
        System.out.println("My Stored Member List");
        System.out.println("---------------------");

        for (Member member : memberList) {
            System.out.println(member);
        }


        /////////////////////////
        //TESTING ADDED FUNCS
        /////////////////////////
        System.out.println("<<<<<<<<<<<<<<<<<<<<TESTING STUFF>>>>>>>>>>>>>>>>>>>>>");
        //System.out.println(parseFileVersion());
        File_info n = new File_info(parseFileVersion());
        System.out.println(n);
        System.out.println(">>>>>>>>>>>>>>>>>END OFF TESTING STUFF>>>>>>>>>>>>>>>>>>>>>");
        /////////////////////////
        //END OF TESTING ADDED FUNCS
        /////////////////////////


        System.out.println("---------------------");
        System.out.println("---- NOW STARTING ---");
        System.out.println("---------------------");


        if (port != 0) {
            // TODO: starting the server could probably be moved to the constructor of the receiver thread.
            server = new DatagramSocket(port);
        } else {
            // This is bad, so no need proceeding on
            System.err.println("Could not find myself in startup list >> my IP is: " + myAddress);
            System.exit(-1);
        }
    }


    /**
     * In order to have some membership lists at startup, we read the IP addresses
     * and port at a newline delimited config file.
     *
     * @return List of <IP address:port> Strings
     */
    private ArrayList<String> parseStartupMembers() {
        ArrayList<String> startupHostsList = new ArrayList<String>();

        File startupConfig = new File("config", "startup_members");

        try {
            BufferedReader br = new BufferedReader(new FileReader(startupConfig));
            String line;
            while ((line = br.readLine()) != null) {
                startupHostsList.add(line.trim());
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return startupHostsList;
    }


    /**
     * In order know which file version we have to compare with others
     * we read the FILE NAME
     * the NUMBER OF CHUNCKS
     * and the CHUNKS I HAVE
     * at a newline delimited config file.
     *
     * @return List of [0]<File_Name,
     * [1]#Chuncks,
     * [2]part 1 part 2 ... part n> Strings
     */
    private ArrayList<String> parseFileVersion() {
        ArrayList<String> startupFile = new ArrayList<String>();

        File startupConfig = new File("config", "file_info");

        try {
            BufferedReader br = new BufferedReader(new FileReader(startupConfig));
            String line;
            String file_name;
            String n_chunks;
            String chunks;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                file_name = line.split(":")[0];
                n_chunks = line.split(":")[1];
                chunks = line.split(":")[2];

                startupFile.add(file_name);
                startupFile.add(n_chunks);
                startupFile.add(chunks);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return startupFile;
    }


    /**
     * Performs the sending of the membership list, after we have
     * incremented our own heartbeat.
     * * **************************************************************************
     * REVIEW: define a FANOUT make it so that a random number of Members is chosen
     * this way is faster to propagate the msg
     * REVIEW: define a A MAXIMUM NUMBER OF NODES PER MEMBER_LIST
     * or else everyone will have a list with the whole network of peers
     */
    private void sendMembershipList() {

        this.me.setHeartbeat(me.getHeartbeat() + 1);

        synchronized (this.memberList) {
            try {
                Member member;
                //get a priority member first
                // if a member in the oldVersionMembersList exists i should send him my list
                //remove right away it has to be the one that is late to deal with synch
                if ( oldVersionMembersList.isEmpty() ){
                    //get a random member from member list
                    //to send my membership Listing
                    member = getRandomMember();
                }else{
                    //get the last of list
                    int nth = oldVersionMembersList.size()-1;
                    member = oldVersionMembersList.get( nth );
                    oldVersionMembersList.remove(nth);

                }

                if (member != null) {
                    //prepare object to send
                    //sending serialized object
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(this.memberList);
                    byte[] buf = baos.toByteArray();

                    // get random member adressinfo info
                    // format IP:PORT
                    String address = member.getAddress();
                    String host = address.split(":")[0];
                    int port = Integer.parseInt(address.split(":")[1]);

                    // using member adress to set Host for Datagram
                    InetAddress dest;
                    dest = InetAddress.getByName(host);

                    System.out.println("Sending my list to random member with IP: " + dest);
                    System.out.println("LIST SENT_____________________");
                    for (Member m : memberList) {
                        System.out.println(m);
                    }
                    System.out.println("------------------------------");


                    //SENDING THE MEMBERSHIP LIST TO RANDOM MEMBER
                    // TODO: make configurable
                    // 2 options simulate data loss or not
                    boolean loss_option = false;
                    if (loss_option) {
                        // simulate some packet loss ~25%
                        int percentToSend = random.nextInt(100);
                        if (percentToSend > 25) {
                            DatagramSocket socket = new DatagramSocket();
                            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
                            socket.send(datagramPacket);
                            socket.close();
                        }
                    } else {
                        // no loss
                        DatagramSocket socket = new DatagramSocket();
                        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
                        socket.send(datagramPacket);
                        socket.close();
                    }
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Find a random peer from the local membership list.
     * Ensure that we do not select ourselves, and keep
     * trying 10 times if we do.  Therefore, in the case
     * where this client is the only member in the list,
     * this method will return null
     *
     * @return Member random member if list is greater than 1, null otherwise
     */
    private Member getRandomMember() {
        Member member = null;

        if (this.memberList.size() > 1) {
            int tries = 10;
            do {
                int randomNeighborIndex = random.nextInt(this.memberList.size());
                member = this.memberList.get(randomNeighborIndex);
                if (--tries <= 0) {
                    member = null;
                    break;
                }
            } while (member.getAddress().equals(this.myAddress));
        } else {
            System.out.println(">>>>>> NO MEMBERS OTHER THAN ME FOUND <<<<<<");
        }

        return member;
    }



    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    // INNER CLASS >> MembershipGossiper
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The class handles gossiping the membership list.
     * This information is important to maintaining a common
     * state among all the nodes, and is important for detecting
     * failures.
     */
    private class MembershipGossiper implements Runnable {

        private AtomicBoolean keepRunning;

        public MembershipGossiper() {
            this.keepRunning = new AtomicBoolean(true);
        }


        @Override
        public void run() {
            while (this.keepRunning.get()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(t_gossip);
                    sendMembershipList();
                } catch (InterruptedException e) {
                    // TODO: handle exception
                    // This membership thread was interrupted externally, shutdown
                    e.printStackTrace();
                    keepRunning.set(false);
                }
            }

            this.keepRunning = null;
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    // END OFF INNER CLASS >> MembershipGossiper
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    // INNER CLASS >> AsychronousReceiver
    ///////////////////////////////////////////////////////////////////////////

    /**
     * This class handles the passive cycle, where this client
     * has received an incoming message.  For now, this message
     * is always the membership list, but if you choose to gossip
     * additional information, you will need some logic to determine
     * the incoming message.
     */
    private class AsychronousReceiver implements Runnable {

        private AtomicBoolean keepRunning;

        public AsychronousReceiver() {
            keepRunning = new AtomicBoolean(true);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            while (keepRunning.get()) {
                try {
                    //TODO XXX: be mindful of this array size for later
                    //the size should be sent first
                    byte[] buf = new byte[256];

                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    server.receive(p);

                    // extract the member arraylist out of the packet
                    // TODO: maybe abstract this out to pass just the bytes needed
                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    Object readObject = ois.readObject();
                    if (readObject instanceof ArrayList<?>) {
                        ArrayList<Member> list = (ArrayList<Member>) readObject;

                        System.out.println("Received member list: ");
                        System.out.println("from [" + p.getAddress() + "]");
                        for (Member member : list) {
                            System.out.println(member);
                        }

                        //this will be checking versions
                        checkNewVersionInList(list);




                        // Merge our list with the one we just received
                        mergeLists(list);

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    keepRunning.set(false);
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        /**
         * Merge remote list (received from peer), and our local member list.
         * Simply, we must update the heartbeats that the remote list has with
         * our list.  Also, some additional logic is needed to make sure we have
         * not timed out a member and then immediately received a list with that
         * member.
         * REVIEW: how to merge when version received is bigger. Have to keep a list of peers to download
         * @param remoteList
         */
        private void mergeLists(ArrayList<Member> remoteList) {

            synchronized (Client.this.deadList) {

                synchronized (Client.this.memberList) {

                    for (Member remoteMember : remoteList) {
                        if (Client.this.memberList.contains(remoteMember)) {
                            Member localMember = Client.this.memberList.get(Client.this.memberList.indexOf(remoteMember));

                            if (remoteMember.getHeartbeat() > localMember.getHeartbeat()) {
                                // update local list with latest heartbeat
                                localMember.setHeartbeat(remoteMember.getHeartbeat());
                                // and reset the timeout of that member
                                localMember.resetTimeoutTimer();
                            }
                        } else {
                            // the local list does not contain the remote member

                            // the remote member is either brand new, or a previously declared dead member
                            // if its dead, check the heartbeat because it may have come back from the dead

                            if (Client.this.deadList.contains(remoteMember)) {
                                Member localDeadMember = Client.this.deadList.get(Client.this.deadList.indexOf(remoteMember));
                                if (remoteMember.getHeartbeat() > localDeadMember.getHeartbeat()) {
                                    // it's baa-aack
                                    Client.this.deadList.remove(localDeadMember);
                                    Member newLocalMember = new Member(remoteMember.getAddress(), remoteMember.getHeartbeat(), Client.this, t_cleanup);
                                    Client.this.memberList.add(newLocalMember);
                                    newLocalMember.startTimeoutTimer();
                                } // else ignore
                            } else {
                                // brand spanking new member - welcome
                                Member newLocalMember = new Member(remoteMember.getAddress(), remoteMember.getHeartbeat(), Client.this, t_cleanup);
                                Client.this.memberList.add(newLocalMember);
                                newLocalMember.startTimeoutTimer();
                            }
                        }
                    }
                }
            }
        }

        /**
         * Check if a newly received list as a new file version
         * REVIEW: for now is just printing if found, later add start download new file
         */
        private void checkNewVersionInList(ArrayList<Member> remoteList) {
            int myVersion = Client.this.me.getF_i().version;
            int receivedVersion;

            for( Member m : remoteList){
                receivedVersion = m.getF_i().version;
                if ( myVersion != receivedVersion){
                    System.out.println( "Version diferent from: " + m.getAddress() );
                    if( myVersion < receivedVersion ){
                        System.out.println( "MY VERSION IS SMALLER" );
                        System.out.println( "TODO: have co call the method to start updating my version" );
                    }else{
                        System.out.println( "MY VERSION IS BIGGER" );
                        System.out.println( "TODO: add this member to the list of Member to be notified");
                    }
                }
            }
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    // END OFF INNER CLASS >> AsychronousReceiver
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Starts the client.  Specifically, start the various cycles for this protocol.
     * Start the gossip thread and start the receiver thread.
     *
     * @throws InterruptedException
     */
    protected void start() throws InterruptedException {

        // Start all timers except for me
        for (Member member : memberList) {
            if (member != me) {
                member.startTimeoutTimer();
            }
        }

        // Start the two worker threads
        ExecutorService executor = Executors.newCachedThreadPool();
        //  The receiver thread is a passive player that handles
        //  merging incoming membership lists from other neighbors.
        executor.execute(new AsychronousReceiver());
        //  The gossiper thread is an active player that
        //  selects a neighbor to share its membership list
        executor.execute(new MembershipGossiper());

        // Potentially, you could kick off more threads here
        //  that could perform additional data synching



        // keep the main thread around
        while (true) {
            TimeUnit.SECONDS.sleep(10);
        }
    }


    public static void main(String[] args) throws InterruptedException, SocketException, UnknownHostException {

        Client client = new Client();
        client.start();
    }


    /**
     * All timers associated with a member will trigger this method when it goes
     * off.  The timer will go off if we have not heard from this member in
     * <code> t_cleanup </code> time.
     */
    @Override
    public void handleNotification(Notification notification, Object handback) {

        Member deadMember = (Member) notification.getUserData();

        System.out.println("Dead member detected: " + deadMember);

        synchronized (this.memberList) {
            this.memberList.remove(deadMember);
        }

        synchronized (this.deadList) {
            this.deadList.add(deadMember);
        }

    }
}
