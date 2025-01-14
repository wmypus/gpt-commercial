package com.warape.aimechanician.impl;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warape.aimechanician.config.properties.MemberConfigProperties;
import com.warape.aimechanician.domain.Constants.ExchangeCardStateEnum;
import com.warape.aimechanician.entity.ExchangeCardDetail;
import com.warape.aimechanician.entity.MemberCard;
import com.warape.aimechanician.entity.UserInfo;
import com.warape.aimechanician.mapper.ExchangeCardDetailMapper;
import com.warape.aimechanician.service.ExchangeCardDetailService;
import com.warape.aimechanician.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 兑换卡详情 服务实现类
 * </p>
 *
 * @author warape
 * @since 2023-04-05 04:51:24
 */
@Service
public class ExchangeCardDetailServiceImpl extends ServiceImpl<ExchangeCardDetailMapper, ExchangeCardDetail> implements ExchangeCardDetailService {

  @Autowired
  @Lazy
  private UserInfoService userInfoService;
  @Autowired
  private MemberConfigProperties memberConfigProperties;

  @Override
  public void consume (Long userId, Integer consumeCount) {
    int consume = baseMapper.consume(userId, consumeCount);
    if (consume < 1) {
      throw new RuntimeException("消费卡失败 更新条数为0");
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void exchange (Long userId, MemberCard memberCard) {

    DateTime memberTime = DateUtil.offsetDay(new Date(), memberCard.getCardDay());
    Map<String, Integer> rights = memberConfigProperties.getRights();
    Integer limitCount = rights.getOrDefault(memberCard.getCardCode(), -1);
    ExchangeCardDetail entity = new ExchangeCardDetail();
    entity.setUserId(userId);
    entity.setMemberCardId(memberCard.getId());
    entity.setTotalCount(limitCount);
    entity.setSurplusCount(limitCount);
    entity.setExpiresTime(memberTime);
    entity.setExchangeState(ExchangeCardStateEnum.EXCHANGE.getState());

    if (!save(entity)) {
      throw new RuntimeException("保存兑换详情失败");
    }
    UserInfo userInfo = userInfoService.getById(userId);
    Date memberValidTime = userInfo.getMemberValidTime();
    if (memberValidTime == null) {
      userInfo.setMemberValidTime(memberTime);
    } else {
      userInfo.setMemberValidTime(DateUtil.offsetDay(memberValidTime, memberCard.getCardDay()));
    }
    userInfoService.updateById(userInfo);
  }

  @Override
  public List<ExchangeCardDetail> selectByUserId (Long userId) {
    LambdaQueryWrapper<ExchangeCardDetail> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(ExchangeCardDetail::getUserId, userId);
    queryWrapper.eq(ExchangeCardDetail::getExchangeState, ExchangeCardStateEnum.EXCHANGE.getState());

    return baseMapper.selectList(queryWrapper);
  }

  @Override
  public int getSurplusCount (Long userId) {
    return baseMapper.sumSurplusCount(userId);
  }

  @Override
  public List<ExchangeCardDetail> selectExpire () {
    Date now = new Date();
    LambdaQueryWrapper<ExchangeCardDetail> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.le(ExchangeCardDetail::getExpiresTime, now);
    queryWrapper.ge(ExchangeCardDetail::getExpiresTime, DateUtil.offsetDay(now, -1));
    queryWrapper.eq(ExchangeCardDetail::getExchangeState, ExchangeCardStateEnum.EXCHANGE.getState());
    return baseMapper.selectList(queryWrapper);

  }

  @Override
  public boolean checkGive (UserInfo userInfo, MemberCard memberCard) {
    LambdaQueryWrapper<ExchangeCardDetail> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.le(ExchangeCardDetail::getCreateTime, LocalDate.now());
    queryWrapper.ge(ExchangeCardDetail::getCreateTime, LocalDate.now());
    queryWrapper.eq(ExchangeCardDetail::getUserId, userInfo.getId());
    queryWrapper.eq(ExchangeCardDetail::getMemberCardId, memberCard.getId());
    return CollUtil.isNotEmpty(baseMapper.selectList(queryWrapper));
  }
}
