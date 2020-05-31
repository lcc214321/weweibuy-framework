package com.weweibuy.framework.compensate.mybatis.repository;

import com.weweibuy.framework.compensate.mybatis.mapper.CompensateMapper;
import com.weweibuy.framework.compensate.mybatis.mapper.CompensateMethodArgsExtMapper;
import com.weweibuy.framework.compensate.mybatis.po.Compensate;
import com.weweibuy.framework.compensate.mybatis.po.CompensateMethodArgsExt;
import com.weweibuy.framework.compensate.mybatis.po.CompensateMethodArgsExtExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * @author durenhao
 * @date 2020/4/25 11:26
 **/
@Repository
public class CompensateRepository {

    @Autowired
    private CompensateMapper compensateMapper;

    @Autowired
    private CompensateMethodArgsExtMapper compensateMethodArgsExtMapper;


    @Transactional(rollbackFor = Exception.class)
    public int batchInsert(List<Compensate> compensates) {
        compensates.forEach(compensateMapper::insertSelective);
        return compensates.size();
    }

    @Transactional(rollbackFor = Exception.class)
    public int insertCompensate(Compensate compensate, List<CompensateMethodArgsExt> collect) {
        int i1 = compensateMapper.insertSelective(compensate);
        for (int i = 0; i < collect.size(); i++) {
            CompensateMethodArgsExt compensateMethodArgsExt = collect.get(i);
            compensateMethodArgsExt.setCompensateId(compensate.getId());
            compensateMethodArgsExt.setArgsOrder(i);
            compensateMethodArgsExtMapper.insertSelective(compensateMethodArgsExt);
        }
        return i1;
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(Collection<Long> id, Compensate compensate) {
        id.stream()
                .peek(i -> compensate.setId(i))
                .forEach(i -> compensateMapper.updateByPrimaryKeySelective(compensate));
    }


    public List<CompensateMethodArgsExt> selectExtByCompensateId(List<Long> idList) {
        CompensateMethodArgsExtExample extExample = new CompensateMethodArgsExtExample();
        extExample.createCriteria().andCompensateIdIn(idList);
        return compensateMethodArgsExtMapper.selectByExample(extExample);
    }
}
