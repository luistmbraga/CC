/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.InetSocketAddress;

/**
 * A classe AgenteUDP trata de toda a comunicação da aplicação,
 * mas não da fidelidade da mesma.
 * Um AgenteUDP é listener que escuta pedidos internos e externos.
 * 
 * @author luisb
 */
public class AgenteUDP{
    
    /**
     * Socket usado para comandos dados pelo utilizador.
     */
    private DatagramSocket userSocket;
    
    /**
     * Porta standard de envio. 
     *
     */
    private final int PORTA = 7777;
    
    private int DESTPORT;
    private InetAddress DESTIP;
    private int MAX_BUFFER_SIZE = 512;
    
    public AgenteUDP() throws SocketException{
        this.userSocket = new DatagramSocket(0);
    }
    
    public AgenteUDP(int port) throws SocketException{
        this.userSocket = new DatagramSocket(port);
    }
    
    public void send(String data, InetAddress address) throws IOException{
        
        byte[] buffer = data.getBytes();
        
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, PORTA);
        
        userSocket.send(packet);
    }
    
    public void send(String data) throws IOException{
       
        byte[] buffer = data.getBytes();
        
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, DESTIP, DESTPORT);
        
        userSocket.send(packet);
    }
    
    
    public Packet receive() throws IOException{
        byte[] buffer = new byte[MAX_BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                  
        userSocket.receive(packet);
                        
        DESTPORT = packet.getPort();
        
        InetSocketAddress ip = (InetSocketAddress) packet.getSocketAddress();
        DESTIP = ip.getAddress();
        
        System.out.println(packet.getLength());
        
        /*
        byte[] bufresult = new byte[packet.getLength()];
        DatagramPacket result = new DatagramPacket(bufresult, bufresult.length);
        result.setData(buffer, 0, bufresult.length);
        */ 
        
        Packet p = Packet.valueOf(new String(packet.getData()));
      
        return p;
    }
    
    
    
}

/*
public class AgenteUDP {

    // socket do cliente UDP
    private DatagramSocket socket;
    // porta usada pela aplicação
    private int port;
    // tabela de estado 
    // *******
    // tamanho do cabeçalho
    private int headerSize = 12; // bytes
    //tamanho da String flags
    private int flagSize = 16; // 2 bytes
    // tamanho maximo do payload
    private int MAX_PAYLOAD = 1460; // MSS (MAX SEGMENT SIZE)
    private int MTU = 1500; // 1460 + 20 + 8 + 12
    
    
    public AgenteUDP() throws SocketException{ 
        this.socket = new DatagramSocket(0);
        this.port = this.socket.getLocalPort();
        // lançar thread de listning
    }
    
    public AgenteUDP(int port) throws SocketException{
        this.port = port;
        this.socket = new DatagramSocket(this.port);
        // lançar thread de listening
    }
    
    public int getPort(){
        return this.port;
    }
    
    public void iniciaLigacao(String ipDestino, int op, String filename) throws UnknownHostException, IOException, SocketTimeoutException{
        
        byte[] b = filename.getBytes();
        int size = b.length;
        byte[] sendData = new byte[headerSize + size];
        byte[] receiveData = new byte[headerSize];
        DatagramPacket sendPacket;
        DatagramPacket receivePacket;
        
        
        switch(op){
            // 1 é GET
            case(1):
                packetLoader(sendData, 0, 0, "0000000100000000", Short.MAX_VALUE, size, b);
                break;
            // 2 é PUT
            case(2):
                packetLoader(sendData, 0, 0, "0000100100000000", Short.MAX_VALUE, size, b);
                break;
        }
        
        // prepara o packet para envio
        InetAddress IPAddress = InetAddress.getByName(ipDestino);
        sendPacket = 
                new DatagramPacket(sendData, sendData.length, IPAddress, this.port);
        
        // envia um SYN
        socket.send(sendPacket);
        
        receivePacket = 
                new DatagramPacket(receiveData, receiveData.length);
                
        
        // recebe o SYN + ACK
        socket.receive(receivePacket);
        
        
        // envia o ACK e o inicio de ligação é iniciado
        
    }
    
    public void sender() throws FileNotFoundException{
        RandomAccessFile f = new RandomAccessFile("C:/Users/luisb/Desktop/CC/TP2/Input/teste.txt", "r");
        int n = 0;
        
        
        while(n!=-1){
        n = f.read(sendData);
        
        if (n != -1){
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9999);
        this.socket.send(sendPacket);
        System.out.println("I am stuck " + n);
        } 
      }
    }
    
    public void receiver() throws SocketTimeoutException{
        
        
        
    }
    
    public void packetLoader(byte[] array, int ack, int seq, String flags, short windowSize, int size, byte[] b){
        
        // carrega o ack
        byte[] bytes = ByteBuffer.allocate(4).putInt(ack).array();

        System.arraycopy(bytes, 0, array, 0, 4);
        
        // carrega o ack
        bytes = ByteBuffer.allocate(4).putInt(seq).array();
        
        System.arraycopy(bytes, 0, array, 4, 4);
        
        // carrega as flags 
        bytes = ByteBuffer.allocate(2).putShort((short) Integer.parseInt(flags, 2)).array();
        
        System.arraycopy(bytes, 0, array, 8, 2);
        
        // carrega o windowSize
        bytes = ByteBuffer.allocate(2).putShort(windowSize).array();
        
        System.arraycopy(bytes, 0, array, 10, 2);
        
        // carrega dados no ficheiro
        System.arraycopy(b, 0, array, 12, size);
    }
    
}
*/