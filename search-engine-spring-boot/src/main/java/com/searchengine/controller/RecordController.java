package com.searchengine.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.searchengine.dto.RecordDto;
import com.searchengine.entity.Record;
import com.searchengine.service.RecordSegService;
import com.searchengine.service.RecordService;
import com.searchengine.service.SegmentationService;
import com.searchengine.utils.RedisUtil_db1;
import com.searchengine.utils.jieba.keyword.TFIDFAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@Slf4j
public class RecordController {

    @Autowired
    private RecordService recordService;

    @Autowired
    private SegmentationService segmentationService;

    @Autowired
    private RecordSegService recordSegService;

    @Autowired
    private RedisUtil_db1 redisUtilDb1;

    private final int pageSize = 15;


    TFIDFAnalyzer tfidfAnalyzer = new TFIDFAnalyzer();
    JiebaSegmenter segmenter = new JiebaSegmenter();

    /**
     * 查询所有文本
     *
     * @return
     */
    @GetMapping("/getAll")
    public List<Record> getAllRecord() {
        long start = System.currentTimeMillis();
        List<Record> records = recordService.queryAllRecord();
        long end = System.currentTimeMillis();
        System.out.println((end - start) + "ms");
        return records;
    }

//    /**
//     * 分词查询
//     * @param searchInfo
//     * @return
//     */
//    @GetMapping("/search")
//    public List<Record> search(@RequestParam("word") String searchInfo){
//        //调用jieba分词进行分词
//        log.info(searchInfo);
//        List<Record> recordList = new ArrayList<>();
//        List<SegToken> segTokens = segmenter.process(searchInfo, JiebaSegmenter.SegMode.INDEX);
//        for (SegToken token : segTokens) {
//            //查出每个分词对应的caption
//            log.info("分词为{}",token.word);
//            Segmentation oneSeg = segmentationDao.selectOneSeg(token.word);
//            if (oneSeg!=null) {
//                List<Long> RecordsIdList = recordSegService.queryRecordBySeg(oneSeg);
//                for (Long dataId : RecordsIdList) {
//                    recordList.add(recordDao.selectById(dataId));
//                }
//            }
//        }
//
//
//        return recordList;
//    }

    /**
     * redis 先查询redis,找不到再重定向到/search查询
     *
     * @param searchInfo
     * @return
     */
    @GetMapping("/search_redis")
    public List<RecordDto> search_redis(@RequestParam("word") String searchInfo, @RequestParam("pageNum") int pageNum, HttpServletRequest request,HttpServletResponse response) throws Exception {
        List<RecordDto> recordDtoList = null;
        if (redisUtilDb1.hasKey(searchInfo)) {
//            recordDtoList = (List<RecordDto>) redisUtilDb1.get(searchInfo);
            recordDtoList  = JSONObject.parseObject((String) redisUtilDb1.get(searchInfo), new TypeReference<List<RecordDto>>(){});
        } else {
            //不能用重发请求，太慢了;重定向的word参数总是传不过去
            //request.getRequestDispatcher("/search?word=" + searchInfo + "&pageNum=" + pageNum).forward(request,response);
            log.info(searchInfo);
            recordDtoList = recordService.search(searchInfo);
            /*自定义排序器根据weight排序*/
            recordDtoList.sort(new Comparator<RecordDto>() {
                @Override
                public int compare(RecordDto o1, RecordDto o2) {
                    if ((o2.getWeight() > o1.getWeight())) return 1;
                    else if ((o2.getWeight() < o1.getWeight())) return -1;
                    return 0;
                }
            });
            List<RecordDto> finalRecordDtoList = recordDtoList;
            Thread thread=new Thread(new Runnable() {
                @Override
                public void run() {
                    String sList = JSONObject.toJSONString(finalRecordDtoList);
                    redisUtilDb1.set(searchInfo, sList, 1800);
                }
            });
            thread.start();
        }
        int start = (pageNum - 1) * pageSize;
        int end = pageNum * pageSize;
        return recordDtoList.subList(start, end);
    }

    /**
     * 分词查询  根据tidif值从大到小排序
     *
     * @param searchInfo
     * @return
     */
    @GetMapping("/search")
    public List<RecordDto> search(@RequestParam("word") String searchInfo, @RequestParam("pageNum") int pageNum) {
        //调用jieba分词进行分词
        log.info(searchInfo);
        List<RecordDto> recordDtoList = recordService.search(searchInfo);


        /*//选择排序  忘了springboot里咋排序了 先凑合用
        for (int i = 0; i < recordDtoList.size()-1; i++) {
            int index = i;
            for (int j = i + 1; j < recordDtoList.size(); j++) {
                if(recordDtoList.get(index).getWeight() > recordDtoList.get(j).getWeight()){
                    index = j;
                }
            }
            RecordDto tmp = recordDtoList.get(i);
            recordDtoList.set(i,recordDtoList.get(index));
            recordDtoList.set(index,tmp);
        }
        Collections.reverse(recordDtoList);*/


        /*自定义排序器根据weight排序*/
        recordDtoList.sort(new Comparator<RecordDto>() {
            @Override
            public int compare(RecordDto o1, RecordDto o2) {
                if ((o2.getWeight() > o1.getWeight())) return 1;
                else if ((o2.getWeight() < o1.getWeight())) return -1;
                return 0;
            }
        });
        /*截取结果集recordDtoList*/
        int start = (pageNum - 1) * pageSize;
        int end = pageNum * pageSize;
        return recordDtoList.subList(start, end);
    }

    /**
     * 新增文本
     *
     * @param record
     * @return
     */
    @PostMapping("/add")
    public boolean add(Record record) {
        return recordService.addRecord(record);
    }

    /**
     * 模糊查询
     *
     * @param word
     * @return
     */
    @GetMapping("/s_word")
    public List<Record> getRecordByWord(@RequestParam("word") String word) {
        System.out.println(word);
        log.info(word);
        return recordService.queryRecordByWord(word);
    }


}
