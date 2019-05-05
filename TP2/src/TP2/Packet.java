/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.util.Arrays; 
import java.util.Formatter;
import static sun.security.krb5.Confounder.bytes;


/**
 *
 * @author luisb
 */
public class Packet {
        private boolean syncFlag;          // flag que indica se o pacote a mandar é um SYN.
	private boolean ackFlag;           // flag que indica se o pacote a mandar é um ACK.
	private boolean finFlag;           // flag que indica se o pacote a mandar é um FIN.
        private boolean wrFlag;            // flag que indica caso o ficheiro esteja para escritga
	private int syncNum;               // número de sequência 
	private int ackNum;                // número do ACK.
	private int windowSize;            // tamanho da janela, de modo a saber quandos pacotes a mandar 
        private int length_data;           // tamanho dos dados
        private long checksum;             // checksum do pacote
	private byte[] data;               // array de bytes referente aos dados do pacote
                
	
	/*SYNCF, ACKF, FINF, WRF, SYNN, ACKN, WS, DATA*/
	public Packet(){
		this(false, false, false, false,0,0,0,4,0,"null".getBytes());
	}
	public Packet(boolean syncFlag, 
			boolean ackFlag, 
			boolean finFlag, 
                        boolean wrFlag,
			int synNum, 
			int ackNum, 
			int windowSize, 
                        int length_data, 
                        long checksum,
			byte[] data){
		
		this.syncFlag = syncFlag;
		this.ackFlag = ackFlag;
		this.finFlag = finFlag;
		this.wrFlag = wrFlag;
                
		this.syncNum = synNum;
		this.ackNum = ackNum;
		this.windowSize = windowSize; 
                this.length_data = length_data; 
                this.checksum = checksum;
		
		this.data = data;
	}

	public boolean isSyncFlag() {
		return syncFlag;
	}

	public void setSyncFlag(boolean sYN_FLAG) {
		syncFlag = sYN_FLAG;
	}

	public boolean isAckFlag() {
		return ackFlag;
	}

	public void setAckFlag(boolean aCK_FLAG) {
		ackFlag = aCK_FLAG;
	}

	public boolean isFinFlag() {
		return finFlag;
	}

	public void setFinFlag(boolean fIN_FLAG) {
		finFlag = fIN_FLAG;
	}

        public boolean isWRFlag(){
            return this.wrFlag;
        }
        
        public void setWRFlag(boolean f){
            this.wrFlag = f;
        }
        
	public int getSyncNum() {
		return syncNum;
	}

	public void setSyncNum(int sYN_NUM) {
		syncNum = sYN_NUM;
	}

	public int getAckNum() {
		return ackNum;
	}

	public void setAckNum(int aCK_NUM) {
		ackNum = aCK_NUM;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int wINDOW_SIZE) {
		windowSize = wINDOW_SIZE;
	}

        public int getLengthData(){ 
                return length_data;
        }
        
        public long getChecksum(){ 
                return checksum;
        }
        
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	} 
        
        public void setLengthData(int length){ 
                this.length_data = length;
        }  
        
        public void setChecksum(long check){ 
                this.checksum = check;
        }
           

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("SYNF=").append(syncFlag?1:0).append(";").
		append("SYNN=").append(syncNum).append(";").
		append("ACKF=").append(ackFlag?1:0).append(";").
		append("ACKN=").append(ackNum).append(";").
                append("WRFlag=").append(wrFlag?1:0).append(";").
		append("FINF=").append(finFlag?1:0).append(";").
		append("WINDOW=").append(windowSize).append(";"). 
                append("LENGTH DATA=").append(length_data).append(";"). 
                append("CHECKSUM=").append(checksum).append(";").
		append("DATA=").append(new String(data));
		
		return builder.toString();
	}
	
	public static Packet valueOf(String packetData){
		boolean syncFlag, ackFlag, finFlag;
		int synNum, ackNum, winSize;
		
		syncFlag=ackFlag=finFlag=false;
		synNum=ackNum=winSize=0;
		String data="";
		
		String[] attribs = packetData.split(";");
		Packet packet = new Packet();
		for(String token: attribs){
			String[] pair = token.split("=");
			switch(pair[0]){
				case "SYNF":
					packet.setSyncFlag(pair[1].equals("1"));
					break;
				case "ACKF":
					packet.setAckFlag(pair[1].equals("1"));
					break;
				case "FINF":
					packet.setFinFlag(pair[1].equals("1"));
					break;
				case "SYNN":
					packet.setSyncNum(Integer.parseInt(pair[1]));
					break;
				case "ACKN":
					packet.setAckNum(Integer.parseInt(pair[1]));
					break;
                                case "WRFlag":
                                        packet.setWRFlag(pair[1].equals("1"));
                                        break;
				case "WINDOW":
					packet.setWindowSize(Integer.parseInt(pair[1]));
					break; 
                                case "LENGTH DATA": 
                                        packet.setLengthData(Integer.parseInt(pair[1])); 
                                        break; 
                                case "CHECKSUM": 
                                        packet.setChecksum(Long.parseLong(pair[1]));                 
                                        break;
				case "DATA": 
                                        int length = packet.getLengthData(); 
                                        byte[] bytes; 
                                        bytes = Arrays.copyOf(pair[1].getBytes(),length);
                                        packet.setData(bytes);
					break;
			}
		}
		return packet;
	}
}
