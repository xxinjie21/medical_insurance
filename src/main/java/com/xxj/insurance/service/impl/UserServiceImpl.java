package com.xxj.insurance.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.utils.JwtUtil;
import com.xxj.insurance.domain.dto.UserLoginDTO;
import com.xxj.insurance.domain.dto.UserRegisterDTO;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.vo.UserLoginVO;
import com.xxj.insurance.domain.vo.UserRegisterVO;
import com.xxj.insurance.mapper.UserMapper;
import com.xxj.insurance.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final StringRedisTemplate redisTemplate;

    private final JwtUtil jwtUtil;

    @Override
    public Result login(UserLoginDTO userLoginDTO) {

        Long userId = userLoginDTO.getUserId();
        String password = userLoginDTO.getPassword();
        Integer role = userLoginDTO.getRole();

        // 1. 校验
        if (userId == null || StrUtil.isBlank(password) || role == null) {
            return Result.fail("用户ID、密码、角色不能为空");
        }

        // 2. 查询用户
        User user = getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 3. 校验角色
        if (!user.getRole().equals(role)) {
            return Result.fail("角色不匹配");
        }

        // 4. 校验密码（MD5 加密后比较）
        String md5Password = DigestUtil.md5Hex(password);
        if (!user.getPassword().equals(md5Password)) {
            return Result.fail("密码错误");
        }

        // 5. 生成 token（包含 userId）
        String token = jwtUtil.createToken(userId);

        // 6. 存入 Redis - 使用 token 作为 key，userId 作为 value
        redisTemplate.opsForValue().set("login:token:"+token, userId.toString(), 2, TimeUnit.HOURS);
        
        // 7. 保存用户角色到 Redis（用于权限校验）
        redisTemplate.opsForValue().set("login:role:"+userId, user.getRole().toString(), 2, TimeUnit.HOURS);

        // 8. 返回
        UserLoginVO vo = new UserLoginVO();
        BeanUtils.copyProperties(user, vo);
        vo.setToken(token);
        vo.setUserId(user.getId());

        return Result.ok(vo);
    }

    @Override
    public Result sign(UserRegisterDTO userRegisterDTO) {
        String password = userRegisterDTO.getPassword();
        String name = userRegisterDTO.getName();
        String idCard = userRegisterDTO.getIdCard();
        Long hospitalId = userRegisterDTO.getHospitalId();
        Integer role = userRegisterDTO.getRole();

        // 基础校验
        if (StrUtil.isBlank(name) || StrUtil.isBlank(password) || role == null) {
            return Result.fail("姓名、密码、角色不能为空");
        }
        if (StrUtil.isBlank(idCard) && hospitalId == null) {
            return Result.fail("患者必须传身份证，医院必须传医院ID");
        }

        // 构建用户
        User user = new User();
        user.setPassword(DigestUtil.md5Hex(password)); // MD5 加密
        user.setName(name);
        user.setRole(role);
        user.setCreateTime(LocalDateTime.now());

        if (StrUtil.isNotBlank(idCard)) {
            user.setIdCard(idCard);
        }
        if (hospitalId != null) {
            user.setHospitalId(hospitalId);
        }

        // 保存
        save(user);

        // 返回VO
        UserRegisterVO vo = new UserRegisterVO();
        BeanUtils.copyProperties(user, vo);
        return Result.ok(vo);
    }

    @Override
    public Result loginout(String token) {
        // 删除 Redis 里的 token
        redisTemplate.delete("login:token:" + token);
        return Result.ok("登出成功");
    }
}