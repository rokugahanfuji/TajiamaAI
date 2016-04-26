/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai;

import blokusElements.Board;
import blokusElements.BoardSub;
import blokusElements.Game;
import blokusElements.Piece;
import gui.MessageRecevable;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import network.ServerConnecter;

/**
 *
 * @author koji
 */
public class RandomSelectAI extends BlokusAI{
    private static final String AINAME = "HAYASHI MASAYA";

    //自分自身の処理状態
    private int state;
    //サーバとの接続を保持するクラス
    private ServerConnecter connecter;
    //自分に割り当てられたID（接続前は-1）
    private int myPlayerID = -1;
    //自分が利用したピース一覧
    private ArrayList<String> usedPeices;
    //自分がまだ使えるピース一覧
    private ArrayList<String> havingPeices;
    //結果の出力先など
    private MessageRecevable userInterface;
    //このAIの名前
    
    public RandomSelectAI(Game game){
        super(game);
        this.TurnCount = 0;
        this.state = Game.STATE_WAIT_PLAYER_CONNECTION;
        this.usedPeices = new ArrayList<String>();
        this.havingPeices = new ArrayList<String>();
        for(String pcid:Piece.PieceIDList){
            this.havingPeices.add(pcid);
        }
    }

    public int[][] recevedData;
    public int receveline;
    private int TurnCount;
    
    //メッセージ解析用の正規表現パターン
    private Pattern PLAYEDMSGPTN = Pattern.compile("401 PLAYED ([0-1]) (1?[0-9]) (1?[0-9]) ([0-5][0-9A-F])-([0-8])");
    private Pattern PASSEDMSGPTN = Pattern.compile("402 PASSED ([0-1])");

    /** このプログラムがサーバからメッセージを受信すると呼び出される */
    @Override
    public void getNewMessage(String message) {
        if(this.state == Game.STATE_WAIT_PLAYER_CONNECTION){
            if(message.toUpperCase().equals("100 HELLO")){
                //名前登録時の処理
                String sendmessage = "101 NAME "+AINAME;
                this.connecter.sendMessage(sendmessage);
                this.userInterface.addMessage(sendmessage);
            } else if(message.toUpperCase().equals("102 PLYERID 0")){
                //先手の場合
                this.myPlayerID = 0;
                super.gameBoard.setPlayerName(0,"RandomAI");
                super.gameBoard.setPlayerName(1,"opponent");
                this.state = Game.STATE_WAIT_PLAYER_PLAY;
            } else if(message.toUpperCase().equals("102 PLYERID 1")){
                //後手の場合
                this.myPlayerID = 1;
                super.gameBoard.setPlayerName(1,"RandomAI");
                super.gameBoard.setPlayerName(0,"opponent");
                this.state = Game.STATE_WAIT_PLAYER_PLAY;
            }
        } else if(this.state == Game.STATE_WAIT_PLAYER_PLAY){
            Matcher mc = PLAYEDMSGPTN.matcher(message);
            Matcher mc2 = PASSEDMSGPTN.matcher(message);
            if(mc.matches()){//401 PLAYED
                int pid = Integer.parseInt(mc.group(1));
                int x = Integer.parseInt(mc.group(2));
                int y = Integer.parseInt(mc.group(3));
                String PieceID = mc.group(4);
                int PieceDirection = Integer.parseInt(mc.group(5));
                this.gameBoard.play(pid, new Piece(PieceID,PieceDirection) , x, y);
            } else if(mc2.matches()){//402 PASSED [01]
                int pid = Integer.parseInt(mc2.group(1));
                this.gameBoard.pass(pid);
            } else if(message.toUpperCase().equals("404 DOPLAY")){
                
                // おくことができるピースとその場所を確認（総当りで探します）
                HashMap<String,ArrayList<Point>> canPutList = getCanPutList();
                
                // サーバに送るメッセージを決定
                //このメソッドを修正すれば、最低限の自分なりのプログラムが作れます。
                String sendmessage = SelectPutPiece(canPutList);
                this.connecter.sendMessage(sendmessage);
                this.userInterface.addMessage(sendmessage);
            }            
        }
    }

    /** 配置可能なピースのIDとその場所の一覧を取得する */
    private HashMap<String,ArrayList<Point>> getCanPutList(){
        HashMap<String,ArrayList<Point>> canPutList = new HashMap<String,ArrayList<Point>>();
        for(String id:this.havingPeices){
            for(int d=0;d<8;d++){
                String fullID = id+"-"+d;
                ArrayList<Point> putlist = new ArrayList<Point>();
                for(int x=0;x<Board.BOARDSIZE;x++){
                    for(int y=0;y<Board.BOARDSIZE;y++){
                        if(this.gameBoard.getBoard().canPutPiece(this.myPlayerID, new Piece(id,d), x, y) > 0){
                            putlist.add(new Point(x,y));
                        }
                    }
                }
                if(putlist.size() > 0){
                    canPutList.put(fullID, putlist);
                }
            }
        }
        return canPutList;
    }
    
