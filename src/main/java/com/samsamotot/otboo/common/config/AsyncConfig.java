package com.samsamotot.otboo.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final String CONFIG = "[AsyncConfig] ";

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 스레드 풀 실행자를 생성하는 공통 메소드입니다.
     *
     * @param core 코어 스레드 수
     * @param max 최대 스레드 수
     * @param queue 큐 용량
     * @param keepAlive 유휴 스레드 유지 시간 (초)
     * @param prefix 스레드 이름 접두사
     * @return 구성된 ThreadPoolTaskExecutor
     * @throws IllegalArgumentException 잘못된 설정값이 제공된 경우
     */
    private ThreadPoolTaskExecutor buildExecutor(int core, int max, int queue, int keepAlive, String prefix) {

        if (core <= 0 || max <= 0 || queue < 0 || keepAlive < 0) {
            throw new IllegalArgumentException("ThreadPool 설정값은 양수여야 합니다.");
        }
        if (core > max) {
            throw new IllegalArgumentException("Core Pool Size는 Max Pool Size보다 클 수 없습니다.");
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setKeepAliveSeconds(keepAlive);
        executor.setThreadNamePrefix(prefix + "-");

        executor.setTaskDecorator(new SecurityContextTaskDecorator());

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }

    /**
     * 기본 비동기 실행자를 반환합니다.
     *
     * @return 메인 작업용 스레드 풀 실행자
     */
    @Override
    public Executor getAsyncExecutor() {
        return applicationContext.getBean("mainTaskExecutor", ThreadPoolTaskExecutor.class);
    }

    /**
     * 메인 작업을 위한 스레드 풀 실행자를 생성합니다.
     *
     * @param core 코어 스레드 수 (기본값: 2)
     * @param max 최대 스레드 수 (기본값: 4)
     * @param queue 큐 용량 (기본값: 100)
     * @param keepAlive 유휴 스레드 유지 시간 (기본값: 60초)
     * @return 메인 작업용 ThreadPoolTaskExecutor
     */
    @Bean(name = "mainTaskExecutor")
    public ThreadPoolTaskExecutor mainTaskExecutor(
            @Value("${async.executors.main.core-size:2}") int core,
            @Value("${async.executors.main.max-size:4}") int max,
            @Value("${async.executors.main.queue-capacity:100}") int queue,
            @Value("${async.executors.main.keep-alive-seconds:60}") int keepAlive
    ) {
        return buildExecutor(core, max, queue, keepAlive, "main-exec");
    }

    /**
     * 기상청 Open API 데이터 호출 처리를 위한 스레드 풀 실행자를 생성합니다.
     *
     * @param core 코어 스레드 수 (기본값: 2)
     * @param max 최대 스레드 수 (기본값: 4)
     * @param queue 큐 용량 (기본값: 100)
     * @param keepAlive 유휴 스레드 유지 시간 (기본값: 60초)
     * @return 기상청 Open API 데이터 호출 처리용 ThreadPoolTaskExecutor
     */
    @Bean(name = "weatherApiTaskExecutor")
    public ThreadPoolTaskExecutor weatherTaskExecutor(
            @Value("${async.executors.weather.core-size:2}") int core,
            @Value("${async.executors.weather.max-size:4}") int max,
            @Value("${async.executors.weather.queue-capacity:100}") int queue,
            @Value("${async.executors.weather.keep-alive-seconds:60}") int keepAlive
    ) {
        return buildExecutor(core, max, queue, keepAlive, "weather-exec");
    }

    /**
     * 이미지 다운로드 및 S3 업로드를 위한 스레드 풀 실행자를 생성합니다.
     *
     * <p>외부 이미지 서버로부터 다운로드를 수행하는 동안 네트워크 I/O 지연이 발생할 수 있으므로,
     * 메인 스레드 풀과 별도로 구성합니다.</p>
     *
     * @param core 코어 스레드 수 (기본값: 3)
     * @param max 최대 스레드 수 (기본값: 10)
     * @param queue 큐 용량 (기본값: 100)
     * @param keepAlive 유휴 스레드 유지 시간 (기본값: 60초)
     * @return 이미지 다운로드 전용 ThreadPoolTaskExecutor
     */
    @Bean(name = "imageTaskExecutor")
    public ThreadPoolTaskExecutor imageTaskExecutor(
        @Value("${async.executors.image.core-size:3}") int core,
        @Value("${async.executors.image.max-size:10}") int max,
        @Value("${async.executors.image.queue-capacity:100}") int queue,
        @Value("${async.executors.image.keep-alive-seconds:60}") int keepAlive
    ) {
        return buildExecutor(core, max, queue, keepAlive, "image-exec");
    }

    /**
     * 비동기 작업에서 발생한 예외를 처리하는 핸들러를 반환합니다.
     *
     * @return 로깅 기반 예외 처리 핸들러
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncUncaughtExceptionHandler();
    }

    /**
     * 비동기 작업에서 발생한 예외를 로깅하는 내부 클래스입니다.
     */
    private static class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

        /**
         * 처리되지 않은 비동기 예외를 로깅합니다.
         *
         * @param ex 발생한 예외
         * @param method 예외가 발생한 메소드
         * @param params 메소드 호출 시 전달된 매개변수
         */
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {

            Logger logger = LoggerFactory.getLogger(method.getDeclaringClass());
            String methodName = method.getName();

            logger.error(CONFIG + "method={}, exType={}, message={}", methodName, ex.getClass().getName(), ex.getMessage(), ex);

            if (params != null && params.length > 0) {
                try {
                    logger.error(CONFIG + "params={}", Arrays.toString(params));
                } catch (Exception ignore) {
                    // 일반적으로 로깅 중 발생한 예외는 무시하여 원본 오류 유지
                }
            }
        }
    }
    /**
     * SecurityContext를 비동기 스레드에 전파하는 TaskDecorator입니다.
     *
     * <p>비동기 작업 실행 시 부모 스레드의 SecurityContext를 자식 스레드로 복사하여,
     * 인증 정보가 유지되도록 합니다.</p>
     */
    private static class SecurityContextTaskDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {
            // 현재 스레드(부모)의 SecurityContext 가져오기
            SecurityContext context = SecurityContextHolder.getContext();

            return () -> {
                try {
                    // 새 스레드(자식)에 SecurityContext 설정
                    SecurityContextHolder.setContext(context);
                    runnable.run();
                } finally {
                    // 작업 완료 후 SecurityContext 정리
                    SecurityContextHolder.clearContext();
                }
            };
        }
    }
}
