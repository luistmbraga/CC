/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

/**
 *
 * @author luisb
 */
public class FicheiroInfo {
    
    // IP com o destino
    private String origem;
    // IP com a origem
    private String destino;
    // porta de origem
    private int portOrigem;
    // porta de destino
    private int portDestino;
    
    public FicheiroInfo(String origem, String destino, int portOrigem,
            int portDestino){
        
        this.origem = origem;
        this.destino = destino;
        this.portOrigem = portOrigem;
        this.portDestino = portDestino;
    }
    
    public FicheiroInfo(FicheiroInfo info){
        this.origem = info.getOrigem();
        this.destino = info.getDestino();
        this.portOrigem = info.getPortOrigem();
        this.portDestino = info.getPortDestino();
    }
    
    public String getOrigem(){
        return this.origem;
    }
    
    public String getDestino(){
        return this.destino;
    }
    
    public int getPortOrigem(){
        return this.portOrigem;
    }
    
    public int getPortDestino(){
        return this.portDestino;
    }
    
    public void setOrigem(String origem){
        this.origem = origem;
    }
    
    public void setDestino(String destino){
        this.destino = destino;
    }
    
    public void setPortOrigem(int portOrigem){
        this.portOrigem = portOrigem;
    }
    
    public void setPortDestino(int portDestino){
        this.portDestino = portDestino;
    }
    
    public FicheiroInfo clone(){
        return new FicheiroInfo(this);
    }
    
}
