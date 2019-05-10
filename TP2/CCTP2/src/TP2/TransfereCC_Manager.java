/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.io.IOException;
import java.net.SocketException;

/**
 *
 * @author joaon
 */ 

  public class TransfereCC_Manager extends Thread{
 
  private AgenteUDP agente_request;
  
  public TransfereCC_Manager(){}
        
        /* 
        *   Runnable interface, de modo a tirar vantagem das threads, é aberto um 
        *   port no 7777 por pré definição, existindo um ciclo infrinito, onde 
        *   são recebidos os pacotes de dados, e caso o pacote de dados recebido 
        *   não possua uma fin flag a true, caso o apcote seja para escrita, 
        *   é retirado o nome do ficheiro do pacote, sendo este de seguida 
        *   processado e passado para o mecanismo do GET. Caso não seja para escrita
        *   é retirado novamente o nome do ficheiro, é é passado para o mecanismo PUT. 
        *   @return void.
        */
        public void run(){

            try {
                    this.agente_request = new AgenteUDP(7777);
                } catch (SocketException ex) {
                    System.err.println(ex.getMessage());
                    return;
                }
            
            while(true){
                Packet receive_datapacket;

                try {
                    receive_datapacket = this.agente_request.receive();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    return;
                }
                
                
                if(!receive_datapacket.isFinFlag()){
                    String filename = new String(receive_datapacket.getData());
                    filename = filename.replaceAll("\0", "");
                    TransfereCC_ThreadRequest tr = new TransfereCC_ThreadRequest(
                                                   this.agente_request.getDestPort(),
                                                   this.agente_request.getDestIp(),
                                                   filename,
                                                   receive_datapacket.isWRFlag(),
                                                   receive_datapacket.getSyncNum());
                    
                    tr.start();
                }
            
        }
    }
    
  }
