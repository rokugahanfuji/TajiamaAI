/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blokusElements;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 盤面を管理するクラス
 * intの配列で状態を取得可能で,
 * -1:誰もおいていない
 * 0:プレイヤー0の設置ピース　●
 * 1:プレイヤー1の設置ピース　▲
 * とする。
 * ピース配置のメソッドを持つ
 * @author ktajima
 */
public class Board {
    public static final int BOARDSIZE = 15;
    public static final int PLAYER_COUNT = 2;
    public static final Point PLAYER_0_START = new Point(0,0);
    public static final Point PLAYER_1_START = new Point(14,14);
    
    private int[][] boardState;
    //各プレイヤーが設置済みのピース（同じ物は置けない）
    private ArrayList<ArrayList<String>> PlayerPutPieces;
    
    public Board(){
        init();
    }
    
    private void init(){
        //ボードの初期化
        this.boardState = new int[BOARDSIZE][BOARDSIZE];
        for(int i=0;i<BOARDSIZE;i++){
            for(int j=0;j<BOARDSIZE;j++){
                this.boardState[i][j] = -1;
            }
        }

        //プレイヤーピースの初期化
        this.PlayerPutPieces = new ArrayList<ArrayList<String>>();
        for(int p=0;p<PLAYER_COUNT;p++){
            this.PlayerPutPieces.add(new ArrayList<String>());
        }
    }
    
    /** ピース設置可能かの判定
     * 第1引数:player プレイヤー番号0または1
     * 第2引数:pice 設置するピース
     * 第3引数:x ピースを設置する場所のX座標（ピースの左上座標）
     * 第4引数:y ピースを設置する場所のY座標（ピースの左上座標）
     * 
     * 戻り値は接していた角の個数（設置不能時は0）
     */
    public int canPutPiece(int player,Piece piece,int x,int y){
        if(player > 1){
            //プレイヤー名が不正
            return 0;
        }
        
        //ピースが盤面から出ないことを確認
        int[][] pieceshape = piece.getPiecePattern();
        int width = pieceshape[0].length;
        int height = pieceshape.length;
        if(x+width > BOARDSIZE) { return 0;}
        if(y+height > BOARDSIZE) { return 0;}

        
        ArrayList<String> list = this.PlayerPutPieces.get(player);
        //初手の場合は特別処理
        if(list.isEmpty()){
            Point startPoint = null;
            if(player == 0){
                startPoint = PLAYER_0_START;
            } else if(player == 1){
                startPoint = PLAYER_1_START;
            } else {
                //プレイヤー名が不正（既にチェック済みのため、ここにはたどり着かない）
                return 0;
            }
            
            //ピースの1部がスタート位置に重なるかを確認
            for(int i=0;i<width;i++){
                for(int j=0;j<height;j++){
                    if(pieceshape[j][i] == 1){
                        if(startPoint.x == x+i && startPoint.y == y+j){
                            //スタート位置に乗っていたらOK。この場合角１つに接しているのと同じ扱い
                            return 1;
                        }
                    }
                }
            }
            return 0;
        }
        //↑↑↑↑初手に対する特別処理はここまで↑↑↑↑
        
        //↓↓↓↓初手ではない場合の処理↓↓↓↓
        
        //一度置いたピースは置けない
        if(list.contains(piece.getPieceID())){
            return 0;
        }
        
        //既存ピースに重なると置けない
        for(int i=0;i<width;i++){
            for(int j=0;j<height;j++){
                if(pieceshape[j][i] == 1){
                    if(this.boardState[y+j][x+i] != -1){
                        return 0;
                    }
                }
            }
        }
        
        //ボードを調べて接続可能場所と設置不可の場所を確認
        HashMap<String,ArrayList<Point>> plist = getConnectablePosition(player);
        ArrayList<Point> OKList = plist.get("OK");
        ArrayList<Point> NGList = plist.get("NG");
        
        //設置不能な場所に１か所でも乗っていたら置けない
        for(int i=0;i<width;i++){
            for(int j=0;j<height;j++){
                if(pieceshape[j][i] == 1){
                    for(Point ngp:NGList){
                        if(ngp.x == x+i && ngp.y == y+j){
                            return 0;
                        }
                    }
                }
            }
        }
        
        //つなげて良い場所に１か所でものっていたらOK
        //ここまでのプログラムで他の設置不可能な条件は網羅しているので、これでOKならば大丈夫
        int scount = 0;
        for(int i=0;i<width;i++){
            for(int j=0;j<height;j++){
                if(pieceshape[j][i] == 1){
                    for(Point okp:OKList){
                        if(okp.x == x+i && okp.y == y+j){
                            scount++;
                        }
                    }
                }
            }
        }
        
        //設置不可能な場所につながっていなかったら0、つながっている接点の数が返る
        return scount;
    }
    
