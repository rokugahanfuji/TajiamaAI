/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blokusElements;

import java.util.Observable;

/**
 * ゲームの進行状況を管理するクラス
 * 手を打てるプレイヤーや得点を管理する
 * @author koji
 */
public class Game extends Observable{
    public static final int STATE_WAIT_PLAYER_CONNECTION = 0;
    public static final int STATE_WAIT_PLAYER_PLAY = 1;
    public static final int STATE_GAME_END = 2;
    
    private int[] Score;
    private int CurrentPlayer;
    private Board gameBoard;
    private boolean[] playAble;
    private String[] PlayerName;
    private int gameState;
      /** タイマー */
    private TimerThread timerThread;

    
    public Game(){
        this.init();
    }
    
    public int getGameState(){
        return this.gameState;
    }
    
    private void init(){
        this.Score = new int[2];
        this.Score[0] = 0;
        this.Score[1] = 0;
        
        this.playAble = new boolean[2];
        this.playAble[0] = true;
        this.playAble[1] = true;
        
        this.CurrentPlayer = 0;
        this.gameBoard = new Board();
        
        this.PlayerName = new String[2];
        this.PlayerName[0] = null;
        this.PlayerName[1] = null;
        
        this.gameState = STATE_WAIT_PLAYER_CONNECTION;
        
        this.timerThread = new TimerThread();
        
        this.setChanged();
        this.notifyObservers();
    }
    
    /** 手を打つメソッド  
     * 第1引数:player プレイヤー番号0または1
     * 第2引数:pice 設置するピース
     * 第3引数:x ピースを設置する場所のX座標（ピースの左上座標）
     * 第4引数:y ピースを設置する場所のY座標（ピースの左上座標）
     * 
     * 戻り値はプレイできたらtrue, できなかったらfalse
     */
    public boolean play(int player,Piece piece,int x,int y){
        if(this.gameState != STATE_WAIT_PLAYER_PLAY){
            return false;
        }
        if(this.CurrentPlayer == player){
            int putPiece = this.gameBoard.putPiece(player, piece, x, y);
            if(putPiece > 0){
                int point = piece.getPieceCount() * putPiece;
                this.Score[player] += point;
                this.changePlayer();
                this.setChanged();
                this.notifyObservers(this);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    
    /** パスした場合に呼び出される。投了になる */
    public boolean pass(int player){
        if(this.CurrentPlayer == player){
            this.playAble[player] = false;
            this.changePlayer();
            this.setChanged();
            this.notifyObservers();
            return true;
        } else {
            return false;
        }
    }
    
    private void changePlayer(){
        int pcount = 0;
        for(boolean p:this.playAble){
            if(p) { pcount++; }
        }
        if(pcount == 0){
            //誰もプレイできない場合
            this.CurrentPlayer = -1;
            this.gameState = STATE_GAME_END;
            return;
        }
        
        this.CurrentPlayer = (this.CurrentPlayer+1)%2;
        if(this.playAble[this.CurrentPlayer] != true){
            this.changePlayer();
        }
    }
    
    /** ボードの状態を取得 */
    public int[][] getBoardState(){
        return this.gameBoard.getBoardMatrix();
    }
    /** ボードそのもののメソッドを呼び出すための取得 */
    public Board getBoard(){
        return this.gameBoard;
    }
    
    public int setPlayerName(String name){
        if(this.gameState == STATE_WAIT_PLAYER_CONNECTION){
            if(this.PlayerName[0] == null){
                this.setPlayerName(0, name);
                return 0;
            } else if(this.PlayerName[1] == null){
                this.setPlayerName(1, name);
                return 1;
            }
        }
        return -1;
    }
    
    public void setPlayerName(int player,String name){
        if(player>=0 && player < 2){
            this.PlayerName[player] = name;
        }
        if(this.PlayerName[0] != null && this.PlayerName[1] != null){
            this.gameState = STATE_WAIT_PLAYER_PLAY;
        }
        this.setChanged();
        this.notifyObservers();
    }
    public String[] getPlayerName(){
        return this.PlayerName;
    }
    
    public int[] getScore(){
        return this.Score;
    }
    
    public int getCurrentPlayer(){
        return this.CurrentPlayer;
    }
    
    public void printMessage(String text){
        this.setChanged();
        this.notifyObservers(text);
    }
    
    /** 時間計測開始 */
    public void TimerStart(int PlayerID){
        this.timerThread.StartTimeCount(PlayerID);
    }
    /** 時間計測終了 */
    public void TimerStop(int PlayerID){
        this.timerThread.StopTimeCount(PlayerID);
    }
}
