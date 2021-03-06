package xyz.fz.fire.fight.controller;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import xyz.fz.fire.fight.model.Task;
import xyz.fz.fire.fight.model.TaskHelper;
import xyz.fz.fire.fight.util.BaseUtil;
import xyz.fz.fire.fight.util.SSHUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static xyz.fz.fire.fight.util.SSHUtil.*;

@Controller
public class TaskController implements InitializingBean {

    private static String PASS_CODE = "";

    private static String FINAL_ANSWER = "";

    private static final String DATE_FORMAT = "yyyyMMdd";

    private static final String SUCCESS = "success";

    private static final String BLANK_COUNT = "blankCount";

    public static final String MISSION_COMPLETE = "任务执行完成";

    private static final String CONSOLE_OVER = "CONSOLE_OVER";

    @Value("${task.passCode}")
    private String taskPassCode;

    @Value("${task.finalAnswer}")
    private String taskFinalAnswer;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static Logger logger = LoggerFactory.getLogger(TaskController.class);

    @RequestMapping("/list")
    @ResponseBody
    public List<String> list(@RequestParam(required = false, defaultValue = "") String passCode) {
        String todayPassCode = passCode();
        if (StringUtils.equals(todayPassCode, passCode)) {
            return TaskHelper.taskList();
        } else {
            return null;
        }
    }

    @RequestMapping("/execute")
    @ResponseBody
    public Map<String, Object> execute(@RequestParam(required = false, defaultValue = "") String cmd) {

        Map<String, Object> result = Maps.newHashMap();
        String todayPassCode = passCode();
        if (StringUtils.startsWith(cmd, todayPassCode)) {
            if (cache.getIfPresent(RUNNING) == null) {
                cache.put(RUNNING, true);
                String finalCmd = cmd.replace(todayPassCode, "");
                Task.ShellTask shellTask = TaskHelper.shellTask(finalCmd);
                if (shellTask != null) {
                    executorService.execute(() -> {
                        try {
                            SSHUtil.execute(shellTask);
                        } catch (Exception e) {
                            logger.error(BaseUtil.getExceptionStackTrace(e));
                            e.printStackTrace();
                        } finally {
                            cache.invalidate(RUNNING);
                        }
                    });
                    result.put(SUCCESS, true);
                } else {
                    cache.invalidate(RUNNING);
                    result.put(SUCCESS, false);
                }
            } else {
                result.put(SUCCESS, false);
            }
        } else {
            result.put(SUCCESS, false);
        }
        return result;
    }

    @RequestMapping("/console")
    @ResponseBody
    public String console(@RequestParam(required = false, defaultValue = "") String passCode) {

        if (!StringUtils.startsWith(passCode, passCode())) {
            return "";
        }

        Object console = cache.getIfPresent(CONSOLE);
        if (console != null) {
            cache.invalidate(CONSOLE);
            cache.invalidate(BLANK_COUNT);
            StringBuilder consoleBuilder = (StringBuilder) console;
            return consoleBuilder.toString();
        } else {
            Object blankCountObject = cache.getIfPresent(BLANK_COUNT);
            Integer blankCount;
            if (blankCountObject == null) {
                blankCount = 0;
                cache.put(BLANK_COUNT, blankCount);
            } else {
                blankCount = (Integer) blankCountObject;
                blankCount = blankCount + 1;
                cache.put(BLANK_COUNT, blankCount);
            }
            if (blankCount > 5) {
                return CONSOLE_OVER;
            } else {
                return "";
            }
        }
    }

    private String passCode() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return PASS_CODE + sdf.format(new Date()) + FINAL_ANSWER;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        PASS_CODE = taskPassCode;
        FINAL_ANSWER = taskFinalAnswer;
    }
}
