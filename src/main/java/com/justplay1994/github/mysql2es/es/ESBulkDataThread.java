package com.justplay1994.github.mysql2es.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justplay1994.github.mysql2es.database.DatabaseNodeListInfo;
import com.justplay1994.github.mysql2es.http.client.urlConnection.MyURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * Created by JustPlay1994 on 2018/4/3.
 * https://github.com/JustPlay1994/daily-log-manager
 */

/**
 * 批量插入数据的线程实例
 */
public class ESBulkDataThread implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(ESBulkDataThread.class);

//    static int threadCount = 0;
    String ESUrl;   /*ES的URL*/

    static int nowRowNumber = 0; /*已导入数据总量（总行数）*/
    static int nowFailedRowNumber = 0; /*已导入失败的数据量（失败数据的总行数）*/
    private int blockRowNumber = 0;/*当前数据块大小（本次导入数据块中数据的行数）*/

    /**
     * 请求相关参数
     */
    String url;        /*请求的url*/
    String type;    /*请求的类型*/
    String json;    /*请求的数据体*/
    String result;/*请求返回数据包*/

    public ESBulkDataThread(String ESUrl, String json, int blockRowNumber){
        this.ESUrl = ESUrl;
        this.json = json;
        this.blockRowNumber = blockRowNumber;
    }

    public void run() {
        try {
            /*开始导入数据，当前工作线程数量打印*/
//            logger.info("input begin! Thread count = " + threadCount);

            url = ESUrl + "_bulk";
            type = "POST";/*必须大写*/
            result = new MyURLConnection().request(url,type,json);

            logger.debug(getRequestFullData());

            /*201是成功插入，209是失败，*/
            ObjectMapper objectMapper = new ObjectMapper();
            Map map =objectMapper.readValue(result.getBytes(),Map.class);
            if("true".equals(map.get("errors").toString())){
                logger.error("insert error:");
                logger.error(getRequestFullData());
                addNowFailedRowNumber(blockRowNumber);
            }else {
                addNowRowNumber(blockRowNumber);
            }
        } catch (MalformedURLException e) {
            addNowFailedRowNumber(blockRowNumber);
            logger.error("【BulkDataError1】", e);
            logger.error(getRequestFullData());
        } catch (ProtocolException e) {
            addNowFailedRowNumber(blockRowNumber);
            logger.error("【BulkDataError2】", e);
            logger.error(getRequestFullData());
        } catch (IOException e) {
            addNowFailedRowNumber(blockRowNumber);
            logger.error("【BulkDataError3】", e);
            logger.error(getRequestFullData());
        }finally {
//            changeThreadCount();/*同步操作，互斥锁*/
//            logger.info("Thread input end! Thread count = " + threadCount);
            printNowRowNumber();/*打印进度条*/
        }
    }

//    synchronized public static void changeThreadCount() {
//        threadCount --;
//    }

    /*打印进度条*/
    synchronized public void printNowRowNumber(){

        DecimalFormat df = new DecimalFormat("0.00");
        logger.info("has finished: " + df.format(((float) nowRowNumber / DatabaseNodeListInfo.rowNumber) * 100) + "% "+nowRowNumber+"/"+DatabaseNodeListInfo.rowNumber);
        logger.info("has error: " + df.format(((float) nowFailedRowNumber / DatabaseNodeListInfo.rowNumber) * 100) + "%");
    }

    /*获取完整请求信息，包括数据体*/
    public String getRequestFullData(){
        return "[request] url:"+url+",type:"+type+",body:"+json+"" +
                "\n[result]: "+result;
    }

    synchronized void addNowRowNumber(long addNumber){
        nowRowNumber+=addNumber;
    }

    synchronized void addNowFailedRowNumber(long addNumber){
        nowFailedRowNumber+=addNumber;
    }
}
