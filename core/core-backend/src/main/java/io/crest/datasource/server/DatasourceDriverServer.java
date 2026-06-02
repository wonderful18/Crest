package io.crest.datasource.server;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.crest.api.ds.DatasourceDriverApi;
import io.crest.api.ds.vo.DriveDTO;
import io.crest.api.ds.vo.DriveJarDTO;
import io.crest.datasource.dao.auto.entity.CoreDriver;
import io.crest.datasource.dao.auto.entity.CoreDriverJar;
import io.crest.datasource.dao.auto.mapper.CoreDriverJarMapper;
import io.crest.datasource.dao.auto.mapper.CoreDriverMapper;
import io.crest.extensions.datasource.dto.DatasourceDTO;
import io.crest.utils.BeanUtils;
import io.crest.utils.FileUtils;
import io.crest.utils.Md5Utils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
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
            datasourceDrive.setTypeDesc(""); //TODO 设置数据源类型desc
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
    public DriveDTO save(DriveDTO datasourceDrive) {
        CoreDriver coreDriver = new CoreDriver();
        BeanUtils.copyBean(coreDriver, datasourceDrive);
        coreDriverMapper.insert(coreDriver);
        return datasourceDrive;
    }

    @Override
    public DriveDTO update(DriveDTO datasourceDrive) {
        CoreDriver coreDriver = new CoreDriver();
        BeanUtils.copyBean(coreDriver, datasourceDrive);
        coreDriverMapper.updateById(coreDriver);
        return datasourceDrive;
    }

    @Override
    public void delete(String driverId) {
        String normalizedDriverId = FileUtils.requireNumericId(driverId, "驱动ID");
        coreDriverMapper.deleteById(normalizedDriverId);
        Map<String, Object> map = new HashMap<>();
        map.put("driverId", normalizedDriverId);
        coreDriverJarMapper.deleteByMap(map);
    }


    @Override
    public List<DriveJarDTO> listDriverJar(String driverId) {
        String normalizedDriverId = FileUtils.requireNumericId(driverId, "驱动ID");
        List<DriveJarDTO> driveJarDTOS = new ArrayList<>();
        QueryWrapper<CoreDriverJar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("driverId", normalizedDriverId);
        coreDriverJarMapper.selectList(queryWrapper).forEach(coreDriverJar -> {
            DriveJarDTO driveJarDTO = new DriveJarDTO();
            BeanUtils.copyBean(driveJarDTO, coreDriverJar);
            driveJarDTOS.add(driveJarDTO);
        });
        return driveJarDTOS;
    }

    @Override
    public void deleteDriverJar(String jarId) {
        String normalizedJarId = FileUtils.requireNumericId(jarId, "驱动文件ID");
        CoreDriverJar driverJar = coreDriverJarMapper.selectById(normalizedJarId);
        coreDriverJarMapper.deleteById(normalizedJarId);
        CoreDriver driver = coreDriverMapper.selectById(driverJar.getDriverId());
        FileUtils.validateUploadFilename(driverJar.getTransName());
        String normalizedDriverId = FileUtils.requireNumericId(String.valueOf(driverJar.getDriverId()), "驱动ID");
        try {
            File driverDirectory = FileUtils.ensureChildDirectory(DRIVER_PATH, normalizedDriverId);
            FileUtils.deleteFile(FileUtils.resolveUnderDirectory(driverDirectory, driverJar.getTransName()).getPath());
        } catch (Exception e) {
            throw new RuntimeException("DRIVER_JAR_DELETE_FAILED", e);
        }
        //TODO 更新classloader
    }

    @Override
    public DriveJarDTO uploadJar(@RequestParam("driverId") String driverId, @RequestParam("jarFile") MultipartFile jarFile) throws Exception {
        String normalizedDriverId = FileUtils.requireNumericId(driverId, "驱动ID");
        CoreDriver coreDriver = coreDriverMapper.selectById(normalizedDriverId);
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

        String transName = Md5Utils.md5(filename) + ".jar";
        saveJarFile(jarFile, normalizedDriverId, transName);

        CoreDriverJar coreDriverJar = new CoreDriverJar();
        coreDriverJar.setDriverId(normalizedDriverId);
        coreDriverJar.setVersion("");
        coreDriverJar.setFileName(filename);
        coreDriverJar.setDriverClass(String.join(",", new ArrayList<>()));
        coreDriverJar.setIsTransName(true);
        coreDriverJar.setTransName(transName);
        coreDriverJarMapper.insert(coreDriverJar);
        //TODO 并更新classloader

        DriveJarDTO driveJarDTO = new DriveJarDTO();
        BeanUtils.copyBean(driveJarDTO, coreDriverJar);
        return driveJarDTO;
    }

    private String saveJarFile(MultipartFile file, String driverId, String transName) throws Exception {
        File driverDirectory = FileUtils.ensureChildDirectory(DRIVER_PATH, driverId);
        File f = FileUtils.resolveUnderDirectory(driverDirectory, transName);
        try (FileOutputStream fileOutputStream = new FileOutputStream(f)) {
            fileOutputStream.write(file.getBytes());
            fileOutputStream.flush();
        }
        return f.getPath();
    }
}
