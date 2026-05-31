package io.crest.rsa.manage;

import io.crest.model.RSAModel;
import io.crest.rsa.dao.entity.CoreRsa;
import io.crest.rsa.dao.mapper.CoreRsaMapper;
import io.crest.utils.CommonBeanFactory;
import io.crest.utils.CryptoMode;
import io.crest.utils.SmCryptoUtils;
import io.crest.utils.RsaUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import static io.crest.constant.CacheConstant.CommonCacheConstant.RSA_CACHE;

@Component
public class RsaManage {

    @Resource
    private CoreRsaMapper coreRsaMapper;


    public void check() {
        RsaManage proxy = proxy();
        if (ObjectUtils.isEmpty(proxy.query())) {
            proxy.save();
        }
        if (CryptoMode.isSmSuite()) {
            SmCryptoUtils.validateConfiguration();
            proxy.ensureSmSuiteKey();
        }
    }

    @CacheEvict(value = RSA_CACHE, key = "'-de-'")
    public void save() {
        RSAModel model = RsaUtils.generate();
        CoreRsa coreRsa = new CoreRsa();
        coreRsa.setId(1);
        coreRsa.setCreateTime(System.currentTimeMillis());
        coreRsa.setPrivateKey(model.getPrivateKey());
        coreRsa.setPublicKey(model.getPublicKey());
        coreRsa.setAesKey(model.getAesKey());
        coreRsaMapper.insert(coreRsa);
    }

    @CacheEvict(value = RSA_CACHE, key = "'-de-'")
    public CoreRsa ensureSmSuiteKey() {
        CoreRsa coreRsa = coreRsaMapper.selectById(1);
        if (ObjectUtils.isEmpty(coreRsa)) {
            save();
            coreRsa = coreRsaMapper.selectById(1);
        }
        if (StringUtils.isBlank(coreRsa.getSm2PrivateKey()) || StringUtils.isBlank(coreRsa.getSm2PublicKey())) {
            SmCryptoUtils.Sm2KeyPair keyPair = SmCryptoUtils.generateSm2KeyPair();
            CoreRsa update = new CoreRsa();
            update.setId(coreRsa.getId());
            update.setSm2PrivateKey(SmCryptoUtils.encryptSm2PrivateKeyForStorage(keyPair.privateKey()));
            update.setSm2PublicKey(keyPair.publicKey());
            update.setSm2CreateTime(System.currentTimeMillis());
            coreRsaMapper.updateById(update);
            coreRsa = coreRsaMapper.selectById(1);
        }
        return coreRsa;
    }

    @Cacheable(value = RSA_CACHE, key = "'-de-'", unless = "#result == null")
    public CoreRsa query() {
        return coreRsaMapper.selectById(1);
    }

    private RsaManage proxy() {
        return CommonBeanFactory.getBean(RsaManage.class);
    }
}
