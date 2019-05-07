/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 *
 * @author joaon
 */ 

  public class TransfereCC_Manager extends Thread{
 
  private static int tam_buf = 200;
      
  private int SYNC_NUM = 0; 
  private int SYNC_NUM_PROX =  0;
  private int ACK_NUM = 0;
  private int WINDOW_SIZE = 0;
  private int INITIAL_SEGMENT = 0;
  private List<DatagramPacket> BUFFER = new ArrayList<DatagramPacket>(); 

  private long check = 0;
  private int MAX_BUFFER_SIZE = 512;
  private AgenteUDP agente_request;
  private byte[] buffer = new byte[MAX_BUFFER_SIZE];
   
   
   
   public TransfereCC_Manager(){}
        
        
        /* 
        *   Função responsável pelo mecanismo do GET na perspetiva do destino, onde é aberto um escritor 
        *   de ficheiro para um path pré definido, sendo criadas as pastas caso ainda não as possua 
        *   criadas. De seguida, num loop infinito, são recebidos os pacotes, sendo calculado agora o checksum 
        *   dos dados recebidos, caso o checksum calculado aqui corresponda com o do pacote, os dados 
        *   são retirados do pacote recebidos, e são escritos no ficheiro desde a posição zero, até 
        *   ao próprio tamanho dos dados. De seguida é enviado novamente um ACK de confirmação. 
        *   @param filename Nome do ficheiro. 
        *   @return void. 
        */
        public void downloadControl(String filename) throws FileNotFoundException, IOException{
            
            long check = 0;
            
            Shared_Functions sf = new Shared_Functions();
            
            String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "CCTP2" + File.separator + "Fich" + File.separator + "Output";
            File f = new File(path);
            if(!f.exists())f.mkdirs();
            
            File f2 = new File(path+File.separator+filename);
            System.out.println(f2.getPath());
            
            
            
            if (f2.exists()) { System.out.println("O ficheiro já existe!"); }
            else{
                System.out.println("O ficheiro está a ser criado!");
                boolean createNewFile = f2.createNewFile();
                System.out.println("O ficheiro foi criado = " + createNewFile);
            }
          
            System.out.println(f2.getPath());
            System.out.println("Posso ler? " + f.canRead());
            RandomAccessFile downloadFile = new RandomAccessFile(f2 , "rw");
            System.out.println("Foi aqui acima que fodeu");

            Packet received;
            Packet ack = new Packet();

            while(true){
                received = agente_request.receive();
                ack = new Packet();

                //BUFFER.add(received);
                               
                //downloadFile.writeChars(new String(conv.getData()).replaceAll("\0", "")); 
                
                // calcula o checksum do pacote recebido após o put           
                check = sf.CRC32checksumByteArray(received.getData());  
                
                if (received.isFinFlag()) break;
                
                if (received.getChecksum() == check){
                    
                    // retira o array de bytes do pacote que está a ler, começa na posição 0 e vai até ao length
                    downloadFile.write(received.getData(),0,received.getLengthData());                    
                    
                    // Calcular o ACK e meter o próprio ACK no pacote
                    ACK_NUM += received.getData().length; 
                    ack.setAckNum(ACK_NUM);     
                    
                    // Enviar o pacote ACK
                    agente_request.send(ack.toString());
                    
                    System.out.println("ACK NO ENVIO " + ack.getAckNum());
                } 
                else { 
                    // Mecanismos de retransmissão 
                    ack.setAckNum(ACK_NUM); 
                    agente_request.send(ack.toString()); 
                    System.out.println("Reenvio do pacote!");
                }
            
            }
            
            downloadFile.close();
            
         }
        
        /* 
        *   Responsável pelo PUT na perspetiva da origem, ou seja, é novamente aberto um escritor 
        *   de ficheiros num path pré definido, sendo calculado o tamanho do ficheiro, caso 
        *   o tamanho seja maior que zero, os dados são lidos e passados para um array de bytes 
        *   sendo este array de bytes depois passado para dentro do pacote a enviar, sendo calculado 
        *   também o payload (length) dos dados, a ser adicionados no pacote também, bem como o chekcsum 
        *   do array de bytes correspondente aos dados, onde tal como nos outros, é adicionado também 
        *   ao pacote de dados a enviar. por fim o pacote é enviado. No final de todo este processo, 
        *   é enviado um FIN com um mecanismo semelhante à função sendFin. 
        *   @param filename Nome do ficheiro. 
        *   @return void.
        */
        public void uploadControl(String filename) throws FileNotFoundException, IOException{
                
        try{
             String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "CCTP2" + File.separator + "Fich" + File.separator + "Input"; 
             
            RandomAccessFile uploadFile = new RandomAccessFile(path + File.separator + filename, "r");
            
            System.out.println("Já abri o descritor de ficheiro ");
            long n = 0;
            Packet sendPacket = new Packet();
            long size = uploadFile.getChannel().size();
            int tam = 0;
            long check = 0; 
            Packet ack = new Packet();
            
            Shared_Functions sf = new Shared_Functions();
            
            while(size > 0){
                byte[] sendData;
                
                if(size > tam_buf) {sendData = new byte[tam_buf]; n = uploadFile.read(sendData, 0, tam_buf);}
                else {
                    sendData = new byte[(int) size]; 
                    n = uploadFile.read(sendData, 0, (int) size);
                }
        
                
                size = size - n;
                sendPacket.setData(sendData);
                
                tam = sendPacket.getData().length; 
                sendPacket.setLengthData(tam); 
                
                check = sf.CRC32checksumByteArray(sendPacket.getData()); 
                System.out.println("CHECK UPLOAD " + check);
                
                sendPacket.setChecksum(check); 
                System.out.println("Pacote possui check: " + sendPacket.getChecksum());
               
                SYNC_NUM_PROX += tam; 
                
                do{ 
                    ack = new Packet();
                    
                    this.agente_request.send(sendPacket.toString()); 
                    ack = agente_request.receive();
                    System.out.println("O ACK NUM NO ENVIO: " + ack.getAckNum() + " sync num :  " + SYNC_NUM_PROX); 
                    
                    System.out.println(new String(sendPacket.getData()));
                }while (ack.getAckNum() != SYNC_NUM_PROX);
               
                SYNC_NUM = SYNC_NUM_PROX;
               
                
                /*
                ack = agente_request.receive();
                System.out.println("O ACK NUM NO ENVIO00000: " + ack.getAckNum() + "sync num :  " + SYNC_NUM_PROX);
                
                if (ack.getAckNum() == SYNC_NUM_PROX){                    
                    SYNC_NUM = SYNC_NUM_PROX;           // recebeu direito 
                }else { 
                    //this.agente_request.send(sendPacket.toString()); // repete o último pacote enviado 
                }
                */
            }
           
            uploadFile.close();
            
            sendPacket.setFinFlag(true);
            sendPacket.setLengthData(4);
            sendPacket.setData("null".getBytes());
            System.out.println("SEND PACKET FINAL: " + sendPacket.isFinFlag());
            this.agente_request.send(sendPacket.toString()); 
           
        }
        catch( IOException e ){
            e.printStackTrace();
            return;
        }
        
        System.out.println("Ja li");
    }
        
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
                    this.agente_request = new AgenteUDP(7778);
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
                    if(receive_datapacket.isWRFlag()){

                        String filename = new String(receive_datapacket.getData());
                        filename = filename.replaceAll("\0", ""); 
                        System.out.println(filename);

                        try {
                            System.out.println("Download " + filename);
                            downloadControl(filename);
                        } catch (IOException ex) {
                           System.err.println(ex.getMessage());
                        }                              
                    }
                    else{
                        String filename = new String(receive_datapacket.getData());
                        try {
                            System.out.println("Upload |" + filename + "|");

                            uploadControl(filename);
                        } catch (IOException ex) {
                            System.err.println(ex.getMessage());
                        }
                    }
            }
            
        }
    }
    
  }
