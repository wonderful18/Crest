package io.crest.datasource.server;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.crest.api.ds.DatasourceDriverApi;
import io.crest.api.ds.vo.DriveDTO;
import io.crest.api.ds.vo.DriveJarDTO;
import io.crest.datasource.dao.auto.entity.CoreDriver;
import io.crest.datasource.dao.auto.entity.CoreDriverJar;
import io.crest.datasource.dao.auto.mapper.CoreDriverJarMapper;
import io.crest.datasource.dao.auto.mapper.CoreDriverMapper;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasourceDTO;
import io.crest.utils.BeanUtils;
import io.crest.utils.CrestPermissionUtils;
import io.crest.utils.FileUtils;
import io.crest.utils.Md5Utils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Transactional(rollbackFor = Exception.class)
@RestController
@RequestMapping("/datasource-driver")
public class DatasourceDriverServer implements DatasourceDriverApi {

    @Value("${crest.path.custom-drivers:/opt/crest/custom-drivers/}")
    private String DRIVER_PATH;

    @Resource
    private CoreDriverMapper coreDriverMapper;

    @Resource
    private CoreDriverJarMapper coreDriverJarMapper;

    @Override
    public List<DatasourceDTO> query(String keyWord) {
        return null;
    }

    @Override
    public List<DriveDTO> list() {
        List<DriveDTO> driveDTOS = new ArrayList<>();
        List<CoreDriver> coreDrivers = coreDriverMapper.selectList(null);
        coreDrivers.forEach(coreDriver -> {
            DriveDTO datasourceDrive = new DriveDTO();
            BeanUtils.copyBean(datasourceDrive, coreDriver);
            datasourceDrive.setTypeDesc(""); // 驱动描述未在该接口中补充，保持空值由调用方兜底展示。
        });
        return driveDTOS;
    }

    @Override
    public List<DriveDTO> listByDsType(String dsType) {
        List<DriveDTO> driveDTOS = new ArrayList<>();
        QueryWrapper<CoreDriver> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", dsType);
        List<CoreDriver> coreDrivers = coreDriverMapper.selectList(queryWrapper);
        coreDrivers.forEach(coreDriver -> {
            DriveDTO datasourceDrive = new DriveDTO();
            BeanUtils.copyBean(datasourceDrive, coreDriver);
        });
        return driveDTOS;
    }

    @Override
    public DriveDTO save(@RequestBody DriveDTO datasourceDrive) {
        CrestPermissionUtils.requireAdmin();
        CoreDriver coreDriver = new CoreDriver();
        BeanUtils.copyBean(coreDriver, datasourceDrive);
        coreDriverMapper.insert(coreDriver);
        return datasourceDrive;
    }

    @Override
    public DriveDTO update(@RequestBody DriveDTO datasourceDrive) {
        CrestPermissionUtils.requireAdmin();
        CoreDriver coreDriver = new CoreDriver();
        BeanUtils.copyBean(coreDriver, datasourceDrive);
        coreDriverMapper.updateById(coreDriver);
        return datasourceDrive;
    }

    @Override
    public void delete(String driverId) {
        CrestPermissionUtils.requireAdmin();
        coreDriverMapper.deleteById(driverId);
        Map<String, Object> map = new HashMap<>();
        map.put("driverId", driverId);
        coreDriverJarMapper.deleteByMap(map);
    }


    @Override
    public List<DriveJarDTO> listDriverJar(String driverId) {
        List<DriveJarDTO> driveJarDTOS = new ArrayList<>();
        QueryWrapper<CoreDriverJar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("driverId", driverId);
        coreDriverJarMapper.selectList(queryWrapper).forEach(coreDriverJar -> {
            DriveJarDTO driveJarDTO = new DriveJarDTO();
            BeanUtils.copyBean(driveJarDTO, coreDriverJar);
            driveJarDTOS.add(driveJarDTO);
        });
        return driveJarDTOS;
    }

    @Override
    public void deleteDriverJar(String jarId) {
        CrestPermissionUtils.requireAdmin();
        CoreDriverJar driverJar = coreDriverJarMapper.selectById(jarId);
        coreDriverJarMapper.deleteById(jarId);
        try {
            FileUtils.deleteFile(resolveDriverJar(driverJar.getDriverId(), driverJar.getTransName()).getPath());
        } catch (IOException e) {
            CrestException.throwException("驱动文件路径无效");
        }
        // 删除驱动文件后，后续连接会重新按现有驱动文件加载。
    }

    @Override
    public DriveJarDTO uploadJar(@RequestParam("driverId") String driverId, @RequestParam("jarFile") MultipartFile jarFile) throws Exception {
        CrestPermissionUtils.requireAdmin();
        CoreDriver coreDriver = coreDriverMapper.selectById(driverId);
        if (coreDriver == null) {
            throw new RuntimeException("DRIVER_NOT_FOUND");
        }
        String filename = jarFile.getOriginalFilename();
        FileUtils.validateUploadFilename(filename);
        if (!filename.endsWith(".jar")) {
            throw new RuntimeException("NOT_JAR");
        }

        QueryWrapper<CoreDriverJar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("fileName", filename);
        if (!CollectionUtils.isEmpty(coreDriverJarMapper.selectList(queryWrapper))) {
            throw new Exception("A file with the same name already exists：" + filename);
        }

        File driverDirectory = FileUtils.ensureSubDirectory(DRIVER_PATH, driverId);
        String transName = Md5Utils.md5(filename) + ".jar";
        File jarFileTarget = FileUtils.resolveUnderDirectory(driverDirectory.getPath(), transName);
        saveJarFile(jarFile, jarFileTarget);

        CoreDriverJar coreDriverJar = new CoreDriverJar();
        coreDriverJar.setDriverId(driverId);
        coreDriverJar.setVersion("");
        coreDriverJar.setFileName(filename);
        coreDriverJar.setDriverClass(String.join(",", new ArrayList<>()));
        coreDriverJar.setIsTransName(true);
        coreDriverJar.setTransName(transName);
        coreDriverJarMapper.insert(coreDriverJar);
        // 上传后的驱动信息立即入库，后续连接按最新驱动文件加载。

        DriveJarDTO driveJarDTO = new DriveJarDTO();
        BeanUtils.copyBean(driveJarDTO, coreDriverJar);
        return driveJarDTO;
    }

    private File resolveDriverJar(String driverId, String transName) throws IOException {
        File driverDirectory = FileUtils.resolveSubDirectory(DRIVER_PATH, driverId);
        return FileUtils.resolveUnderDirectory(driverDirectory.getPath(), transName);
    }

    private void saveJarFile(MultipartFile file, File f) throws Exception {
        try (FileOutputStream fileOutputStream = new FileOutputStream(f)) {
            fileOutputStream.write(file.getBytes());
            fileOutputStream.flush();
        }
    }
}
