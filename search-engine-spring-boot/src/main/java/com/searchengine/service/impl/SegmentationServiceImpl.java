package com.searchengine.service.impl;

import com.searchengine.dao.RecordSegDao;
import com.searchengine.dao.SegmentationDao;
import com.searchengine.entity.RecordSeg;
import com.searchengine.entity.Segmentation;
import com.searchengine.service.SegmentationService;
import com.searchengine.utils.jieba.keyword.Keyword;
import com.searchengine.utils.jieba.keyword.TFIDFAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SegmentationServiceImpl implements SegmentationService {

    @Autowired
    private SegmentationDao segmentationDao;

    @Autowired
    private RecordSegDao recordSegDao;

    TFIDFAnalyzer tfidfAnalyzer=new TFIDFAnalyzer();

    @Override
    public List<Segmentation> queryAllSeg() {
        return segmentationDao.selectAllSeg();
    }

    @Override
    public Boolean addSeg(String word,Long dataId,Double tidifValue) {

//        Segmentation segmentation = new Segmentation();
        Segmentation seg = segmentationDao.selectOneSeg(word);
        if (seg == null){
            //分词不存在 加入分词表
            segmentationDao.insertSeg(word);
        }
        Long segId = segmentationDao.selectOneSeg(word).getId();

        //加入关系表
        RecordSeg recordSeg = new RecordSeg();
        recordSeg.setSegId(segId);
        recordSeg.setDataId(dataId);
        recordSeg.setTidifValue(tidifValue);
        RecordSeg rs = recordSegDao.selectOneRecordSeg(dataId, segId);
        if (rs==null) {
            recordSeg.setCount(1);
            recordSegDao.insertRecordSeg(recordSeg);
        }
        else {
            int count = rs.getCount();
            //文中出现次数>1
            recordSeg.setCount(++count);
            recordSegDao.updateRecordSeg(recordSeg);
        }

        return true;
    }


}
