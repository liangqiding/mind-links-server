package com.mind.links.netty.mqtt.config;

import annotation.Desc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Date: 2020/6/06 0010 13:55
 * Description: BoundedElastic线程池配置
 *
 * @author qiding
 */
@Configuration
@Desc("自定义有边界的BoundedElastic线程池,reactor默认可通过配置文件加载")
public class ScheduleConfig {

    @Desc("系统线程数")
    private static final int CPU_NUM = Runtime.getRuntime().availableProcessors();

    @Desc("线程池大小（reactor 默认*10 ）,我这里因为启动服务较多,所有配置保守些")
    private static final Integer THREAD_CAP = CPU_NUM*3;

    @Desc("最大排队任务数 （reactor 默认 100000 ）")
    private final static Integer QUEUED_TASK_CAP = THREAD_CAP * 300;

    @Desc("线程名")
    private final static String NAME = "serverService-";

    @Bean
    public Scheduler myScheduler() {
        return Schedulers.newBoundedElastic(THREAD_CAP, QUEUED_TASK_CAP, NAME);
    }

}
