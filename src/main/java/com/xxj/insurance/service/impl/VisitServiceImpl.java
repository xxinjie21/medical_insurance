package com.xxj.insurance.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.constants.RedisConstants;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.dto.VisitAddDTO;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.domain.vo.VisitVO;
import com.xxj.insurance.mapper.VisitMapper;
import com.xxj.insurance.service.IHospitalService;
import com.xxj.insurance.service.IUserService;
import com.xxj.insurance.service.IVisitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 就诊表 服务实现类
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
@Service
@RequiredArgsConstructor
public class VisitServiceImpl extends ServiceImpl<VisitMapper, Visit> implements IVisitService {


    private final IHospitalService hospitalService;
    private final IUserService userService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 新增就诊记录
     * 校验医院和患者信息后保存就诊记录，并清除相关缓存
     */
    @Override
    public Result add(VisitAddDTO dto) {
        if (dto == null) {
            return Result.fail("就诊信息不能为空");
        }
        if (dto.getUserId() == null) {
            return Result.fail("患者 ID 不能为空");
        }
        if (dto.getHospitalId() == null) {
            return Result.fail("医院 ID 不能为空");
        }
        if (dto.getType() == null || (dto.getType() != 1 && dto.getType() != 2)) {
            return Result.fail("就诊类型无效");
        }
        if (StrUtil.isBlank(dto.getDiagnosis())) {
            return Result.fail("诊断结果不能为空");
        }

        Hospital hospital = hospitalService.getById(dto.getHospitalId());
        if (hospital == null) {
            return Result.fail("医院不存在");
        }

        User user = userService.getById(dto.getUserId());
        if (user == null) {
            return Result.fail("患者不存在");
        }

        Visit visit = new Visit();
        BeanUtils.copyProperties(dto, visit);
        visit.setStatus(0);
        save(visit);

        VisitVO visitVO = new VisitVO();
        BeanUtils.copyProperties(visit, visitVO);
        visitVO.setHospitalName(hospital.getName());
        visitVO.setUserName(user.getName());

        String cacheKey = RedisConstants.CACHE_VISIT_KEY + visit.getId();
        redisTemplate.delete(cacheKey);

        return Result.ok(visitVO);
    }

    /**
     * 患者查询个人就诊记录列表（分页）
     * 仅返回当前登录患者的就诊记录
     */
    @Override
    public Result myList(PageDTO pageDTO) {
        Page<Visit> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());

        Long userId = UserHolder.getUserId();
        LambdaQueryWrapper<Visit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Visit::getUserId, userId)
                .orderByDesc(Visit::getCreateTime);

        Page<Visit> visitPage = page(page, wrapper);

        return Result.ok(visitPage);
    }

    /**
     * 医院查询本院就诊记录列表（分页）
     */
    @Override
    public Result hospitalList(Long hospitalId, PageDTO pageDTO) {
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }
        
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }
        
        Page<Visit> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());

        LambdaQueryWrapper<Visit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Visit::getHospitalId, hospitalId)
                .orderByDesc(Visit::getCreateTime);
        Page<Visit> visitPage = page(page, wrapper);
        return Result.ok(visitPage);
    }

    /**
     * 根据 ID 查询就诊记录
     */
    @Override
    public Result getVisitById(Long visitId) {
        if (visitId == null) {
            return Result.fail("就诊 ID 不能为空");
        }
        
        Visit visit = getWithCache(visitId);
        if (visit == null) {
            return Result.fail("就诊记录不存在");
        }
        return Result.ok(visit);
    }

    /**
     * 根据 ID 查询就诊记录（带缓存）
     * 优先从 Redis 缓存读取，缓存未命中时查询数据库并回写缓存
     */
    public Visit getWithCache(Long visitId) {
        String cacheKey = RedisConstants.CACHE_VISIT_KEY + visitId;
        
        String cacheJson = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cacheJson)) {
            return JSON.parseObject(cacheJson, Visit.class);
        }
        
        Visit visit = getById(visitId);
        
        if (visit != null) {
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(visit), 
                RedisConstants.CACHE_VISIT_TTL, TimeUnit.MINUTES);
        }
        
        return visit;
    }
}
