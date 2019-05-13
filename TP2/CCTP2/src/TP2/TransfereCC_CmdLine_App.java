/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author luisb
 */
public class TransfereCC_CmdLine_App {
    
    private static void imprimeHelp(){
        System.out.println(
                  "************************* HELP *************************\n"
                + "*                                                      *\n"
                + "*    SYNOPSIS:                                         *\n"
                + "*             ccget [OPTIONS] <filename> <IPDEST>      *\n"
                + "*             ccput <filename> <IPDEST>                *\n"
                + "*                                                      *\n"
                + "*    COMMANDS:                                         *\n"
                + "*             ccget - used to download a file          *\n"
                + "*             ccput - used to upload a file            *\n" 
                + "*                                                      *\n"
                + "*    OPTIONS:                                          *\n"
                + "*             -name <localfilename>                    *\n"
                + "*                   This option is used to define the  *\n"
                + "*                 local file name if used on ccget     *\n"
                + "*                 command.                             *\n"
                + "*                                                      *\n"
                + "*        In <filename> should be specified the name    *\n"
                + "*    of the file that you want to download or          *\n"
                + "*    upload.                                           *\n"
                + "*        In <localfilename> should be specified the    *\n"
                + "*    local name of the file to download.               *\n"
                + "*                                                      *\n"
                + "********************************************************");
    }
    
    
    public static void main(String args[]){
        
        File inputFile = new File(System.getProperty("user.home") + File.separator + "Files" + File.separator + "Input" + File.separator);
        inputFile.mkdirs();
        File outputFile = new File(System.getProperty("user.home") + File.separator + "Files" + File.separator + "Output" + File.separator);
        outputFile.mkdirs();
        
        // buffer para processar comandos dados pelo cliente
        BufferedReader inFromUser =
            new BufferedReader(new InputStreamReader(System.in));
        
        // string que irá conter o comando escrito
        String sentence = null;
        
        TransfereCC trans;
        try {
            trans = new TransfereCC();
        } catch (SocketException ex) {
            System.err.println("Erro ao iniciar o TransfereCC" + ex.getMessage());
            return;
        }
        
        String[] argumentos;
        
        do{
            // definição da interface
            System.out.print("TransfereCC > ");
            
            try { // ler o que o utilizador escreveu
                sentence = inFromUser.readLine(); 
            } catch (IOException ex) {
                System.out.println("Ocorreu um erro na leitura do comando.");
            }
            
            // separa as várias partes do argumento em strings diferentes
            argumentos = sentence.split(" ");

            /*
            Inicio da interpretacao dos comandos
            */
            
            if(argumentos[0].equals("exit")) continue;
            
            // menu de ajuda
            if((argumentos[0].equals("h") || argumentos[0].equals("help"))){
                imprimeHelp();
            }
            else{
                
                int n = argumentos.length;
                
                if(argumentos[0].equals("ccget") || argumentos[0].equals("ccput") && (n == 3 || n == 5)){
                
                    InetAddress dest;

                    try {
                        
                        if(n == 3){
                            dest = InetAddress.getByName(argumentos[2]);
                            trans.iniciaTransferencia(argumentos[0], argumentos[1], dest);
                        }else{
                            if(n == 5 && argumentos[1].equals("-name") && argumentos[0].equals("ccget")){
                                dest = InetAddress.getByName(argumentos[4]);
                                trans.iniciaTransferencia(argumentos[0], argumentos[3],argumentos[2], dest);
                            }else System.err.println("Comando não reconhecido. Comando h ou help para mais informações.");
                        }
                    
                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                    }
                }
                else System.err.println("Comando não reconhecido. Comando h ou help para mais informações.");
            }
           
            /*
            Fim da interpretacao dos comandos
            */
                
            // se se escrever exit sairemos do aplicação
        }while(!argumentos[0].equals("exit"));
        
    }
}