    /** ピースを接続できる場所と置くことができない場所をまとめて取得するメソッド */
    private HashMap<String,ArrayList<Point>> getConnectablePosition(int player){
        ArrayList<Point> OKList = new ArrayList<Point>();
        ArrayList<Point> NGList = new ArrayList<Point>();
        HashMap<String,ArrayList<Point>> plist = new HashMap<String,ArrayList<Point>>();
        //盤面を調べてそのプレイヤーがピースをつなげられる場所と設置不能な場所を獲得
        for(int y=0;y<BOARDSIZE;y++){
            for(int x=0;x<BOARDSIZE;x++){
                if(boardState[y][x] == player){
                    //該当の場所の上の行を確認
                    if(y>0){
                        if(x>0){
                            //左上なので既にNGでなければ接続できる
                            if(!NGList.contains(new Point(x-1,y-1))){
                                OKList.add(new Point(x-1,y-1));
                            }
                        }
                        Point up = new Point(x,y-1);
                        if(!NGList.contains(up)){
                            NGList.add(up);
                            if(OKList.contains(up)){
                                OKList.remove(up);
                            }
                        }
                        if(x<BOARDSIZE-1){
                            //右上も既にNGでなければ接続できる
                            if(!NGList.contains(new Point(x+1,y-1))){
                                OKList.add(new Point(x+1,y-1));
                            }
                        }
                    }
                    //該当場所の左を確認
                    if(x>0){
                        //自分のピースの左はNG
                        Point lp = new Point(x-1,y);
                        if(!NGList.contains(lp)){
                            NGList.add(lp);
                            if(OKList.contains(lp)){
                                OKList.remove(lp);
                            }
                        }
                    }
                    //自分自身の場所もNG
                    Point cp = new Point(x,y);
                    if(!NGList.contains(cp)){
                        NGList.add(cp);
                        if(OKList.contains(cp)){
                            OKList.remove(cp);
                        }
                    }
                    //該当場所の右を確認
                    if(x<BOARDSIZE-1){
                        //自分のピースの右はNG
                        Point rp = new Point(x+1,y);
                        if(!NGList.contains(rp)){
                            NGList.add(rp);
                            if(OKList.contains(rp)){
                                OKList.remove(rp);
                            }
                        }
                    }
                    //該当場所の下の行を確認
                    if(y<BOARDSIZE-1){
                        if(x>0){
                            //左下なので既にNGでなければ接続できる
                            if(!NGList.contains(new Point(x-1,y+1))){
                                OKList.add(new Point(x-1,y+1));
                            }
                        }
                        //自分のピースの真下はNG
                        Point dp = new Point(x,y+1);
                        if(!NGList.contains(dp)){
                            NGList.add(dp);
                            if(OKList.contains(dp)){
                                OKList.remove(dp);
                            }
                        }
                        if(x<BOARDSIZE-1){
                            //右下も既にNGでなければ接続できる
                            if(!NGList.contains(new Point(x+1,y+1))){
                                OKList.add(new Point(x+1,y+1));
                            }
                        }
                    }
                    
                }
            }
        }
        
        plist.put("OK", OKList);
        plist.put("NG", NGList);
        return plist;
    }
    
    /** ピースを設置するメソッド
     * 第1引数:player プレイヤー番号0または1
     * 第2引数:pice 設置するピース
     * 第3引数:x ピースを設置する場所のX座標（ピースの左上座標）
     * 第4引数:y ピースを設置する場所のY座標（ピースの左上座標）
     * 
     * 戻り値は接していた角の個数（設置不能時は0）
     */
    public int putPiece(int player,Piece piece,int x,int y){
        int scount = this.canPutPiece(player, piece, x, y);
        if(scount != 0){
            int[][] pieceshape = piece.getPiecePattern();
            int width = pieceshape[0].length;
            int height = pieceshape.length;

            try{
                //ボード状態を書き換える
                for(int i=0;i<width;i++){
                    for(int j=0;j<height;j++){
                        if(pieceshape[j][i] == 1){
                            this.boardState[y+j][x+i] = player;
                        }
                    }
                }

                //設置済みピースリストを更新する
                ArrayList<String> list = this.PlayerPutPieces.get(player);
                list.add(piece.getPieceID());
                this.PlayerPutPieces.set(player, list);
                
            } catch(java.lang.ArrayIndexOutOfBoundsException e){
                //e.printStackTrace();
                return 0;
            }
            
            return scount;
        }
        return 0;
    }
            
    public int[][] getBoardMatrix() {
        return this.boardState;
    }
    
    
    public void printCurrentBoard(){
        for(int y=0;y<BOARDSIZE;y++){
            for(int x=0;x<BOARDSIZE;x++){
                switch(this.boardState[y][x]){
                    case -1: 
                        System.out.print("□");
                        break;
                    case 0:
                        System.out.print("●");
                        break;
                    case 1:
                        System.out.print("▲");
                        break;
                    default:
                        System.out.print("？");
                }
            }
            System.out.println();
        }
        System.out.println();
    }


    
}