    /** ピースの配置を考えるメソッド */
    private String SelectPutPiece(HashMap<String,ArrayList<Point>> canPutList){
        //TODO 最低限、ここを修正すれば自分なりのプログラムが作れます。
        //乱数をつかって１つ置く場所を決める
        String message;
        if(canPutList.keySet().size() > 0){
            String pid = "";
            String[] pdata;
            pdata = new String[2];
            String[] ids;
            Piece putPiece;
            Point putPlace = new Point();
            ArrayList<Point> points;
            if(TurnCount < 3){
                switch(TurnCount){
                    case 0:
                        if(this.myPlayerID == 0){
                            pid = "5A-1";
                            putPlace.x = 0;
                            putPlace.y = 0;
                        } else {
                            pid = "5A-4";
                            putPlace.x = 12;
                            putPlace.y = 12;
                        }
                        break;
                    case 1:
                        if(this.myPlayerID == 0){
                            pid = "5B-0";
                            putPlace.x = 2;
                            putPlace.y = 2;
                        } else {
                            pid = "5B-0";
                            putPlace.x = 10;
                            putPlace.y = 10;
                        }
                        break;
                    case 2:
                        if(this.myPlayerID == 0){
                            pid = "57-0";
                            putPlace.x = 4;
                            putPlace.y = 4;
                        } else {
                            pid = "57-2";
                            putPlace.x = 8;
                            putPlace.y = 8;
                        }
                        break;      
                }
                pdata = pid.split("-");
                putPiece = new Piece(pdata[0],Integer.parseInt(pdata[1]));
                
                TurnCount++;
            }else{
                //canPutList -> Key:57-2 Value:Array(ピースが置ける位置)
                //rd  -> random
                //ids -> canPutListキーを配列
                //pid -> ピースID 例；"57-2" （初期ではランダム）
                //pdata[0] -> "57" ピース形 
                //pdata[1] -> "2"  回転  
                //putPiece  pdataをもとに作成したPiece型
                //points -> pidをもとにcanPutList
                Random rd = new Random(System.currentTimeMillis());
                ids = canPutList.keySet().toArray(new String[0]);
                pid = ids[rd.nextInt(ids.length)];
                pdata = pid.split("-");
                putPiece = new Piece(pdata[0],Integer.parseInt(pdata[1]));
                points = canPutList.get(pid);
                putPlace = points.get(rd.nextInt(points.size()));
            }
            
            //手の評価を表示
            this.nextPutCount(this.myPlayerID,putPiece,putPlace.x,putPlace.y);
            BoardSub.ValidCornerTroutSet(this.myPlayerID, this.gameBoard.getBoardState());
            //手の評価リストを返却
            
            //自分のデータを更新し、サーバにもデータを送る
            this.gameBoard.play(this.myPlayerID, putPiece, putPlace.x, putPlace.y);
            this.usedPeices.add(pdata[0]);
            this.havingPeices.remove(pdata[0]);
            message = "405 PLAY "+putPlace.x+" "+putPlace.y+" "+pid;
        } else {
            //おく手がなければパス
            this.gameBoard.pass(this.myPlayerID);
            message = "406 PASS";
        }
        return message;
    }
    
    //その手を置いた後に、どれだけ置けるか
    private void nextPutCount(int playerID,Piece piece,int x,int y){
        int[][] nowBoard = this.gameBoard.getBoardState();
        int[][] shadowBoard = this.gameBoard.getBoardState();
        
        shadowBoard = BoardSub.putPiece(playerID,piece,x,y,shadowBoard);
        
        
        
        
        
        /*
        Hashmap
        for (String Key : canPutList.keySet()){
            for (Point Value : canPutList.get(Key)){
                int[][] shadowBoard = this.gameBoard.getBoardState();
                
                
                
                System.out.println(Value);
                
            }
            
        }
        */

        //System.out.println("手によって減る数；");
        //System.out.println("手によって増える数；");
    }
    
    
    @Override
    public void setConnecter(ServerConnecter c) {
        this.connecter = c;
    }

    @Override
    public void setOutputInterface(MessageRecevable mr) {
        this.userInterface = mr;
    }

    @Override
    public void reciveMessage(String text) {
        //メッセージを受信したときに呼び出される
        if(!super.isThinking){
            //思考モードでなければ処理を終了
            return;
        }
        this.getNewMessage(text);
    }

    @Override
    public void addMessage(String text) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
