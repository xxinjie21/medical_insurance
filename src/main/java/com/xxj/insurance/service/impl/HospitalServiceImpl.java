package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.HospitalDTO;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.mapper.HospitalMapper;
import com.xxj.insurance.mapper.UserMapper;
import com.xxj.insurance.service.IHospitalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxj.insurance.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 医院表 服务实现类
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
@Service
@RequiredArgsConstructor
public class HospitalServiceImpl extends ServiceImpl<HospitalMapper, Hospital> implements IHospitalService {

    private final IUserService userService;

    @Override
    public Result sign(HospitalDTO hospitalDTO) {
        // 1. 获取名称
        String name = hospitalDTO.getName();

        // 2. 新建医院对象
        Hospital hospital = new Hospital();
        hospital.setName(name);

        // 3. 保存（MyBatis-Plus 保存后自动回写 ID 到对象里）
        save(hospital);

        // 4. 直接获取保存后的 ID
        Long hospitalId = hospital.getId();

        // 5. 返回 ID 给前端
        return Result.ok(hospitalId);
    }


    @Override
    public Result listMyPatient(Long hospitalId, PageDTO pageDTO) {
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }
        
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }
        
        Page<User> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, 1)
                .eq(User::getHospitalId, hospitalId);

        Page<User> userPage = userService.page(page, wrapper);

        return Result.ok(userPage);
    }


}
