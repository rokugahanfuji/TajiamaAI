/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blokusElements;

import java.text.DecimalFormat;

/**
 *
 * @author koji
 */
public class TimerThread implements Runnable {
    
    private boolean runnable = true;
    
    private long counter[];
    private long startTime[];
    
    public TimerThread(){
        //Thread th = new Thread(this);
        this.init();
        //th.start();
    }

    public void init(){
        this.startTime = new long[2];
        this.counter = new long[2];
        this.counter[0] = 0;
        this.counter[1] = 0;
    }
    
    public void StartTimeCount(int PlayerID){
        this.startTime[PlayerID] = System.currentTimeMillis();
    }
    
    public long StopTimeCount(int PlayerID){
        this.counter[PlayerID] += System.currentTimeMillis() - this.startTime[PlayerID];
        return this.counter[PlayerID];
    }
    
    private static DecimalFormat ddec = new DecimalFormat("00");
    private static DecimalFormat qdec = new DecimalFormat("0000");
    public static String formatTimes(long millis){
        long sec = millis / 1000;
        long ms  = millis % 1000;
        int min = (int)(sec / 60);
        sec = sec % 60;
        int hour = min / 60;
        min = min % 60;
        
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(ddec.format(hour));
        sbuf.append(":");
        sbuf.append(ddec.format(min));
        sbuf.append(":");
        sbuf.append(ddec.format(sec));
        sbuf.append(".");
        sbuf.append(qdec.format(ms));
        return sbuf.toString();
    }
            
    
    public void run() {
//        this.init();
//        while(this.runnable){
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException ex) {
//                //
//            }
//        }
    }
    
}
