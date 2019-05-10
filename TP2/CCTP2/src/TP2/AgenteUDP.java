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
import java.net.SocketTimeoutException;

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
    
    
    private int DESTPORT = 7777;
    private InetAddress DESTIP;
    private int MAX_BUFFER_SIZE = 512;
    
    public AgenteUDP() throws SocketException{
        this.userSocket = new DatagramSocket(0);
    }
    
    public AgenteUDP(int port) throws SocketException{
        this.userSocket = new DatagramSocket(port);
    }
    
    public int getDestPort(){
        return this.DESTPORT;
    }
    
    public void setDestPort(int destPort){
        this.DESTPORT = destPort;
    }
    
    public InetAddress getDestIp(){
        return this.DESTIP;
    }
    
    public void setDestIp(InetAddress destIp){
        this.DESTIP = destIp;
    }
    
    public void setTimeOut(int time) throws SocketException{
        this.userSocket.setSoTimeout(time);
    }
    /* 
    *   Função responsável pelo envio dos dados, sendo os dados tratados como 
    *   uma string para um certo endereço IP destino. Os dados são passados 
    *   de uma String para um array de bytes, e de seguida são passados para 
    *   dentro de um datagram packet, onde serão enviados para o destino 
    *   IP através de uma porta pré definida para o envio (7777). 
    *   @param data Dados a ser transferidos na forma de uma String. 
    *   @param address Endereço IP destino. 
    *   @return void.
    */
    public void send(String data, InetAddress address) throws IOException{
        
        byte[] buffer = data.getBytes();
        
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, DESTPORT);
        
        userSocket.send(packet);
    }
    
    
    /* 
    *   Função responsável pelo envio de dados, onde a única diferença para a 
    *   função anterior se concentra na ausência de um endereço IP destino 
    *   passado como argumento na função. 
    *   @param data Dados a ser transferidos na forma de uma String. 
    *   @return void. 
    */
    public void send(String data) throws IOException{
       
        byte[] buffer = data.getBytes();
        
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, DESTIP, DESTPORT);
        
        userSocket.send(packet);
    }
    
    /* 
    *   Função responsável pela rececção dos pacotes de dados, onde é aberto 
    *   um array de bytes com um tamanho pré definido, e um datagrampacket 
    *   de seguida, o pacote é recebido, e é retirado o endereço IP do pacote
    *   sendo de seguida processado e retornado o próprio pacote de dados. 
    *   @return Packet Pacote processado.
    */
    public Packet receive() throws IOException{
        byte[] buffer = new byte[MAX_BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        try{
            userSocket.receive(packet);
        }
        catch(SocketTimeoutException ex){
            throw new SocketTimeoutException();
        }
      
        DESTPORT = packet.getPort();
        
        InetSocketAddress ip = (InetSocketAddress) packet.getSocketAddress();
        DESTIP = ip.getAddress();
        
        Packet p = Packet.valueOf(new String(packet.getData()));
      
        return p;
    }
}