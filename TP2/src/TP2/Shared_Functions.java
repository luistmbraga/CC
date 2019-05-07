/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 *
 * @author joaon
 */
public class Shared_Functions {
    
     /*
    *   Estabelece a conecção inicial com um envio de um SYN, onde é criado um novo pacote com 
    *   este propósito, sendo que a flag que representa o SYN é posta a true, é também gerado 
    *   um número aleatória para o número de sequência, sendo este depois adicionado ao pacote, 
    *   onde é também passado dados, de seguida o pacote é enviado para o endereço IP destino. 
    *   @param filename Nome do ficheiro.
    *   @param isWrite Indica se está para escrita ou não. 
    *   @param dest Endereço ip destino. 
    *   @return void.
    */
    public int sendSyn(AgenteUDP agente, String filename, boolean isWrite, InetAddress dest) throws IOException{
        Packet syncPacket = new Packet();
        syncPacket.setSyncFlag(true);
        int SYNC_NUM = ThreadLocalRandom.current().nextInt(1,5000);
        syncPacket.setSyncNum(SYNC_NUM); 
        syncPacket.setLengthData(filename.getBytes().length);
        syncPacket.setData(filename.getBytes());
        
        if(isWrite) syncPacket.setWRFlag(true);
        
        agente.send(syncPacket.toString(), dest);
        
        return SYNC_NUM;
    } 
    
    /*
    *   Responsável por enviar o sinal do término de conecção, onde, de maneira análoga à 
    *   função anterior, é criado um novo pacote, onde é colocada a flag do FIN a true, 
    *   sendo também gerado um número de sequência (embora não sendo necessário), sendo depois
    *   colocados dados aleatórios, uma vez que não interessa o tipo de dados a ser tratados 
    *   neste término ou início de conecção. 
    *   @param dest Endereço IP destino. 
    *   @return void.
    */
    public void sendFin(AgenteUDP agente, InetAddress dest) throws IOException{ 
        Packet finPacket = new Packet(); 
        finPacket.setFinFlag(true); 
        finPacket.setLengthData(4);
        finPacket.setData("null".getBytes());
        
        agente.send(finPacket.toString(),dest); 
    }
    
        /* 
    *   Cálculo do CRC32 (cyclic redundacy check) de um array de bytes, de modo
    *   a poder efetuar a validação do PDU recebido no destino.
    *   1ºlugar -> Calcula o checksum dos dados na origem; 
    *   2ºlugar -> Adiciona o checksum calculado anteriormente ao 
    *   pacote de dados; 
    *   3ºlugar -> Calcula o checksum novamente no destino, e compara esse valor 
    *   com o valor que veio no pacote de dados.
    *   @param bytes Array de bytes sobre o qual irá ser calculado o checksum 
    *   @return long Resultado do checksum do array de bytes
    */
    public long CRC32checksumByteArray(byte[] bytes){ 
        
        Checksum checksum = new CRC32();
         
        // update do checksum com o array de bytes passado como argumento
        checksum.update(bytes, 0, bytes.length);
          
        // retorna o valor em long do checksum do array de bytes
        long checksumValue = checksum.getValue(); 
        
        return checksumValue;
    } 
    
    
    
}
