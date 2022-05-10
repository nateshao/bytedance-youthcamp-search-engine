package com.searchengine.dao;

import com.searchengine.entity.Record;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordDao {

    List<Record> selectAllRecords();

    List<Record> selectRecordsByWord(String word);

    List<Record> selectRecordsFilter(String word);

    Record selectById(@Param("id") Long id);

    int insertRecord(Record record);
}
