/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai;

import blokusElements.Game;
import gui.ClientGUI;
import gui.MessageRecevable;
import network.ServerConnecter;

/**
 * このクラスは抽象クラスです。
 * 継承して機能を実装しないと使うことができません。
 * @author koji
 */
public abstract class BlokusAI implements MessageRecevable{
    protected Game gameBoard;
    protected boolean isThinking;
  
    public BlokusAI(Game game){
        this.gameBoard = game;
    }
    public abstract void getNewMessage(String message);

    public abstract void setConnecter(ServerConnecter connecter);

    public void thinkStart(){
        this.isThinking = true;
    }

    public void stopThinking(){
        this.isThinking = false;
    }
    
    public abstract void setOutputInterface(MessageRecevable mr);
    
}
