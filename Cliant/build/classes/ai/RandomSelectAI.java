/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai;

import blokusElements.Board;
import blokusElements.Game;
import blokusElements.Piece;
import gui.MessageRecevable;
import java.awt.Point;
import java.util.ArrayList;
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
    private static final String AINAME = "RandomAI";

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
        this.state = Game.STATE_WAIT_PLAYER_CONNECTION;
        this.usedPeices = new ArrayList<String>();
        this.havingPeices = new ArrayList<String>();
        for(String pcid:Piece.PieceIDList){
            this.havingPeices.add(pcid);
        }
    }

    public int[][] recevedData;
    public int receveline;
    
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
            Random rd = new Random(System.currentTimeMillis());
            String[] ids = canPutList.keySet().toArray(new String[0]);
            String pid = ids[rd.nextInt(ids.length)];
            String[] pdata = pid.split("-");
            Piece putPiece = new Piece(pdata[0],Integer.parseInt(pdata[1]));
            ArrayList<Point> points = canPutList.get(pid);
            Point putPlace = points.get(rd.nextInt(points.size()));

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
