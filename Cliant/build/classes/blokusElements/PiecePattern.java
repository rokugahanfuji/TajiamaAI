/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blokusElements;

import java.util.ArrayList;

/**
 * このクラスはピースの形状を表すために利用する
 * 基本形状を与えて初期化すると、回転および反転された８つの形状がすべて作られる
 * intの２重配列として値は受け渡し
 * -------------------------
 *| [0][0] | [0][1] | [0][2] |
 * ------------------------
 *| [1][0] | [1][1] | [1][2] |
 * --------------------------
 *| [2][0] | [2][1] | [2][2] |
 * --------------------------
 * のように次元を定める。ピース上のブロックがあるところは１、ないところは０で記述される。
 * 長方形のため、各配列サイズは回転で変わることがある。
 * 
 * @author ktajima
 */
public class PiecePattern {
    int[][] BasicShape;
    ArrayList<int[][]> Shapes;
    
    /** ピースの初期化形状を渡すと、回転・反転した形は勝手に作ってくれる */
    public PiecePattern(int[][] base){
        this.Shapes = new ArrayList<int[][]>();
        this.Shapes.add(base);

        this.BasicShape = base;
        //右回転
        int[][] r1 = getRotationShape(this.BasicShape);
        this.Shapes.add(r1);
        int[][] r2 = getRotationShape(r1);
        this.Shapes.add(r2);
        int[][] r3 = getRotationShape(r2);
        this.Shapes.add(r3);

        //反転
        int[][] r4 = getReversalShape(this.BasicShape);
        this.Shapes.add(r4);
        int[][] r5 = getReversalShape(r1);
        this.Shapes.add(r5);
        int[][] r6 = getReversalShape(r2);
        this.Shapes.add(r6);
        int[][] r7 = getReversalShape(r3);
        this.Shapes.add(r7);
    }
    
    /** このピースの形状を取得　向きを引数に取る*/
    public int[][] getPiecePattern(int direction){
        if(direction >= 0 && direction < 8){
            return this.Shapes.get(direction);
        }
        return null;
    }
    
    
    private static int[][] getReversalShape(int[][] sh){
        int width = sh[0].length;
        int hight = sh.length;
        
        //右回転
        int[][] r1 = new int[hight][width];
        for(int x=0;x<width;x++){
            for(int y=0;y<hight;y++){
                r1[y][x] = sh[y][width-x-1];
            }
        }
        return r1;
    }
    
    private static int[][] getRotationShape(int[][] sh){
        int width = sh[0].length;
        int hight = sh.length;
        
        //右回転
        int[][] r1 = new int[width][hight];
        for(int x=0;x<hight;x++){
            for(int y=0;y<width;y++){
                r1[y][x] = sh[hight - x - 1][y];
            }
        }
        return r1;
    }
    
    
    /** 確認用　文字列としてピースの形を取得する */
    private String getPrintedString(int direction){
        if(direction >= 0 && direction < 8){
            StringBuilder sbuf = new StringBuilder();
            int[][] sh = this.Shapes.get(direction);
            for(int x=0;x<sh.length;x++){
                for(int y=0;y<sh[x].length;y++){
                    if(sh[x][y] == 0){
                        sbuf.append("□");
                    } else if(sh[x][y] == 1){
                        sbuf.append("■");
                    } else {
                        sbuf.append("？");
                    }
                }
                sbuf.append("\n");
            }
            return sbuf.toString();
        }
        return null;
        
    }
    /** デバッグ用　このピースの形を出力する */
    public void printPieces(){
        for(int d=0;d<8;d++){
            System.out.println(this.getPrintedString(d));
            System.out.println(d);
            System.out.println();
        }
    }

    void printPieces(int d) {
        System.out.println(this.getPrintedString(d));
    }
    
}
